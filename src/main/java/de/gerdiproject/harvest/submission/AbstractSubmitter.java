/*
# *  Licensed to the Apache Software Foundation (ASF) under one
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
package de.gerdiproject.harvest.submission;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

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
import de.gerdiproject.harvest.submission.events.SubmissionFinishedEvent;
import de.gerdiproject.harvest.submission.events.SubmissionStartedEvent;
import de.gerdiproject.harvest.utils.CancelableFuture;
import de.gerdiproject.json.GsonUtils;
import de.gerdiproject.json.datacite.DataCiteJson;

/**
 * This abstract class offers a basis for sending documents to a DataBase any WebService.
 *
 * @author Robin Weiss
 */
public abstract class AbstractSubmitter
{
    /**
     * Event listener for aborting the submitter.
     */
    private final Consumer<StartAbortingEvent> onStartAborting = (StartAbortingEvent e) -> {
        isAborting = true;
        EventSystem.removeListener(StartAbortingEvent.class, this.onStartAborting);
        EventSystem.sendEvent(new AbortingStartedEvent());
    };


    protected final Logger logger; // NOPMD - we want to retrieve the type of the inheriting class

    private int submittedDocumentCount;
    private int failedDocumentCount;
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
     * Reads the cached documents and passes them onto an {@linkplain AbstractSubmitter}.
     *
     * @param cachedDocuments the file in which the cached documents are stored as a JSON array
     * @param numberOfDocuments the number of documents that are to be submitted
     */
    public void submit(File cachedDocuments, int numberOfDocuments)
    {
        isAborting = false;
        submittedDocumentCount = 0;
        failedDocumentCount = numberOfDocuments;

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
                createSubmissionProcess(cachedDocuments, submissionUrl, credentials, batchSize));

            // finished handler
            currentSubmissionProcess.thenApply((isSuccessful) -> {
                onSubmissionFinished();
                return isSuccessful;
            })
            // exception handler
            .exceptionally(throwable -> {
                if (throwable instanceof CancellationException || throwable.getCause() instanceof CancellationException)
                    onSubmissionAborted();
                else
                {
                    logger.error(SubmissionConstants.SUBMISSION_INTERRUPTED, throwable);
                    onSubmissionFinished();
                }
                return false;
            });
        } else // fail the submission
            onSubmissionFinished();
    }



    /**
     * Creates a callable function that sequentially submits all harvested documents in fixed chunks.
     *
     * @param cachedDocuments the file in which the cached documents are stored as a JSON array
     * @param submissionUrl the URL to which the documents are to be submitted
     * @param credentials user credentials or null, if they do not exist
     * @param batchSize the max number of documents to be processed in a batch submission
     *
     * @return a function that can be used of asynchronous requests
     */
    protected Callable<Boolean> createSubmissionProcess(File cachedDocuments, URL submissionUrl, String credentials, int batchSize)
    {
        return () -> {
            boolean areAllSubmissionsSuccessful = true;

            // prepare variables
            final Gson gson = GsonUtils.getGson();

            // prepare json reader for the cached document list
            JsonReader reader = new JsonReader(
                new InputStreamReader(
                    new FileInputStream(cachedDocuments),
                    MainContext.getCharset()));

            // iterate through cached array
            List<IDocument> documentList = new LinkedList<IDocument>();
            reader.beginArray();

            while (reader.hasNext())
            {
                // abort submission if the flag is set
                if (isAborting) {
                    areAllSubmissionsSuccessful = false;
                    documentList.clear();
                    break;
                }

                // read a document from the array
                documentList.add(gson.fromJson(reader, DataCiteJson.class));

                // send documents in chunks of a configurable size
                if (documentList.size() == batchSize) {
                    areAllSubmissionsSuccessful &= trySubmitBatch(documentList, submissionUrl, credentials);
                    documentList.clear();
                }
            }

            // close reader
            reader.endArray();
            reader.close();

            // send remainder of documents
            if (documentList.size() > 0)
            {
                areAllSubmissionsSuccessful &= trySubmitBatch(documentList, submissionUrl, credentials);
                documentList.clear();
            }

            // cancel the asynchronous process
            if (isAborting)
                currentSubmissionProcess.cancel(false);

            return areAllSubmissionsSuccessful;
        };
    }


    /**
     * The core of the submission function which is to be implemented by subclasses.
     *
     * @param documents a subset of harvested documents that are to be submitted
     * @param submissionUrl the URL to which the documents are to be submitted
     * @param credentials user credentials or null, if they do not exist
     *
     * @return an error message if the submission failed, or null if it was successful
     */
    protected abstract String submitBatch(List<IDocument> documents, URL submissionUrl, String credentials);


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
     * Marks the submission as finished, logging a brief summary and sending an event.
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
    }


    /**
     * This function is called after the submission process was stopped due to
     * being aborted.
     */
    protected void onSubmissionAborted()
    {
        currentSubmissionProcess = null;
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
    protected boolean trySubmitBatch(List<IDocument> documents, URL submissionUrl, String credentials)
    {
        if (documents == null || documents.isEmpty()) {
            logger.warn(String.format(
                            SubmissionConstants.SUBMIT_PARTIAL_FAILED,
                            String.valueOf(submittedDocumentCount),
                            SubmissionConstants.UNKNOWN_DOCUMENT_COUNT,
                            SubmissionConstants.NO_DOCS_ERROR));
            return false;
        }

        String errorMessage = submitBatch(documents, submissionUrl, credentials);
        boolean isSuccessful = errorMessage == null;
        int numberOfDocs = documents.size();

        if (isSuccessful) {
            logger.info(String.format(SubmissionConstants.SUBMIT_PARTIAL_OK, submittedDocumentCount, submittedDocumentCount + numberOfDocs));
            EventSystem.sendEvent(new DocumentsSubmittedEvent(numberOfDocs));
            failedDocumentCount -= numberOfDocs;
        } else {
            logger.warn(String.format(
                            SubmissionConstants.SUBMIT_PARTIAL_FAILED,
                            String.valueOf(submittedDocumentCount),
                            String.valueOf(submittedDocumentCount + numberOfDocs),
                            errorMessage));
        }

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
        else {
            return Base64.getEncoder()
                   .encodeToString((userName + ":" + password)
                                   .getBytes(MainContext.getCharset()));
        }
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
}
