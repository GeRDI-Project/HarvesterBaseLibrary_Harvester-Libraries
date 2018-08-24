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
package de.gerdiproject.harvest.submission;

import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gerdiproject.harvest.IDocument;
import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.config.events.GlobalParameterChangedEvent;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.IEventListener;
import de.gerdiproject.harvest.harvester.events.HarvestFinishedEvent;
import de.gerdiproject.harvest.state.events.AbortingFinishedEvent;
import de.gerdiproject.harvest.state.events.AbortingStartedEvent;
import de.gerdiproject.harvest.state.events.StartAbortingEvent;
import de.gerdiproject.harvest.submission.constants.SubmissionConstants;
import de.gerdiproject.harvest.submission.events.DocumentsSubmittedEvent;
import de.gerdiproject.harvest.submission.events.StartSubmissionEvent;
import de.gerdiproject.harvest.submission.events.SubmissionFinishedEvent;
import de.gerdiproject.harvest.submission.events.SubmissionStartedEvent;
import de.gerdiproject.harvest.utils.CancelableFuture;
import de.gerdiproject.harvest.utils.cache.HarvesterCache;
import de.gerdiproject.harvest.utils.cache.HarvesterCacheManager;
import de.gerdiproject.json.datacite.DataCiteJson;

/**
 * This abstract class offers a basis for sending documents to a DataBase any
 * WebService.
 *
 * @author Robin Weiss
 */
public abstract class AbstractSubmitter implements IEventListener
{
    private CancelableFuture<Boolean> currentSubmissionProcess;
    private int failedDocumentCount;
    private String userName;
    private String password;
    private boolean canSubmitOutdatedDocs;
    private boolean canSubmitFailedDocs;

    protected boolean isHarvestIncomplete;
    protected boolean hasSubmittedAll;
    protected Charset charset;

    protected final Logger logger; // NOPMD - we want to retrieve the type of the inheriting class


    /**
     * A mapping between document IDs and documents that are to be submitted as
     * a batch.
     */
    protected final Map<String, IDocument> batchMap;

    /**
     * The number of processed documents.
     */
    protected int processedDocumentCount;

    /**
     * True, if the submission process is being aborted.
     */
    protected boolean isAborting;


    /**
     * The size of the current batch request in bytes.
     */
    private int currentBatchSize = 0;

    /**
     * The maximum number of bytes that are allowed to be submitted in each
     * batch.
     */
    protected int maxBatchSize;

    /**
     * The optional authentication credentials for accessing the submission URL.
     */
    protected String credentials;

    /**
     * The URL to which is submitted
     */
    protected URL url;

    private HarvesterCacheManager cacheManager;


    /**
     * Constructor that initializes the {@linkplain Logger}.
     */
    public AbstractSubmitter()
    {
        logger = LoggerFactory.getLogger(getClass());
        batchMap = new HashMap<>();
        failedDocumentCount = 0;
    }


    @Override
    public void addEventListeners()
    {
        EventSystem.addSynchronousListener(StartSubmissionEvent.class, this::onStartSubmission);
        EventSystem.addListener(GlobalParameterChangedEvent.class, onGlobalParameterChanged);
        EventSystem.addListener(HarvestFinishedEvent.class, onHarvestFinished);
    }


    @Override
    public void removeEventListeners()
    {
        EventSystem.removeSynchronousListener(StartSubmissionEvent.class);
        EventSystem.removeListener(GlobalParameterChangedEvent.class, onGlobalParameterChanged);
        EventSystem.removeListener(HarvestFinishedEvent.class, onHarvestFinished);
    }


    /**
     * Changes the {@linkplain HarvesterCacheManager} used for retrieving the documents to be submitted.
     *
     * @param cacheManager the {@linkplain HarvesterCacheManager} used for retrieving documents
     */
    public void setCacheManager(HarvesterCacheManager cacheManager)
    {
        this.cacheManager = cacheManager;
    }


    /**
     * Sets the charset of the harvested documents.
     *
     * @param charset the charset of the harvested documents
     */
    public void setCharset(Charset charset)
    {
        this.charset = charset;
    }


    /**
     * Sets the indicator that determines if the latest harvest has failed.
     *
     * @param state if true, the latest harvest failed or was aborted
     */
    public void setHarvestIncomplete(boolean state)
    {
        this.isHarvestIncomplete = state;
    }


    /**
     * Sets the indicator that determines if there are unsubmitted changes.
     *
     * @param state if true, there are unsubmitted changes
     */
    public void setHasSubmittedAll(boolean state)
    {
        this.hasSubmittedAll = state;
    }


    /**
     * Reads cached documents and submits them.
     *
     * @return a feedback message
     *
     * @throws IllegalStateException thrown when the submission cannot start due to
     * unfulfilled preconditions
     */
    protected String submitAll() throws IllegalStateException // NOPMD events need arguments
    {
        final int numberOfDocuments = getNumberOfSubmittableChanges();
        failedDocumentCount = numberOfDocuments;
        isAborting = false;
        processedDocumentCount = 0;
        currentBatchSize = 0;
        batchMap.clear();

        // send event
        EventSystem.sendEvent(new SubmissionStartedEvent(numberOfDocuments));

        // check if we can submit
        final String errorMessage = checkPreconditionErrors();

        if (errorMessage != null) {
            finishSubmission();
            throw new IllegalStateException(errorMessage);
        }

        // listen to abort requests
        EventSystem.addListener(StartAbortingEvent.class, onStartAborting);

        // log the beginning of the submission
        logger.info(String.format(SubmissionConstants.SUBMISSION_START, url.toString()));

        // start asynchronous submission
        currentSubmissionProcess = startSubmissionProcess();

        // finished handler
        currentSubmissionProcess.thenApply((isSuccessful) -> {
            finishSubmission();
            return isSuccessful;
        })
        // exception handler
        .exceptionally(throwable -> {
            if (isAborting)
                finishAbortingSubmission();
            else
            {
                logger.error(SubmissionConstants.SUBMISSION_INTERRUPTED, throwable);
                finishSubmission();
            }
            return false;
        });

        return String.format(SubmissionConstants.SUBMISSION_START, url.toString());
    }


    /**
     * Creates an asynchronous function that sequentially submits all harvested
     * documents in subsets of adjustable size.
     *
     * @return a function that can be used of asynchronous requests
     */
    protected CancelableFuture<Boolean> startSubmissionProcess()
    {
        return new CancelableFuture<>(() -> {
            boolean areAllSubmissionsSuccessful = true;

            // go through all registered caches and process their documents
            final List<HarvesterCache> cacheList = cacheManager.getHarvesterCaches();

            for (final HarvesterCache cache : cacheList)
            {
                // stop cache iteration if aborting
                if (isAborting)
                    break;

                boolean wasCacheSubmitted = cache.getChangesCache().forEach(
                (String documentId, DataCiteJson addedDoc) -> {
                    addDocument(documentId, addedDoc);
                    return !isAborting;
                });

                areAllSubmissionsSuccessful &= wasCacheSubmitted;
            }

            // cancel the asynchronous process
            if (isAborting)
            {
                batchMap.clear();
                currentSubmissionProcess.cancel(false);
            }
            // send remainder of documents
            else if (batchMap.size() > 0)
            {
                areAllSubmissionsSuccessful &= trySubmitBatch();
                batchMap.clear();
            }

            return areAllSubmissionsSuccessful;
        });
    }


    /**
     * Adds a document to the batch of submissions.
     *
     * @param documentId the unique identifier of the document
     * @param document the document that is to be added to the submission, or
     *            null if the document is supposed to be deleted from the index
     */
    protected void addDocument(String documentId, DataCiteJson document)
    {
        if (!isAborting) {
            int documentSize = getSizeOfDocument(documentId, document);

            // check if the document alone is bigger than the maximum allowed submission size
            if (currentBatchSize == 0 && documentSize > maxBatchSize) {
                logger.error(
                    String.format(
                        SubmissionConstants.DOCUMENT_TOO_LARGE,
                        documentId,
                        documentSize,
                        maxBatchSize));

                // abort here, because we must skip this document
                processedDocumentCount++;
                EventSystem.sendEvent(new DocumentsSubmittedEvent(false, 1));
                return;
            }

            // check if the batch size is reached and submit
            if (currentBatchSize + documentSize > maxBatchSize) {
                trySubmitBatch();
                batchMap.clear();
                currentBatchSize = 0;
            }

            batchMap.put(documentId, document);
            currentBatchSize += documentSize;
        }
    }


    /**
     * Calculates the size of a single document within the batch in bytes.
     *
     * @param documentId the unique identifier of the document
     * @param document the document of which the size is measured
     *
     * @return the size of the document in bytes
     */
    protected abstract int getSizeOfDocument(String documentId, IDocument document);


    /**
     * The core of the submission function which is to be implemented by
     * subclasses.
     *
     * @param documents a map of documentIDs to documents that are to be
     *            submitted, the values may also be null, in which case the
     *            document is to be removed from the index
     *
     * @throws Exception any kind of exception that can be thrown by the
     *             submission process
     */
    protected abstract void submitBatch(Map<String, IDocument> documents) throws Exception; // NOPMD - Exception is explicitly thrown, because it is up to the implementation which Exception causes the submission to fail


    /**
     * Checks if the submission can start
     *
     * @return a message explaining if the preconditions failed
     */
    protected String checkPreconditionErrors()
    {
        if (url == null)
            return SubmissionConstants.NO_URL_ERROR;

        if (getNumberOfSubmittableChanges() == 0)
            return SubmissionConstants.NO_DOCS_ERROR;

        // check if the cache was submitted already
        if (!canSubmitOutdatedDocs && hasSubmittedAll)
            return SubmissionConstants.OUTDATED_ERROR;

        // check if the harvest is incomplete
        if (!canSubmitFailedDocs && isHarvestIncomplete)
            return SubmissionConstants.FAILED_HARVEST_ERROR;


        return null;
    }


    /**
     * Marks the submission as finished, logging a brief summary and sending an
     * event.
     */
    protected void finishSubmission()
    {
        currentSubmissionProcess = null;
        EventSystem.removeListener(StartAbortingEvent.class, onStartAborting);

        // log the end of the submission

        if (failedDocumentCount == processedDocumentCount || processedDocumentCount == 0)
            logger.warn(SubmissionConstants.SUBMISSION_DONE_ALL_FAILED);

        else if (failedDocumentCount == 0) {
            logger.info(SubmissionConstants.SUBMISSION_DONE_ALL_OK);
            setHasSubmittedAll(true);
        }

        else
            logger.warn(String.format(SubmissionConstants.SUBMISSION_DONE_SOME_FAILED, failedDocumentCount));

        EventSystem.sendEvent(new SubmissionFinishedEvent(failedDocumentCount == 0 && processedDocumentCount > 0));

        // prevents dead-locks if the submission was aborted after it finished
        if (isAborting)
            finishAbortingSubmission();
    }


    /**
     * This function is called after the submission process was stopped due to
     * being aborted.
     */
    protected void finishAbortingSubmission()
    {
        currentSubmissionProcess = null;
        isAborting = false;
        EventSystem.sendEvent(new AbortingFinishedEvent());
    }


    /**
     * Sends documents to an external place.
     *
     * @return true, if the submission succeeded
     */
    protected boolean trySubmitBatch()
    {
        int numberOfDocs = batchMap.size();
        boolean isSuccessful;

        try {
            // attempt to submit the batch
            submitBatch(batchMap);

            // log success and send an event
            logger.info(
                String.format(
                    SubmissionConstants.SUBMIT_PARTIAL_OK,
                    processedDocumentCount,
                    processedDocumentCount + numberOfDocs));
            failedDocumentCount -= numberOfDocs;
            isSuccessful = true;
        } catch (Exception e) {
            // log the failure
            logger.error(
                String.format(
                    SubmissionConstants.SUBMIT_PARTIAL_FAILED,
                    String.valueOf(processedDocumentCount),
                    String.valueOf(processedDocumentCount + numberOfDocs)),
                e);
            isSuccessful = false;
        }

        // send event
        EventSystem.sendEvent(new DocumentsSubmittedEvent(isSuccessful, numberOfDocs));
        processedDocumentCount += numberOfDocs;

        return isSuccessful;
    }


    /**
     * Iterates through all registered caches and calculates the total number of
     * submitted changes.
     *
     * @return the total number of submitted changes.
     */
    protected int getNumberOfSubmittableChanges()
    {
        return cacheManager.getNumberOfHarvestedDocuments();
    }


    /**
     * Changes the credentials that may be necessary to authenticate the submitter
     * with the URL.
     *
     * @param userName the new user name used for authenticating the submitter
     * @param password the new user password used for authenticating the submitter
     */
    protected void setCredentials(String userName, String password)
    {
        this.userName = userName;
        this.password = password;

        if (userName == null || password == null || userName.isEmpty())
            this.credentials = null;
        else
            this.credentials = Base64.getEncoder().encodeToString((userName + ":" + password).getBytes(StandardCharsets.UTF_8));
    }


    /**
     * Changes the maximum allowed submission request size, used
     * for dividing the submission into multiple submission requests.
     *
     * @param maxBatchSize the new submission request size in bytes
     */
    protected void setMaxBatchSize(int maxBatchSize)
    {
        this.maxBatchSize = maxBatchSize;
    }


    /**
     * Changes the URL to which the documents are submitted.
     *
     * @param url the new URL to which documents are submitted
     */
    protected void setUrl(URL url)
    {
        this.url = url;
    }


    /**
     * Changes the flag that determines if documents that have been submitted already
     * can be submitted again.
     *
     * @param state the new value of the flag
     */
    protected void setSubmitOutdatedDocs(boolean state)
    {
        this.canSubmitOutdatedDocs = state;
    }


    /**
     * Changes the flag that determines if documents that have been harvested incompletely
     * due to a failed harvest can be submitted.
     *
     * @param state the new value of the flag
     */
    protected void setSubmitFailedDocs(boolean state)
    {
        this.canSubmitFailedDocs = state;
    }


    //////////////////////////////
    // Event Callback Functions //
    //////////////////////////////

    /**
     * Event callback: When a submission starts, submit the cache file via the
     * {@linkplain AbstractSubmitter}.
     */
    private String onStartSubmission(StartSubmissionEvent e)
    {
        return submitAll();
    }


    /**
     * Event listener for aborting the submitter.
     */
    private final Consumer<StartAbortingEvent> onStartAborting = (StartAbortingEvent e) -> {
        isAborting = true;
        EventSystem.removeListener(StartAbortingEvent.class, this.onStartAborting);
        EventSystem.sendEvent(new AbortingStartedEvent());
    };


    /**
     * Event callback: When a harvest is finished, notify that there are unsubmitted changes.
     * {@linkplain AbstractSubmitter}.
     */
    private final Consumer<HarvestFinishedEvent> onHarvestFinished = (HarvestFinishedEvent e) -> {
        setHarvestIncomplete(!e.isSuccessful());
        setHasSubmittedAll(false);
    };


    /**
     * Event listener for changing submission parameters.
     *
     * @param e the event that triggered the parameter change
     */
    private Consumer<GlobalParameterChangedEvent> onGlobalParameterChanged = (GlobalParameterChangedEvent e) -> {
        final String parameterName = e.getParameter().getKey();
        final Object newValue = e.getParameter().getValue();

        switch (parameterName)
        {
            case ConfigurationConstants.SUBMISSION_SIZE:
                setMaxBatchSize((Integer) newValue);
                break;

            case ConfigurationConstants.SUBMISSION_URL:
                setUrl((URL) newValue);
                break;

            case ConfigurationConstants.SUBMISSION_USER_NAME:
                setCredentials((String) newValue, password);
                break;

            case ConfigurationConstants.SUBMISSION_PASSWORD:
                setCredentials(userName, (String) newValue);
                break;

            case ConfigurationConstants.SUBMIT_OUTDATED:
                setSubmitOutdatedDocs((Boolean) newValue);
                break;

            case ConfigurationConstants.SUBMIT_INCOMPLETE:
                setSubmitFailedDocs((Boolean) newValue);
                break;

            default: // ignore parameter
        }
    };
}
