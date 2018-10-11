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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gerdiproject.harvest.IDocument;
import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.parameters.BooleanParameter;
import de.gerdiproject.harvest.config.parameters.IntegerParameter;
import de.gerdiproject.harvest.config.parameters.StringParameter;
import de.gerdiproject.harvest.config.parameters.UrlParameter;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.submission.constants.SubmissionConstants;
import de.gerdiproject.harvest.submission.events.DocumentsSubmittedEvent;

/**
 * This abstract class offers a basis for sending documents to a DataBase any
 * WebService.
 *
 * @author Robin Weiss
 */
public abstract class AbstractSubmitter
{
    private final StringParameter userName;
    private final StringParameter password;
    private final BooleanParameter canSubmitOutdatedDocs;
    private final BooleanParameter canSubmitFailedDocs;

    /**
     * The maximum number of bytes that are allowed to be submitted in each
     * batch.
     */
    protected final IntegerParameter maxBatchSize;

    /**
     * The URL to which is submitted
     */
    protected final UrlParameter url;

    /**
     * If true, the harvest was aborted, or it failed
     */
    protected boolean isHarvestIncomplete;

    /**
     * If true, the submission has completed successfully.
     */
    protected boolean hasSubmittedAll;

    /**
     * The charset used to serialize the submitted documents.
     */
    protected Charset charset;

    /**
     * The logger for possible errors.
     */
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
     * The size of the current batch request in bytes.
     */
    private int currentBatchSize = 0;


    /**
     * Constructor that initializes the {@linkplain Logger}.
     */
    public AbstractSubmitter()
    {
        logger = LoggerFactory.getLogger(getClass());
        batchMap = new HashMap<>();

        url = Configuration.registerParameter(SubmissionConstants.URL_PARAM);
        userName = Configuration.registerParameter(SubmissionConstants.USER_NAME_PARAM);
        password = Configuration.registerParameter(SubmissionConstants.PASSWORD_PARAM);
        canSubmitOutdatedDocs = Configuration.registerParameter(SubmissionConstants.SUBMIT_OUTDATED_PARAM);
        canSubmitFailedDocs = Configuration.registerParameter(SubmissionConstants.SUBMIT_INCOMPLETE_PARAM);
        maxBatchSize = Configuration.registerParameter(SubmissionConstants.MAX_BATCH_SIZE_PARAM);
    }


    /**
     * Checks if the submitter is set up correctly.
     *
     * @throws IllegalStateException if the submitter is not set up correctly
     */
    public void startSubmission()
    {
        batchMap.clear();

        // check if we can submit
        final String errorMessage = checkPreconditionErrors();

        if (errorMessage != null)
            throw new IllegalStateException(errorMessage);
    }


    /**
     * Submits the remaining unsubmitted documents and resets the submitter.
     *
     * @return true if all remaining documents were submitted
     */
    public boolean finishSubmission()
    {
        boolean isSuccessful = true;

        if (batchMap.size() > 0) {
            isSuccessful = trySubmitBatch();
            batchMap.clear();
        }

        return isSuccessful;
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
     * Adds a document to the batch of submissions.
     *
     * @param documentId the unique identifier of the document
     * @param document the document that is to be added to the submission, or
     *            null if the document is supposed to be deleted from the index
     */
    protected boolean addDocument(String documentId, IDocument document)
    {
        int documentSize = getSizeOfDocument(documentId, document);

        // check if the document alone is bigger than the maximum allowed submission size
        if (currentBatchSize == 0 && documentSize > maxBatchSize.getValue()) {
            logger.error(
                String.format(
                    SubmissionConstants.DOCUMENT_TOO_LARGE,
                    documentId,
                    documentSize,
                    maxBatchSize.getValue()));

            // abort here, because we must skip this document
            processedDocumentCount++;
            EventSystem.sendEvent(new DocumentsSubmittedEvent(false, 1));
            return false;
        }

        boolean isSuccessful = true;

        // check if the batch size is reached and submit
        if (currentBatchSize + documentSize > maxBatchSize.getValue()) {
            isSuccessful = trySubmitBatch();
            batchMap.clear();
            currentBatchSize = 0;
        }

        batchMap.put(documentId, document);
        currentBatchSize += documentSize;

        return isSuccessful;
    }


    /**
     * Returns a unique identifier of this submitter class.
     * This identifier is used to be able to set the submitter during runtime
     * via the Configuration.
     *
     * @return a unique identifier of this submitter class
     */
    public abstract String getId();


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
        final String url = getUrl();

        if (url == null || url.isEmpty())
            return SubmissionConstants.NO_URL_ERROR;

        // check if the cache was submitted already
        if (!canSubmitOutdatedDocs.getValue() && hasSubmittedAll)
            return SubmissionConstants.OUTDATED_ERROR;

        // check if the harvest is incomplete
        if (!canSubmitFailedDocs.getValue() && isHarvestIncomplete)
            return SubmissionConstants.FAILED_HARVEST_ERROR;


        return null;
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
            if (logger.isInfoEnabled()) {
                logger.info(
                    String.format(
                        SubmissionConstants.SUBMIT_PARTIAL_OK,
                        processedDocumentCount,
                        processedDocumentCount + numberOfDocs));
            }

            isSuccessful = true;
        } catch (Exception e) {
            // log the failure
            if (logger.isErrorEnabled()) {
                logger.error(
                    String.format(
                        SubmissionConstants.SUBMIT_PARTIAL_FAILED,
                        String.valueOf(processedDocumentCount),
                        String.valueOf(processedDocumentCount + numberOfDocs)),
                    e);
            }

            isSuccessful = false;
        }

        // send event
        EventSystem.sendEvent(new DocumentsSubmittedEvent(isSuccessful, numberOfDocs));
        processedDocumentCount += numberOfDocs;

        return isSuccessful;
    }


    /**
     * Retrieves the credentials that may be necessary to authenticate the submitter
     * with the URL.
     *
     * @return the credentials that may be necessary to authenticate the submitter
     * with the URL
     */
    protected String getCredentials()
    {

        if (userName.getValue() == null || password.getValue() == null || userName.getValue().isEmpty())
            return null;
        else
            return Base64.getEncoder().encodeToString((userName.getValue() + ":" + password.getValue()).getBytes(StandardCharsets.UTF_8));
    }


    /**
     * Returns the submission URL as a string.
     *
     * @return the submission URL as a string
     */
    protected String getUrl()
    {
        return url.getStringValue();
    }


    /**
     * Copies all field values from another submitter to this one.
     *
     * @param other the submitter of which the values are copied
     */
    public void setValues(AbstractSubmitter other)
    {
        setCharset(other.charset);
        setHasSubmittedAll(other.hasSubmittedAll);
        setHarvestIncomplete(other.isHarvestIncomplete);
    }
}
