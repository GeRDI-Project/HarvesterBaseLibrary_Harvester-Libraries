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
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gerdiproject.harvest.IDocument;
import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.parameters.IntegerParameter;
import de.gerdiproject.harvest.config.parameters.StringParameter;
import de.gerdiproject.harvest.etls.AbstractETL;
import de.gerdiproject.harvest.etls.loaders.constants.LoaderConstants;
import de.gerdiproject.harvest.utils.HashGenerator;

/**
 * This abstract class offers a basis for sending documents to a search index
 * via a REST request.
 *
 * @param <S> The type of the sent documents
 *
 * @author Robin Weiss
 */
public abstract class AbstractURLLoader <S extends IDocument> extends AbstractIteratorLoader<S>
{
    protected final Logger logger; // NOPMD - we want to retrieve the type of the inheriting class
    protected final Map<String, S> batchMap;
    protected final IntegerParameter maxBatchSizeParam;
    protected final StringParameter urlParam;
    protected final HashGenerator hashGenerator;

    protected volatile Charset charset;
    private final StringParameter userNameParam;
    private final StringParameter passwordParam;

    private int currentBatchSize;


    /**
     * Constructor that initializes the {@linkplain Logger}.
     */
    public AbstractURLLoader()
    {
        super();
        this.logger = LoggerFactory.getLogger(getClass());
        this.batchMap = new HashMap<>();

        this.urlParam = Configuration.registerParameter(LoaderConstants.URL_PARAM);
        this.userNameParam = Configuration.registerParameter(LoaderConstants.USER_NAME_PARAM);
        this.passwordParam = Configuration.registerParameter(LoaderConstants.PASSWORD_PARAM);
        this.maxBatchSizeParam = Configuration.registerParameter(LoaderConstants.MAX_BATCH_SIZE_PARAM);
        this.hashGenerator = new HashGenerator(StandardCharsets.UTF_8);
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
    public void init(final AbstractETL<?, ?> etl)
    {
        super.init(etl);

        batchMap.clear();
        currentBatchSize = 0;
        charset = etl.getCharset();

        // check if we can load
        final String errorMessage = checkPreconditionErrors();

        if (errorMessage != null)
            throw new IllegalStateException(errorMessage);
    }


    @Override
    public void load(final Iterator<S> documents) throws LoaderException
    {
        super.load(documents);

        // execute the final submission
        if (!batchMap.isEmpty()) {
            tryLoadingBatch();
            batchMap.clear();
        }
    }


    @Override
    public void loadElement(final S document) throws LoaderException
    {
        if (document == null)
            return;

        final String documentId = getDocumentId(document);
        final int documentSize = getSizeOfDocument(documentId, document);

        // check if the document alone is bigger than the maximum load request size
        if (currentBatchSize == 0 && documentSize > maxBatchSizeParam.getValue()) {
            throw new LoaderException(
                String.format(
                    LoaderConstants.DOCUMENT_TOO_LARGE,
                    documentId,
                    documentSize,
                    maxBatchSizeParam.getValue()));
        }

        // check if the batch size is reached and load
        if (currentBatchSize + documentSize > maxBatchSizeParam.getValue()) {
            tryLoadingBatch();
            batchMap.clear();
            currentBatchSize = 0;
        }

        batchMap.put(documentId, document);
        currentBatchSize += documentSize;
    }


    /**
     * Retrieves a unique identifier of a document that is to be submitted.
     *
     * @param document the document that is to be submitted
     *
     * @return a uinque identifier of the document
     */
    protected String getDocumentId(final S document)
    {
        return hashGenerator.getShaHash(document.getSourceId());
    }


    @Override
    public void clear()
    {
        if (!batchMap.isEmpty()) {
            try {
                tryLoadingBatch();
            } catch (final LoaderException e) {
                logger.warn(LoaderConstants.CLEAN_LOAD_ERROR, e);
            }

            batchMap.clear();
        }

        currentBatchSize = 0;
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
     * The core of the loading function which is to be implemented by
     * subclasses. This method should load each document of the map to the destination URL
     * via a REST request.
     *
     * @param documents a map of documentIDs to documents that are to be
     *            loaded, the values may also be null, in which case the
     *            document is to be removed from the index
     */
    protected abstract void loadBatch(Map<String, S> documents);


    /**
     * Checks if the loader can start
     *
     * @return a message explaining if the pre-conditions failed
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
     * @throws LoaderException when the batch could not be loaded
     */
    protected void tryLoadingBatch() throws LoaderException
    {
        final int numberOfDocs = batchMap.size();

        try {
            // attempt to load the batch
            loadBatch(batchMap);

            // log success and send an event
            if (logger.isInfoEnabled()) {
                logger.info(
                    String.format(
                        LoaderConstants.LOADED_PARTIAL_OK, numberOfDocs));
            }
        } catch (final RuntimeException e) { // NOPMD exception depends on the implementation of loadBatch
            throw new LoaderException(e);
        }
    }


    /**
     * Retrieves the credentials that may be necessary for to authenticate the loader
     * with the URL.
     *
     * @return the credentials that may be necessary to authenticate the loader
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
     * Returns the loader target URL as a string.
     *
     * @return the loader target  URL as a string
     */
    protected String getUrl()
    {
        return urlParam.getStringValue();
    }
}
