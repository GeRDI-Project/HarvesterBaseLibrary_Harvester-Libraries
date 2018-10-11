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
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;

import de.gerdiproject.harvest.ICleanable;
import de.gerdiproject.harvest.IDocument;
import de.gerdiproject.harvest.application.enums.HealthStatus;
import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.parameters.BooleanParameter;
import de.gerdiproject.harvest.config.parameters.IntegerParameter;
import de.gerdiproject.harvest.config.parameters.ParameterCategory;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.IEventListener;
import de.gerdiproject.harvest.harvester.constants.HarvesterConstants;
import de.gerdiproject.harvest.harvester.enums.HarvesterStatus;
import de.gerdiproject.harvest.harvester.events.DocumentsHarvestedEvent;
import de.gerdiproject.harvest.harvester.rest.HarvesterRestResource;
import de.gerdiproject.harvest.state.events.StartAbortingEvent;
import de.gerdiproject.harvest.submission.elasticsearch.ElasticSearchSubmitter;
import de.gerdiproject.harvest.utils.HashGenerator;
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
 * from the {@link HarvesterRestResource}, as well as some utility objects that are
 * required by all harvests. Subclasses must implement the concrete harvesting
 * process.
 *
 * @author Robin Weiss
 */
public abstract class AbstractETL <EOUT, TOUT, E extends IExtractor<EOUT>, T extends ITransformer<EOUT, TOUT>, L extends ILoader<TOUT>> implements IEventListener
{
    protected final E extractor;
    protected final T transformer;
    protected final L loader;
    protected String targetUrl;

    protected final ParameterCategory harvesterCategory;
    protected volatile BooleanParameter forceHarvestParameter;
    protected volatile IntegerParameter startIndexParameter;
    protected volatile IntegerParameter endIndexParameter;
    protected volatile BooleanParameter enableHarvesterParameter;
    protected volatile BooleanParameter cacheParameter;

    private volatile int maxDocumentCount;
    private volatile HarvesterCache documentsCache;

    protected final Logger logger; // NOPMD - we want to retrieve the type of the inheriting class
    protected final HttpRequester httpRequester;
    protected volatile boolean isAborting;
    protected volatile boolean isFailing;
    protected volatile String hash;

    protected final String name;

    protected volatile HealthStatus health;
    protected volatile HarvesterStatus status;


    /**
     * Simple constructor that uses the class name as the harvester name.
     *
     * @param extractor retrieves an object from the harvested repository
     * @param transformer transforms the extracted object to a document that can be put to the search index
     * @param loader submits the transformed object to a search index
     */
    public AbstractETL(E extractor, T transformer, L loader)
    {
        this(null, extractor, transformer, loader);
    }


    /**
     * Constructor that initializes helper classes and fields.
     *
     * @param harvesterName a unique name that describes the harvester
     * @param extractor retrieves an object from the harvested repository
     * @param transformer transforms the extracted object to a document that can be put to the search index
     * @param loader submits the transformed object to a search index
     */
    public AbstractETL(String harvesterName, E extractor, T transformer, L loader)
    {
        this.status = HarvesterStatus.BUSY;
        this.health = HealthStatus.OK;

        this.name = (harvesterName != null) ? harvesterName : getClass().getSimpleName();
        this.logger = LoggerFactory.getLogger(name);

        this.extractor = extractor;
        this.transformer = transformer;
        this.loader = loader;

        this.maxDocumentCount = 0;

        this.harvesterCategory = new ParameterCategory(
            name,
            HarvesterConstants.PARAMETER_CATEGORY.getAllowedStates());

        registerParameters();

        this.httpRequester = new HttpRequester(createGsonBuilder().create(), getCharset());
    }


    /**
     * Registers all configurable parameters.
     */
    protected void registerParameters()
    {
        this.startIndexParameter =
            Configuration.registerParameter(new IntegerParameter(
                                                HarvesterConstants.START_INDEX_PARAM.getKey(),
                                                harvesterCategory));

        this.endIndexParameter =
            Configuration.registerParameter(new IntegerParameter(
                                                HarvesterConstants.END_INDEX_PARAM.getKey(),
                                                harvesterCategory));

        this.enableHarvesterParameter =
            Configuration.registerParameter(new BooleanParameter(
                                                HarvesterConstants.ENABLED_PARAM.getKey(),
                                                harvesterCategory));

        this.cacheParameter = Configuration.registerParameter(HarvesterConstants.CACHE_PARAM);

        // all harvesters share the 'forced' parameter
        this.forceHarvestParameter = Configuration.registerParameter(HarvesterConstants.FORCED_PARAM);

    }


    @Override
    public void addEventListeners()
    {
    }


    @Override
    public void removeEventListeners()
    {
        EventSystem.removeListener(StartAbortingEvent.class, onStartAborting);
    }


    /**
     * Aborts the harvesting process, allowing a new harvest to be started.
     */
    public void abortHarvest()
    {
        if (status == HarvesterStatus.HARVESTING) {
            this.status = HarvesterStatus.ABORTING;
            isAborting = true;
        }
    }


    /**
     * Initializes the Harvester, calculating the hash and maximum number of
     * harvestable documents.
     *
     * @param moduleName the name of the harvester service
     */
    public void init(final String moduleName)
    {
        this.httpRequester.setCacheFolder(
            String.format(DataOperationConstants.CACHE_FOLDER_PATH, moduleName)
        );

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
    }


    /**
     * Calculates the total number of harvested documents.
     *
     * @return the total number of documents that are to be harvested
     */
    protected int initMaxNumberOfDocuments()
    {
        return extractor.size();
    }


    /**
     * Computes a hash value of the files that are to be harvested, which is
     * used for checking if the files have changed.
     *
     * @return a hash as a checksum of the data which is to be harvested
     */
    protected String initHash()
    {
        final String versionString = extractor.getUniqueVersionString();

        if (versionString != null) {
            final HashGenerator generator = new HashGenerator(getCharset());
            return generator.getShaHash(versionString);
        }

        return null;
    }


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
        final HarvesterCache cache = new HarvesterCache(
            getName(),
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
            documentsCache.init(hash, getStartIndex(), getEndIndex());
        }
    }


    /**
     * Updates the harvested source documents, calculating the hash and maximum number of
     * harvestable documents.
     */
    public void update()
    {
        final HarvesterStatus previousStatus = status;
        this.status = HarvesterStatus.BUSY;

        extractor.init();
        transformer.init();
        loader.init();

        isFailing = false;

        // calculate hash
        try {
            hash = initHash();
        } catch (NullPointerException e) {
            logger.error(String.format(HarvesterConstants.HASH_CREATION_FAILED, name), e);
            hash = null;
            isFailing = true;
        }

        // calculate number of documents
        maxDocumentCount = initMaxNumberOfDocuments();

        // update documents cache
        updateCache();

        this.status = previousStatus;
    }


    /**
     * Returns start index 'a' of the harvesting range [a,b).
     *
     * @return the start index of the harvesting range
     */
    protected int getStartIndex()
    {
        int index = startIndexParameter.getValue();

        if (index < 0)
            return 0;

        if (index == Integer.MAX_VALUE)
            return maxDocumentCount;

        return index;
    }

    /**
     * Returns the end index 'b' of the harvesting range [a,b).
     *
     * @return the end index of the harvesting range
     */
    protected int getEndIndex()
    {
        int index = endIndexParameter.getValue();

        if (index < 0)
            return 0;

        if (index == Integer.MAX_VALUE)
            return maxDocumentCount;

        return index;
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

            documentsCache.cacheDocument(document, forceHarvestParameter.getValue());
        }

        EventSystem.sendEvent(DocumentsHarvestedEvent.singleHarvestedDocument());
    }


    /**
     * Returns the total number of documents that can possibly be harvested.
     *
     * @return the total number of documents that can possibly be harvested
     */
    public final int getMaxNumberOfDocuments()
    {
        return maxDocumentCount;
    }

    /**
     * Checks pre-conditions required for starting a harvest and updates the data
     * that is to be extracted.
     *
     * @throws ETLPreconditionException thrown if the harvest cannot start
     */
    public void prepareHarvest() throws ETLPreconditionException
    {
        this.status = HarvesterStatus.BUSY;

        if (!enableHarvesterParameter.getValue()) {
            skipAllDocuments();
            this.health = HealthStatus.OK;
            this.status = HarvesterStatus.DONE;

            throw new ETLPreconditionException(
                String.format(HarvesterConstants.HARVESTER_SKIPPED_DISABLED, name));
        }

        // update to check if source data has changed
        update();

        if (!forceHarvestParameter.getValue()) {
            // cancel harvest if the checksum has not changed since the last harvest
            if (!isOutdated()) {
                skipAllDocuments();
                this.health = HealthStatus.OK;
                this.status = HarvesterStatus.DONE;

                throw new ETLPreconditionException(
                    String.format(HarvesterConstants.HARVESTER_SKIPPED_NO_CHANGES, name));
            }
        }

        if (getStartIndex() == getEndIndex()) {
            throw new ETLPreconditionException(
                String.format(HarvesterConstants.HARVESTER_SKIPPED_OUT_OF_RANGE, name));
        }

        this.status = HarvesterStatus.HARVESTING;
    }


    /**
     * Starts the harvest.
     */
    public final void harvest()
    {
        EventSystem.addListener(StartAbortingEvent.class, onStartAborting);
        this.status = HarvesterStatus.HARVESTING;

        logger.info(String.format(HarvesterConstants.HARVESTER_START, getName()));

        // start harvest
        try {
            harvestInternal();
            finishHarvestSuccessfully();
        } catch (Exception e) {
            finishHarvestExceptionally(e);
        }
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
    protected void harvestInternal() throws Exception // NOPMD - we want the inheriting class to be able to throw any exception
    {
        final EOUT exOut = extractor.extract();
        final TOUT transOut = transformer.transform(exOut);
        loader.load(transOut);
    }


    /**
     * Finishes the harvesting process, allowing a new harvest to be started.
     */
    protected void finishHarvestSuccessfully()
    {
        this.health = HealthStatus.OK;
        this.status = HarvesterStatus.BUSY;
        EventSystem.removeListener(StartAbortingEvent.class, onStartAborting);

        logger.info(String.format(HarvesterConstants.HARVESTER_END, name));

        applyCacheChanges();

        // dead-lock fix: clear aborting status
        if (isAborting) {
            isAborting = false;

            // TODO EventSystem.sendEvent(new AbortingFinishedEvent(isMainHarvester));
        }

        this.status = HarvesterStatus.DONE;
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
        EventSystem.removeListener(StartAbortingEvent.class, onStartAborting);

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
        this.health = HealthStatus.HARVEST_FAILED;
        this.status = HarvesterStatus.BUSY;

        isFailing = true;

        // log the error
        logger.error(reason.getMessage(), reason);

        // log failure
        logger.warn(String.format(HarvesterConstants.HARVESTER_FAILED, name));

        // finish caching
        applyCacheChanges();

        this.status = HarvesterStatus.DONE;
    }


    /**
     * This function is called after the harvesting process was stopped due to
     * being aborted.
     */
    protected void onHarvestAborted()
    {
        this.health = HealthStatus.HARVEST_FAILED;
        this.status = HarvesterStatus.ABORTING;

        applyCacheChanges();
        // TODO EventSystem.sendEvent(new AbortingFinishedEvent(isMainHarvester));

        isAborting = false;

        logger.warn(String.format(HarvesterConstants.HARVESTER_ABORTED, name));

        this.status = HarvesterStatus.DONE;
    }



    /**
     * Returns the checksum hash of the documents which are to be harvested.
     *
     * @return the checksum hash of the documents which are to be harvested
     */
    public String getHash()
    {
        return hash;
    }


    /**
     * Checks if the data provider has new data.
     *
     * @return true if the previously harvested documents are outdated or the
     *         harvesting range changed
     */
    public boolean isOutdated()
    {
        return documentsCache.getVersionsCache().isOutdated();
    }


    /**
     * Returns an enum that represents what the harvester is currently doing.
     *
     * @return an enum that represents the state of the harvester
     */
    public HarvesterStatus getStatus()
    {
        return status;
    }


    /**
     * Returns an enum that represents the health status of the harvester.
     *
     * @return an enum that represents the health status of the harvester
     */
    public HealthStatus getHealth()
    {
        return health;
    }


    /**
     * Retrieves the number of documents that have been loaded.
     *
     * @return the number of documents that have been loaded
     */
    public abstract int getHarvestedCount();


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
        HarvesterStatus oldStatus = getStatus();
        this.status = HarvesterStatus.BUSY;

        documentsCache.skipAllDocuments();
        EventSystem.sendEvent(new DocumentsHarvestedEvent(getMaxNumberOfDocuments()));

        this.status = oldStatus;
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
        abortHarvest();
    };
}
