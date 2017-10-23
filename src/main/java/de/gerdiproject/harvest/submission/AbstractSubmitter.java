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

import java.net.URL;
import java.util.Base64;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gerdiproject.harvest.IDocument;
import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.impl.DocumentsSubmittedEvent;
import de.gerdiproject.harvest.event.impl.SubmissionFinishedEvent;
import de.gerdiproject.harvest.event.impl.SubmissionStartedEvent;

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

    protected final Logger logger;

    private int submittedDocumentCount;
    private int failedDocumentCount;

    public AbstractSubmitter()
    {
        logger = LoggerFactory.getLogger(getClass());
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
    protected abstract String submit(List<IDocument> documents, URL submissionUrl, String credentials);


    /**
     * Attempts to initiate document submission.
     *
     * @return true, if the submission can proceed
     */
    public boolean startSubmission()
    {
        // check if we are ready to submit
        final Configuration config = MainContext.getConfiguration();
        URL submissionUrl = getSubmissionUrl(config);

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
    public void endSubmission()
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
     *
     * @return true, if the submission succeeded
     */
    public boolean submit(List<IDocument> documents)
    {
        if (documents == null || documents.isEmpty()) {
            logger.warn(String.format(
                            SUBMIT_PARTIAL_FAILED,
                            String.valueOf(submittedDocumentCount),
                            UNKNOWN_DOCUMENT_COUNT,
                            NO_DOCS_ERROR));
            return false;
        }

        final Configuration config = MainContext.getConfiguration();
        String errorMessage = submit(documents, getSubmissionUrl(config), getCredentials(config));
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
