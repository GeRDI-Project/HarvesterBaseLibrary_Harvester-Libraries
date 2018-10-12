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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;

import de.gerdiproject.harvest.application.enums.HealthStatus;
import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.parameters.BooleanParameter;
import de.gerdiproject.harvest.config.parameters.ParameterCategory;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.IEventListener;
import de.gerdiproject.harvest.harvester.constants.HarvesterConstants;
import de.gerdiproject.harvest.harvester.enums.HarvesterStatus;
import de.gerdiproject.harvest.harvester.extractors.IExtractor;
import de.gerdiproject.harvest.harvester.loaders.ElasticSearchLoader;
import de.gerdiproject.harvest.harvester.loaders.ILoader;
import de.gerdiproject.harvest.harvester.loaders.events.CreateLoaderEvent;
import de.gerdiproject.harvest.harvester.rest.HarvesterRestResource;
import de.gerdiproject.harvest.harvester.transformers.ITransformer;
import de.gerdiproject.harvest.state.events.StartAbortingEvent;
import de.gerdiproject.harvest.utils.HashGenerator;
import de.gerdiproject.harvest.utils.cache.HarvesterCache;
import de.gerdiproject.harvest.utils.cache.events.RegisterHarvesterCacheEvent;
import de.gerdiproject.harvest.utils.data.HttpRequester;
import de.gerdiproject.harvest.utils.data.constants.DataOperationConstants;
import de.gerdiproject.json.GsonUtils;


/**
 * AbstractHarvesters offer a skeleton for harvesting a data provider to
 * retrieve all of its metadata. The metadata can subsequently be submitted to
 * ElasticSearch via the {@link ElasticSearchLoader}. This most basic
 * Harvester class offers functions that can be controlled via REST requests
 * from the {@link HarvesterRestResource}, as well as some utility objects that are
 * required by all harvests. Subclasses must implement the concrete harvesting
 * process.
 *
 * @author Robin Weiss
 */
public abstract class AbstractETL <EOUT, TOUT> implements IEventListener
{
    protected IExtractor<EOUT> extractor;
    protected ITransformer<EOUT, TOUT> transformer;
    protected ILoader<TOUT> loader;

    protected final ParameterCategory harvesterCategory;
    protected volatile BooleanParameter forceHarvestParameter;
    protected volatile BooleanParameter enableHarvesterParameter;

    private AtomicInteger maxDocumentCount;

    protected final Logger logger; // NOPMD - we want to retrieve the type of the inheriting class
    protected final HttpRequester httpRequester;
    protected volatile boolean isAborting;
    protected volatile String hash;


    protected volatile HealthStatus health;
    protected volatile HarvesterStatus status;


    /**
     * Constructor that initializes helper classes and fields.
     *
     */
    public AbstractETL()
    {
        this.status = HarvesterStatus.BUSY;
        this.health = HealthStatus.OK;

        this.logger = LoggerFactory.getLogger(getName());

        this.maxDocumentCount = new AtomicInteger(0);

        this.harvesterCategory = new ParameterCategory(
            getName(),
            HarvesterConstants.PARAMETER_CATEGORY.getAllowedStates());

        registerParameters();

        this.httpRequester = new HttpRequester(createGsonBuilder().create(), getCharset());

        this.extractor = createExtractor();
        this.transformer = createTransformer();
        this.loader = createLoader();
    }


    /**
     * Registers all configurable parameters.
     */
    protected void registerParameters()
    {
        this.enableHarvesterParameter =
            Configuration.registerParameter(new BooleanParameter(
                                                HarvesterConstants.ENABLED_PARAM.getKey(),
                                                harvesterCategory,
                                                HarvesterConstants.ENABLED_PARAM.getValue()));

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


    protected abstract IExtractor<EOUT> createExtractor();


    protected abstract ITransformer<EOUT, TOUT> createTransformer();


    @SuppressWarnings("unchecked") // NOPMD the possible ClassCastException is caught
    protected ILoader<TOUT> createLoader()
    {
        try {
            return (ILoader<TOUT>) EventSystem.sendSynchronousEvent(new CreateLoaderEvent());

        } catch (ClassCastException e) {
            logger.error(e.getMessage());
            return null;
        }
    }


    /**
     * Updates the harvested source documents, calculating the hash and maximum number of
     * harvestable documents.
     */
    public void update() throws ETLPreconditionException
    {
        // the extractor may need to retrieve information about the repository, initialize it early
        try {
            extractor = createExtractor();

            if (extractor == null)
                throw new ETLPreconditionException(String.format(HarvesterConstants.EXTRACTOR_CREATE_ERROR, getName()));

            extractor.init(this);
        } catch (IllegalStateException e) {
            throw new ETLPreconditionException(e.getMessage());
        }

        final HarvesterStatus previousStatus = status;
        this.status = HarvesterStatus.BUSY;

        // calculate hash
        try {
            hash = initHash();
        } catch (NullPointerException e) {
            logger.error(String.format(HarvesterConstants.HASH_CREATION_FAILED, getName()), e);
            hash = null;
        }

        // calculate number of documents
        maxDocumentCount.set(initMaxNumberOfDocuments());

        this.status = previousStatus;
    }


    /**
     * Returns the total number of documents that can possibly be harvested.
     *
     * @return the total number of documents that can possibly be harvested
     */
    public final int getMaxNumberOfDocuments()
    {
        return maxDocumentCount.get();
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
            this.health = HealthStatus.OK;
            this.status = HarvesterStatus.DONE;

            throw new ETLPreconditionException(
                String.format(HarvesterConstants.HARVESTER_SKIPPED_DISABLED, getName()));
        }

        // update to check if source data has changed
        update();

        // loader and transformer only become relevant when the harvest is about to start, load them now
        try {
            transformer = createTransformer();

            if (transformer == null)
                throw new ETLPreconditionException(String.format(HarvesterConstants.TRANSFORMER_CREATE_ERROR, getName()));

            transformer.init(this);

            loader = createLoader();

            if (loader == null)
                throw new ETLPreconditionException(String.format(HarvesterConstants.LOADER_CREATE_ERROR, getName()));

            loader.init(this);
        } catch (IllegalStateException e) {
            throw new ETLPreconditionException(e.getMessage());
        }

        // cancel harvest if the checksum has not changed since the last harvest
        if (!forceHarvestParameter.getValue() && !isOutdated()) {
            this.health = HealthStatus.OK;
            this.status = HarvesterStatus.DONE;

            throw new ETLPreconditionException(
                String.format(HarvesterConstants.HARVESTER_SKIPPED_NO_CHANGES, getName()));
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
     * @throws Exception any kind of exception that can occur during the
     *             harvesting process
     */
    protected void harvestInternal() throws Exception // NOPMD - we want the inheriting class to be able to throw any exception
    {
        final EOUT exOut = extractor.extract();
        final TOUT transOut = transformer.transform(exOut);
        loader.load(transOut, true);
    }


    /**
     * Finishes the harvesting process, allowing a new harvest to be started.
     */
    protected void finishHarvestSuccessfully()
    {
        this.health = HealthStatus.OK;
        this.status = HarvesterStatus.BUSY;
        EventSystem.removeListener(StartAbortingEvent.class, onStartAborting);

        logger.info(String.format(HarvesterConstants.HARVESTER_END, getName()));

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

        // log the error
        logger.error(reason.getMessage(), reason);

        // log failure
        logger.warn(String.format(HarvesterConstants.HARVESTER_FAILED, getName()));

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

        // TODO EventSystem.sendEvent(new AbortingFinishedEvent(isMainHarvester));

        isAborting = false;

        logger.warn(String.format(HarvesterConstants.HARVESTER_ABORTED, getName()));

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
        //TODO return documentsCache.getVersionsCache().isOutdated();
        return true;
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
        return getClass().getSimpleName();
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
