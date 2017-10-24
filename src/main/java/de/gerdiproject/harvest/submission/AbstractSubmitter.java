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
package de.gerdiproject.harvest.submission;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import de.gerdiproject.harvest.IDocument;
import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.event.EventSystem;
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
    private static final String SUBMISSION_START = "Submitting documents to: %s";
    private static final String SUBMISSION_DONE_ALL_OK = "Submission done! All documents were submitted!";
    private static final String SUBMISSION_DONE_SOME_FAILED = "Submission done! Failed to submit %d documents!";
    private static final String SUBMIT_PARTIAL_OK = " Submitted documents %d to %d.";
    private static final String SUBMIT_PARTIAL_FAILED = "Error submitting documents %s to %s: %s";
    private static final String UNKNOWN_DOCUMENT_COUNT = "???";

    private static final String NO_URL_ERROR = "Cannot submit documents: You need to set up a valid submission URL!";
    private static final String NO_DOCS_ERROR = "There are no documents to submit!";

    protected final Logger logger; // NOPMD - we want to retrieve the type of the inheriting class

    private int submittedDocumentCount;
    private int failedDocumentCount;

    public AbstractSubmitter()
    {
        logger = LoggerFactory.getLogger(getClass());
    }


    /**
     * Reads the cached documents and passes them onto an {@linkplain AbstractSubmitter}.
     *
     * @param cachedDocuments the file in which the cached documents are stored as a JSON array
     */
    public void submit(File cachedDocuments)
    {
        final Configuration config = MainContext.getConfiguration();
        URL submissionUrl = getSubmissionUrl(config);
        String credentials = getCredentials(config);
        int batchSize = config.getParameterValue(ConfigurationConstants.SUBMISSION_SIZE, Integer.class);

        startSubmission(submissionUrl);

        // start asynchronous submission
        CancelableFuture<Boolean> asyncSubmission = new CancelableFuture<>(
            createSubmissionProcess(cachedDocuments, submissionUrl, credentials, batchSize));

        // finished handler
        asyncSubmission.thenApply((isSuccessful) -> {
            endSubmission();
            return isSuccessful;
        })
        .exceptionally(throwable -> {
            endSubmission();
            return false;
        });
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
            logger.error(NO_URL_ERROR);
            return false;
        }

        submittedDocumentCount = 0;
        failedDocumentCount = 0;

        // log the beginning of the submission
        logger.info(String.format(SUBMISSION_START, submissionUrl.toString()));

        // send event
        EventSystem.sendEvent(new SubmissionStartedEvent());

        return true;
    }


    /**
     * Marks the submission as finished, logging a brief summary and sending an event.
     */
    protected void endSubmission()
    {
        // log the end of the submission
        if (failedDocumentCount == 0)
            logger.info(SUBMISSION_DONE_ALL_OK);
        else
            logger.info(String.format(SUBMISSION_DONE_SOME_FAILED, failedDocumentCount));

        EventSystem.sendEvent(new SubmissionFinishedEvent(failedDocumentCount == 0));
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
                            SUBMIT_PARTIAL_FAILED,
                            String.valueOf(submittedDocumentCount),
                            UNKNOWN_DOCUMENT_COUNT,
                            NO_DOCS_ERROR));
            return false;
        }

        String errorMessage = submitBatch(documents, submissionUrl, credentials);
        boolean isSuccessful = errorMessage == null;
        int numberOfDocs = documents.size();

        if (isSuccessful) {
            logger.info(String.format(SUBMIT_PARTIAL_OK, submittedDocumentCount, submittedDocumentCount + numberOfDocs));
            EventSystem.sendEvent(new DocumentsSubmittedEvent(numberOfDocs));
        } else {
            failedDocumentCount += numberOfDocs;
            logger.warn(String.format(
                            SUBMIT_PARTIAL_FAILED,
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
