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


import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;

import javax.xml.ws.http.HTTPException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.gerdiproject.harvest.IDocument;
import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.utils.HttpRequester;
import de.gerdiproject.harvest.utils.HttpRequester.RestRequestType;
import de.gerdiproject.json.GsonUtils;


/**
 * This Singleton class serves as a communicator for an Elastic Search node. An
 * URL and optionally a username and password must be set up first. Afterwards,
 * the harvested database can be uploaded.
 *
 * @author Robin Weiss
 */
public class ElasticSearchSender
{
    private static final String ERROR_PREFIX = "Cannot send search index to Elastic Search: ";
    private static final String EMPTY_INDEX_ERROR = ERROR_PREFIX + "JSON 'data' array is empty";
    private static final String NO_URL_ERROR = ERROR_PREFIX + "You need to set up a valid  Elastic Search URL";
    private static final String NO_MAPPINGS_ERROR = ERROR_PREFIX + "Could not create mappings. Is the Elastic Search URL correct and is the server up and running?";

    private static final String MAPPINGS_URL = "%s/%s?pretty";
    private static final String BASIC_MAPPING = "{\"mappings\":{\"%s\":{\"properties\":{}}}}";

    private static final String BATCH_POST_INSTRUCTION = "{\"index\":{\"_id\":\"%s\"}}%n%s%n";
    private static final String BULK_SUBMISSION_URL = "%s/%s/%s/_bulk?pretty";

    private static final String URL_SET_OK = "Set ElasticSearch URL to '%s/%s/%s/'.";
    private static final String URL_SET_FAILED = "Elastic Search URL '%s' is malformed!";

    private static final String SUBMISSION_START = "Submitting documents to '%s'...";
    private static final String SUBMISSION_DONE = "SUBMISSION DONE!";
    private static final String SUBMIT_ERROR_INDICATOR = "\"status\" : 400";
    private static final String SUBMIT_PARTIAL_OK = "Succcessfully submitted documents %d to %d";
    private static final String SUBMIT_PARTIAL_FAILED = "There were errors while submitting documents %d to %d:";
    private static final String SUBMIT_PARTIAL_FAILED_FORMAT = "%n\t%s of document '%s': %s - %s'";


    private static final String ID_JSON = "_id";
    private static final String INDEX_JSON = "index";
    private static final String ITEMS_JSON = "items";
    private static final String ERROR_JSON = "error";
    private static final String REASON_JSON = "reason";
    private static final String CAUSED_BY_JSON = "caused_by";
    private static final String TYPE_JSON = "type";

    private static final String NULL_JSON = "null";

    private static final int BULK_SUBMISSION_SIZE = 1024;

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticSearchSender.class);

    private static final ElasticSearchSender instance = new ElasticSearchSender();

    private final HttpRequester httpRequester;


    /**
     * Returns the Singleton instance of this class.
     *
     * @return a Singleton instance of this class
     */
    public static ElasticSearchSender instance()
    {
        return instance;
    }


    /**
     * Private constructor, because this is a singleton.
     *
     * @throws NoSuchAlgorithmException
     */
    private ElasticSearchSender()
    {
        httpRequester = new HttpRequester();
    }


    /**
     * Creates login credentials for a submission.
     *
     * @param userName the login user name
     * @param password the password for the user
     *
     * @return a base64 encoded username/password string
     */
    public String getCredentials(String userName, String password)
    {
        if (userName == null || password == null || userName.isEmpty())
            return null;
        else {
            return Base64.getEncoder()
                   .encodeToString((userName + ":" + password)
                                   .getBytes(MainContext.getCharset()));
        }
    }


    /**
     * Updates or creates a search index in Elastic Search.
     *
     * @param documents a JSON-array of searchable objects
     * @return the HTTP response of the Elastic Search POST request, or an error
     *         message if the operation does not succeed
     */
    public static String submit(String elasticSearchUrl, List<IDocument> documents)
    {

        // check if a URL has been set up
        if (elasticSearchUrl == null) {
            LOGGER.warn(NO_URL_ERROR);
            return NO_URL_ERROR;
        }

        // if the type does not exist on ElasticSearch yet, initialize it
        boolean hasMappings = validateAndCreateMappings();

        // if no mappings were created, abort
        if (!hasMappings) {
            LOGGER.error(NO_MAPPINGS_ERROR);
            return NO_MAPPINGS_ERROR;
        }

        // check if entries exist
        if (documents == null || documents.isEmpty()) {
            LOGGER.warn(EMPTY_INDEX_ERROR);
            return EMPTY_INDEX_ERROR;
        }

        LOGGER.info(String.format(SUBMISSION_START, elasticSearchUrl));

        // build a string for bulk-posting to Elastic search
        StringBuilder bulkRequestBuilder = new StringBuilder();
        HttpRequester httpRequester = new HttpRequester();
        int from = 0;

        try {
            for (int i = 0, len = documents.size(); i < len; i++) {
                IDocument doc = documents.get(i);
                String id = doc.getElasticSearchId();

                bulkRequestBuilder
                .append(String.format(BATCH_POST_INSTRUCTION, id, GsonUtils.getGson().toJson(doc)));

                // submit every 1024 posts, to decrease memory usage
                if ((i + 1) % BULK_SUBMISSION_SIZE == 0) {
                    // submit to elasticsearch
                    String response = httpRequester
                                      .getRestResponse(RestRequestType.POST, elasticSearchUrl, bulkRequestBuilder.toString(), credentials);

                    // handle response
                    handleSubmissionResponse(response, from, i);

                    // reset the string builder and free memory
                    bulkRequestBuilder = new StringBuilder();
                    from = i + 1;
                }
            }

            // send final POST request to Elastic search
            if (bulkRequestBuilder.length() > 0) {
                String response = httpRequester
                                  .getRestResponse(RestRequestType.POST, elasticSearchUrl, bulkRequestBuilder.toString(), credentials);

                // log response
                handleSubmissionResponse(response, from, documents.size() - 1);
            }
        } catch (Exception e) {
            LOGGER.error(ERROR_PREFIX, e);
            return e.toString();
        }

        LOGGER.info(SUBMISSION_DONE);
        return SUBMISSION_DONE;
    }


    /**
     * Checks if mappings exist for the index and type combination. If they do
     * not, they are created on the ElasticSearch node.
     *
     * @return true, if a mapping exists or was just created
     */
    public boolean validateAndCreateMappings()
    {
        HttpRequester httpRequester = new HttpRequester();
        String mappingsUrl = String.format(MAPPINGS_URL, baseUrl, index.toLowerCase());
        boolean hasMapping;

        try {
            httpRequester.getRestResponse(RestRequestType.GET, mappingsUrl, null);
            hasMapping = true;

        } catch (HTTPException e) {
            hasMapping = false;
        }

        if (!hasMapping) {
            try {
                // create mappings on ElasticSearch
                httpRequester.getRestResponse(
                    RestRequestType.PUT,
                    mappingsUrl,
                    String.format(BASIC_MAPPING, type),
                    null);
                hasMapping = true;

            } catch (HTTPException e) {
                hasMapping = false;
            }
        }

        return hasMapping;
    }


    /**
     * Handles the response from ElasticSearch that is sent after a bulk
     * submission. If any document failed, it will be logged and attempted to be
     * fixed.
     *
     * @param response
     *            the response string from ElasticSearch
     * @param from
     *            the index of the first document that was submitted in this
     *            bulk
     * @param to
     *            the index of the first document that is not submitted anymore
     */
    private void handleSubmissionResponse(String response, int from, int to)
    {
        // parse a json object from the response string
        JsonObject responseObj = GsonUtils.getGson().fromJson(response, JsonObject.class);
        JsonArray responseArray = responseObj.get(ITEMS_JSON).getAsJsonArray();

        // if the server response is not JSON, it's probably an error
        if (responseArray == null) {
            LOGGER.info(String.format(SUBMIT_PARTIAL_FAILED, from, to) + response);
            return;
        }

        // if the server response is JSON and does not contain "status: 400",
        // there are no errors
        if (response.indexOf(SUBMIT_ERROR_INDICATOR) == -1) {
            LOGGER.info(String.format(SUBMIT_PARTIAL_OK, from, to));
            return;
        }

        boolean hasErrors = false;
        StringBuilder errorBuilder = new StringBuilder(String.format(SUBMIT_PARTIAL_FAILED, from, to));

        // collect failed documents
        for (JsonElement r : responseArray) {
            // get the json object that holds the response to a single document
            JsonObject singleDocResponse = r.getAsJsonObject().get(INDEX_JSON).getAsJsonObject();

            // if document was transmitted successfully, check the next one
            if (singleDocResponse.get(ERROR_JSON) == null)
                continue;

            hasErrors = true;

            // log the error message
            String errorMessage = formatSubmissionError(singleDocResponse);
            errorBuilder.append(errorMessage);
        }

        // log failed documents
        if (hasErrors)
            LOGGER.error(errorBuilder.toString());
    }


    private String formatSubmissionError(JsonObject elasticSearchResponse)
    {
        JsonObject errorObject = elasticSearchResponse.get(ERROR_JSON).getAsJsonObject();
        final String documentId = elasticSearchResponse.get(ID_JSON).getAsString();

        // get the reason of the submission failure
        final String submitFailedReason = errorObject.has(REASON_JSON)
                                          ? errorObject.get(REASON_JSON).getAsString()
                                          : NULL_JSON;

        final JsonObject cause = errorObject.get(CAUSED_BY_JSON).getAsJsonObject();

        final String exceptionType = cause.has(TYPE_JSON)
                                     ? cause.get(TYPE_JSON).getAsString()
                                     : NULL_JSON;

        final String exceptionReason = cause.has(REASON_JSON)
                                       ? cause.get(REASON_JSON).getAsString()
                                       : NULL_JSON;

        // append document failure to error log
        return String.format(
                   SUBMIT_PARTIAL_FAILED_FORMAT,
                   submitFailedReason,
                   documentId,
                   exceptionType,
                   exceptionReason);
    }
}
