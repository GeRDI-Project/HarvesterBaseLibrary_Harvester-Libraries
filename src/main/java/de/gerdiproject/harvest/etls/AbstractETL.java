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
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gerdiproject.harvest.application.events.ContextDestroyedEvent;
import de.gerdiproject.harvest.application.events.ResetContextEvent;
import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.events.ParameterChangedEvent;
import de.gerdiproject.harvest.config.parameters.AbstractParameter;
import de.gerdiproject.harvest.config.parameters.BooleanParameter;
import de.gerdiproject.harvest.etls.constants.ETLConstants;
import de.gerdiproject.harvest.etls.enums.ETLHealth;
import de.gerdiproject.harvest.etls.enums.ETLState;
import de.gerdiproject.harvest.etls.events.HarvestFinishedEvent;
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
import de.gerdiproject.harvest.etls.utils.ETLManager;
import de.gerdiproject.harvest.etls.utils.TimestampedEntry;
import de.gerdiproject.harvest.etls.utils.TimestampedList;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.IEventListener;
import de.gerdiproject.harvest.utils.HashGenerator;


/**
 * This class offers a skeleton for harvesting a repository to
 * retrieve all of its metadata. The metadata can subsequently be sent to
 * a search index via an {@link ILoader}. This most basic
 * ETL class offers functions that can be controlled via REST requests
 * from the {@link ETLRestResource}, as well as some utility objects that are
 * required by all harvests. Subclasses must implement the concrete harvesting
 * process.
 *
 * @param <T> the type of the extracted source data
 * @param <S> the type of the transformed documents
 *
 * @author Robin Weiss
 */
@SuppressWarnings("PMD.GodClass")
public abstract class AbstractETL <T, S> implements IEventListener
{
    protected IExtractor<T> extractor;
    protected ITransformer<T, S> transformer;
    protected ILoader<S> loader;

    protected volatile BooleanParameter enabledParameter;

    private AtomicInteger maxDocumentCount;

    protected final Logger logger; // NOPMD - we want to retrieve the type of the inheriting class
    protected String name;
    protected volatile String hash;

    protected final TimestampedList<ETLHealth> healthHistory;
    protected final TimestampedList<ETLState> stateHistory;

    // event listener callback functions
    private final Consumer<ResetContextEvent> onResetContextCallback = this::onResetContext;
    private final Consumer<HarvestFinishedEvent> onHarvestFinishedCallback = this::onHarvestFinished;
    private final Consumer<ParameterChangedEvent> onParameterChangedCallback = this::onParameterChanged;
    private final Consumer<ContextDestroyedEvent> onContextDestroyedCallback = this::onContextDestroyed;


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
    public AbstractETL(final String name)
    {
        this.stateHistory = new TimestampedList<>(ETLState.INITIALIZING, 10);
        this.healthHistory = new TimestampedList<>(ETLHealth.OK, 1);

        // set the name to camel case
        this.name = name == null ? getClass().getSimpleName() : name;

        this.logger = LoggerFactory.getLogger(getName());
        this.maxDocumentCount = new AtomicInteger(0);
    }


    /**
     * Creates an {@linkplain IExtractor} for retrieving elements from
     * the harvested repository to be used by the {@linkplain ITransformer}.
     *
     * @return an {@linkplain IExtractor} for retrieving elements from
     * the harvested repository
     */
    protected abstract IExtractor<T> createExtractor();


    /**
     * Creates an {@linkplain ITransformer} for transforming source elements
     * to documents that can be used by the {@linkplain ILoader}.
     *
     * @return {@linkplain ITransformer} for transforming source elements
     */
    protected abstract ITransformer<T, S> createTransformer();


    /**
     * Creates an {@linkplain ILoader} for sending the harvested documents
     * to a search index.
     *
     * @return an {@linkplain ILoader} for sending the harvested documents
     */
    @SuppressWarnings("unchecked") // NOPMD the possible ClassCastException is caught
    protected ILoader<S> createLoader()
    {
        try {
            return (ILoader<S>) EventSystem.sendSynchronousEvent(new CreateLoaderEvent());

        } catch (final ClassCastException e) {
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
        EventSystem.addListener(ResetContextEvent.class, onResetContextCallback);
        EventSystem.addListener(ContextDestroyedEvent.class, onContextDestroyedCallback);
        EventSystem.addListener(ParameterChangedEvent.class, onParameterChangedCallback);
        EventSystem.addListener(HarvestFinishedEvent.class, onHarvestFinishedCallback);
    }


    @Override
    public void removeEventListeners()
    {
        EventSystem.removeListener(ResetContextEvent.class, onResetContextCallback);
        EventSystem.removeListener(ContextDestroyedEvent.class, onContextDestroyedCallback);
        EventSystem.removeListener(ParameterChangedEvent.class, onParameterChangedCallback);
        EventSystem.removeListener(HarvestFinishedEvent.class, onHarvestFinishedCallback);
    }


    /**
     * Loads the state from a JSON representation of this ETL.
     *
     * @param json a simplified JSON representation of the ETL
     */
    public void loadFromJson(final ETLJson json)
    {
        this.stateHistory.addAllSorted(json.getStateHistory());

        // if the loaded health indicates a harvesting failure, make sure to persist it
        final List<TimestampedEntry<ETLHealth>> loadedHealthHistory = json.getHealthHistory();

        if (loadedHealthHistory != null
            && getHealth() == ETLHealth.OK
            && loadedHealthHistory.get(0).getValue() != ETLHealth.INITIALIZATION_FAILED) {
            this.healthHistory.clear();
            this.healthHistory.addAllSorted(loadedHealthHistory);
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
                   stateHistory,
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
        switch (getState()) {
            case HARVESTING:
                setStatus(ETLState.ABORTING);
                break;

            case QUEUED:
                cancelHarvest();
                setStatus(ETLState.DONE);
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
        if (getState() != ETLState.INITIALIZING)
            throw new IllegalStateException(ETLConstants.INIT_INVALID_STATE);

        registerParameters();

        this.extractor = createExtractor();
        this.transformer = createTransformer();
        this.loader = createLoader();

        setStatus(ETLState.IDLE);
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
     * @return a hash as a checksum of the data which is to be harvested,
     * or null if no hash could be retrieved
     */
    protected String initHash()
    {
        if (extractor == null)
            return null;

        final String versionString = extractor.getUniqueVersionString();

        if (versionString == null)
            return null;

        final HashGenerator generator = new HashGenerator(StandardCharsets.UTF_8);
        return generator.getShaHash(versionString);
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
            extractor.init(this);
        } catch (final ETLPreconditionException e) { // NOPMD forward precondition exceptions
            throw e;

        } catch (final RuntimeException e) { // NOPMD wrap any other exception
            throw new ETLPreconditionException(String.format(ETLConstants.EXTRACTOR_CREATE_ERROR, getName()), e);
        }

        // calculate hash
        hash = initHash();

        // calculate number of documents
        maxDocumentCount.set(initMaxNumberOfDocuments());
    }


    /**
     * Returns the total number of documents that can possibly be harvested.
     *
     * @return the total number of documents that can possibly be harvested
     */
    public int getMaxNumberOfDocuments()
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
        setStatus(ETLState.QUEUED);
        setHealth(ETLHealth.OK);

        if (!enabledParameter.getValue()) {
            skipHarvest();

            throw new ETLPreconditionException(
                String.format(ETLConstants.ETL_SKIPPED_DISABLED, getName()));
        }

        // this string contains a descriptive error message and will only be used if an exception occurs
        String errorMessage = ETLConstants.UPDATE_ERROR;

        try {
            // update to check if source data has changed
            update();

            // initialize transformer
            errorMessage = ETLConstants.TRANSFORMER_CREATE_ERROR;
            transformer.init(this);

            // initialize loader
            errorMessage = ETLConstants.LOADER_CREATE_ERROR;
            loader.init(this);

        } catch (final ETLPreconditionException e) {
            // update ETL status
            setStatus(ETLState.DONE);
            setHealth(ETLHealth.HARVEST_FAILED);
            throw e;

        } catch (final RuntimeException e) { // NOPMD catch any runtime exception to improve stability
            // update ETL status
            setStatus(ETLState.DONE);
            setHealth(ETLHealth.HARVEST_FAILED);

            // log the failure
            logger.error(String.format(ETLConstants.ETL_START_FAILED, getName()), e);

            // wrap exception into a Precondition exception
            throw new ETLPreconditionException(String.format(errorMessage, getName()), e);
        }
    }


    /**
     * Cancels the ETL if it is queued to harvest,
     * cleaning up readers and writers if necessary.
     */
    public void cancelHarvest()
    {
        switch (getState()) {
            case HARVESTING:
            case QUEUED:
                setStatus(ETLState.CANCELLING);
                loader.clear();
                transformer.clear();
                extractor.clear();
                setStatus(ETLState.DONE);
                break;

            default:
                // do nothing
        }
    }


    /**
     * Marks the harvest as done.
     */
    protected void skipHarvest()
    {
        setHealth(ETLHealth.OK);
        setStatus(ETLState.DONE);
    }


    /**
     * Starts the harvest.
     */
    public final void harvest()
    {
        try {
            logger.info(String.format(ETLConstants.ETL_STARTED, getName()));
            setStatus(ETLState.HARVESTING);

            final T exOut = extractor.extract();
            final S transOut = transformer.transform(exOut);
            loader.load(transOut);

            // clear up temporary variables and readers
            loader.clear();
            transformer.clear();
            extractor.clear();

            if (getState() == ETLState.ABORTING)
                logger.info(String.format(ETLConstants.ETL_ABORTED, getName()));
            else
                logger.info(String.format(ETLConstants.ETL_FINISHED, getName()));

            setStatus(ETLState.DONE);
        } catch (final RuntimeException e) { // NOPMD catch any runtime exception to improve stability
            finishHarvestExceptionally(e);
        }
    }


    /**
    * This method is called after an ongoing harvest failed due to an
    * exception.
    *
    * @param reason the exception that caused the harvest to fail
    */
    protected void finishHarvestExceptionally(final Throwable reason)
    {
        if (getState() == ETLState.ABORTING)
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

        loader.clear();
        transformer.clear();
        extractor.clear();

        setStatus(ETLState.DONE);
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
    public ETLState getState()
    {
        return stateHistory.getLatestValue();
    }


    /**
     * Changes the status that represents what the ETL is currently doing.
     * @param state a new status
     */
    public void setStatus(final ETLState state)
    {
        this.stateHistory.addValue(state);
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
    public void setHealth(final ETLHealth health)
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
     * Changes the name of the ETL.
     * This method should not be called manually.
     * It is required by the {@linkplain ETLManager}.
     *
     * @param name the new name of the ETL
     */
    public final void setName(final String name)
    {
        this.name = name;
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
        return enabledParameter != null && enabledParameter.getValue() && getHealth() != ETLHealth.INITIALIZATION_FAILED;
    }


    @Override
    public String toString()
    {
        final ETLState state = getState();
        final String etlStatus;

        // check if the ETL is enabled
        if (enabledParameter != null && enabledParameter.getValue()) {
            final StringBuilder sb = new StringBuilder(state.toString().toLowerCase(Locale.ENGLISH));

            // add more info if the ETL is harvesting
            if (state == ETLState.HARVESTING) {
                final int currCount = getHarvestedCount();
                final int maxCount = getMaxNumberOfDocuments();

                if (maxCount == -1)
                    sb.append(String.format(ETLConstants.PROGRESS_NO_BOUNDS, currCount));
                else {
                    sb.append(String.format(
                                  ETLConstants.PROGRESS,
                                  Math.round(100f * currCount / maxCount),
                                  currCount,
                                  maxCount));
                }
            }

            etlStatus = sb.toString();

        } else
            etlStatus = ETLConstants.ETL_DISABLED;

        return String.format(ETLConstants.ETL_PRETTY, getName(), etlStatus, getHealth().toString());
    }


    //////////////////////////////
    // Event Callback Functions //
    //////////////////////////////

    /**
     * The implementation of the service reset callback.
     * Attempts to cancel ongoing harvests.
     *
     * @param event the event that triggered the callback
     */
    protected void onResetContext(final ResetContextEvent event)
    {
        cancelHarvest();
    }


    /**
     * The implementation of the harvest finished callback.
     * Sets the status to IDLE if it is DONE.
     *
     * @param event the event that triggered the callback
     */
    protected void onHarvestFinished(final HarvestFinishedEvent event)
    {
        if (getState() == ETLState.DONE)
            setStatus(ETLState.IDLE);
    }


    /**
     * The implementation of the parameter changed callback.
     *
     * @param event the event that triggered the callback
     */
    protected void onParameterChanged(final ParameterChangedEvent event)
    {
        final AbstractParameter<?> param = event.getParameter();

        if (param.getKey().equals(LoaderConstants.LOADER_TYPE_PARAM_KEY)
            && param.getCategory().equals(LoaderConstants.PARAMETER_CATEGORY)) {

            if (this.loader != null)
                this.loader.unregisterParameters();

            this.loader = createLoader();
        }
    }


    /**
     * The implementation of the context destroyed callback.
     *
     * @param event the event that triggered the callback
     */
    protected void onContextDestroyed(final ContextDestroyedEvent event)
    {
        cancelHarvest();
    }

}
