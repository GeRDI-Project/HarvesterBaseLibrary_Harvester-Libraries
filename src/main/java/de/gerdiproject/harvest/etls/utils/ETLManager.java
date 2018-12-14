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
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;

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
import de.gerdiproject.harvest.utils.HashGenerator;
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
     */
    public ETLManager(String moduleName)
    {
        super(moduleName, GetETLManagerEvent.class);
        this.combinedStateHistory = new TimestampedList<>(ETLState.INITIALIZING, 10);

        this.etls = new LinkedList<>();
        this.concurrentParam = Configuration.registerParameter(ETLConstants.CONCURRENT_PARAM);
        this.forceHarvestParameter = Configuration.registerParameter(ETLConstants.FORCED_PARAM);
        this.cacheFile = new File(String.format(ETLConstants.ETL_MANAGER_CACHE_PATH, moduleName));
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

            if (overallInfo != null && etlInfos != null) {
                // load the overall hash
                this.lastHarvestHash = overallInfo.getVersionHash();

                // load status history
                this.combinedStateHistory.addAllSorted(overallInfo.getStateHistory());

                // load ETL states
                for (AbstractETL<?, ?> etl : etls) {
                    final String etlName = etl.getName();
                    final ETLJson etlInfo = etlInfos.get(etlName);

                    if (etlInfo != null)
                        etl.loadFromJson(etlInfo);
                    else
                        LOGGER.warn(String.format(ETLConstants.ETL_LOADING_FAILED, etlName));
                }

                LOGGER.debug(String.format(ETLConstants.ETL_MANAGER_LOADED, cacheFile));
            } else
                LOGGER.warn(ETLConstants.ETL_MANAGER_LOAD_ERROR);
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

        for (AbstractETL<?, ?> etl : etls) {
            sb.append(etl.toString());

            if (etl.isEnabled()) {
                totalCurrCount += etl.getHarvestedCount();
                final int maxCount = etl.getMaxNumberOfDocuments();

                if (maxCount != -1 && totalMaxCount != -1)
                    totalMaxCount += maxCount;
                else
                    totalMaxCount = -1;
            }
        }

        final ETLState state = getState();
        String stateString = state.toString().toLowerCase();

        if (state == ETLState.HARVESTING) {
            if (totalMaxCount != -1)
                stateString += String.format(ETLConstants.PROGRESS, Math.round(100f * totalCurrCount / totalMaxCount), totalCurrCount, totalMaxCount);
            else
                stateString += String.format(ETLConstants.PROGRESS_NO_BOUNDS, totalCurrCount);
        }

        sb.append(String.format(ETLConstants.ETL_PRETTY, ETLConstants.NAME_TOTAL, stateString, getHealth()));

        if (state == ETLState.HARVESTING) {
            final long remainingMilliSeconds = estimateRemainingHarvestTime(totalCurrCount, totalMaxCount);
            sb.append(getDurationText(remainingMilliSeconds));
        }

        return sb.toString();
    }


    @Override
    public ETLManagerJson getAsJson(MultivaluedMap<String, String> query)
    {
        final String repositoryName = EventSystem.sendSynchronousEvent(new GetRepositoryNameEvent());
        final int harvestedCount = getHarvestedCount();
        final int maxDocumentCount = getMaxNumberOfDocuments();
        final long remainingHarvestTime = estimateRemainingHarvestTime(harvestedCount, maxDocumentCount);
        final long lastHarvestTimestamp = getLatestHarvestTimestamp();
        final Date nextHarvestDate = EventSystem.sendSynchronousEvent(new GetSchedulerEvent()).getNextHarvestDate();

        boolean hasEnabledETLs = false;

        for (AbstractETL<?, ?> etl : etls) {
            if (etl.isEnabled()) {
                hasEnabledETLs = true;
                break;
            }
        }

        return new ETLManagerJson(
                   repositoryName,
                   getState(),
                   getHealth(),
                   harvestedCount,
                   maxDocumentCount != -1 ? maxDocumentCount : null,
                   remainingHarvestTime != -1 ? remainingHarvestTime : null,
                   lastHarvestTimestamp != -1 ? new Date(lastHarvestTimestamp).toString() : null,
                   nextHarvestDate != null ? nextHarvestDate.toString() : null,
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
                       new TimestampedList<>(getHealth(), 1),
                       getHarvestedCount(),
                       getMaxNumberOfDocuments(),
                       getHash()),
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
    public ETLJson getETLAsJson(MultivaluedMap<String, String> query)
    {
        final List<String> nameList = query != null
                                      ? query.get(ETLConstants.ETL_NAME_QUERY)
                                      : null;

        if (nameList == null || nameList.isEmpty())
            throw new IllegalArgumentException(ETLConstants.ETL_NAME_QUERY_ERROR_EMPTY);

        final String etlName = nameList.get(0);
        final Optional<AbstractETL<?, ?>> etl =
            etls.stream()
            .filter((AbstractETL<?, ?>  e) -> e.getName().equalsIgnoreCase(etlName))
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
    public void register(AbstractETL<?, ?> addedEtl)
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

                if (etls.stream().noneMatch((AbstractETL<?, ?> e) -> e.getName().equals(etlNameTemp)))
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
            processETLs((AbstractETL<?, ?> harvester) -> harvester.update());

        final int maxDocs = getMaxNumberOfDocuments();
        final int currentDocs = getHarvestedCount();

        // it's outdated if not all documents have been harvested
        if (maxDocs == -1 && currentDocs == 0 || currentDocs < maxDocs)
            return true;

        String currentHash = getHash();
        return currentHash == null || !currentHash.equals(lastHarvestHash);
    }


    @Override
    public void addEventListeners()
    {
        super.addEventListeners();

        // event listeners must be added even if the ETL is disabled
        for (AbstractETL<?, ?> etl : etls)
            etl.addEventListeners();
    }


    @Override
    public void removeEventListeners()
    {
        super.removeEventListeners();

        // event listeners must be removed even if the ETL is disabled
        for (AbstractETL<?, ?> etl : etls)
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

            boolean isPrepared = prepareETLsForHarvest();

            if (isPrepared)
                harvestETLs();
            else
                throw new ETLPreconditionException(ETLConstants.PREPARE_ETLS_FAILED);
        })
        .thenAccept((Void v) -> {
            // save to disk only if we were successful
            this.lastHarvestHash = getHash();

            saveToDisk();

            final ETLState status = getState();

            if (status == ETLState.ABORTING)
                LOGGER.info(ETLConstants.ABORT_FINISHED);

            else if (status == ETLState.HARVESTING)
                LOGGER.info(ETLConstants.HARVEST_FINISHED);

            EventSystem.sendEvent(new HarvestFinishedEvent(true, lastHarvestHash));
            setStatus(ETLState.IDLE);
        })
        .exceptionally((Throwable reason) -> {
            LOGGER.error(ETLConstants.ETLS_FAILED_UNKNOWN_ERROR, reason);

            // clean up all ETLs
            processETLs((AbstractETL<?, ?> harvester) -> harvester.cancelHarvest());
            saveToDisk();

            LOGGER.info(ETLConstants.HARVEST_FAILED);

            EventSystem.sendEvent(new HarvestFinishedEvent(false, getHash()));
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
        final List<Integer> sizes = processETLs((AbstractETL<?, ?> harvester) ->
                                                harvester.getMaxNumberOfDocuments());
        int total = 0;

        for (int size : sizes) {
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

            processETLs((AbstractETL<?, ?> harvester) -> {
                if (harvester.getState() == ETLState.HARVESTING || harvester.getState() == ETLState.QUEUED)
                    harvester.abortHarvest();
            });
        } else
            throw new IllegalStateException(String.format(ETLConstants.ABORT_INVALID_STATE, combinedStateHistory.toString()));
    }


    /**
     * Generates a combined hash over the concatenated hashes of all registered ETLs.
     *
     * @return a combined hash over the concatenated hashes of all registered ETLs
     */
    public String getHash()
    {
        // concatenate all hashes
        final StringBuffer hashBuilder = new StringBuffer();

        for (AbstractETL<?, ?> etl : etls) {
            // skip disabled ETLs
            if (!etl.isEnabled())
                continue;

            final String subHash = etl.getHash();

            // if a single hash value is unknown, we cannot generate a combined version hash
            if (subHash == null)
                return null;

            hashBuilder.append(subHash);
        }

        // rare case when no enabled ETLs exist
        if (hashBuilder.length() == 0)
            return null;

        final HashGenerator generator = new HashGenerator(StandardCharsets.UTF_8);
        return generator.getShaHash(hashBuilder.toString());
    }


    /**
     * Retrieves the number of documents that have been loaded.
     *
     * @return the number of documents that have been loaded
     */
    public int getHarvestedCount()
    {
        return sumUpETLValues(
                   (AbstractETL<?, ?> harvester) -> harvester.getHarvestedCount()
               );
    }


    /**
     * Retrieves an overall health status.
     *
     * @return a health status representing the combined health of all registered ETLs
     */
    public ETLHealth getHealth()
    {
        final List<ETLHealth> healthStatuses = processETLs((AbstractETL<?, ?> harvester) ->
                                                           harvester.getHealth());
        boolean hasExtractorFailed = false;
        boolean hasTransformerFailed = false;
        boolean hasLoaderFailed = false;

        for (ETLHealth subStatus : healthStatuses) {
            switch (subStatus) {
                case INITIALIZATION_FAILED:
                    return subStatus;

                case HARVEST_FAILED:
                    hasExtractorFailed = true;
                    hasTransformerFailed = true;
                    hasLoaderFailed = true;
                    break;

                case EXTRACTION_FAILED:
                    hasExtractorFailed = true;
                    break;

                case TRANSFORMATION_FAILED:
                    hasTransformerFailed = true;
                    break;

                case LOADING_FAILED:
                    hasLoaderFailed = true;
                    break;

                default:
                    // do nothing
            }
        }

        if (!hasExtractorFailed && !hasTransformerFailed && !hasLoaderFailed)
            return ETLHealth.OK;

        if (hasExtractorFailed && !hasTransformerFailed && !hasLoaderFailed)
            return ETLHealth.EXTRACTION_FAILED;

        if (hasTransformerFailed && !hasExtractorFailed && !hasLoaderFailed)
            return ETLHealth.TRANSFORMATION_FAILED;

        if (hasLoaderFailed && !hasExtractorFailed && !hasTransformerFailed)
            return ETLHealth.LOADING_FAILED;

        return ETLHealth.HARVEST_FAILED;
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
        return estimateRemainingHarvestTime(getHarvestedCount(), getMaxNumberOfDocuments());
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
        int preparedCount = sumUpETLValues((AbstractETL<?, ?> harvester) -> {
            try
            {
                // prepareHarvest() can take time, abort as early as possible
                if (getState() != ETLState.ABORTING) {
                    harvester.prepareHarvest();
                    return 1;
                }
            } catch (ETLPreconditionException e)
            {
                LOGGER.info(e.getMessage());
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
     * Harvests prepared and queued ETLs.
     */
    private void harvestETLs()
    {
        LOGGER.info(ETLConstants.START_ETLS);
        setStatus(ETLState.HARVESTING);

        EventSystem.sendEvent(new HarvestStartedEvent(getHash(), getMaxNumberOfDocuments()));

        // check parameter to see if harvests must run sequentially or not
        if (concurrentParam.getValue()) {
            // run all harvests asynchronously
            final List<CompletableFuture<?>> asyncHarvests = processETLs(
                                                                 (AbstractETL<?, ?> etl) ->
            CompletableFuture.runAsync(() -> {
                if (getState() != ETLState.ABORTING && etl.getState() == ETLState.QUEUED)
                    etl.harvest();
            })
                                                             );

            // wait for all async harvests to complete
            try {
                final CompletableFuture<?>[] asyncHarvestArray =
                    asyncHarvests.toArray(new CompletableFuture<?>[asyncHarvests.size()]);
                CompletableFuture.allOf(asyncHarvestArray).get();

            } catch (InterruptedException | ExecutionException e) {
                LOGGER.error(ETLConstants.ETL_PROCESSING_ERROR, e);
            }
        } else {
            processETLs((AbstractETL<?, ?> etl) -> {
                if (getState() != ETLState.ABORTING && etl.getState() == ETLState.QUEUED)
                    etl.harvest();
            });
        }
    }


    /**
     * Iterates all registered ETLs either sequentially,
     * and stores the return values of a specified function in a list.
     *
     * @param function a function that is called on each ETL
     *
     * @return a list containing the return values of each ETL
     */
    private <T> List<T> processETLs(Function<AbstractETL<?, ?>, T> function)
    {
        final List<T> returnValues = new ArrayList<>(etls.size());

        for (AbstractETL<?, ?> etl : etls)
            if (etl.isEnabled())
                returnValues.add(function.apply(etl));

        return returnValues;
    }


    /**
     * Sequentially iterates all registered and enabled ETLs and executes a function
     * on them.
     *
     * @param consumer the function that is called on each ETL
     */
    private void processETLs(Consumer<AbstractETL<?, ?>> consumer)
    {
        for (AbstractETL<?, ?> etl : etls)
            if (etl.isEnabled())
                consumer.accept(etl);
    }


    /**
     * Iterates all registered and enabled ETLs either sequentially or concurrently,
     * depending on the value of the corresponding parameter,
     * and sums up the return value of a common function.
     *
     * @param intFunction a function that is called on each ETL that returns an integer value
     *
     * @return the sum of all return values of the functions
     */
    private int sumUpETLValues(Function<AbstractETL<?, ?>, Integer> intFunction)
    {
        final List<Integer> processedData = processETLs(intFunction);

        int total = 0;

        for (int pd : processedData)
            total += pd;

        return total;
    }


    /**
     * Changes the overall status of the registry.
     *
     * @param status the new status
     */
    private void setStatus(ETLState status)
    {
        combinedStateHistory.addValue(status);
    }


    /**
     * Estimates the remaining harvesting duration in milliseconds.
     *
     * @param harvestedDocuments the number of documents that were harvested
     * @param maxDocuments the total number of harvestable documents, or -1
     * if unknown
     *
     * @return the remaining harvesting duration in milliseconds,
     * or -1 if it cannot be estimated
     */
    private long estimateRemainingHarvestTime(final int harvestedDocuments, final int maxDocuments)
    {
        final ETLState currentStatus = combinedStateHistory.getLatestValue();

        // if there is no ongoing harvest, we cannot estimate the time
        if (currentStatus != ETLState.HARVESTING)
            return -1;

        // if we do not now how many documents there are, we cannot estimate the time
        if (maxDocuments == -1)
            return -1;

        // if nothing was harvested yet, we cannot estimate the time
        if (harvestedDocuments == 0)
            return -1;

        // check when the harvest started
        final long harvestStartTimestamp = combinedStateHistory.getLatestTimestamp();

        // calculate for how many milliseconds the harvest has been going on
        final long millisSinceHarvestStarted = System.currentTimeMillis() - harvestStartTimestamp;

        // calculate the average milliseconds for harvesting a single document
        final long averageMillisPerDocument = millisSinceHarvestStarted / harvestedDocuments;

        // estimate how many milliseconds it will take to harvest the remaining documents
        return averageMillisPerDocument * (maxDocuments - harvestedDocuments);
    }


    /**
     * Creates a duration string out of a specified number of seconds
     *
     * @param milliseconds the time span in milliseconds
     * @return a formatted duration string, or "unknown" if the duration is
     *         negative
     */
    private static String getDurationText(long milliseconds)
    {
        if (milliseconds < 0 || milliseconds == Long.MAX_VALUE)
            return ETLConstants.REMAINING_TIME_UNKNOWN;

        long hours = milliseconds / 3600000;
        return String.format(ETLConstants.REMAINING_TIME, hours, milliseconds);
    }
}
