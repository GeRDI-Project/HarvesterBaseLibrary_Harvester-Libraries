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



import de.gerdiproject.harvest.utils.cache.DocumentsCache;

/**
 * This static class is a collection of constants, used by the {@linkplain DocumentsCache}.
 *
 * @author Robin Weiss
 */
public class CacheConstants
{
    public static final String JSON_FILE_EXTENSION = ".json";

    public static final String SAVE_FILE_NAME = "harvestedIndices/%s_result_%d" + JSON_FILE_EXTENSION;
    public static final String SAVE_FILE_NAME_PARTIAL = "harvestedIndices/%s_partialResult_%d-%d_%d" + JSON_FILE_EXTENSION;
    public static final String SAVE_FAILED_DIRECTORY = "Could not save documents: Unable to create directories!";
    public static final String SAVE_FAILED_ERROR = "Could not save harvested documents!";
    public static final String START_CACHE_ERROR = "Error starting the cache writer";
    public static final String FINISH_CACHE_ERROR = "Error closing the cache writer";

    public static final String CACHE_FOLDER_PATH = "cachedIndex/%s/";
    public static final String CACHE_FILE_PATH = CACHE_FOLDER_PATH + "cachedDocuments_%d" + JSON_FILE_EXTENSION;
    public static final String CACHE_FILE_REGEX = "cachedDocuments_\\d+\\" + JSON_FILE_EXTENSION;

    public static final String DELETE_FILE_SUCCESS = "Deleted old cache file '%s'.";
    public static final String DELETE_FILE_FAILED = "Could not deleted old cache file '%s'!";

    // AbstractStreamHarvester
    public static final String CACHE_ENTRY_STREAM_PATH = CACHE_FOLDER_PATH + "StreamHarvester/%s" + JSON_FILE_EXTENSION;
    public static final String ENTRY_STREAM_WRITE_ERROR = "Could not write entries to file at path '%s'!";


    /**
     * Private constructor, because this class just serves
     * as a place to define constants.
     */
    private CacheConstants()
    {
    }
}
