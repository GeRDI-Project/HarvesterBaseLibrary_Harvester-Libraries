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
package de.gerdiproject.harvest.etls;


import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.config.events.ParameterChangedEvent;
import de.gerdiproject.harvest.config.parameters.AbstractParameter;
import de.gerdiproject.harvest.config.parameters.BooleanParameter;
import de.gerdiproject.harvest.etls.constants.ETLConstants;
import de.gerdiproject.harvest.etls.enums.ETLHealth;
import de.gerdiproject.harvest.etls.enums.ETLStatus;
import de.gerdiproject.harvest.etls.events.DocumentsHarvestedEvent;
import de.gerdiproject.harvest.etls.extractors.ExtractorException;
import de.gerdiproject.harvest.etls.extractors.IExtractor;
import de.gerdiproject.harvest.etls.json.ETLJson;
import de.gerdiproject.harvest.etls.loaders.ILoader;
import de.gerdiproject.harvest.etls.loaders.LoaderException;
import de.gerdiproject.harvest.etls.loaders.constants.LoaderConstants;
import de.gerdiproject.harvest.etls.loaders.events.CreateLoaderEvent;
import de.gerdiproject.harvest.etls.rest.ETLRestResource;
import de.gerdiproject.harvest.etls.transformers.ITransformer;
import de.gerdiproject.harvest.etls.transformers.TransformerException;
import de.gerdiproject.harvest.etls.utils.TimestampedList;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.IEventListener;
import de.gerdiproject.harvest.utils.HashGenerator;


/**
 * This class offers a skeleton for harvesting a repository to
 * retrieve all of its metadata. The metadata can subsequently be submitted to
 * a search index via an {@link ILoader}. This most basic
 * ETL class offers functions that can be controlled via REST requests
 * from the {@link ETLRestResource}, as well as some utility objects that are
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

    protected volatile BooleanParameter enabledParameter;

    private AtomicInteger maxDocumentCount;

    protected final Logger logger; // NOPMD - we want to retrieve the type of the inheriting class
    protected final String name;
    protected volatile String hash;

    protected volatile TimestampedList<ETLHealth> healthHistory;
    protected volatile TimestampedList<ETLStatus> statusHistory;


    /**
     * Constructor that initializes helper classes and fields.
     * And uses the class name as ETL name.
     */
    public AbstractETL()
    {
        this(null);
    }


    /**
     * Constructor that initializes helper classes and fields.
     *
     * @param name the name of this ETL
     */
    public AbstractETL(String name)
    {
        this.statusHistory = new TimestampedList<>(ETLStatus.INITIALIZING, 10);
        this.healthHistory = new TimestampedList<>(ETLHealth.OK, 1);

        // set the name to camel case
        this.name = name != null
                    ? name.replaceAll(ConfigurationConstants.INVALID_PARAM_NAME_REGEX, "")
                    : getClass().getSimpleName();

        this.logger = LoggerFactory.getLogger(getName());
        this.maxDocumentCount = new AtomicInteger(0);

        registerParameters();

        this.extractor = createExtractor();
        this.transformer = createTransformer();
        this.loader = createLoader();
    }


    /**
     * Creates an {@linkplain IExtractor} for retrieving elements from
     * the harvested repository.
     *
     * @return an {@linkplain IExtractor} for retrieving elements from
     * the harvested repository
     */
    protected abstract IExtractor<EOUT> createExtractor();


    /**
     * Creates an {@linkplain ITransformer} for transforming source elements
     * to documents that can be submitted.
     *
     * @return {@linkplain ITransformer} for transforming source elements
     */
    protected abstract ITransformer<EOUT, TOUT> createTransformer();


    /**
     * Creates an {@linkplain ILoader} for submitting the harvested documents
     * to a search index.
     *
     * @return an {@linkplain ILoader} for submitting the harvested documents
     */
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
     * Registers all configurable parameters.
     */
    protected void registerParameters()
    {
        this.enabledParameter =
            Configuration.registerParameter(new BooleanParameter(
                                                ETLConstants.ENABLED_PARAM.getKey(),
                                                getName(),
                                                ETLConstants.ENABLED_PARAM.getValue(),
                                                ETLConstants.ENABLED_PARAM.getMappingFunction()));
    }


    @Override
    public void addEventListeners()
    {
        EventSystem.addListener(ParameterChangedEvent.class, onParameterChangedCallback);
    }


    @Override
    public void removeEventListeners()
    {
        EventSystem.removeListener(ParameterChangedEvent.class, onParameterChangedCallback);
    }


    /**
     * Loads the state from a JSON representation of this ETL.
     *
     * @param json a simplified JSON representation of the ETL
     */
    public void loadFromJson(ETLJson json)
    {
        this.statusHistory.addAllSorted(json.getStatusHistory());

        // if the loaded health indicates a harvesting failure, make sure to persist it
        if (getHealth() == ETLHealth.OK && json.getHealthHistory().get(0).getValue() != ETLHealth.INITIALIZATION_FAILED) {
            this.healthHistory.clear();
            this.healthHistory.addAllSorted(json.getHealthHistory());
        }
    }


    /**
     * Returns a simplified JSON representation of the ETL.
     *
     * @return a simplified JSON representation of the ETL
     */
    public ETLJson getAsJson()
    {
        return new ETLJson(
                   getName(),
                   statusHistory,
                   healthHistory,
                   getHarvestedCount(),
                   getMaxNumberOfDocuments(),
                   getHash());
    }


    /**
     * Aborts the harvesting process, allowing a new harvest to be started.
     *
     * @throws IllegalStateException if the current process cannot be aborted
     */
    public void abortHarvest() throws IllegalStateException
    {
        switch (getStatus()) {
            case HARVESTING:
                setStatus(ETLStatus.ABORTING);
                break;

            case QUEUED:
                setStatus(ETLStatus.DONE);
                break;

            default:
                // nothing to abort
        }
    }


    /**
     * Initializes the ETL, calculating the hash and maximum number of
     * harvestable documents.
     *
     * @param moduleName the name of the harvester service
     * @throws IllegalStateException thrown if init is called after the ETL is initialized
     */
    public void init(final String moduleName) throws IllegalStateException
    {
        if (getStatus() != ETLStatus.INITIALIZING)
            throw new IllegalStateException(ETLConstants.INIT_INVALID_STATE);

        setStatus(ETLStatus.IDLE);
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
            final HashGenerator generator = new HashGenerator(StandardCharsets.UTF_8);
            return generator.getShaHash(versionString);
        }

        return null;
    }


    /**
     * Updates the harvested source documents, calculating the hash and maximum number of
     * harvestable documents.
     */
    public void update() throws ETLPreconditionException
    {
        // the extractor may need to retrieve information about the repository, initialize it early
        extractor = createExtractor();

        if (extractor == null)
            throw new ETLPreconditionException(String.format(ETLConstants.EXTRACTOR_CREATE_ERROR, getName()));

        extractor.init(this);

        // calculate hash
        try {
            hash = initHash();
        } catch (NullPointerException e) {
            logger.error(String.format(ETLConstants.HASH_CREATION_FAILED, getName()), e);
            hash = null;
        }

        // calculate number of documents
        maxDocumentCount.set(initMaxNumberOfDocuments());
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
        setStatus(ETLStatus.QUEUED);
        setHealth(ETLHealth.OK);

        if (!enabledParameter.getValue()) {
            skipHarvest();

            throw new ETLPreconditionException(
                String.format(ETLConstants.ETL_SKIPPED_DISABLED, getName()));
        }

        try {
            // update to check if source data has changed
            update();

            // loader and transformer only become relevant when the harvest is about to start, load them now
            if (transformer == null)
                throw new ETLPreconditionException(String.format(ETLConstants.TRANSFORMER_CREATE_ERROR, getName()));

            if (loader == null)
                throw new ETLPreconditionException(String.format(ETLConstants.LOADER_CREATE_ERROR, getName()));

            transformer.init(this);
            loader.init(this);

        } catch (ETLPreconditionException e) {
            setStatus(ETLStatus.DONE);
            setHealth(ETLHealth.HARVEST_FAILED);
            throw e;

        } catch (Exception e) {
            setStatus(ETLStatus.DONE);
            setHealth(ETLHealth.HARVEST_FAILED);

            logger.error(String.format(ETLConstants.ETL_START_FAILED, getName()), e);
            throw new ETLPreconditionException(e.getMessage());
        }
    }


    /**
     * Cancels the ETL if it is queued to harvest,
     * cleaning up readers and writers if necessary.
     */
    public void cancelHarvest()
    {
        if (getStatus() != ETLStatus.DONE) {
            setStatus(ETLStatus.CANCELLING);
            loader.clear();
            transformer.clear();
            extractor.clear();
            setStatus(ETLStatus.DONE);
        }
    }


    /**
     * Marks the harvest as done.
     */
    protected void skipHarvest()
    {
        setHealth(ETLHealth.OK);
        setStatus(ETLStatus.DONE);
        EventSystem.sendEvent(new DocumentsHarvestedEvent(getMaxNumberOfDocuments()));
    }


    /**
     * Starts the harvest.
     */
    public final void harvest()
    {
        try {
            logger.info(String.format(ETLConstants.ETL_STARTED, getName()));
            setStatus(ETLStatus.HARVESTING);

            final EOUT exOut = extractor.extract();
            final TOUT transOut = transformer.transform(exOut);
            loader.load(transOut);

            // clear up temporary variables and readers
            loader.clear();
            transformer.clear();
            extractor.clear();

            if (getStatus() == ETLStatus.ABORTING)
                logger.info(String.format(ETLConstants.ETL_ABORTED, getName()));
            else
                logger.info(String.format(ETLConstants.ETL_FINISHED, getName()));

            setStatus(ETLStatus.DONE);
        } catch (Exception e) {
            finishHarvestExceptionally(e);
        }
    }


    /**
    * This method is called after an ongoing harvest failed due to an
    * exception.
    *
    * @param reason the exception that caused the harvest to fail
    */
    protected void finishHarvestExceptionally(Throwable reason)
    {
        if (getStatus() == ETLStatus.ABORTING)
            logger.info(String.format(ETLConstants.ETL_ABORTED, getName()));
        else {
            if (reason instanceof ExtractorException)
                setHealth(ETLHealth.EXTRACTION_FAILED);

            else if (reason instanceof TransformerException)
                setHealth(ETLHealth.TRANSFORMATION_FAILED);

            else if (reason instanceof LoaderException)
                setHealth(ETLHealth.LOADING_FAILED);

            else
                setHealth(ETLHealth.HARVEST_FAILED);

            // log the error
            logger.error(reason.getMessage(), reason);

            // log failure
            logger.warn(String.format(ETLConstants.ETL_FAILED, getName()));
        }

        setStatus(ETLStatus.DONE);
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
     * Returns an enum that represents what the ETL is currently doing.
     *
     * @return an enum that represents the state of the ETL
     */
    public ETLStatus getStatus()
    {
        return statusHistory.getLatestValue();
    }


    /**
     * Changes the status that represents what the ETL is currently doing.
     * @param status a new status
     */
    public void setStatus(ETLStatus status)
    {
        this.statusHistory.addValue(status);
    }


    /**
     * Returns an enum that represents the health status of the ETL.
     *
     * @return an enum that represents the health status of the ETL
     */
    public ETLHealth getHealth()
    {
        return healthHistory.getLatestValue();
    }


    /**
     * Changes the health status.
     * @param health the new health status value
     */
    public void setHealth(ETLHealth health)
    {
        this.healthHistory.addValue(health);
    }


    /**
     * Retrieves the number of documents that have been loaded.
     *
     * @return the number of documents that have been loaded
     */
    public abstract int getHarvestedCount();


    /**
     * Returns the name of the ETL.
     *
     * @return the name of the ETL
     */
    public final String getName()
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


    /**
     * Returns true if this ETL is enabled and initialized.
     *
     * @return true if this ETL is enabled and initialized
     */
    public boolean isEnabled()
    {
        return enabledParameter.getValue() && getHealth() != ETLHealth.INITIALIZATION_FAILED;
    }


    //////////////////////////////
    // Event Callback Functions //
    //////////////////////////////

    @Override
    public String toString()
    {
        String etlStatus = getStatus().toString().toLowerCase();

        if (!enabledParameter.getValue())
            etlStatus = ETLConstants.ETL_DISABLED;
        else if (getStatus() == ETLStatus.HARVESTING) {
            final int currCount = getHarvestedCount();
            final int maxCount = getMaxNumberOfDocuments();

            if (maxCount != -1)
                etlStatus += String.format(ETLConstants.PROGRESS, Math.round(100f * currCount / maxCount), currCount, maxCount);
            else
                etlStatus += String.format(ETLConstants.PROGRESS_NO_BOUNDS, currCount);
        }

        return String.format(ETLConstants.ETL_PRETTY, getName(), etlStatus);
    }



    /**
     * Event callback for changing the loader.
     */
    private final Consumer<ParameterChangedEvent> onParameterChangedCallback =
        (ParameterChangedEvent event) -> onParameterChanged(event.getParameter());


    /**
     * The implementation of the parameter changed callback.
     *
     * @param param the parameter that has changed
     */
    protected void onParameterChanged(AbstractParameter<?> param)
    {
        if (param.getKey().equals(LoaderConstants.LOADER_TYPE_PARAM_KEY)
            && param.getCategory().equals(LoaderConstants.PARAMETER_CATEGORY)) {

            if (this.loader != null)
                this.loader.unregisterParameters();

            this.loader = createLoader();
        }
    }
}
