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


import de.gerdiproject.harvest.ICleanable;
import de.gerdiproject.harvest.IDocument;
import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.impl.AbortingFinishedEvent;
import de.gerdiproject.harvest.event.impl.DocumentHarvestedEvent;
import de.gerdiproject.harvest.event.impl.HarvestFinishedEvent;
import de.gerdiproject.harvest.event.impl.HarvestStartedEvent;
import de.gerdiproject.harvest.event.impl.HarvesterParameterChangedEvent;
import de.gerdiproject.harvest.event.impl.StartAbortingEvent;
import de.gerdiproject.harvest.event.impl.StartHarvestEvent;
import de.gerdiproject.harvest.harvester.constants.HarvesterConstants;
import de.gerdiproject.harvest.harvester.rest.HarvesterFacade;
import de.gerdiproject.harvest.submission.ElasticSearchSubmitter;
import de.gerdiproject.harvest.utils.CancelableFuture;
import de.gerdiproject.harvest.utils.HarvesterStringUtils;
import de.gerdiproject.harvest.utils.HttpRequester;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * AbstractHarvesters offer a skeleton for harvesting a data provider to
 * retrieve all of its metadata. The metadata can subsequently be submitted to
 * ElasticSearch via the {@link ElasticSearchSubmitter}. This most basic Harvester
 * class offers functions that can be controlled via REST requests from the
 * {@link HarvesterFacade}, as well as some utility objects that are required by
 * all harvests. Subclasses must implement the concrete harvesting process.
 *
 * @author Robin Weiss
 */
public abstract class AbstractHarvester
{
    /**
     * Event listener for harvester parameter changes. Parameters include the harvesting range, as well as
     * any implementation specific properties.
     */
    private final Consumer<HarvesterParameterChangedEvent> onParameterChanged = (HarvesterParameterChangedEvent e) -> {
        String key = e.getParameter().getKey();

        if (key.equals(ConfigurationConstants.HARVEST_START_INDEX))
            setStartIndex((Integer) e.getParameter().getValue());

        else if (key.equals(ConfigurationConstants.HARVEST_END_INDEX))
            setEndIndex((Integer) e.getParameter().getValue());

        else
            setProperty(key, e.getParameter().getValue().toString());
    };

    private final Map<String, String> properties;

    protected CancelableFuture<Boolean> currentHarvestingProcess;

    private final AtomicInteger maxDocumentCount;
    private final AtomicInteger harvestedDocumentCount;

    private final AtomicInteger startIndex;
    private final AtomicInteger endIndex;

    protected boolean isMainHarvester;
    protected String name;
    protected String hash;
    protected final HttpRequester httpRequester;

    protected final Logger logger; // NOPMD - we want to retrieve the type of the inheriting class


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
     */
    abstract protected boolean harvestInternal(int startIndex, int endIndex) throws Exception; // NOPMD - we want the inheriting class to be able to throw any exception


    /**
     * Calculates the total number of harvested documents.
     *
     * @return the total number of documents that are to be harvested
     */
    abstract protected int initMaxNumberOfDocuments();

    /**
     * Computes a hash value of the files that are to be harvested, which is
     * used for checking if the files have changed.
     *
     * @return a hash as a checksum of the data which is to be harvested
     *
     * @throws NoSuchAlgorithmException occurs if an invalid algorithm is used for a {@linkplain MessageDigest}
     * @throws NullPointerException occurs for several reasons, depending on the implementation
     */
    abstract protected String initHash() throws NoSuchAlgorithmException, NullPointerException;


    /**
     * Aborts the harvesting process, allowing a new harvest to be started.
     */
    abstract protected void abortHarvest();


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

        currentHarvestingProcess = null;
        maxDocumentCount = new AtomicInteger();
        harvestedDocumentCount = new AtomicInteger();

        startIndex = new AtomicInteger(0);
        endIndex = new AtomicInteger(0);

        EventSystem.addListener(HarvesterParameterChangedEvent.class, onParameterChanged);
    }



    /**
     * Initializes the Harvester, calculating the hash and maximum number of harvestable documents.
     */
    public void init()
    {
        // calculate hash
        try {
            hash = initHash();
        } catch (NoSuchAlgorithmException | NullPointerException e) {
            logger.error(String.format(HarvesterConstants.HASH_CREATION_FAILED, name), e);
            hash = null;
        }

        // calculate number of documents
        int maxHarvestableDocs = initMaxNumberOfDocuments();
        maxDocumentCount.set(maxHarvestableDocs);
        endIndex.set(maxHarvestableDocs);

        EventSystem.addListener(StartHarvestEvent.class, (StartHarvestEvent e) -> harvest());
        EventSystem.addListener(StartAbortingEvent.class, (StartAbortingEvent e) -> abortHarvest());
    }


    /**
     * Marks this harvester as the main harvester.
     * Warning: This should not be called manually! This is only called once by the {@linkplain MainContext}.
     */
    public void setAsMainHarvester()
    {
        isMainHarvester = true;
    }


    /**
     * Retrieves the value of a property.
     *
     * @param key
     *            the name of the property
     * @return the property value
     */
    protected String getProperty(String key)
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
    protected void setProperty(String key, String value)
    {
        properties.put(key, value);
    }


    /**
     * Adds a document to the search index and logs the progress. If the
     * document is null, it is not added to the search index, but the progress
     * is incremented regardlessly.
     *
     * @param document
     *            the document that is to be added to the search index
     */
    protected void addDocument(IDocument document)
    {
        if (document != null) {
            if (document instanceof ICleanable)
                ((ICleanable) document).clean();

            EventSystem.sendEvent(new DocumentHarvestedEvent(document));
        }

        // increment harvest count
        final int harvestedDocs = harvestedDocumentCount.incrementAndGet();
        final int from = startIndex.get();

        // log progress
        logger.info(HarvesterStringUtils.formatProgress(
                        name,
                        from + harvestedDocs,
                        endIndex.get()));
    }


    /**
     * Adds multiple documents to the search index and logs the progress. If a
     * document is null, it is not added to the search index, but the progress
     * is incremented regardlessly.
     *
     * @param documents
     *            the documents that are to be added to the search index
     */
    protected void addDocuments(List<IDocument> documents)
    {
        documents.forEach((IDocument doc) -> addDocument(doc));
    }


    /**
     * Returns the total number of documents that can possibly be harvested.
     *
     * @return the total number of documents that can possibly be harvested
     */
    protected final int getMaxNumberOfDocuments()
    {
        return maxDocumentCount.get();
    }


    /**
     * Sets the start index of the harvesting range.
     *
     * @param from  the index of the first document to be harvested
     */
    protected void setStartIndex(int from)
    {
        if (from >= maxDocumentCount.get())
            startIndex.set(maxDocumentCount.get());

        else if (from <= 0)
            startIndex.set(0);

        else
            startIndex.set(from);
    }


    /**
     * Sets the end index of the harvesting range.
     *
     * @param to the index of the first document that is not to be harvested
     *            anymore
     */
    protected void setEndIndex(int to)
    {
        if (to >= maxDocumentCount.get())
            endIndex.set(maxDocumentCount.get());

        else if (to <= 0)
            endIndex.set(0);

        else
            endIndex.set(to);
    }


    /**
     * Starts an asynchronous harvest with the implemented harvestInternal()
     * method and saves the result and date for this session
     */
    protected final void harvest()
    {
        logger.info(String.format(HarvesterConstants.HARVESTER_START, name));

        harvestedDocumentCount.set(0);

        EventSystem.sendEvent(new HarvestStartedEvent(endIndex.get(), startIndex.get()));

        // start asynchronous harvest
        currentHarvestingProcess = new CancelableFuture<>(
            () -> harvestInternal(startIndex.get(), endIndex.get()));

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
        logger.info(String.format(HarvesterConstants.HARVESTER_END, name));

        // do some things, only if this is the main harvester
        if (isMainHarvester)
            EventSystem.sendEvent(new HarvestFinishedEvent(true, getHash(false)));
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

        // log failure
        logger.warn(String.format(HarvesterConstants.HARVESTER_FAILED, name));

        if (isMainHarvester)
            EventSystem.sendEvent(new HarvestFinishedEvent(false, getHash(false)));
    }


    /**
     * This function is called after the harvesting process was stopped due to
     * being aborted.
     */
    protected void onHarvestAborted()
    {
        EventSystem.sendEvent(new AbortingFinishedEvent());
        logger.warn(String.format(HarvesterConstants.HARVESTER_ABORTED, name));
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
        if (recalculate) {
            try {
                hash = initHash();
            } catch (NoSuchAlgorithmException | NullPointerException e) {
                logger.error(String.format(HarvesterConstants.HASH_CREATION_FAILED, name), e);
            }
        }

        return hash;
    }
}
