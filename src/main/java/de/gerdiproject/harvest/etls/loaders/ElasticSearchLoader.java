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


import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;

import javax.ws.rs.core.MediaType;
import javax.xml.ws.http.HTTPException;

import com.google.gson.Gson;

import de.gerdiproject.harvest.IDocument;
import de.gerdiproject.harvest.etls.loaders.constants.ElasticSearchConstants;
import de.gerdiproject.harvest.etls.loaders.json.ElasticSearchError;
import de.gerdiproject.harvest.etls.loaders.json.ElasticSearchIndex;
import de.gerdiproject.harvest.etls.loaders.json.ElasticSearchIndexWrapper;
import de.gerdiproject.harvest.etls.loaders.json.ElasticSearchResponse;
import de.gerdiproject.harvest.utils.data.WebDataRetriever;
import de.gerdiproject.harvest.utils.data.enums.RestRequestType;
import de.gerdiproject.json.GsonUtils;
import de.gerdiproject.json.datacite.DataCiteJson;


/**
 * This class serves as a communicator for an Elastic Search node. An URL and
 * optionally a username and password must be set up prior to the loader execution.
 *
 * @author Robin Weiss
 */
public class ElasticSearchLoader extends AbstractURLLoader<DataCiteJson>
{
    private final Gson gson;
    private final WebDataRetriever webRequester;


    /**
     * Constructor that initializes a Json parser for server responses.
     */
    public ElasticSearchLoader()
    {
        super();

        this.gson = GsonUtils.createGerdiDocumentGsonBuilder(ElasticSearchConstants.GEO_SHAPE_PRECISION).create();
        this.webRequester = new WebDataRetriever(gson, StandardCharsets.UTF_8);
    }


    @Override
    protected void loadBatch(final Map<String, DataCiteJson> documents)
    {
        // clear previous batch
        final StringBuilder batchRequestBuilder = new StringBuilder();

        // build a string for bulk-posting to Elastic search
        for (final Entry<String, DataCiteJson> entry : documents.entrySet()) {
            final String documentAddInstruction =
                createBulkInstruction(entry.getKey(), entry.getValue());
            batchRequestBuilder.append(documentAddInstruction);
        }

        // send POST request to Elastic search
        String response;

        try {
            response = webRequester.getRestResponse(
                           RestRequestType.POST,
                           getUrl(),
                           batchRequestBuilder.toString(),
                           getCredentials(),
                           MediaType.APPLICATION_JSON);
        } catch (HTTPException | IOException e) {
            throw new LoaderException(e);
        }

        // parse JSON response
        final ElasticSearchResponse responseJson = gson.fromJson(response, ElasticSearchResponse.class);

        // check if ElasticSearch responded with errors
        if (responseJson.hasErrors()) {
            // log the error
            logger.error(getSubmissionErrorText(responseJson));

            // try to fix documents that could not be parsed entirely
            final Map<String, DataCiteJson> fixedDocuments = fixInvalidDocuments(responseJson, documents);

            // if documents can be fixed, attepmt to resubmit them
            if (!fixedDocuments.isEmpty()) {
                logger.warn(ElasticSearchConstants.DOCUMENTS_RESUBMIT);
                loadBatch(fixedDocuments);
            }
        }
    }


    /**
     * Retrieves errors from a JSON response and creates an error string out of
     * them.
     *
     * @param responseJson the JSON response to an ElasticSearch bulk submission
     *
     * @return an error string of failed submissions
     */
    private String getSubmissionErrorText(final ElasticSearchResponse responseJson)
    {
        final List<ElasticSearchIndexWrapper> loadedItems = responseJson.getItems();
        final StringBuilder sb = new StringBuilder();
        final int len = loadedItems.size();

        for (int i = 0; i < len; i++) {
            final ElasticSearchIndex indexElement = loadedItems.get(i).getIndex();

            if (indexElement.getError() != null) {
                if (sb.length() > 0)
                    sb.append('\n');

                // append error text
                sb.append(indexElement.getErrorText());
            }
        }

        return sb.toString();
    }


    /**
     * Creates a single instruction for an ElasticSearch bulk-submission.
     *
     * @param doc the document for which the instruction is created
     *
     * @return a bulk-submission instruction for a single document
     */
    private String createBulkInstruction(final String documentId, final IDocument doc)
    {
        final String bulkInstruction;

        if (doc == null)
            bulkInstruction = String.format(ElasticSearchConstants.BATCH_DELETE_INSTRUCTION, documentId);
        else {
            // convert document to UTF-8 JSON string and make DateRanges compatible with ElasticSearch
            final String jsonString;

            if (charset == StandardCharsets.UTF_8)
                jsonString = toElasticSearchJson(doc);
            else
                jsonString = new String(toElasticSearchJson(doc).getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

            bulkInstruction = String.format(
                                  ElasticSearchConstants.BATCH_INDEX_INSTRUCTION,
                                  documentId,
                                  jsonString);
        }

        return bulkInstruction;
    }


    /**
     * Converts a document to an ElasticSearch JSON string by mapping regular
     * dates to date ranges.
     *
     * @param document the document of which the ElasticSearch JSON string is
     *            generated
     *
     * @return an ElasticSearch compatible JSON string
     */
    private String toElasticSearchJson(final IDocument document)
    {
        final String jsonString = document.toJson();

        return jsonString.replaceAll(
                   ElasticSearchConstants.DATE_RANGE_REGEX,
                   ElasticSearchConstants.DATE_RANGE_REPLACEMENT).replaceAll(
                   ElasticSearchConstants.DATE_REGEX,
                   ElasticSearchConstants.DATE_REPLACEMENT);
    }


    /**
     * Parses an {@linkplain ElasticSearchResponse}, retrieving potential error messages.
     * If at least one error message was caused by Elasticsearch trying to parse an invalid
     * field, the corresponding field is removed. All documents that can be hotfixed in that
     * manner are returned in a map of document IDs to documents.
     *
     * @param response an Elasticsearch response JSON object
     * @param documents the map of originally submitted documents
     *
     * @return a map of documents with removed invalid fields
     */
    private Map<String, DataCiteJson> fixInvalidDocuments(final ElasticSearchResponse response, final Map<String, DataCiteJson> documents)
    {
        final Map<String, DataCiteJson> fixedDocMap = new HashMap<>(); // NOPMD map is not used concurrently

        for (final ElasticSearchIndexWrapper documentFeedback : response.getItems()) {
            final ElasticSearchError docError = documentFeedback.getIndex().getError();

            if (docError != null) {
                final String documentId = documentFeedback.getIndex().getId();
                final DataCiteJson doc = documents.get(documentId);

                if (doc != null && tryFixInvalidDocument(doc, docError))
                    fixedDocMap.put(documentId, doc);
            }
        }

        return fixedDocMap;
    }


    /**
     * Attempts to fix a document that could not be submitted to Elasticsearch,
     * by removing fields that caused parsing errors.
     *
     * @param errorDocument the document that could not be submitted
     * @param docError a JSON error object containing error details
     *
     * @return true if an error was found and fixed, otherwise false
     */
    private boolean tryFixInvalidDocument(final DataCiteJson errorDocument, final ElasticSearchError docError)
    {
        // check if a specific field could not be parsed
        final Matcher errorReasonMatcher =
            ElasticSearchConstants.PARSE_ERROR_REASON_PATTERN.matcher(docError.getReason());

        if (errorReasonMatcher.find()) {
            final String invalidFieldName = errorReasonMatcher.group(1);

            // try to remove the invalid field
            try {
                final Field invalidField = errorDocument.getClass().getDeclaredField(invalidFieldName);

                // replace with "invalidField.canAccess()" in Java9 or higher
                final boolean accessibility = invalidField.isAccessible();
                invalidField.setAccessible(true);
                invalidField.set(errorDocument, null);
                invalidField.setAccessible(accessibility);

                logger.debug(String.format(
                                 ElasticSearchConstants.FIXED_INVALID_DOCUMENT,
                                 invalidFieldName,
                                 getDocumentId(errorDocument)));
                return true;
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                logger.warn(String.format(
                                ElasticSearchConstants.CANNOT_FIX_INVALID_DOCUMENT_ERROR,
                                invalidFieldName,
                                getDocumentId(errorDocument)));
            }
        }

        return false;
    }


    @Override
    protected String getUrl()
    {

        if (urlParam.getValue() == null)
            return null;

        final String rawPath;

        try {
            rawPath = new URL(urlParam.getValue()).getPath() + '/';
        } catch (final MalformedURLException e) {
            logger.error(String.format(ElasticSearchConstants.INVALID_URL_ERROR, urlParam.getValue()));
            return null;
        }

        final String[] path = rawPath.substring(1).split("/");
        final String rawUrl = urlParam.getStringValue();

        // check if the URL requires the bulk submission suffix
        if (path.length == 0 || !path[path.length - 1].equals(ElasticSearchConstants.BULK_SUBMISSION_URL_SUFFIX)) {
            final StringBuilder bulkSubmitUrlBuilder = new StringBuilder();

            // extract URL without Query, add a slash if necessary
            final int queryIndex = rawUrl.indexOf('?');
            final char lastChar;

            // remove potential query parameters
            if (queryIndex == -1) {
                bulkSubmitUrlBuilder.append(rawUrl);
                lastChar = rawUrl.charAt(rawUrl.length() - 1);
            } else {
                bulkSubmitUrlBuilder.append(rawUrl.substring(0, queryIndex));
                lastChar = rawUrl.charAt(queryIndex - 1);
            }

            // add slash if it is missing
            if (lastChar != '/')
                bulkSubmitUrlBuilder.append('/');

            // add bulk suffix
            bulkSubmitUrlBuilder.append(ElasticSearchConstants.BULK_SUBMISSION_URL_SUFFIX);

            // return assembled url
            return bulkSubmitUrlBuilder.toString();
        } else
            return rawUrl;
    }


    @Override
    protected String getCredentials()
    {
        final String credentials = super.getCredentials();

        // prepend Basic-Authorization keyword
        if (credentials != null)
            return ElasticSearchConstants.BASIC_AUTH_PREFIX + credentials;

        return null;
    }


    @Override
    protected int getSizeOfDocument(final String documentId, final IDocument document)
    {
        return createBulkInstruction(documentId, document).getBytes(StandardCharsets.UTF_8).length;
    }


}
