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


import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.ServerException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.ws.rs.core.MediaType;

import de.gerdiproject.harvest.IDocument;
import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.harvester.constants.HarvesterConstants;
import de.gerdiproject.harvest.submission.AbstractSubmitter;
import de.gerdiproject.harvest.submission.elasticsearch.constants.ElasticSearchConstants;
import de.gerdiproject.harvest.submission.elasticsearch.json.ElasticSearchIndex;
import de.gerdiproject.harvest.submission.elasticsearch.json.ElasticSearchIndexWrapper;
import de.gerdiproject.harvest.submission.elasticsearch.json.ElasticSearchResponse;
import de.gerdiproject.harvest.utils.data.HttpRequester;
import de.gerdiproject.harvest.utils.data.HttpRequester.RestRequestType;
import de.gerdiproject.json.GsonUtils;


/**
 * This class serves as a communicator for an Elastic Search node. An
 * URL and optionally a username and password must be set up prior to the submission.
 *
 * @author Robin Weiss
 */
public class ElasticSearchSubmitter extends AbstractSubmitter
{
    private final MessageDigest messageDigest;


    /**
     * Constructor that initializes the SHA-MessageDigest for creating IDs from documents.
     */
    public ElasticSearchSubmitter()
    {
        // initialize the SHA Digest fo creating ElasticSearch IDs
        MessageDigest md;

        try {
            md = MessageDigest.getInstance(HarvesterConstants.SHA_HASH_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            // this should never ever happen
            md = null;
        }

        messageDigest = md;
    }


    @Override
    protected void submitBatch(List<IDocument> documents, URL submissionUrl, String credentials) throws Exception // NOPMD - Exception is explicitly thrown, because it is up to the implementation which Exception causes the submission to fail
    {
        // build a string for bulk-posting to Elastic search
        StringBuilder bulkRequestBuilder = new StringBuilder();
        HttpRequester httpRequester = new HttpRequester();

        for (int i = 0, len = documents.size(); i < len; i++)
            bulkRequestBuilder.append(createBulkInstruction(documents.get(i)));

        // send POST request to Elastic search
        String response = httpRequester.getRestResponse(
                              RestRequestType.POST,
                              submissionUrl.toString(),
                              bulkRequestBuilder.toString(),
                              credentials,
                              MediaType.APPLICATION_JSON
                          );

        // parse JSON response
        ElasticSearchResponse responseJson = GsonUtils.getGson().fromJson(response, ElasticSearchResponse.class);

        // throw error if some documents could not be submitted
        if (responseJson.hasErrors())
            throw new ServerException(getSubmissionErrorText(responseJson, documents));
    }


    /**
     * Retrieves errors from a JSON response and creates an error string out of them.
     *
     * @param responseJson the JSON response to an ElasticSearch bulk submission
     * @param documents the submitted documents that caused the issues
     *
     * @return an error string of failed submissions
     */
    private String getSubmissionErrorText(ElasticSearchResponse responseJson, List<IDocument> documents)
    {
        List<ElasticSearchIndexWrapper> submittedItems = responseJson.getItems();
        StringBuilder sb = new StringBuilder();

        for (int i = 0, l = submittedItems.size(); i < l; i++) {
            ElasticSearchIndex indexElement = submittedItems.get(i).getIndex();

            if (indexElement.getError() != null) {
                if (sb.length() > 0)
                    sb.append('\n');

                // append error text
                sb.append(indexElement.getErrorText(submittedDocumentCount + i)).append('\n');

                // append failed document
                sb.append(toElasticSearchJson(GsonUtils.getGson().toJson(documents.get(i), documents.get(i).getClass()))).append('\n');
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
    private String createBulkInstruction(IDocument doc)
    {
        // convert document to JSON string and make DateRanges compatible with ElasticSearch
        String jsonString = toElasticSearchJson(GsonUtils.getGson().toJson(doc, doc.getClass()));
        String id = getDocumentId(jsonString);

        return String.format(ElasticSearchConstants.BATCH_POST_INSTRUCTION, id, jsonString);
    }


    /**
     * Converts a regular JSON string to an ElasticSearch JSON string by formatting dates to date ranges.
     *
     * @param jsonString the source JSON string
     *
     * @return an ElasticSearch compatible JSON string
     */
    private String toElasticSearchJson(String jsonString)
    {
        return jsonString
               .replaceAll(ElasticSearchConstants.DATE_RANGE_REGEX, ElasticSearchConstants.DATE_RANGE_REPLACEMENT)
               .replaceAll(ElasticSearchConstants.DATE_REGEX, ElasticSearchConstants.DATE_REPLACEMENT);
    }


    /**
     * Retrieves the SHA-hash of a String and returns it to be used as an ID
     * for ElasticSearch.
     *
     * @param documentJson the JSON string of the document
     *
     * @return a SHA-hash in octat format
     */
    protected String getDocumentId(String documentJson)
    {
        messageDigest.update(documentJson.getBytes(MainContext.getCharset()));

        final byte[] digest = messageDigest.digest();

        final StringWriter buffer = new StringWriter(digest.length * 2);
        final PrintWriter pw = new PrintWriter(buffer);

        for (byte b : digest)
            pw.printf(HarvesterConstants.OCTAT_FORMAT, b);

        pw.close();

        return buffer.toString();
    }


    @Override
    protected URL getSubmissionUrl(Configuration config)
    {
        URL elasticSearchUrl = super.getSubmissionUrl(config);
        URL bulkSubmissionUrl = null;

        if (elasticSearchUrl != null) {
            String[] path = elasticSearchUrl.getPath().substring(1).split("/");
            String bulkSubmitUrl = elasticSearchUrl.toString();

            // check if the URL already is a bulk submission URL
            if (!path[path.length - 1].equals(ElasticSearchConstants.BULK_SUBMISSION_URL_SUFFIX)) {
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
                bulkSubmissionUrl = new URL(bulkSubmitUrl);
            } catch (MalformedURLException e) {
                bulkSubmissionUrl = null;
            }
        }

        return bulkSubmissionUrl;
    }

    @Override
    protected String getCredentials(Configuration config)
    {
        String credentials = super.getCredentials(config);

        // prepend Basic-Authorization keyword
        if (credentials != null)
            return "Basic " + credentials;
        else
            return null;
    }
}
