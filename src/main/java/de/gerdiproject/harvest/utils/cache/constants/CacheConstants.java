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
package de.gerdiproject.harvest.utils.cache.constants;


import de.gerdiproject.harvest.utils.cache.HarvesterCache;

/**
 * This static class is a collection of constants, used by
 * {@linkplain HarvesterCache}s.
 *
 * @author Robin Weiss
 */
public class CacheConstants
{
    public static final String JSON_FILE_EXTENSION = ".json";
    public static final String TEMP_FILE_EXTENSION = ".tmp";

    public static final String START_CACHE_ERROR = "Error starting the cache writer";
    public static final String FINISH_CACHE_ERROR = "Error closing the cache writer";

    public static final String CACHE_FOLDER_PATH = "cache/%s/";

    public static final String UPDATE_CACHE_FILE_NAME = "%s_updatedDocuments" + JSON_FILE_EXTENSION;
    public static final String UPDATE_CACHE_FILE_PATH = CACHE_FOLDER_PATH
                                                        + UPDATE_CACHE_FILE_NAME
                                                        + JSON_FILE_EXTENSION;

    public static final String UPDATE_CACHE_TEMP_FILE_PATH = CACHE_FOLDER_PATH
                                                             + "%s_updatedDocumentsPending"
                                                             + JSON_FILE_EXTENSION;

    public static final String HARVEST_TIME_KEEPER_CACHE_FILE_PATH = CACHE_FOLDER_PATH
                                                                     + "processTimes"
                                                                     + JSON_FILE_EXTENSION;

    public static final String OLD_VERSIONS_CACHE_FILE_PATH = CACHE_FOLDER_PATH
                                                              + "%s_documentVersions"
                                                              + JSON_FILE_EXTENSION;

    public static final String OLD_VERSIONS_CACHE_TEMP_FILE_PATH = CACHE_FOLDER_PATH
                                                                   + "%s_documentVersionsPending"
                                                                   + JSON_FILE_EXTENSION;


    public static final String STABLE_FOLDER_PATH = CACHE_FOLDER_PATH + "documents/%s";
    public static final String TEMP_FOLDER_PATH = CACHE_FOLDER_PATH + "documents_temp/%s";

    public static final String VERSION_FILE_PATH = "%s/versions/%s/%s.json";
    public static final String DOCUMENT_FILE_PATH = "%s/latestChanges/%s/%s.json";
    public static final String SOURCE_HASH_FILE_PATH = "%s/versions/sourceHash.json";



    public static final String DELETE_FILE_SUCCESS = "Deleted file '%s'.";
    public static final String DELETE_FILE_FAILED = "Could not delete file '%s'!";

    public static final String CACHE_CREATE_FAILED = "Could not create file '%s'!";
    public static final String CACHE_INIT_FAILED = "Could not initialize %s!";
    public static final String COPY_FILE_FAILED = "Could not copy file '%s' to '%s'!";

    // DocumentVersionsCache
    public static final String HARVESTER_VALUES_JSON = "harvesterValues";
    public static final String HARVESTER_SOURCE_HASH_JSON = "sourceHash";
    public static final String HARVESTER_FROM_JSON = "rangeFrom";
    public static final String HARVESTER_TO_JSON = "rangeTo";

    // AbstractStreamHarvester
    public static final String CACHE_ENTRY_STREAM_PATH = CACHE_FOLDER_PATH + "StreamHarvester/%s" + JSON_FILE_EXTENSION;
    public static final String ENTRY_STREAM_WRITE_ERROR = "Could not write entries to file at path '%s'!";


    /**
     * Private constructor, because this class just serves as a place to define
     * constants.
     */
    private CacheConstants()
    {
    }
}
