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
package de.gerdiproject.harvest.submission.constants;

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

    public static final String SUBMIT_ERROR_INDICATOR = "\"status\" : 400";
    public static final String SUBMIT_PARTIAL_FAILED_FORMAT = "%n\t%s of document '%s': %s - %s'";

    public static final String ID_JSON = "_id";
    public static final String INDEX_JSON = "index";
    public static final String ITEMS_JSON = "items";
    public static final String ERROR_JSON = "error";
    public static final String REASON_JSON = "reason";
    public static final String CAUSED_BY_JSON = "caused_by";
    public static final String TYPE_JSON = "type";

    public static final String NULL_JSON = "null";

    /**
     * Private constructor, because this is a static class.
     */
    private ElasticSearchConstants()
    {
    }
}
