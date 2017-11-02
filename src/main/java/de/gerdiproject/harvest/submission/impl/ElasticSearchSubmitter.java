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
package de.gerdiproject.harvest.submission.impl;


import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.rmi.ServerException;
import java.util.Base64;
import java.util.List;

import javax.xml.ws.http.HTTPException;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.gerdiproject.harvest.IDocument;
import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.submission.AbstractSubmitter;
import de.gerdiproject.harvest.submission.constants.ElasticSearchConstants;
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
    private static final Base64.Encoder ENCODER = Base64.getEncoder();
    
    @Override
    protected void submitBatch(List<IDocument> documents, URL submissionUrl, String credentials) throws Exception // NOPMD - Exception is explicitly thrown, because it is up to the implementation which Exception causes the submission to fail
    {
        final Configuration config = MainContext.getConfiguration();

        // if the type does not exist on ElasticSearch yet, initialize it
        boolean hasMappings = validateAndCreateMappings(config);

        // if no mappings were created, abort
        if (!hasMappings)
            throw new IllegalStateException(ElasticSearchConstants.NO_MAPPING_ERROR);

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
                              credentials
                          );
        // log response
        String errorMessage = handleSubmissionResponse(response);

        // throw error if the server responds in an unexpected way
        if (errorMessage != null)
            throw new ServerException(errorMessage);
    }
    
    
    /**
     * Creates a single instruction for an ElasticSearch bulk-submission.
     * 
     * @param doc the document for which the instruction is created
     * 
     * @return a bulk-submission instruction for a single document
     */
    private String createBulkInstruction( IDocument doc)
    {
    	String jsonString = GsonUtils.getGson().toJson(doc, doc.getClass());
    	String id = new String(ENCODER.encode(jsonString.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        
        return String.format(ElasticSearchConstants.BATCH_POST_INSTRUCTION, id, jsonString);
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
                    bulkSubmitUrl += "/";

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

        // we need at least 2 path parts, one for the index and one for the type
        if (path.length < 2)
            return false;

        String index = path[path.length - 2].toLowerCase();
        String type = path[path.length - 1].toLowerCase();
        String mappingsUrl;

        // assemble all that comes before the index path
        StringBuilder hostBuilder = new StringBuilder(elasticSearchUrl.getHost());
        int i = 0;

        while (i < path.length - 2) {
            hostBuilder.append('/').append(path[i]);
            i++;
        }

        // assemble mappings URL
        if (elasticSearchUrl.getPort() == -1)
            mappingsUrl = String.format(
                              ElasticSearchConstants.MAPPINGS_URL,
                              elasticSearchUrl.getProtocol(),
                              hostBuilder.toString(),
                              index);
        else
            mappingsUrl = String.format(
                              ElasticSearchConstants.MAPPINGS_URL_WITH_PORT,
                              elasticSearchUrl.getProtocol(),
                              hostBuilder.toString(),
                              elasticSearchUrl.getPort(),
                              index);

        String credentials = getCredentials(config);
        HttpRequester httpRequester = new HttpRequester();
        boolean hasMapping;

        try {
            httpRequester.getRestResponse(RestRequestType.GET, mappingsUrl, null, credentials);
            hasMapping = true;

        } catch (HTTPException | IOException e) {
            hasMapping = false;
            logger.error(String.format(ElasticSearchConstants.NO_MAPPING_WARNING, index), e);
        }

        if (!hasMapping) {
            try {
                // create mappings on ElasticSearch
                httpRequester.getRestResponse(
                    RestRequestType.PUT,
                    mappingsUrl,
                    String.format(ElasticSearchConstants.BASIC_MAPPING, type),
                    credentials);

                hasMapping = true;
                logger.info(String.format(ElasticSearchConstants.MAPPING_CREATE_SUCCESS, mappingsUrl, type));

            } catch (HTTPException | IOException e) {
                hasMapping = false;
                logger.error(String.format(ElasticSearchConstants.MAPPING_CREATE_FAILURE, mappingsUrl, type), e);
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
        JsonArray responseArray = responseObj.get(ElasticSearchConstants.ITEMS_JSON).getAsJsonArray();

        // if the server response is not JSON, it's probably an error
        if (responseArray == null)
            return response;

        // if the server response is JSON and does not contain "status: 400",
        // there are no errors
        if (response.indexOf(ElasticSearchConstants.SUBMIT_ERROR_INDICATOR) != -1) {

            StringBuilder errorBuilder = new StringBuilder();

            // collect failed documents
            for (JsonElement r : responseArray) {
                // get the json object that holds the response to a single document
                JsonObject singleDocResponse = r.getAsJsonObject().get(ElasticSearchConstants.INDEX_JSON).getAsJsonObject();

                // if document was transmitted successfully, check the next one
                if (singleDocResponse.get(ElasticSearchConstants.ERROR_JSON) != null) {
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
        JsonObject errorObject = elasticSearchResponse.get(ElasticSearchConstants.ERROR_JSON).getAsJsonObject();
        final String documentId = elasticSearchResponse.get(ElasticSearchConstants.ID_JSON).getAsString();

        // get the reason of the submission failure
        final String submitFailedReason = errorObject.has(ElasticSearchConstants.REASON_JSON)
                                          ? errorObject.get(ElasticSearchConstants.REASON_JSON).getAsString()
                                          : ElasticSearchConstants.NULL_JSON;

        final JsonObject cause = errorObject.get(ElasticSearchConstants.CAUSED_BY_JSON).getAsJsonObject();

        final String exceptionType = cause.has(ElasticSearchConstants.TYPE_JSON)
                                     ? cause.get(ElasticSearchConstants.TYPE_JSON).getAsString()
                                     : ElasticSearchConstants.NULL_JSON;

        final String exceptionReason = cause.has(ElasticSearchConstants.REASON_JSON)
                                       ? cause.get(ElasticSearchConstants.REASON_JSON).getAsString()
                                       : ElasticSearchConstants.NULL_JSON;

        // append document failure to error log
        return String.format(
                   ElasticSearchConstants.SUBMIT_PARTIAL_FAILED_FORMAT,
                   submitFailedReason,
                   documentId,
                   exceptionType,
                   exceptionReason);
    }
}
