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
import de.gerdiproject.harvest.config.parameters.IntegerParameter;
import de.gerdiproject.harvest.config.parameters.StringParameter;
import de.gerdiproject.harvest.config.parameters.UrlParameter;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.harvester.AbstractETL;
import de.gerdiproject.harvest.harvester.loaders.ILoader;
import de.gerdiproject.harvest.harvester.loaders.LoaderException;
import de.gerdiproject.harvest.submission.constants.SubmissionConstants;
import de.gerdiproject.harvest.submission.events.DocumentsSubmittedEvent;
import de.gerdiproject.harvest.utils.HashGenerator;
import de.gerdiproject.json.datacite.DataCiteJson;

/**
 * This abstract class offers a basis for sending documents to a DataBase any
 * WebService.
 *
 * @author Robin Weiss
 */
public abstract class AbstractURLLoader <OUT extends DataCiteJson> implements ILoader<OUT>
{
    private HashGenerator hashGenerator;

    private final StringParameter userName;
    private final StringParameter password;

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
     * The size of the current batch request in bytes.
     */
    private int currentBatchSize = 0;


    /**
     * Constructor that initializes the {@linkplain Logger}.
     */
    public AbstractURLLoader()
    {
        this.logger = LoggerFactory.getLogger(getClass());
        this.batchMap = new HashMap<>();

        this.url = Configuration.registerParameter(SubmissionConstants.URL_PARAM);
        this.userName = Configuration.registerParameter(SubmissionConstants.USER_NAME_PARAM);
        this.password = Configuration.registerParameter(SubmissionConstants.PASSWORD_PARAM);
        this.maxBatchSize = Configuration.registerParameter(SubmissionConstants.MAX_BATCH_SIZE_PARAM);
    }


    @Override
    public <H extends AbstractETL<?, ?>> void init(H harvester)
    {
        this.charset = harvester.getCharset();
        this.hashGenerator = new HashGenerator(charset);

        batchMap.clear();
        currentBatchSize = 0;

        // check if we can submit
        final String errorMessage = checkPreconditionErrors();

        if (errorMessage != null)
            throw new IllegalStateException(errorMessage);
    }


    @Override
    public void load(OUT document, boolean isLastDocument) throws LoaderException
    {
        final String documentId = hashGenerator.getShaHash(document.getSourceId());
        int documentSize = getSizeOfDocument(documentId, document);

        // check if the document alone is bigger than the maximum allowed submission size
        if (currentBatchSize == 0 && documentSize > maxBatchSize.getValue()) {
            throw new LoaderException(
                String.format(
                    SubmissionConstants.DOCUMENT_TOO_LARGE,
                    documentId,
                    documentSize,
                    maxBatchSize.getValue()));
        }

        // check if the batch size is reached and submit
        if (currentBatchSize + documentSize > maxBatchSize.getValue()) {
            trySubmitBatch();
            batchMap.clear();
            currentBatchSize = 0;
        }

        batchMap.put(documentId, document);
        currentBatchSize += documentSize;

        if (isLastDocument) {
            trySubmitBatch();
            batchMap.clear();
            currentBatchSize = 0;
        }
    }


    /**
     * Submits the remaining unsubmitted documents and resets the submitter.
     */
    @Override
    public void reset()
    {
        batchMap.clear();
        currentBatchSize = 0;
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

        return null;
    }


    /**
     * Sends documents to an external place.
     *
     * @throws LoaderException when the batch could nto be submitted
     */
    protected void trySubmitBatch() throws LoaderException
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
                        SubmissionConstants.SUBMIT_PARTIAL_OK, numberOfDocs));
            }

            isSuccessful = true;
        } catch (Exception e) {
            throw new LoaderException(e.getMessage());
        }

        // send event
        EventSystem.sendEvent(new DocumentsSubmittedEvent(isSuccessful, numberOfDocs));
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
}
