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
package de.gerdiproject.harvest.submission.elasticsearch;


import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.ServerException;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import com.google.gson.Gson;

import de.gerdiproject.harvest.IDocument;
import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.submission.AbstractSubmitter;
import de.gerdiproject.harvest.submission.elasticsearch.constants.ElasticSearchConstants;
import de.gerdiproject.harvest.submission.elasticsearch.json.ElasticSearchIndex;
import de.gerdiproject.harvest.submission.elasticsearch.json.ElasticSearchIndexWrapper;
import de.gerdiproject.harvest.submission.elasticsearch.json.ElasticSearchResponse;
import de.gerdiproject.harvest.utils.data.HttpRequester;
import de.gerdiproject.harvest.utils.data.enums.RestRequestType;


/**
 * This class serves as a communicator for an Elastic Search node. An URL and
 * optionally a username and password must be set up prior to the submission.
 *
 * @author Robin Weiss
 */
public class ElasticSearchSubmitter extends AbstractSubmitter
{
    private final HttpRequester httpRequester;
    private final Gson gson;


    /**
     * Constructor that initializes a Json parser for server responses.
     */
    public ElasticSearchSubmitter()
    {
        super();
        this.gson = new Gson();
        httpRequester = new HttpRequester(MainContext.getCharset(), gson);
    }


    @Override
    protected void submitBatch(Map<String, IDocument> documents) throws Exception // NOPMD - Exception is explicitly thrown, because it is up to the implementation which Exception causes the submission to fail
    {
        // clear previous batch
        final StringBuilder batchRequestBuilder = new StringBuilder();

        // build a string for bulk-posting to Elastic search
        documents.forEach(
            (String documentId, IDocument document) -> batchRequestBuilder.append(
                createBulkInstruction(documentId, document)));


        // send POST request to Elastic search
        String response = httpRequester.getRestResponse(
                              RestRequestType.POST,
                              url.toString(),
                              batchRequestBuilder.toString(),
                              credentials,
                              MediaType.APPLICATION_JSON);

        // parse JSON response
        ElasticSearchResponse responseJson = gson.fromJson(response, ElasticSearchResponse.class);

        // throw error if some documents could not be submitted
        if (responseJson.hasErrors())
            throw new ServerException(getSubmissionErrorText(responseJson));
    }


    /**
     * Retrieves errors from a JSON response and creates an error string out of
     * them.
     *
     * @param responseJson the JSON response to an ElasticSearch bulk submission
     *
     * @return an error string of failed submissions
     */
    private String getSubmissionErrorText(ElasticSearchResponse responseJson)
    {
        List<ElasticSearchIndexWrapper> submittedItems = responseJson.getItems();
        StringBuilder sb = new StringBuilder();

        for (int i = 0, l = submittedItems.size(); i < l; i++) {
            ElasticSearchIndex indexElement = submittedItems.get(i).getIndex();

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
    private String createBulkInstruction(String documentId, IDocument doc)
    {
        final String bulkInstruction;

        if (doc != null) {
            // convert document to JSON string and make DateRanges compatible with ElasticSearch
            String jsonString = toElasticSearchJson(doc);

            bulkInstruction = String.format(
                                  ElasticSearchConstants.BATCH_INDEX_INSTRUCTION,
                                  documentId,
                                  jsonString);
        } else
            bulkInstruction = String.format(ElasticSearchConstants.BATCH_DELETE_INSTRUCTION, documentId);

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


    @Override
    protected void setUrl(URL url)
    {
        if (url != null) {
            String rawPath = url.getPath() + '/';
            String[] path = rawPath.substring(1).split("/");
            String bulkSubmitUrl = url.toString();

            // check if the URL already is a bulk submission URL
            if (path.length == 0 || !path[path.length - 1].equals(ElasticSearchConstants.BULK_SUBMISSION_URL_SUFFIX)) {
                // extract URL without Query, add a slash if necessary
                int queryIndex = bulkSubmitUrl.indexOf('?');

                if (queryIndex != -1)
                    bulkSubmitUrl = bulkSubmitUrl.substring(0, queryIndex);

                if (bulkSubmitUrl.charAt(bulkSubmitUrl.length() - 1) != '/')
                    bulkSubmitUrl += '/';

                // add bulk suffix
                bulkSubmitUrl += ElasticSearchConstants.BULK_SUBMISSION_URL_SUFFIX;
            }

            try {
                // check if the URL is valid
                this.url = new URL(bulkSubmitUrl);
            } catch (MalformedURLException e) {
                this.url = null;
            }
        } else
            this.url = null;
    }


    @Override
    protected void setCredentials(String userName, String password)
    {
        super.setCredentials(userName, password);

        // prepend Basic-Authorization keyword
        if (this.credentials != null)
            this.credentials = ElasticSearchConstants.BASIC_AUTH_PREFIX + credentials;
    }


    @Override
    protected int getSizeOfDocument(String documentId, IDocument document)
    {
        return createBulkInstruction(documentId, document).getBytes(MainContext.getCharset()).length;
    }


}
