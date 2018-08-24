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
package de.gerdiproject.harvest.harvester;


import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;

import de.gerdiproject.harvest.ICleanable;
import de.gerdiproject.harvest.IDocument;
import de.gerdiproject.harvest.application.MainContext;
import de.gerdiproject.harvest.application.constants.ApplicationConstants;
import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.config.events.HarvesterParameterChangedEvent;
import de.gerdiproject.harvest.config.parameters.AbstractParameter;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.IEventListener;
import de.gerdiproject.harvest.harvester.constants.HarvesterConstants;
import de.gerdiproject.harvest.harvester.events.DocumentsHarvestedEvent;
import de.gerdiproject.harvest.harvester.events.GetHarvesterOutdatedEvent;
import de.gerdiproject.harvest.harvester.events.GetMaxDocumentCountEvent;
import de.gerdiproject.harvest.harvester.events.GetProviderNameEvent;
import de.gerdiproject.harvest.harvester.events.HarvestFinishedEvent;
import de.gerdiproject.harvest.harvester.events.HarvestStartedEvent;
import de.gerdiproject.harvest.harvester.events.StartHarvestEvent;
import de.gerdiproject.harvest.harvester.rest.HarvesterFacade;
import de.gerdiproject.harvest.state.events.AbortingFinishedEvent;
import de.gerdiproject.harvest.state.events.AbortingStartedEvent;
import de.gerdiproject.harvest.state.events.StartAbortingEvent;
import de.gerdiproject.harvest.submission.elasticsearch.ElasticSearchSubmitter;
import de.gerdiproject.harvest.utils.CancelableFuture;
import de.gerdiproject.harvest.utils.cache.HarvesterCache;
import de.gerdiproject.harvest.utils.cache.constants.CacheConstants;
import de.gerdiproject.harvest.utils.cache.events.RegisterHarvesterCacheEvent;
import de.gerdiproject.harvest.utils.data.HttpRequester;
import de.gerdiproject.harvest.utils.data.constants.DataOperationConstants;
import de.gerdiproject.json.GsonUtils;


/**
 * AbstractHarvesters offer a skeleton for harvesting a data provider to
 * retrieve all of its metadata. The metadata can subsequently be submitted to
 * ElasticSearch via the {@link ElasticSearchSubmitter}. This most basic
 * Harvester class offers functions that can be controlled via REST requests
 * from the {@link HarvesterFacade}, as well as some utility objects that are
 * required by all harvests. Subclasses must implement the concrete harvesting
 * process.
 *
 * @author Robin Weiss
 */
public abstract class AbstractHarvester implements IEventListener
{
    private final Map<String, String> properties;
    private final AtomicInteger maxDocumentCount;
    private final AtomicInteger startIndex;
    private final AtomicInteger endIndex;
    private HarvesterCache documentsCache;
    protected HttpRequester httpRequester;

    protected final Logger logger; // NOPMD - we want to retrieve the type of the inheriting class

    protected CancelableFuture<Boolean> currentHarvestingProcess;
    protected boolean isMainHarvester;
    protected boolean isAborting;
    protected boolean isFailing;
    protected AtomicBoolean forceHarvest;
    protected String name;
    protected String hash;


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
     * @param harvesterName a unique name that describes the harvester
     */
    public AbstractHarvester(String harvesterName)
    {
        name = (harvesterName != null) ? harvesterName : getClass().getSimpleName();
        logger = LoggerFactory.getLogger(name);

        properties = new HashMap<>();

        currentHarvestingProcess = null;
        maxDocumentCount = new AtomicInteger();

        startIndex = new AtomicInteger(0);
        endIndex = new AtomicInteger(0);
        forceHarvest = new AtomicBoolean(false);
    }


    /**
     * The main harvesting method. The overridden implementation should add
     * documents to the search index by calling addDocumentToIndex().
     *
     * @param startIndex the index of the first document to be harvested
     * @param endIndex the index of the last document to be harvested
     * @throws Exception any kind of exception that can occur during the
     *             harvesting process
     * @return true, if everything was harvested
     */
    protected abstract boolean harvestInternal(int startIndex, int endIndex) throws Exception; // NOPMD - we want the inheriting class to be able to throw any exception


    @Override
    public void addEventListeners()
    {
        httpRequester.addEventListeners();

        if (isMainHarvester) {
            EventSystem.addListener(HarvesterParameterChangedEvent.class, onParameterChanged);
            EventSystem.addListener(StartHarvestEvent.class, onStartHarvest);
            EventSystem.addSynchronousListener(GetMaxDocumentCountEvent.class, this::onGetMaxDocumentCount);
            EventSystem.addSynchronousListener(GetProviderNameEvent.class, this::onGetDataProviderName);
            EventSystem.addSynchronousListener(GetHarvesterOutdatedEvent.class, this::onGetHarvesterOutdated);
        }
    }


    @Override
    public void removeEventListeners()
    {
        httpRequester.removeEventListeners();

        if (isMainHarvester) {
            EventSystem.removeListener(HarvesterParameterChangedEvent.class, onParameterChanged);
            EventSystem.removeListener(StartHarvestEvent.class, onStartHarvest);
            EventSystem.removeSynchronousListener(GetMaxDocumentCountEvent.class);
            EventSystem.removeSynchronousListener(GetProviderNameEvent.class);
            EventSystem.removeSynchronousListener(GetHarvesterOutdatedEvent.class);
            EventSystem.removeListener(StartAbortingEvent.class, onStartAborting);
        }
    }


    /**
     * Aborts the harvesting process, allowing a new harvest to be started.
     */
    protected void abortHarvest()
    {
        if (currentHarvestingProcess != null)
            isAborting = true;
    }


    /**
     * Initializes the Harvester, calculating the hash and maximum number of
     * harvestable documents.
     *
     * @param isMainHarvester if true, this is the harvester that can be triggered via REST
     * @param moduleName the name of the harvester service
     * @param harvesterParameters a map of parameters used to initialize the harvester
     */
    public void init(final boolean isMainHarvester, final String moduleName, Map<String, AbstractParameter<?>> harvesterParameters)
    {
        this.isMainHarvester = isMainHarvester;
        this.httpRequester = new HttpRequester(
            getCharset(),
            createGsonBuilder().create(),
            false,
            false,
            String.format(DataOperationConstants.CACHE_FOLDER_PATH, moduleName));

        // prepare documents cache
        String tempPath = String.format(
                              CacheConstants.TEMP_HARVESTER_CACHE_FOLDER_PATH,
                              moduleName,
                              getName());

        String stablePath = String.format(
                                CacheConstants.STABLE_HARVESTER_CACHE_FOLDER_PATH,
                                moduleName,
                                getName());

        this.documentsCache = initCache(tempPath, stablePath);

        // init parameters
        harvesterParameters.forEach((String key, AbstractParameter<?> param) ->
                                    setProperty(key, param.getValue() == null ? null : param.getValue().toString())
                                   );
    }


    /**
     * Calculates the total number of harvested documents.
     *
     * @return the total number of documents that are to be harvested
     */
    protected abstract int initMaxNumberOfDocuments();


    /**
     * Computes a hash value of the files that are to be harvested, which is
     * used for checking if the files have changed.
     *
     * @return a hash as a checksum of the data which is to be harvested
     *
     * @throws NoSuchAlgorithmException occurs if an invalid algorithm is used
     *             for a {@linkplain MessageDigest}
     * @throws NullPointerException occurs for several reasons, depending on the
     *             implementation
     */
    protected abstract String initHash() throws NoSuchAlgorithmException, NullPointerException;


    /**
     * Creates a cache for harvested documents.
     *
     * @param temporaryPath the path to a folder were documents are temporarily stored
     * @param stablePath the path to a folder were documents are permanently stored
     *         when the harvest was successful
     *
     * @return a cache for harvested documents
     */
    protected HarvesterCache initCache(final String temporaryPath, final String stablePath)
    {
        final String harvesterID = onGetDataProviderName(null) + getName();

        final HarvesterCache cache = new HarvesterCache(
            harvesterID,
            temporaryPath,
            stablePath,
            getCharset());

        cache.addEventListeners();

        EventSystem.sendEvent(new RegisterHarvesterCacheEvent(cache));
        return cache;
    }


    /**
     * Updates the cache for harvested documents, if it exists.
     */
    protected void updateCache()
    {
        if (documentsCache != null) {
            // update the harvester hash in the cache file
            final int from = startIndex.get() == Integer.MAX_VALUE ? maxDocumentCount.get() : startIndex.get();
            final int to = endIndex.get() == Integer.MAX_VALUE ? maxDocumentCount.get() : endIndex.get();
            documentsCache.init(hash, from, to);
        }
    }


    /**
     * Updates the harvested source documents, calculating the hash and maximum number of
     * harvestable documents.
     */
    public void update()
    {
        isFailing = false;

        // calculate hash
        try {
            hash = initHash();
        } catch (NoSuchAlgorithmException | NullPointerException e) {
            logger.error(String.format(HarvesterConstants.HASH_CREATION_FAILED, name), e);
            hash = null;
            isFailing = true;
        }

        // calculate number of documents
        int maxHarvestableDocs = initMaxNumberOfDocuments();
        maxDocumentCount.set(maxHarvestableDocs);
        endIndex.set(maxHarvestableDocs);

        // update documents cache
        updateCache();
    }


    /**
     * Retrieves the value of a property.
     *
     * @param key the name of the property
     * @return the property value
     */
    protected String getProperty(String key)
    {
        return properties.get(key);
    }


    /**
     * Sets the value of a property.
     *
     * @param key the property name
     * @param value the new property value
     */
    protected void setProperty(String key, String value)
    {
        switch (key) {
            case ConfigurationConstants.HARVEST_START_INDEX:
                setStartIndex(Integer.parseInt(value));
                break;

            case ConfigurationConstants.HARVEST_END_INDEX:
                setEndIndex(Integer.parseInt(value));
                break;

            case ConfigurationConstants.FORCE_HARVEST:
                setForceHarvest(Boolean.getBoolean(value));
                break;

            default:
                properties.put(key, value);
        }
    }


    /**
     * Adds a document to the search index and logs the progress. If the
     * document is null, it is not added to the search index, but the progress
     * is incremented regardlessly.
     *
     * @param document the document that is to be added to the search index
     */
    protected void addDocument(IDocument document)
    {
        if (document != null) {
            if (document instanceof ICleanable)
                ((ICleanable) document).clean();

            documentsCache.cacheDocument(document, forceHarvest.get());
        }

        EventSystem.sendEvent(DocumentsHarvestedEvent.singleHarvestedDocument());
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
     * @param from the index of the first document to be harvested
     */
    protected void setStartIndex(int from)
    {
        if (startIndex.get() == from)
            return;

        if (from <= 0)
            startIndex.set(0);
        else
            startIndex.set(from);

        // when the range changes, the cache hash will change, too
        updateCache();
    }


    /**
     * Sets the end index of the harvesting range.
     *
     * @param to the index of the first document that is not to be harvested
     *            anymore
     */
    protected void setEndIndex(int to)
    {
        if (endIndex.get() == to)
            return;

        if (to <= 0)
            endIndex.set(0);
        else
            endIndex.set(to);

        // when the range changes, the cache hash will change, too
        updateCache();
    }


    /**
     * Changes the force harvest flag.
     *
     * @param state the new state of the flag
     */
    protected void setForceHarvest(boolean state)
    {
        forceHarvest.set(state);
    }


    /**
     * Starts an asynchronous harvest with the implemented harvestInternal()
     * method and saves the result and date for this session
     */
    protected final void harvest()
    {
        logger.info(String.format(HarvesterConstants.HARVESTER_START, name));

        // update to check if source data has changed
        update();

        // convert max value to what is actually possible
        final int from = startIndex.get() == Integer.MAX_VALUE ? maxDocumentCount.get() : startIndex.get();
        final int to = endIndex.get() == Integer.MAX_VALUE ? maxDocumentCount.get() : endIndex.get();

        if (!forceHarvest.get()) {
            // cancel harvest if the checksum changed since the last harvest
            if (!isOutdated()) {
                logger.info(String.format(HarvesterConstants.HARVESTER_SKIPPED_OUTDATED, name));
                skipAllDocuments();
                return;
            }

            // cancel harvest if previous changes were not submitted
            if (isMainHarvester && MainContext.getTimeKeeper().hasUnsubmittedChanges()) {
                logger.info(String.format(HarvesterConstants.HARVESTER_SKIPPED_SUBMIT, name));
                skipAllDocuments();
                return;
            }
        }

        // only send events from the main harvester
        if (isMainHarvester) {
            EventSystem.addListener(StartAbortingEvent.class, onStartAborting);
            EventSystem.sendEvent(new HarvestStartedEvent(from, to, getHash(false)));
        }

        // start asynchronous harvest
        currentHarvestingProcess = new CancelableFuture<>(() -> harvestInternal(from, to));

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
        if (isMainHarvester) {
            EventSystem.removeListener(StartAbortingEvent.class, onStartAborting);

            // finish caching
            applyCacheChanges();

            // send events
            EventSystem.sendEvent(new HarvestFinishedEvent(true, getHash(false)));
        }

        // dead-lock fix: clear aborting status
        if (isAborting) {
            isAborting = false;

            if (isMainHarvester)
                EventSystem.sendEvent(new AbortingFinishedEvent());
        }
    }


    /**
     * This function is called when an exception occurs during the harvest.
     * Cleans up a failed harvesting process, allowing a new harvest to be
     * started. Also calls a function depending on why the harvest failed.
     *
     * @param reason the exception that caused the harvest to fail
     */
    protected void finishHarvestExceptionally(Throwable reason)
    {
        // clean up the harvesting process reference
        currentHarvestingProcess = null;

        // check if the harvest was aborted
        if (isAborting)
            onHarvestAborted();
        else
            onHarvestFailed(reason);
    }


    /**
     *
     * This method is called after an ongoing harvest failed due to an
     * exception.
     *
     * @param reason the exception that caused the harvest to fail
     */
    protected void onHarvestFailed(Throwable reason)
    {
        isFailing = true;

        // log the error
        logger.error(reason.getMessage(), reason);

        // log failure
        logger.warn(String.format(HarvesterConstants.HARVESTER_FAILED, name));

        if (isMainHarvester) {
            EventSystem.removeListener(StartAbortingEvent.class, onStartAborting);

            // finish caching
            applyCacheChanges();

            // send events
            EventSystem.sendEvent(new HarvestFinishedEvent(false, getHash(false)));
        }
    }


    /**
     * This function is called after the harvesting process was stopped due to
     * being aborted.
     */
    protected void onHarvestAborted()
    {
        if (isMainHarvester) {
            applyCacheChanges();
            EventSystem.sendEvent(new AbortingFinishedEvent());
            EventSystem.sendEvent(new HarvestFinishedEvent(false, getHash(false)));
        }

        isAborting = false;

        logger.warn(String.format(HarvesterConstants.HARVESTER_ABORTED, name));
    }


    /**
     * Returns the checksum hash of the entries which are to be harvested.
     *
     * @param recalculate if true, recalculates the hash value
     * @return the checksum hash of the entries which are to be harvested
     */
    protected String getHash(boolean recalculate)
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


    /**
     * Checks if the data provider has new data.
     *
     * @return true if the previously harvested documents are outdated or the
     *         harvesting range changed
     */
    protected boolean isOutdated()
    {
        return documentsCache.getVersionsCache().isOutdated();
    }


    /**
     * Applies all changes caused by the harvest to the cache.
     */
    protected void applyCacheChanges()
    {
        documentsCache.applyChanges(!isFailing, isAborting);
    }


    /**
     * Skips all documents that are to be harvested.
     */
    protected void skipAllDocuments()
    {
        documentsCache.skipAllDocuments();
        EventSystem.sendEvent(new DocumentsHarvestedEvent(getMaxNumberOfDocuments()));
    }


    /**
     * Creates a GsonBuilder for parsing harvested source data. If you
     * have custom JSON (de-)serialization adapters, you can register them to
     * the GsonBuilder when overriding this method.
     *
     * @see JsonDeserializer
     * @see JsonSerializer
     *
     * @return a GsonBuilder that will be used to parse source data
     */
    protected GsonBuilder createGsonBuilder()
    {
        return GsonUtils.createGerdiDocumentGsonBuilder();
    }


    /**
     * Returns the name of the harvester.
     *
     * @return the name of the harvester
     */
    public String getName()
    {
        return name;
    }


    /**
     * Returns the charset of the harvested data.
     *
     * @return the charset of the harvested data
     */
    public Charset getCharset()
    {
        return StandardCharsets.UTF_8;
    }


    //////////////////////////////
    // Event Callback Functions //
    //////////////////////////////

    /**
     * Event callback for aborting the harvester.
     */
    private final Consumer<StartAbortingEvent> onStartAborting = (StartAbortingEvent e) -> {
        EventSystem.removeListener(StartAbortingEvent.class, this.onStartAborting);
        EventSystem.sendEvent(new AbortingStartedEvent());
        abortHarvest();
    };


    /**
     * Event callback for starting the harvester.
     *
     * @param event the event that triggered this callback function
     */
    private final Consumer<StartHarvestEvent> onStartHarvest = (StartHarvestEvent event) -> {
        harvest();
    };


    /**
     * Event callback for harvester parameter changes. Parameters include the
     * harvesting range, as well as any implementation specific properties.
     *
     * @param event the event that triggered this callback function
     */
    private final Consumer<HarvesterParameterChangedEvent> onParameterChanged = (HarvesterParameterChangedEvent event) -> {
        final String key = event.getParameter().getKey();
        final Object value = event.getParameter().getValue();

        setProperty(key, value == null ? null : value.toString());
    };


    /**
     * Synchronous event callback that returns the name of the data provider
     * that is harvested.
     *
     * @param event the event that triggered this callback function
     *
     * @return the name of the data provider that is harvested
     */
    protected String onGetDataProviderName(GetProviderNameEvent event)
    {
        String name = getClass().getSimpleName();

        // remove HarvesterXXX if it exists within the name
        int harvesterIndex = name.toLowerCase().lastIndexOf(ApplicationConstants.HARVESTER_NAME_SUB_STRING);

        if (harvesterIndex != -1)
            name = name.substring(0, harvesterIndex);

        return name;
    }


    /**
     * Synchronous event callback that returns true if the harvester requires an
     * update.
     *
     * @param event the event that triggered this callback function
     *
     * @return true if the harvester requires an update
     */
    private Boolean onGetHarvesterOutdated(GetHarvesterOutdatedEvent event) // NOPMD events must be defined as parameter, even if not used
    {
        update();
        return isOutdated();
    }


    /**
     * Synchronous event callback for retrieving the max number of harvestable
     * documents.
     *
     * @param event the event that triggered this callback function
     *
     * @return the max number of harvestable documents
     */
    private final Integer onGetMaxDocumentCount(GetMaxDocumentCountEvent event) // NOPMD events must be defined as parameter, even if not used
    {
        int beginning = startIndex.get();
        int end = endIndex.get();

        if (end == Integer.MAX_VALUE)
            end = getMaxNumberOfDocuments();

        return end - beginning;
    }
}
