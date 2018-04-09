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
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gerdiproject.harvest.IDocument;
import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.state.events.AbortingFinishedEvent;
import de.gerdiproject.harvest.state.events.AbortingStartedEvent;
import de.gerdiproject.harvest.state.events.StartAbortingEvent;
import de.gerdiproject.harvest.submission.constants.SubmissionConstants;
import de.gerdiproject.harvest.submission.events.DocumentsSubmittedEvent;
import de.gerdiproject.harvest.submission.events.StartSubmissionEvent;
import de.gerdiproject.harvest.submission.events.SubmissionFinishedEvent;
import de.gerdiproject.harvest.submission.events.SubmissionStartedEvent;
import de.gerdiproject.harvest.utils.CancelableFuture;
import de.gerdiproject.harvest.utils.cache.DocumentsCache;
import de.gerdiproject.harvest.utils.cache.events.RegisterCacheEvent;
import de.gerdiproject.json.datacite.DataCiteJson;

/**
 * This abstract class offers a basis for sending documents to a DataBase any
 * WebService.
 *
 * @author Robin Weiss
 */
public abstract class AbstractSubmitter
{
    private final List<DocumentsCache> cacheList = Collections.synchronizedList(new LinkedList<>());
    private int failedDocumentCount;

    protected final Map<String, IDocument> submissionMap = new HashMap<>();
    protected final Logger logger; // NOPMD - we want to retrieve the type of the inheriting class

    protected int submittedDocumentCount;
    protected boolean isAborting;

    private CancelableFuture<Boolean> currentSubmissionProcess;


    /**
     * Constructor that initializes the {@linkplain Logger}.
     */
    public AbstractSubmitter()
    {
        logger = LoggerFactory.getLogger(getClass());

    }


    /**
     * Adds event listeners.
     */
    public void init()
    {
        EventSystem.addListener(RegisterCacheEvent.class, onRegisterCache);
        EventSystem.addListener(StartSubmissionEvent.class, onStartSubmission);
    }


    /**
     * Reads cached documents and submits them.
     *
     */
    public void submit()
    {
        final int numberOfDocuments = getNumberOfSubmittedChanges();
        failedDocumentCount = numberOfDocuments;
        isAborting = false;
        submittedDocumentCount = 0;

        // send event
        EventSystem.sendEvent(new SubmissionStartedEvent(numberOfDocuments));

        final Configuration config = MainContext.getConfiguration();
        URL submissionUrl = getSubmissionUrl(config);
        String credentials = getCredentials(config);
        int batchSize = config.getParameterValue(ConfigurationConstants.SUBMISSION_SIZE, Integer.class);

        // prepare stuff and check if we can submit
        boolean canSubmit = startSubmission(submissionUrl);

        if (canSubmit) {
            // start asynchronous submission
            currentSubmissionProcess = new CancelableFuture<>(
                    createSubmissionProcess(cacheList, submissionUrl, credentials, batchSize));

            // finished handler
            currentSubmissionProcess.thenApply((isSuccessful) -> {
                onSubmissionFinished();
                return isSuccessful;
            })
                    // exception handler
                    .exceptionally(throwable -> {
                        if (isAborting)
                            onSubmissionAborted();
                        else {
                            logger.error(SubmissionConstants.SUBMISSION_INTERRUPTED, throwable);
                            onSubmissionFinished();
                        }
                        return false;
                    });
        } else // fail the submission
            onSubmissionFinished();
    }


    /**
     * Creates a callable function that sequentially submits all harvested
     * documents in subsets of adjustable size.
     *
     * @param cachedDocuments the caches of deleted and added documents
     * @param submissionUrl the URL to which the documents are to be submitted
     * @param credentials user credentials or null, if they do not exist
     * @param batchSize the max number of documents to be processed in a batch
     *            submission
     *
     * @return a function that can be used of asynchronous requests
     */
    protected Callable<Boolean> createSubmissionProcess(List<DocumentsCache> cachedDocuments, URL submissionUrl, String credentials, int batchSize)
    {
        return () -> {
            boolean areAllSubmissionsSuccessful = true;

            // go through all registered caches and process their documents
            for (final DocumentsCache cache : cacheList) {
                // stop cache iteration if aborting
                if (isAborting)
                    break;

                // process all documents that are to be added
                areAllSubmissionsSuccessful &= cache.getChangesCache().forEach(
                        (String documentId, DataCiteJson addedDoc) -> {
                            addDocument(
                                    documentId,
                                    addedDoc,
                                    submissionUrl,
                                    credentials,
                                    batchSize);
                            return !isAborting;
                        });

                // process all documents that are to be deleted
                areAllSubmissionsSuccessful &= cache.getDeletionsCache().forEach(
                        (String documentId) -> {
                            addDocument(
                                    documentId,
                                    null,
                                    submissionUrl,
                                    credentials,
                                    batchSize);
                            return !isAborting;
                        });
            }

            // cancel the asynchronous process
            if (isAborting) {
                submissionMap.clear();
                currentSubmissionProcess.cancel(false);
            }
            // send remainder of documents
            else if (submissionMap.size() > 0) {
                areAllSubmissionsSuccessful &= trySubmitBatch(submissionMap, submissionUrl, credentials);
                submissionMap.clear();
            }

            return areAllSubmissionsSuccessful;
        };
    }


    /**
     * Adds a document to the batch of submissions.
     * 
     * @param documentId the unique identifier of the document
     * @param document the document that is to be added to the submission, or
     *            null if the document is supposed to be deleted from the index
     * @param submissionUrl the URL to which the batch is submitted
     * @param credentials the login credentials of the URL or null, if they are
     *            not required
     * @param batchSize the max number of documents that are to be submitted on
     *            each batch
     */
    private void addDocument(String documentId, DataCiteJson document, URL submissionUrl, String credentials, int batchSize)
    {
        if (!isAborting) {
            submissionMap.put(documentId, document);

            // send documents in chunks of a configurable size
            if (submissionMap.size() == batchSize) {
                trySubmitBatch(submissionMap, submissionUrl, credentials);
                submissionMap.clear();
            }
        }
    }


    /**
     * The core of the submission function which is to be implemented by
     * subclasses.
     *
     * @param documents a map of documentIDs to documents that are to be
     *            submitted, the values may also be null, in which case the
     *            document is to be removed from the index
     * @param submissionUrl the URL to which the documents are to be submitted
     * @param credentials user credentials or null, if they do not exist
     *
     * @throws Exception any kind of exception that can be thrown by the
     *             submission process
     */
    protected abstract void submitBatch(Map<String, IDocument> documents, URL submissionUrl, String credentials) throws Exception; // NOPMD - Exception is explicitly thrown, because it is up to the implementation which Exception causes the submission to fail


    /**
     * Attempts to initiate document submission.
     *
     * @param submissionUrl the URL to which the documents are to be submitted
     *
     * @return true, if the submission can proceed
     */
    protected boolean startSubmission(URL submissionUrl)
    {
        if (submissionUrl == null) {
            logger.error(SubmissionConstants.NO_URL_ERROR);
            return false;
        }

        // listen to abort requests
        EventSystem.addListener(StartAbortingEvent.class, onStartAborting);

        // log the beginning of the submission
        logger.info(String.format(SubmissionConstants.SUBMISSION_START, submissionUrl.toString()));

        return true;
    }


    /**
     * Marks the submission as finished, logging a brief summary and sending an
     * event.
     */
    protected void onSubmissionFinished()
    {
        currentSubmissionProcess = null;
        EventSystem.removeListener(StartAbortingEvent.class, onStartAborting);

        // log the end of the submission
        if (failedDocumentCount == 0)
            logger.info(SubmissionConstants.SUBMISSION_DONE_ALL_OK);

        else if (failedDocumentCount == submittedDocumentCount || submittedDocumentCount == 0)
            logger.warn(SubmissionConstants.SUBMISSION_DONE_ALL_FAILED);

        else
            logger.warn(String.format(SubmissionConstants.SUBMISSION_DONE_SOME_FAILED, failedDocumentCount));

        EventSystem.sendEvent(new SubmissionFinishedEvent(failedDocumentCount == 0));

        // prevents dead-locks if the submission was aborted after it finished
        if (isAborting)
            onSubmissionAborted();
    }


    /**
     * This function is called after the submission process was stopped due to
     * being aborted.
     */
    protected void onSubmissionAborted()
    {
        currentSubmissionProcess = null;
        isAborting = false;
        EventSystem.sendEvent(new AbortingFinishedEvent());
    }


    /**
     * Sends documents to an external place.
     *
     * @param documents a chunk of harvested documents
     * @param submissionUrl the final URL to which the documents are submitted
     * @param credentials login credentials, or null if they are not needed
     *
     * @return true, if the submission succeeded
     */
    protected boolean trySubmitBatch(Map<String, IDocument> documents, URL submissionUrl, String credentials)
    {
        if (documents == null || documents.isEmpty()) {
            logger.error(
                    String.format(
                            SubmissionConstants.SUBMIT_PARTIAL_FAILED,
                            String.valueOf(submittedDocumentCount),
                            SubmissionConstants.UNKNOWN_DOCUMENT_COUNT) + SubmissionConstants.NO_DOCS_ERROR);
            return false;
        }

        int numberOfDocs = documents.size();
        boolean isSuccessful;

        try {
            // attempt to submit the batch
            submitBatch(documents, submissionUrl, credentials);

            // log success and send an event
            logger.info(
                    String.format(
                            SubmissionConstants.SUBMIT_PARTIAL_OK,
                            submittedDocumentCount,
                            submittedDocumentCount + numberOfDocs));
            failedDocumentCount -= numberOfDocs;
            isSuccessful = true;
        } catch (Exception e) {
            // log the failure
            logger.error(
                    String.format(
                            SubmissionConstants.SUBMIT_PARTIAL_FAILED,
                            String.valueOf(submittedDocumentCount),
                            String.valueOf(submittedDocumentCount + numberOfDocs)),
                    e);
            isSuccessful = false;
        }

        // send event
        EventSystem.sendEvent(new DocumentsSubmittedEvent(isSuccessful, numberOfDocs));
        submittedDocumentCount += numberOfDocs;

        return isSuccessful;
    }


    /**
     * Creates login credentials for a submission.
     *
     * @param config the global configuration
     *
     * @return a base64 encoded username/password string
     */
    protected String getCredentials(Configuration config)
    {
        final String userName = config.getParameterValue(ConfigurationConstants.SUBMISSION_USER_NAME, String.class);
        final String password = config.getParameterValue(ConfigurationConstants.SUBMISSION_PASSWORD, String.class);

        if (userName == null || password == null || userName.isEmpty())
            return null;
        else
            return Base64.getEncoder().encodeToString((userName + ":" + password).getBytes(MainContext.getCharset()));
    }


    /**
     * Retrieves and possibly refines the submission URL.
     *
     * @param config the global configuration
     *
     * @return a URL to which the documents are being sent
     */
    protected URL getSubmissionUrl(Configuration config)
    {
        URL submissionUrl = config.getParameterValue(ConfigurationConstants.SUBMISSION_URL, URL.class);
        return submissionUrl;
    }


    /**
     * Iterates through all registered caches and calculates the total number of
     * submitted changes.
     *
     * @return the total number of submitted changes.
     */
    protected int getNumberOfSubmittedChanges()
    {
        int docCount = 0;

        for (final DocumentsCache cache : cacheList) {
            docCount += cache.getChangesCache().getSize();
            docCount += cache.getDeletionsCache().getSize();
        }

        return docCount;
    }


    //////////////////////////////
    // Event Callback Functions //
    //////////////////////////////


    /**
     * Event listener for registering a new documents cache.
     */
    private final Consumer<RegisterCacheEvent> onRegisterCache = (RegisterCacheEvent e) -> {
        cacheList.add(e.getCache());
    };


    /**
     * Event callback: When a submission starts, submit the cache file via the
     * {@linkplain AbstractSubmitter}.
     */
    private final Consumer<StartSubmissionEvent> onStartSubmission = (StartSubmissionEvent e) -> {
        submit();
    };

    /**
     * Event listener for aborting the submitter.
     */
    private final Consumer<StartAbortingEvent> onStartAborting = (StartAbortingEvent e) -> {
        isAborting = true;
        EventSystem.removeListener(StartAbortingEvent.class, this.onStartAborting);
        EventSystem.sendEvent(new AbortingStartedEvent());
    };
}
