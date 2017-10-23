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
import java.util.List;

import javax.xml.ws.http.HTTPException;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.gerdiproject.harvest.IDocument;
import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.utils.HttpRequester;
import de.gerdiproject.harvest.utils.HttpRequester.RestRequestType;
import de.gerdiproject.json.GsonUtils;


/**
 * This class serves as a communicator for an Elastic Search node. An
 * URL and optionally a username and password must be set up first. Afterwards,
 * the harvested database can be uploaded.
 *
 * @author Robin Weiss
 */
public class ElasticSearchSubmitter extends AbstractSubmitter
{
    private static final String NO_MAPPINGS_ERROR = "Could not create mappings. Is the Elastic Search URL correct and is the server up and running?";

    private static final String MAPPINGS_URL = "%s://%s/%s?pretty";
    private static final String MAPPINGS_URL_WITH_PORT = "%s://%s:%d/%s?pretty";
    private static final String BASIC_MAPPING = "{\"mappings\":{\"%s\":{\"properties\":{}}}}";

    private static final String BATCH_POST_INSTRUCTION = "{\"index\":{\"_id\":\"%s\"}}%n%s%n";
    private static final String BULK_SUBMISSION_URL_SUFFIX = "_bulk";

    private static final String SUBMIT_ERROR_INDICATOR = "\"status\" : 400";
    private static final String SUBMIT_PARTIAL_FAILED_FORMAT = "%n\t%s of document '%s': %s - %s'";

    private static final String ID_JSON = "_id";
    private static final String INDEX_JSON = "index";
    private static final String ITEMS_JSON = "items";
    private static final String ERROR_JSON = "error";
    private static final String REASON_JSON = "reason";
    private static final String CAUSED_BY_JSON = "caused_by";
    private static final String TYPE_JSON = "type";

    private static final String NULL_JSON = "null";


    @Override
    protected String submit(List<IDocument> documents, URL submissionUrl, String credentials)
    {
        final Configuration config = MainContext.getConfiguration();

        // if the type does not exist on ElasticSearch yet, initialize it
        boolean hasMappings = validateAndCreateMappings(config);

        // if no mappings were created, abort
        if (!hasMappings)
            return NO_MAPPINGS_ERROR;

        // build a string for bulk-posting to Elastic search
        StringBuilder bulkRequestBuilder = new StringBuilder();
        HttpRequester httpRequester = new HttpRequester();

        try {
            for (int i = 0, len = documents.size(); i < len; i++) {
                IDocument doc = documents.get(i);
                String id = doc.getElasticSearchId();

                bulkRequestBuilder.append(String.format(BATCH_POST_INSTRUCTION, id, GsonUtils.getGson().toJson(doc)));
            }

            // send POST request to Elastic search
            String response = httpRequester.getRestResponse(
                                  RestRequestType.POST,
                                  submissionUrl.toString(),
                                  bulkRequestBuilder.toString(),
                                  getCredentials(config)
                              );
            // log response
            String errorMessage = handleSubmissionResponse(response);
            return errorMessage;

        } catch (Exception e) {
            return e.toString();
        }
    }


    @Override
    protected URL getSubmissionUrl(Configuration config)
    {
        URL elasticSearchUrl = super.getSubmissionUrl(config);

        String[] path = elasticSearchUrl.getPath().substring(1).split("/");
        String bulkSubmitUrl = elasticSearchUrl.toString();

        // check if the URL already is a bulk submission URL
        if (!path[path.length - 1].equals(BULK_SUBMISSION_URL_SUFFIX)) {
            // extract URL without Query, add a slash if necessary
            bulkSubmitUrl = bulkSubmitUrl.substring(0, bulkSubmitUrl.indexOf("?"));

            if (bulkSubmitUrl.charAt(bulkSubmitUrl.length() - 1) != '/')
                bulkSubmitUrl += "/";

            // add bulk suffix
            bulkSubmitUrl += BULK_SUBMISSION_URL_SUFFIX;
        }

        try {
            return new URL(bulkSubmitUrl);
        } catch (MalformedURLException e) {
            return null;
        }
    }


    /**
     * Checks if mappings exist for the index and type combination. If they do
     * not, they are created on the ElasticSearch node.
     *
     * @param config the global configuration
     *
     * @return true, if a mapping exists or was just created
     */
    private boolean validateAndCreateMappings(Configuration config)
    {
        URL elasticSearchUrl = config.getParameterValue(ConfigurationConstants.SUBMISSION_URL, URL.class);

        // extract index and type from URL
        String[] path = elasticSearchUrl.getPath().substring(1).split("/");
        String index = path[0].toLowerCase();
        String type = path[1].toLowerCase();
        String mappingsUrl;

        // assemble mappings URL
        if (elasticSearchUrl.getPort() == -1)
            mappingsUrl = String.format(
                              MAPPINGS_URL,
                              elasticSearchUrl.getProtocol(),
                              elasticSearchUrl.getHost(),
                              index);
        else
            mappingsUrl = String.format(
                              MAPPINGS_URL_WITH_PORT,
                              elasticSearchUrl.getProtocol(),
                              elasticSearchUrl.getHost(),
                              elasticSearchUrl.getPort(),
                              index);

        HttpRequester httpRequester = new HttpRequester();
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
     * @param response the response string from ElasticSearch
     */
    private String handleSubmissionResponse(String response)
    {
        String errorMessage = response;

        // parse a json object from the response string
        JsonObject responseObj = GsonUtils.getGson().fromJson(response, JsonObject.class);
        JsonArray responseArray = responseObj.get(ITEMS_JSON).getAsJsonArray();

        // if the server response is not JSON, it's probably an error
        if (responseArray == null)
            return response;

        // if the server response is JSON and does not contain "status: 400",
        // there are no errors
        if (response.indexOf(SUBMIT_ERROR_INDICATOR) != -1) {

            StringBuilder errorBuilder = new StringBuilder();

            // collect failed documents
            for (JsonElement r : responseArray) {
                // get the json object that holds the response to a single document
                JsonObject singleDocResponse = r.getAsJsonObject().get(INDEX_JSON).getAsJsonObject();

                // if document was transmitted successfully, check the next one
                if (singleDocResponse.get(ERROR_JSON) != null) {
                    // append the error message
                    errorBuilder.append(formatSubmissionError(singleDocResponse));
                }
            }

            if (errorBuilder.length() != 0)
                errorMessage = errorBuilder.toString();

        }

        return errorMessage;
    }


    /**
     * Parses the ElasticSearchResponse to form a responsive error message.
     *
     * @param elasticSearchResponse a Json response from ElasticSearch
     *
     * @return a formatted error string
     */
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
