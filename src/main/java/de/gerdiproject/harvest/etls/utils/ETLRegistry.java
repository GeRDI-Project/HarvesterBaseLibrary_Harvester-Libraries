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
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.ws.rs.core.MultivaluedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.parameters.BooleanParameter;
import de.gerdiproject.harvest.etls.AbstractETL;
import de.gerdiproject.harvest.etls.ETLPreconditionException;
import de.gerdiproject.harvest.etls.constants.ETLConstants;
import de.gerdiproject.harvest.etls.enums.ETLHealth;
import de.gerdiproject.harvest.etls.enums.ETLStatus;
import de.gerdiproject.harvest.etls.events.GetETLRegistryEvent;
import de.gerdiproject.harvest.etls.events.HarvestFinishedEvent;
import de.gerdiproject.harvest.etls.events.HarvestStartedEvent;
import de.gerdiproject.harvest.etls.json.ETLJson;
import de.gerdiproject.harvest.etls.json.ETLRegistryJson;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.IEventListener;
import de.gerdiproject.harvest.rest.AbstractRestObject;
import de.gerdiproject.harvest.utils.HashGenerator;
import de.gerdiproject.harvest.utils.data.DiskIO;
import de.gerdiproject.harvest.utils.file.ICachedObject;


/**
 * This class serves as an interface for all {@linkplain AbstractETL}s that
 * are required to harvest the repository.
 *
 * @author Robin Weiss
 */
public class ETLRegistry extends AbstractRestObject<ETLRegistry, ETLJson> implements ICachedObject
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ETLRegistry.class);

    // fields and members
    private final DiskIO diskIo;
    private final File cacheFile;
    private final List<AbstractETL<?, ?>> etls;
    private final BooleanParameter concurrentParam;
    private final BooleanParameter forceHarvestParameter;
    private final TimestampedList<ETLStatus> combinedStatusHistory;
    private String lastHarvestHash;


    /**
     * Constructor.
     *
     * @param moduleName the name of the harvester service
     */
    public ETLRegistry(String moduleName)
    {
        super(moduleName, GetETLRegistryEvent.class);
        this.combinedStatusHistory = new TimestampedList<>(ETLStatus.INITIALIZING, 10);

        this.etls = new LinkedList<>();
        this.concurrentParam = Configuration.registerParameter(ETLConstants.CONCURRENT_PARAM);
        this.forceHarvestParameter = Configuration.registerParameter(ETLConstants.FORCED_PARAM);
        this.cacheFile = new File(String.format(ETLConstants.ETL_REGISTRY_CACHE_PATH, moduleName));
        this.diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);

        setStatus(ETLStatus.IDLE);
    }


    @Override
    public void loadFromDisk()
    {
        final ETLRegistryJson loadedState = diskIo.getObject(cacheFile, ETLRegistryJson.class);

        if (loadedState != null) {
            // load the overall hash
            this.lastHarvestHash = loadedState.getOverallInfo().getVersionHash();

            // load status history
            this.combinedStatusHistory.addAllSorted(loadedState.getOverallInfo().getStatusHistory());

            // load ETL states
            for (AbstractETL<?, ?> etl : etls) {
                final ETLJson etlInfo = loadedState.getEtlInfos().get(etl.getName());
                etl.loadFromJson(etlInfo);
            }

            LOGGER.debug(String.format(ETLConstants.ETL_REGISTRY_LOADED, cacheFile));
        }
    }


    @Override
    public void saveToDisk()
    {
        diskIo.writeObjectToFile(cacheFile, new ETLRegistryJson(this, etls));
    }


    @Override
    protected String getPrettyPlainText()
    {
        final StringBuilder sb = new StringBuilder();
        int totalCurrCount = 0;
        int totalMaxCount = 0;

        for (AbstractETL<?, ?> etl : etls) {
            sb.append(etl.toString());

            totalCurrCount += etl.getHarvestedCount();
            final int maxCount = etl.getMaxNumberOfDocuments();

            if (maxCount != -1 && totalMaxCount != -1)
                totalMaxCount += maxCount;
            else
                totalMaxCount = -1;
        }

        final ETLStatus status = getStatus();
        String statusString = status.toString().toLowerCase();

        if (status == ETLStatus.HARVESTING) {
            if (totalMaxCount != -1)
                statusString += String.format(ETLConstants.PROGRESS, Math.round(100f * totalCurrCount / totalMaxCount), totalCurrCount, totalMaxCount);
            else
                statusString += String.format(ETLConstants.PROGRESS_NO_BOUNDS, totalCurrCount);
        }

        sb.append(String.format(ETLConstants.ETL_PRETTY, ETLConstants.NAME_TOTAL, statusString));

        if (status == ETLStatus.HARVESTING) {
            final long remainingMilliSeconds = estimateRemainingHarvestTime(totalCurrCount, totalMaxCount);
            sb.append(getDurationText(remainingMilliSeconds));
        }

        return sb.toString();
    }


    @Override
    public ETLJson getAsJson(MultivaluedMap<String, String> query)
    {
        final List<String> etlIndexList = query != null
                                          ? query.get(ETLConstants.ETL_INDEX_QUERY)
                                          : null;

        if (etlIndexList == null || etlIndexList.size() == 0)
            return new ETLJson(
                       getClass().getSimpleName(),
                       combinedStatusHistory,
                       null,
                       getHarvestedCount(),
                       getMaxNumberOfDocuments(),
                       getHash());

        int index;

        try {
            index = Integer.parseInt(etlIndexList.get(0));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format(ETLConstants.ETL_INDEX_OUT_OF_RANGE, etls.size() - 1));
        }

        if (index < 0 || index >= etls.size())
            throw new IllegalArgumentException(String.format(ETLConstants.ETL_INDEX_OUT_OF_RANGE, etls.size() - 1));

        return etls.get(index).getAsJson();
    }


    /**
     * Registers an {@linkplain AbstractETL}, adding it to the list of ETLs
     * that can be controlled via REST.
     *
     * @param etl the {@linkplain AbstractETL} to be registered
     */
    public void register(AbstractETL<?, ?> etl)
    {
        if (etls.contains(etl))
            LOGGER.info(String.format(ETLConstants.DUPLICATE_ETL_REGISTERED_ERROR, etl.getClass().getSimpleName()));
        else
            etls.add(etl);
    }


    /**
     * Updates all registered ETLs and checks if they are outdated.
     *
     * @return true if at least one ETL is outdated
     */
    public boolean hasOutdatedETLs()
    {
        // if a harvest is ongoing or just finished, do not update the ETLs
        final ETLStatus currentStatus = getStatus();

        if (currentStatus == ETLStatus.IDLE || currentStatus == ETLStatus.DONE)
            processETLs((AbstractETL<?, ?> harvester) -> harvester.update());

        String currentHash = getHash();
        return currentHash == null || !currentHash.equals(lastHarvestHash);
    }


    @Override
    public void addEventListeners()
    {
        super.addEventListeners();

        processETLs((AbstractETL<?, ?> harvester) -> {
            if (harvester instanceof IEventListener)
                ((IEventListener)harvester).addEventListeners();
        });
    }


    @Override
    public void removeEventListeners()
    {
        super.removeEventListeners();

        processETLs((AbstractETL<?, ?> harvester) -> {
            if (harvester instanceof IEventListener)
                ((IEventListener)harvester).removeEventListeners();
        });
    }


    /**
     * Attempts to start a harvest, signalling all registered ETLs to start
     *
     * @throws IllegalStateException thrown if another ongoing harvest is blocking this process
     */
    public void harvest() throws IllegalStateException
    {
        if (getStatus() != ETLStatus.IDLE)
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

            EventSystem.sendEvent(new HarvestFinishedEvent(true, lastHarvestHash));
            setStatus(ETLStatus.IDLE);

        })
        .exceptionally((Throwable reason) -> {
            LOGGER.error(ETLConstants.ETLS_FAILED_UNKNOWN_ERROR, reason);

            // clean up all ETLs
            processETLs((AbstractETL<?, ?> harvester) -> harvester.cancelHarvest());
            saveToDisk();

            EventSystem.sendEvent(new HarvestFinishedEvent(false, getHash()));
            setStatus(ETLStatus.IDLE);

            return null;
        });
    }


    /**
     * Sums up the expected number of harvestable documents of all registered ETLs.
     *
     * @return the expected total number of harvestable documents
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
        final ETLStatus currentStatus = getStatus();

        if (currentStatus == ETLStatus.QUEUED || currentStatus == ETLStatus.HARVESTING) {
            setStatus(ETLStatus.ABORTING);

            processETLs((AbstractETL<?, ?> harvester) -> {
                if (harvester.getStatus() == ETLStatus.HARVESTING || harvester.getStatus() == ETLStatus.QUEUED)
                    harvester.abortHarvest();
            });
            // TODO EventSystem.sendEvent(new AbortingFinishedEvent());
        } else
            throw new IllegalStateException(String.format(ETLConstants.ABORT_INVALID_STATE, combinedStatusHistory.toString()));
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
        boolean hasHarvestFailed = false;

        for (ETLHealth subStatus : healthStatuses) {
            switch (subStatus) {
                case INITIALIZATION_FAILED:
                    return subStatus;

                case HARVEST_FAILED:
                case EXTRACTION_FAILED:
                case TRANSFORMATION_FAILED:
                case LOADING_FAILED:
                    hasHarvestFailed = true;
                    break;

                default:
                    // do nothing
            }
        }

        return hasHarvestFailed ? ETLHealth.HARVEST_FAILED : ETLHealth.OK;
    }


    /**
     * Returns a status representing the entirety of all registered ETL statuses.
     *
     * @return a status representing the entirety of all registered ETL statuses
     */
    public ETLStatus getStatus()
    {
        return combinedStatusHistory.getLatestValue();
    }


    /**
     * Prepares all registered ETLs for a subsequent harvest.
     *
     * @return true if all ETLs were sucessfully prepared
     */
    private boolean prepareETLsForHarvest()
    {
        LOGGER.info(ETLConstants.PREPARE_ETLS);
        setStatus(ETLStatus.QUEUED);

        final AtomicInteger preparedCount = new AtomicInteger(0);

        processETLs((AbstractETL<?, ?> harvester) -> {
            try
            {
                // prepareHarvest() can take time, abort as early as possible
                if (getStatus() != ETLStatus.ABORTING) {
                    harvester.prepareHarvest();
                    preparedCount.incrementAndGet();
                }
            } catch (ETLPreconditionException e)
            {
                LOGGER.info(e.getMessage());
            }
        });

        if (preparedCount.get() == 0 || getStatus() == ETLStatus.ABORTING) {
            setStatus(ETLStatus.IDLE);
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
        setStatus(ETLStatus.HARVESTING);

        EventSystem.sendEvent(new HarvestStartedEvent(getHash(), getMaxNumberOfDocuments()));

        processETLs((AbstractETL<?, ?> harvester) -> {
            if (getStatus() != ETLStatus.ABORTING && harvester.getStatus() == ETLStatus.QUEUED)
                harvester.harvest();
        });
    }


    /**
     * Iterates all registered ETLs either sequentially or concurrently,
     * depending on the value of the corresponding parameter,
     * and stores the return values of a common function in a list.
     *
     * @param function a function that is called on each ETL
     *
     * @return a list containing the return values of each ETL
     */
    private <T> List<T> processETLs(Function<AbstractETL<?, ?>, T> function)
    {
        final int len = etls.size();
        final List<T> returnValues = new ArrayList<>(len);

        if (concurrentParam.getValue()) {
            final List<CompletableFuture<?>> subProcesseList = new LinkedList<>();

            for (int i = 0; i < len; i++) {
                final int j = i;

                if (etls.get(j).isEnabled())
                    subProcesseList.add(CompletableFuture.runAsync(() -> returnValues.add(function.apply(etls.get(j)))));
            }

            // convert list to array
            final CompletableFuture<?>[] subProcessArray = subProcesseList.toArray(new CompletableFuture<?>[subProcesseList.size()]);

            // wait for all sub-processes to complete
            try {
                CompletableFuture.allOf(subProcessArray).get();
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.error(ETLConstants.ETL_PROCESSING_ERROR, e);
            }
        } else {
            for (AbstractETL<?, ?> etl : etls)
                if (etl.isEnabled())
                    returnValues.add(function.apply(etl));
        }

        return returnValues;
    }


    /**
     * Iterates all registered and enabled ETLs either sequentially or concurrently,
     * depending on the value of the corresponding parameter.
     *
     * @param consumer the function that is called on each ETL
     */
    private void processETLs(Consumer<AbstractETL<?, ?>> consumer)
    {
        processETLs((AbstractETL<?, ?> etl) -> {consumer.accept(etl); return null;});
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
    private void setStatus(ETLStatus status)
    {
        combinedStatusHistory.addValue(status);
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
        final ETLStatus currentStatus = combinedStatusHistory.getLatestValue();

        // if there is no ongoing harvest, we cannot estimate the time
        if (currentStatus != ETLStatus.HARVESTING)
            return -1;

        // if we do not now how many documents there are, we cannot estimate the time
        if (maxDocuments == -1)
            return -1;

        // if nothing was harvested yet, we cannot estimate the time
        if (harvestedDocuments == 0)
            return -1;

        // check when the harvest started
        final long harvestStartTimestamp = combinedStatusHistory.getLatestTimestamp();

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


    /**
     * Removes a sub-harvester from the list of ongoing harvests and
     * checks if it was successful. It the last running harvester is removed,
     * another {@linkplain HarvestFinishedEvent} ist dispatched.
     *
    private void finishHarvester(HarvestFinishedEvent event)
    {
        if(harvestingSubHarvesters.remove(event.getHarvester()))
        {
            isFailing |= !event.isSuccessful();

            if(harvestingSubHarvesters.isEmpty())
            {
                EventSystem.sendEvent(new HarvestFinishedEvent(!isFailing, getHash(), this));
            }
        }

    }


    //////////////////////////////
    // Event Callback Functions //
    //////////////////////////////


    private final Consumer<HarvestFinishedEvent> onHarvesterFinished = (HarvestFinishedEvent event) -> {
            finishHarvester(event);
    };


    private final Consumer<AbortingFinishedEvent> onAbortingFinished = (AbortingFinishedEvent event) -> {
            finishHarvester(event);
    };
    */

}
