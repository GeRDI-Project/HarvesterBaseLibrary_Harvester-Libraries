/**
 * Copyright © 2017 Robin Weiss (http://www.gerdi-project.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.gerdiproject.harvest.etls.utils;


import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.core.MultivaluedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.parameters.BooleanParameter;
import de.gerdiproject.harvest.config.parameters.constants.ParameterConstants;
import de.gerdiproject.harvest.etls.AbstractETL;
import de.gerdiproject.harvest.etls.ETLPreconditionException;
import de.gerdiproject.harvest.etls.constants.ETLConstants;
import de.gerdiproject.harvest.etls.enums.ETLHealth;
import de.gerdiproject.harvest.etls.enums.ETLState;
import de.gerdiproject.harvest.etls.events.GetETLManagerEvent;
import de.gerdiproject.harvest.etls.events.GetRepositoryNameEvent;
import de.gerdiproject.harvest.etls.events.HarvestFinishedEvent;
import de.gerdiproject.harvest.etls.events.HarvestStartedEvent;
import de.gerdiproject.harvest.etls.json.ETLInfosJson;
import de.gerdiproject.harvest.etls.json.ETLJson;
import de.gerdiproject.harvest.etls.json.ETLManagerJson;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.rest.AbstractRestObject;
import de.gerdiproject.harvest.scheduler.events.GetSchedulerEvent;
import de.gerdiproject.harvest.utils.data.DiskIO;
import de.gerdiproject.harvest.utils.file.ICachedObject;


/**
 * This class serves as an interface for all {@linkplain AbstractETL}s that
 * are required to harvest the repository.
 *
 * @author Robin Weiss
 */
public class ETLManager extends AbstractRestObject<ETLManager, ETLManagerJson> implements ICachedObject
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ETLManager.class);

    // fields and members
    private final DiskIO diskIo;
    private final File cacheFile;
    private final List<AbstractETL<?, ?>> etls;
    private final BooleanParameter concurrentParam;
    private final BooleanParameter forceHarvestParameter;
    private final TimestampedList<ETLState> combinedStateHistory;
    private String lastHarvestHash;


    /**
     * Constructor.
     *
     * @param moduleName the name of the harvester service
     * @param cacheFolder the project's cache folder
     */
    public ETLManager(final String moduleName, final File cacheFolder)
    {
        super(moduleName, GetETLManagerEvent.class);
        this.combinedStateHistory = new TimestampedList<>(ETLState.INITIALIZING, 10);

        this.etls = new LinkedList<>();
        this.concurrentParam = Configuration.registerParameter(ETLConstants.CONCURRENT_PARAM);
        this.forceHarvestParameter = Configuration.registerParameter(ETLConstants.FORCED_PARAM);
        this.cacheFile = new File(cacheFolder, String.format(ETLConstants.ETL_MANAGER_CACHE_PATH, moduleName));
        this.diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);

        setStatus(ETLState.IDLE);
    }


    @Override
    public void loadFromDisk()
    {
        final ETLInfosJson loadedState = diskIo.getObject(cacheFile, ETLInfosJson.class);

        if (loadedState != null) {
            final ETLJson overallInfo = loadedState.getOverallInfo();
            final Map<String, ETLJson> etlInfos = loadedState.getEtlInfos();

            if (overallInfo == null || etlInfos == null)
                LOGGER.warn(ETLConstants.ETL_MANAGER_LOAD_ERROR);
            else {
                // load the overall hash
                this.lastHarvestHash = overallInfo.getVersionHash();

                // load status history
                this.combinedStateHistory.addAllSorted(overallInfo.getStateHistory());

                // load ETL states
                for (final AbstractETL<?, ?> etl : etls) {
                    final String etlName = etl.getName();
                    final ETLJson etlInfo = etlInfos.get(etlName);

                    if (etlInfo == null)
                        LOGGER.warn(String.format(ETLConstants.ETL_LOADING_FAILED, etlName));
                    else
                        etl.loadFromJson(etlInfo);
                }

                LOGGER.debug(String.format(ETLConstants.ETL_MANAGER_LOADED, cacheFile));
            }
        }
    }


    @Override
    public void saveToDisk()
    {
        diskIo.writeObjectToFile(cacheFile, getETLsAsJson());
    }


    @Override
    protected String getPrettyPlainText()
    {
        final StringBuilder sb = new StringBuilder();
        int totalCurrCount = 0;
        int totalMaxCount = 0;

        for (final AbstractETL<?, ?> etl : etls) {
            sb.append(etl.toString());

            if (etl.isEnabled()) {
                totalCurrCount += etl.getHarvestedCount();
                final int maxCount = etl.getMaxNumberOfDocuments();

                if (maxCount == -1 || totalMaxCount == -1)
                    totalMaxCount = -1;
                else
                    totalMaxCount += maxCount;
            }
        }

        final ETLState state = getState();
        final StringBuilder stateStringBuilder = new StringBuilder();
        stateStringBuilder.append(state.toString().toLowerCase(Locale.ENGLISH));

        if (state == ETLState.HARVESTING) {
            if (totalMaxCount == -1)
                stateStringBuilder.append(String.format(ETLConstants.PROGRESS_NO_BOUNDS, totalCurrCount));
            else
                stateStringBuilder.append(String.format(ETLConstants.PROGRESS, Math.round(100f * totalCurrCount / totalMaxCount), totalCurrCount, totalMaxCount));
        }

        sb.append(String.format(ETLConstants.ETL_PRETTY, ETLConstants.NAME_TOTAL, stateStringBuilder.toString(), EtlUtils.getCombinedHealth(etls)));

        if (state == ETLState.HARVESTING) {
            final long remainingMilliSeconds = EtlUtils.estimateRemainingHarvestTime(
                                                   combinedStateHistory.getLatestTimestamp(),
                                                   state,
                                                   totalCurrCount,
                                                   totalMaxCount);
            sb.append(EtlUtils.formatHarvestTime(remainingMilliSeconds));
        }

        return sb.toString();
    }


    @Override
    public ETLManagerJson getAsJson(final MultivaluedMap<String, String> query)
    {
        final String repositoryName = EventSystem.sendSynchronousEvent(new GetRepositoryNameEvent());
        final int harvestedCount = getHarvestedCount();
        final int maxDocumentCount = getMaxNumberOfDocuments();
        final long remainingHarvestTime = estimateRemainingHarvestTime();
        final long lastHarvestTimestamp = getLatestHarvestTimestamp();
        final Date nextHarvestDate = EventSystem.sendSynchronousEvent(new GetSchedulerEvent()).getNextHarvestDate();

        boolean hasEnabledETLs = false;

        for (final AbstractETL<?, ?> etl : etls) {
            if (etl.isEnabled()) {
                hasEnabledETLs = true;
                break;
            }
        }

        return new ETLManagerJson(
                   repositoryName,
                   getState(),
                   EtlUtils.getCombinedHealth(etls),
                   harvestedCount,
                   maxDocumentCount == -1 ? null : maxDocumentCount,
                   remainingHarvestTime == -1 ? null : remainingHarvestTime,
                   lastHarvestTimestamp == -1 ? null : new Date(lastHarvestTimestamp).toString(),
                   nextHarvestDate == null ? null : nextHarvestDate.toString(),
                   hasEnabledETLs
               );
    }


    /**
     * Returns an info JSON object with detailed information about all
     * ETLs.
     *
     * @return an info JSON object with detailed information about all
     * ETLs
     */
    public ETLInfosJson getETLsAsJson()
    {
        return new ETLInfosJson(
                   new ETLJson(
                       getClass().getSimpleName(),
                       combinedStateHistory,
                       new TimestampedList<>(EtlUtils.getCombinedHealth(etls), 1),
                       getHarvestedCount(),
                       getMaxNumberOfDocuments(),
                       EtlUtils.getCombinedHashes(etls)),
                   etls);
    }


    /**
     * Returns a specified ETL as JSON object.
     *
     * @param query query parameters that should contain 'name'
     *
     * @throws IllegalArgumentException if no name was specified or the
     * ETL with that name could not be found
     *
     * @return the ETL specified by the name-query-parameter
     */
    public ETLJson getETLAsJson(final MultivaluedMap<String, String> query) throws IllegalArgumentException
    {
        final List<String> nameList = query == null
                                      ? null
                                      : query.get(ETLConstants.ETL_NAME_QUERY);

        if (nameList == null || nameList.isEmpty())
            throw new IllegalArgumentException(ETLConstants.ETL_NAME_QUERY_ERROR_EMPTY);

        final String etlName = nameList.get(0);
        final Optional<AbstractETL<?, ?>> etl =
            etls.stream()
            .filter((final AbstractETL<?, ?>  e) -> e.getName().equalsIgnoreCase(etlName))
            .findFirst();

        if (!etl.isPresent())
            throw new IllegalArgumentException(String.format(ETLConstants.ETL_NAME_QUERY_ERROR_UNKNOWN, etlName));

        return etl.get().getAsJson();
    }


    /**
     * Registers an {@linkplain AbstractETL}, adding it to the list of ETLs
     * that can be controlled via REST. If the ETL name contains invalid characters,
     * they are removed. If another ETL was registered under the same name as
     * the added ETL, a number is appended to prevent name clashes.
     *
     * @param addedEtl the {@linkplain AbstractETL} to be registered
     */
    public void register(final AbstractETL<?, ?> addedEtl)
    {
        // abort if the ETL instance was already registered
        if (etls.contains(addedEtl))
            LOGGER.info(String.format(ETLConstants.DUPLICATE_ETL_REGISTERED_ERROR, addedEtl.getClass().getSimpleName()));
        else {
            // remove illegal characters from ETL name
            final String etlNameOrig = addedEtl.getName().replaceAll(ParameterConstants.INVALID_PARAM_NAME_REGEX, "");
            String etlName = etlNameOrig;
            int duplicateCount = 1;

            // find the first name for the ETL that is not already taken
            while (true) {
                final String etlNameTemp = etlName;

                if (etls.stream().noneMatch((final AbstractETL<?, ?> e) -> e.getName().equals(etlNameTemp)))
                    break;
                else
                    etlName = etlNameOrig + (++duplicateCount);
            }

            // change the ETL name to the clean one
            addedEtl.setName(etlName);

            // add the ETL to the list
            etls.add(addedEtl);
        }
    }


    /**
     * Updates all registered ETLs and checks if they are outdated.
     *
     * @return true if at least one ETL is outdated
     */
    public boolean hasOutdatedETLs()
    {
        // if a harvest is ongoing or just finished, do not update the ETLs
        final ETLState currentStatus = getState();

        if (currentStatus == ETLState.IDLE || currentStatus == ETLState.DONE)
            EtlUtils.processETLs(etls, (final AbstractETL<?, ?> harvester) -> harvester.update());

        final int maxDocs = getMaxNumberOfDocuments();
        final int currentDocs = getHarvestedCount();

        // it's outdated if not all documents have been harvested
        if (maxDocs == -1 && currentDocs == 0 || currentDocs < maxDocs)
            return true;

        final String currentHash = EtlUtils.getCombinedHashes(etls);
        return currentHash == null || !currentHash.equals(lastHarvestHash);
    }


    @Override
    public void addEventListeners()
    {
        super.addEventListeners();

        // event listeners must be added even if the ETL is disabled
        for (final AbstractETL<?, ?> etl : etls)
            etl.addEventListeners();
    }


    @Override
    public void removeEventListeners()
    {
        super.removeEventListeners();

        // event listeners must be removed even if the ETL is disabled
        for (final AbstractETL<?, ?> etl : etls)
            etl.removeEventListeners();
    }


    /**
     * Attempts to start a harvest, signaling all registered ETLs to start
     *
     * @throws IllegalStateException thrown if another ongoing harvest is blocking this process
     */
    public void harvest() throws IllegalStateException
    {
        if (getState() != ETLState.IDLE)
            throw new IllegalStateException(ETLConstants.BUSY_HARVESTING);


        // cancel harvest if the checksum has not changed since the last harvest
        if (!forceHarvestParameter.getValue() && !hasOutdatedETLs())
            throw new ETLPreconditionException(ETLConstants.ETL_SKIPPED_NO_CHANGES);

        // do it asynchronously, so we can immediately return
        CompletableFuture.runAsync(()-> {

            final boolean isPrepared = prepareETLsForHarvest();

            if (isPrepared)
                harvestETLs();
            else
                throw new ETLPreconditionException(ETLConstants.PREPARE_ETLS_FAILED);
        })
        .thenAccept((final Void v) -> {
            // save to disk only if we were successful
            this.lastHarvestHash = EtlUtils.getCombinedHashes(etls);

            saveToDisk();

            final ETLState status = getState();

            if (status == ETLState.ABORTING)
                LOGGER.info(ETLConstants.ABORT_FINISHED);

            else if (status == ETLState.HARVESTING)
                LOGGER.info(ETLConstants.HARVEST_FINISHED);

            EventSystem.sendEvent(new HarvestFinishedEvent(true, lastHarvestHash));
            setStatus(ETLState.IDLE);
        })
        .exceptionally((final Throwable reason) -> {
            // log stack trace only if it is an unexpected error
            if (reason.getCause() instanceof ETLPreconditionException)
                LOGGER.error(ETLConstants.PREPARE_ETLS_FAILED);
            else
                LOGGER.error(ETLConstants.ETLS_FAILED_UNKNOWN_ERROR, reason);

            // clean up all ETLs
            EtlUtils.processETLs(etls, (final AbstractETL<?, ?> harvester) -> harvester.cancelHarvest());
            saveToDisk();

            LOGGER.info(ETLConstants.HARVEST_FAILED);

            EventSystem.sendEvent(new HarvestFinishedEvent(false, EtlUtils.getCombinedHashes(etls)));
            setStatus(ETLState.IDLE);

            return null;
        });
    }


    /**
     * Sums up the expected number of harvestable documents of all registered ETLs.
     *
     * @return the expected total number of harvestable documents,
     * or -1 if the number cannot be calculated
     *
     */
    public int getMaxNumberOfDocuments()
    {
        final List<Integer> sizes = EtlUtils.processETLsAsList(etls, (final AbstractETL<?, ?> harvester) ->
                                                               harvester.getMaxNumberOfDocuments());
        int total = 0;

        for (final int size : sizes) {
            // of one harvester does not know its size, the total cannot be estimated
            if (size == -1)
                return -1;

            total += size;
        }

        return total;
    }


    /**
     * Attempts to abort an ongoing harvest.
     *
     * @throws IllegalStateException if no harvest is in progress
     */
    public void abortHarvest()
    {
        final ETLState currentStatus = getState();

        if (currentStatus == ETLState.QUEUED || currentStatus == ETLState.HARVESTING) {
            setStatus(ETLState.ABORTING);

            EtlUtils.processETLs(etls, (final AbstractETL<?, ?> harvester) -> {
                if (harvester.getState() == ETLState.HARVESTING || harvester.getState() == ETLState.QUEUED)
                    harvester.abortHarvest();
            });
        } else
            throw new IllegalStateException(String.format(ETLConstants.ABORT_INVALID_STATE, combinedStateHistory.toString()));
    }


    /**
     * Retrieves the number of documents that have been loaded.
     *
     * @return the number of documents that have been loaded
     */
    public int getHarvestedCount()
    {
        return EtlUtils.sumUpETLValues(etls,
                                       (final AbstractETL<?, ?> harvester) -> harvester.getHarvestedCount()
                                      );
    }


    /**
     * Retrieves an overall health status.
     *
     * @return a health status representing the combined health of all registered ETLs
     */
    public ETLHealth getHealth()
    {
        return EtlUtils.getCombinedHealth(etls);
    }


    /**
     * Returns a status representing the entirety of all registered ETL statuses.
     *
     * @return a status representing the entirety of all registered ETL statuses
     */
    public ETLState getState()
    {
        return combinedStateHistory.getLatestValue();
    }


    /**
     * Estimates the remaining harvesting duration in milliseconds.
     *
     * @return the remaining harvesting duration in milliseconds,
     * or -1 if it cannot be estimated
     */
    public long estimateRemainingHarvestTime()
    {
        return EtlUtils.estimateRemainingHarvestTime(
                   combinedStateHistory.getLatestTimestamp(),
                   combinedStateHistory.getLatestValue(),
                   getHarvestedCount(),
                   getMaxNumberOfDocuments());
    }


    /**
     * Returns the unix timestamp of the most recent beginning of a harvest.
     *
     * @return a unix timestamp or -1 if no harvest was executed
     */
    private long getLatestHarvestTimestamp()
    {
        final Iterator<TimestampedEntry<ETLState>> reverseIter = combinedStateHistory.descendingIterator();

        while (reverseIter.hasNext()) {
            final TimestampedEntry<ETLState> item = reverseIter.next();

            if (item.getValue() == ETLState.HARVESTING)
                return item.getTimestamp();
        }

        return -1;
    }


    /**
     * Prepares all registered ETLs for a subsequent harvest.
     *
     * @return true if all ETLs were sucessfully prepared
     */
    private boolean prepareETLsForHarvest()
    {
        LOGGER.info(ETLConstants.PREPARE_ETLS);
        setStatus(ETLState.QUEUED);

        // count the number of ETLs that were successfully prepared
        final int preparedCount = EtlUtils.sumUpETLValues(etls, (final AbstractETL<?, ?> etl) -> {
            try
            {
                // prepareHarvest() can take time, abort as early as possible
                if (getState() != ETLState.ABORTING) {
                    etl.prepareHarvest();
                    return 1;
                }
            } catch (final ETLPreconditionException e)
            {
                LOGGER.info(String.format(ETLConstants.ETL_INIT_FAILED, etl.getName()), e);
            }
            return 0;
        });

        if (preparedCount == 0 || getState() == ETLState.ABORTING) {
            setStatus(ETLState.IDLE);
            return false;
        }

        return true;
    }


    /**
     * Harvests prepared and queued ETLs either sequentially or
     * concurrently, depending on the value of the "concurrentHarvest" parameter.
     */
    private void harvestETLs()
    {
        LOGGER.info(ETLConstants.START_ETLS);
        setStatus(ETLState.HARVESTING);

        EventSystem.sendEvent(new HarvestStartedEvent(EtlUtils.getCombinedHashes(etls), getMaxNumberOfDocuments()));

        // check parameter to see if harvests must run sequentially or not
        if (concurrentParam.getValue()) {
            // run all harvests asynchronously
            final List<CompletableFuture<?>> asyncHarvests =
                EtlUtils.processETLsAsList(etls, (final AbstractETL<?, ?> etl) ->
            CompletableFuture.runAsync(() -> {
                if (getState() != ETLState.ABORTING && etl.getState() == ETLState.QUEUED)
                    etl.harvest();
            }));

            // wait for all async harvests to complete
            try {
                final CompletableFuture<?>[] asyncHarvestArray =
                    asyncHarvests.toArray(new CompletableFuture<?>[asyncHarvests.size()]); // NOPMD allOf() requires an Array
                CompletableFuture.allOf(asyncHarvestArray).get();

            } catch (InterruptedException | ExecutionException e) {
                LOGGER.error(ETLConstants.ETL_PROCESSING_ERROR, e);
            }
        } else {
            EtlUtils.processETLs(etls, (final AbstractETL<?, ?> etl) -> {
                if (getState() != ETLState.ABORTING && etl.getState() == ETLState.QUEUED)
                    etl.harvest();
            });
        }
    }


    /**
     * Changes the overall status of the registry.
     *
     * @param status the new status
     */
    private void setStatus(final ETLState status)
    {
        combinedStateHistory.addValue(status);
    }
}
