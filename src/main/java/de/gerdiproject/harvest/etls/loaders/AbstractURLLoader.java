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
package de.gerdiproject.harvest.etls.loaders;

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
import de.gerdiproject.harvest.etls.AbstractETL;
import de.gerdiproject.harvest.etls.loaders.constants.LoaderConstants;
import de.gerdiproject.harvest.utils.HashGenerator;
import de.gerdiproject.json.datacite.DataCiteJson;

/**
 * This abstract class offers a basis for sending documents to a DataBase any
 * WebService.
 *
 * @author Robin Weiss
 */
public abstract class AbstractURLLoader <OUT extends DataCiteJson> extends AbstractIteratorLoader<OUT>
{
    private volatile HashGenerator hashGenerator;
    private final StringParameter userNameParam;
    private final StringParameter passwordParam;

    protected final Logger logger; // NOPMD - we want to retrieve the type of the inheriting class
    protected final Map<String, IDocument> batchMap;
    protected final IntegerParameter maxBatchSizeParam;
    protected final UrlParameter urlParam;
    protected Charset charset;

    private int currentBatchSize = 0;


    /**
     * Constructor that initializes the {@linkplain Logger}.
     */
    public AbstractURLLoader()
    {
        this.logger = LoggerFactory.getLogger(getClass());
        this.batchMap = new HashMap<>();

        this.urlParam = Configuration.registerParameter(LoaderConstants.URL_PARAM);
        this.userNameParam = Configuration.registerParameter(LoaderConstants.USER_NAME_PARAM);
        this.passwordParam = Configuration.registerParameter(LoaderConstants.PASSWORD_PARAM);
        this.maxBatchSizeParam = Configuration.registerParameter(LoaderConstants.MAX_BATCH_SIZE_PARAM);
    }


    @Override
    public void unregisterParameters()
    {
        Configuration.unregisterParameter(urlParam);
        Configuration.unregisterParameter(userNameParam);
        Configuration.unregisterParameter(passwordParam);
        Configuration.unregisterParameter(maxBatchSizeParam);
    }


    @Override
    public void init(AbstractETL<?, ?> etl)
    {
        super.init(etl);

        this.charset = etl.getCharset();
        this.hashGenerator = new HashGenerator(charset);

        batchMap.clear();
        currentBatchSize = 0;

        // check if we can submit
        final String errorMessage = checkPreconditionErrors();

        if (errorMessage != null)
            throw new IllegalStateException(errorMessage);
    }


    @Override
    public void loadElement(OUT document, boolean isLastDocument) throws LoaderException
    {
        if (document == null)
            return;

        final String documentId = hashGenerator.getShaHash(document.getSourceId());
        int documentSize = getSizeOfDocument(documentId, document);

        // check if the document alone is bigger than the maximum allowed submission size
        if (currentBatchSize == 0 && documentSize > maxBatchSizeParam.getValue()) {
            throw new LoaderException(
                String.format(
                    LoaderConstants.DOCUMENT_TOO_LARGE,
                    documentId,
                    documentSize,
                    maxBatchSizeParam.getValue()));
        }

        // check if the batch size is reached and submit
        if (currentBatchSize + documentSize > maxBatchSizeParam.getValue()) {
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
            return LoaderConstants.NO_URL_ERROR;

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

        try {
            // attempt to submit the batch
            submitBatch(batchMap);

            // log success and send an event
            if (logger.isInfoEnabled()) {
                logger.info(
                    String.format(
                        LoaderConstants.SUBMIT_PARTIAL_OK, numberOfDocs));
            }
        } catch (Exception e) {
            throw new LoaderException(e.toString());
        }
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
        if (userNameParam.getValue() == null || passwordParam.getValue() == null || userNameParam.getValue().isEmpty())
            return null;
        else
            return Base64.getEncoder().encodeToString((userNameParam.getValue() + ":" + passwordParam.getValue()).getBytes(StandardCharsets.UTF_8));
    }


    /**
     * Returns the submission URL as a string.
     *
     * @return the submission URL as a string
     */
    protected String getUrl()
    {
        return urlParam.getStringValue();
    }
}
