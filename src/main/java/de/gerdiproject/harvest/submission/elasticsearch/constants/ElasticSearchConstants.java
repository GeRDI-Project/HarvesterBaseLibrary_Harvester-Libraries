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
package de.gerdiproject.harvest.submission.elasticsearch.constants;

/**
 * This static class is a collection of constants that relate to the submission of documents to ElasticSearch.
 *
 * @author Robin Weiss
 */
public class ElasticSearchConstants
{
    public static final String NO_MAPPING_ERROR = "No Mappings exist or could be created.\nIs the Elastic Search URL correct and is the server available?";
    public static final String NO_MAPPING_WARNING = "Elastic-Search Mapping for index '%s' does NOT exist.";
    public static final String MAPPING_CREATE_SUCCESS = "Created Elastic-Search Mapping for index '%s' and type '%s'.";
    public static final String MAPPING_CREATE_FAILURE = "Could not create Elastic-Search Mapping for index '%s' and type '%s'.";

    public static final String MAPPINGS_URL = "%s://%s/%s?pretty";
    public static final String MAPPINGS_URL_WITH_PORT = "%s://%s:%d/%s?pretty";
    public static final String BASIC_MAPPING = "{\"mappings\":{\"%s\":{\"properties\":{}}}}";

    public static final String BATCH_POST_INSTRUCTION = "{\"index\":{\"_id\":\"%s\"}}%n%s%n";
    public static final String BULK_SUBMISSION_URL_SUFFIX = "_bulk";


    // DATE RANGE FIX
    public static final String DATE_REGEX = "\"value\":(\"\\d\\d\\d\\d-\\d\\d-\\d\\dT\\d\\d:\\d\\d[^\"/]*Z\")";
    public static final String DATE_REPLACEMENT = "\"value\":\\{\"gte\":$1,\"lte\":$1\\}";

    public static final String DATE_RANGE_REGEX = "\"value\":\"(\\d\\d\\d\\d-\\d\\d-\\d\\dT\\d\\d:\\d\\d[^\"]*?Z)?/(\\d\\d\\d\\d-\\d\\d-\\d\\dT\\d\\d:\\d\\d[^\"]*?Z)?\"";
    public static final String DATE_RANGE_REPLACEMENT = "\"value\":\\{\"gte\":\"$1\",\"lte\":\"$2\"\\}";
    public static final String EMPTY_DATE_RANGE_REGEX = "(\"gte\":\"\",)|(,\"lte\":\"\")";
    public static final String EMPTY_DATE_RANGE_REPLACEMENT = "";

    // SERVER RESPONSE JSON
    public static final String DOCUMENT_SUBMIT_ERROR = "Could not submit document #%d%n%s";
    public static final String DOCUMENT_SUBMIT_ERROR_REASON = "  %s: %s";
    public static final String DOCUMENT_SUBMIT_ERROR_CAUSE = ", caused by%n  %s: %s";

    /**
     * Private constructor, because this is a static class.
     */
    private ElasticSearchConstants()
    {
    }
}