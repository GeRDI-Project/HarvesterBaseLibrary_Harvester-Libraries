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

    public static final String CACHE_FOLDER_PATH = "cache/%s/";

    @Deprecated
    public static final String UPDATE_CACHE_FILE_PATH =
        CACHE_FOLDER_PATH
        + "%s_updatedDocuments"
        + JSON_FILE_EXTENSION
        + JSON_FILE_EXTENSION;

    @Deprecated
    public static final String VERSIONS_CACHE_FILE_PATH =
        CACHE_FOLDER_PATH
        + "%s_documentVersions"
        + JSON_FILE_EXTENSION;

    public static final String HARVEST_TIME_KEEPER_CACHE_FILE_PATH =
        CACHE_FOLDER_PATH
        + "processTimes"
        + JSON_FILE_EXTENSION;

    public static final String STABLE_VERSIONS_FOLDER_PATH = CACHE_FOLDER_PATH + "documents/%s/versions";
    public static final String STABLE_CHANGES_FOLDER_PATH = CACHE_FOLDER_PATH + "documents/%s/latestChanges";
    public static final String TEMP_VERSIONS_FOLDER_PATH = CACHE_FOLDER_PATH + "documents_temp/%s/versions";
    public static final String TEMP_CHANGES_FOLDER_PATH = CACHE_FOLDER_PATH + "documents_temp/%s/latestChanges";

    public static final String DOCUMENT_HASH_FILE_PATH = "%s/%s/%s.json";
    public static final String SOURCE_HASH_FILE_PATH = "%s/sourceHash.json";

    public static final String DELETE_FILE_SUCCESS = "Deleted file '%s'.";
    public static final String DELETE_FILE_FAILED = "Could not delete file '%s'!";

    public static final String CACHE_CREATE_FAILED = "Could not create file '%s'!";
    public static final String CACHE_INIT_FAILED = "Could not initialize %s!";
    public static final String COPY_FILE_FAILED = "Could not copy file '%s' to '%s'!";

    // AbstractStreamHarvester
    public static final String CACHE_ENTRY_STREAM_PATH = CACHE_FOLDER_PATH + "StreamHarvester/%s" + JSON_FILE_EXTENSION;
    public static final String ENTRY_STREAM_WRITE_ERROR = "Could not write entries to file at path '%s'!";
    public static final String DIR_MERGE_FAILED_NOT_DIRS = "Could not merge %s into %s: Both paths must denote directories!";
    public static final String DIR_MERGE_FAILED = "Could not merge %s into %s!";


    /**
     * Private constructor, because this class just serves as a place to define
     * constants.
     */
    private CacheConstants()
    {
    }
}
