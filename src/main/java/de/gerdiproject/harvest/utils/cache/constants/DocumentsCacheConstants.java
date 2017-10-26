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
package de.gerdiproject.harvest.utils.cache.constants;


import de.gerdiproject.harvest.utils.cache.DocumentsCache;

/**
 * This static class is a collection of constants, used by the {@linkplain DocumentsCache}.
 *
 * @author Robin Weiss
 */
public class DocumentsCacheConstants
{
    public static final String SAVE_FILE_NAME = "harvestedIndices/%s_result_%d.json";
    public static final String SAVE_FILE_NAME_PARTIAL = "harvestedIndices/%s_partialResult_%d-%d_%d.json";
    public static final String SAVE_FAILED_DIRECTORY = "Could not save documents: Unable to create directories!";
    public static final String SAVE_FAILED_ERROR = "Could not save harvested documents!";
    public static final String CACHE_FILE_PATH = "cachedIndex/%s/cachedDocuments_%d.json";
    public static final String START_CACHE_ERROR = "Error starting the cache writer";
    public static final String FINISH_CACHE_ERROR = "Error closing the cache writer";


    /**
     * Private constructor, because this is a static class.
     */
    private DocumentsCacheConstants()
    {

    }
}
