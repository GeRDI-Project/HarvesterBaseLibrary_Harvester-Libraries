/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package de.gerdiproject.harvest.harvester;


import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.development.DevelopmentTools;
import de.gerdiproject.harvest.elasticsearch.ElasticSearchSender;
import de.gerdiproject.harvest.harvester.rest.HarvesterFacade;
import de.gerdiproject.harvest.utils.CancelableFuture;
import de.gerdiproject.harvest.utils.HarvesterStringUtils;
import de.gerdiproject.harvest.utils.HttpRequester;
import de.gerdiproject.harvest.utils.SearchIndexFactory;
import de.gerdiproject.json.IJsonArray;
import de.gerdiproject.json.IJsonBuilder;
import de.gerdiproject.json.IJsonObject;
import de.gerdiproject.json.impl.JsonBuilder;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * AbstractHarvesters offer a skeleton for harvesting a data provider to
 * retrieve all of its metadata. The metadata can subsequently be submitted to
 * ElasticSearch via the {@link ElasticSearchSender}. This most basic Harvester
 * class offers functions that can be controlled via REST requests from the
 * {@link HarvesterFacade}, as well as some utility objects that are required by
 * all harvests. Subclasses must implement the concrete harvesting process.
 *
 * @author Robin Weiss
 */
public abstract class AbstractHarvester
{
    protected static final String HASH_CREATE_FAILED = "Failed to create hash";
    protected static final String OCTAT_FORMAT = "%02x";
    protected static final String SHA_HASH_ALGORITHM = "SHA";

    private final static String HARVESTER_START = "+++ Starting %s +++";
    private final static String HARVESTER_END = "--- %s finished ---";
    private final static String HARVESTER_FAILED = "!!! %s failed !!!";
    private final static String HARVESTER_ABORTED = "!!! %s aborted !!!";
    private static final String HARVEST_DONE = "HARVEST FINISHED!";
    private static final String HARVEST_FAILED = "HARVEST FAILED!";

    protected static final String HARVEST_ABORTED = "HARVEST ABORTED!";

    private final HashMap<String, String> properties;
    protected IJsonArray harvestedDocuments;

    private Date harvestStartedDate;
    private Date harvestFinishedDate;
    protected CancelableFuture<Boolean> currentHarvestingProcess;

    private final AtomicInteger totalNumberOfDocuments;
    private final AtomicInteger numberOfHarvestedDocuments;

    private final AtomicInteger harvestStartIndex;
    private final AtomicInteger harvestEndIndex;

    protected String name;
    protected String hash;
    protected Logger logger;
    protected final HttpRequester httpRequester;
    protected final SearchIndexFactory searchIndexFactory;
    protected final IJsonBuilder jsonBuilder;


    /**
     * The main harvesting method. The overridden implementation should add
     * documents to the search index by calling addDocumentToIndex().
     *
     * @param startIndex
     *            the index of the first document to be harvested
     * @param endIndex
     *            the index of the last document to be harvested
     * @throws Exception
     *             any kind of exception that can occur during the harvesting
     *             process
     * @return true, if everything was harvested
     * @see de.gerdiproject.harvest.utils.SearchIndexFactory
     */
    abstract protected boolean harvestInternal(int startIndex, int endIndex) throws Exception;


    /**
     * Returns a list of properties that can be set.
     *
     * @return a list of properties that can be set via setProperty()
     */
    abstract public List<String> getValidProperties();


    /**
     * Calculates the total number of harvested documents.
     *
     * @return the total number of documents that are to be harvested
     */
    abstract protected int calculateTotalNumberOfDocumentsInternal();


    /**
     * Computes a hash value of the files that are to be harvested, which is
     * used for checking if the files have changed.
     *
     * @return a hash as a checksum of the data which is to be harvested
     */
    abstract protected String calculateHashInternal();


    /**
     * Aborts the harvesting process, allowing a new harvest to be started.
     */
    public abstract void abortHarvest();


    /**
     * Simple constructor that uses the class name as the harvester name.
     */
    public AbstractHarvester()
    {
        this(null);
    }


    /**
     * Constructor that initializes helper classes and fields.
     *
     * @param harvesterName
     *            a unique name that describes the harvester
     */
    public AbstractHarvester(String harvesterName)
    {
        name = (harvesterName != null)
               ? harvesterName
               : getClass().getSimpleName();
        logger = LoggerFactory.getLogger(name);

        properties = new HashMap<>();
        httpRequester = new HttpRequester();
        searchIndexFactory = new SearchIndexFactory();

        currentHarvestingProcess = null;
        harvestStartedDate = null;
        harvestFinishedDate = null;
        totalNumberOfDocuments = new AtomicInteger();
        numberOfHarvestedDocuments = new AtomicInteger();

        harvestStartIndex = new AtomicInteger(0);
        harvestEndIndex = new AtomicInteger(0);
        jsonBuilder = new JsonBuilder();
        this.harvestedDocuments = jsonBuilder.createArray();
    }


    /**
     * Initializes the Harvester, setting up missing values and the logger.
     */
    public void init()
    {
        // calculate hash
        hash = calculateHashInternal();

        // calculate number of documents
        totalNumberOfDocuments.set(calculateTotalNumberOfDocumentsInternal());

        int oldEndIndex = harvestEndIndex.get();
        int newEndIndex = totalNumberOfDocuments.get();

        // if the end index is out of bounds, set it correctly
        if (oldEndIndex == 0 || oldEndIndex > newEndIndex)
            harvestEndIndex.set(newEndIndex);
    }


    /**
     * Retrieves the value of a property.
     *
     * @param key
     *            the name of the property
     * @return the property value
     */
    public String getProperty(String key)
    {
        return properties.get(key);
    }


    /**
     * Sets the value of a property.
     *
     * @param key
     *            the property name
     * @param value
     *            the new property value
     */
    public void setProperty(String key, String value)
    {
        properties.put(key, value);
    }


    /**
     * Creates and returns the search index and some metadata as a JSON object.
     *
     * @return the JSON object that was harvested via harvestInternal(), or null
     *         if no harvest has been finished successfully
     */
    public final IJsonObject getHarvestResult()
    {
        java.sql.Date sqlDate = new java.sql.Date(harvestStartedDate.getTime());
        int harvestTime = (harvestFinishedDate != null && harvestStartedDate != null)
                          ? (int)((harvestFinishedDate.getTime() - harvestStartedDate.getTime()) / 1000)
                          : -1;

        synchronized (harvestedDocuments) {
            return searchIndexFactory.createSearchIndex(harvestedDocuments, sqlDate, hash, harvestTime);
        }
    }


    /**
     * Returns the harvested documents.
     *
     * @return the harvested documents
     */
    public final IJsonArray getHarvestedDocuments()
    {
        synchronized (harvestedDocuments) {
            return harvestedDocuments;
        }
    }


    /**
     * Returns the timestamp of the last successful harvest.
     *
     * @return the time and date at which the last harvest finished, or null if
     *         no harvest has been finished successfully
     */
    public final java.util.Date getHarvestDate()
    {
        return harvestFinishedDate;
    }


    /**
     * Returns the timestamp of the beginning of the last harvest.
     *
     * @return the time and date at which the last harvest started, or null if
     *         no harvest has been started yet
     */
    public final java.util.Date getHarvestStartDate()
    {
        return harvestStartedDate;
    }


    /**
     * Adds a document to the search index and logs the progress. If the
     * document is null, it is not added to the search index, but the progress
     * is incremented regardlessly.
     *
     * @param document
     *            the document that is to be added to the search index
     */
    protected void addDocumentToIndex(IJsonObject document)
    {
        if (document != null) {
            synchronized (harvestedDocuments) {
                harvestedDocuments.add(document);
            }
        }

        // increment harvest count
        final int harvestedDocs = numberOfHarvestedDocuments.incrementAndGet();
        final int from = harvestStartIndex.get();

        // log progress
        logger.info(HarvesterStringUtils.formatProgress(
                        name,
                        from + harvestedDocs,
                        harvestEndIndex.get()));
    }


    /**
     * Adds multiple documents to the search index and logs the progress. If a
     * document is null, it is not added to the search index, but the progress
     * is incremented regardlessly.
     *
     * @param documents
     *            the documents that are to be added to the search index
     */
    protected void addDocumentsToIndex(Iterable<IJsonObject> documents)
    {
        documents.forEach((IJsonObject doc) -> addDocumentToIndex(doc));
    }


    /**
     * Returns the total number of documents that can possibly be harvested.
     *
     * @return the total number of documents that can possibly be harvested
     */
    public final int getTotalNumberOfDocuments()
    {
        return totalNumberOfDocuments.get();
    }


    /**
     * Returns the index of the first document that is to be harvested.
     *
     * @return the index of the first document that is to be harvested
     */
    public final int getHarvestStartIndex()
    {
        return harvestStartIndex.get();
    }


    /**
     * Returns the index of the first document that is not to be harvested
     * anymore.
     *
     * @return the index of the first document that is not to be harvested
     *         anymore
     */
    public final int getHarvestEndIndex()
    {
        return harvestEndIndex.get();
    }


    /**
     * Sets the harvesting range.
     *
     * @param from
     *            the index of the first document to be harvested
     * @param to
     *            the index of the first document that is not to be harvested
     *            anymore
     */
    public void setRange(int from, int to)
    {
        harvestStartIndex.set(from);
        harvestEndIndex.set(to);
    }


    /**
     * Returns the number of harvested documents.
     *
     * @return the number of harvested documents
     */
    public int getNumberOfHarvestedDocuments()
    {
        return numberOfHarvestedDocuments.get();
    }


    /**
     * Returns true if a harvest is in progress.
     *
     * @return true if a harvest is in progress
     */
    public boolean isHarvesting()
    {
        return currentHarvestingProcess != null;
    }


    /**
     * Estimates the completion date of an ongoing harvest by regarding the
     * already passed time.
     *
     * @return a Date or null, if no harvest is in progress
     */
    public long estimateRemainingSeconds()
    {
        // get number of harvested documents
        long documentCount = getNumberOfHarvestedDocuments();

        // only estimate if some progress was made
        if (isHarvesting() && documentCount > 0) {
            Date now = new Date();

            // calculate how many milliseconds the harvest has been going on
            long milliSecondsUntilNow = now.getTime() - harvestStartedDate.getTime();

            // estimate how many milliseconds the harvest will take
            long milliSecondsTotal = milliSecondsUntilNow
                                     * (harvestEndIndex.get() - harvestStartIndex.get())
                                     / documentCount;

            return (milliSecondsTotal - milliSecondsUntilNow) / 1000l;
        }
        return -1;
    }


    /**
     * Starts an asynchronous harvest with the implemented harvestInternal()
     * method and saves the result and date for this session
     */
    public final void harvest()
    {
        logger.info(String.format(HARVESTER_START, name));
        synchronized (harvestedDocuments) {
            harvestedDocuments.clear();
        }
        numberOfHarvestedDocuments.set(0);
        harvestStartedDate = new Date();

        // start asynchronous harvest
        currentHarvestingProcess = new CancelableFuture<>(
            () -> harvestInternal(harvestStartIndex.get(), harvestEndIndex.get()));

        // success handler
        currentHarvestingProcess.thenApply((isSuccessful) -> {
            finishHarvestSuccessfully();
            return isSuccessful;
        })
        // exception handler
        .exceptionally(throwable -> {
            finishHarvestExceptionally(throwable.getCause());
            return false;
        });
    }


    /**
     * Finishes the harvesting process, allowing a new harvest to be started.
     */
    protected void finishHarvestSuccessfully()
    {
        currentHarvestingProcess = null;
        harvestFinishedDate = new Date();

        logger.info(String.format(HARVESTER_END, name));

        // do some things, only if this is the main harvester
        if (MainContext.getHarvester() == this) {
            final DevelopmentTools devTools = DevelopmentTools.instance();

            // save to disk if auto-save is enabled
            if (devTools.isAutoSaving())
                devTools.saveHarvestResultToDisk();

            // log complete harvest end
            logger.info(HARVEST_DONE);

            // send to elastic search if auto-submission is enabled
            if (devTools.isAutoSubmitting())
                ElasticSearchSender.instance().sendToElasticSearch(harvestedDocuments);
        }
    }


    /**
     * This function is called when an exception occurs during the harvest.
     * Cleans up a failed harvesting process, allowing a new harvest to be
     * started. Also calls a function depending on why the harvest failed.
     *
     * @param reason
     *            the exception that caused the harvest to fail
     */
    protected void finishHarvestExceptionally(Throwable reason)
    {
        // clean up the harvesting process reference
        currentHarvestingProcess = null;

        // check if the harvest was aborted
        if (reason instanceof CancellationException || reason.getCause() instanceof CancellationException)
            onHarvestAborted();
        else
            onHarvestFailed(reason);
    }


    /**
     *
     * This method is called after an ongoing harvest failed due to an
     * exception.
     *
     * @param reason
     *            the exception that caused the harvest to fail
     */
    protected void onHarvestFailed(Throwable reason)
    {
        // log the error
        logger.error(reason.getMessage(), reason);

        // save to disk if auto-save is enabled
        final DevelopmentTools devTools = DevelopmentTools.instance();

        if (devTools.isAutoSaving() && MainContext.getHarvester() == this)
            devTools.saveHarvestResultToDisk();

        // log failure
        logger.warn(String.format(HARVESTER_FAILED, name));

        // log failure for main harvester
        if (MainContext.getHarvester() == this)
            logger.error(HARVEST_FAILED);
    }


    /**
     * This function is called after the harvesting process was stopped due to
     * being aborted.
     */
    protected void onHarvestAborted()
    {
        logger.warn(String.format(HARVESTER_ABORTED, name));

        // log abort for main harvester
        if (MainContext.getHarvester() == this)
            logger.error(HARVEST_ABORTED);
    }


    /**
     * Returns the checksum hash of the entries which are to be harvested.
     *
     * @param recalculate
     *            if true, recalculates the hash value
     * @return the checksum hash of the entries which are to be harvested
     */
    public String getHash(boolean recalculate)
    {
        if (recalculate)
            hash = calculateHashInternal();
        return hash;
    }


    /**
     * Returns a readable name of the harvester.
     *
     * @return the name of the harvester
     */
    public String getName()
    {
        return name;
    }


    /**
     * Checks if a harvest has finished already.
     *
     * @return true if a harvest was completed
     */
    public boolean isHarvestFinished()
    {
        return !isHarvesting() && harvestFinishedDate != null;
    }
}
