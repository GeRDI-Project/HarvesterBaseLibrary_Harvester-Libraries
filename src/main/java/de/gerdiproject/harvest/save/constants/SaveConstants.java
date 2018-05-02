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
package de.gerdiproject.harvest.save.constants;

import de.gerdiproject.harvest.save.HarvestSaver;

/**
 * This static class is a collection of constants that are used by the
 * {@linkplain HarvestSaver}.
 *
 * @author Robin Weiss
 */
public class SaveConstants
{
    public static final String HARVEST_DATE_JSON = "harvestDate";
    public static final String DURATION_JSON = "durationInSeconds";
    public static final String IS_FROM_DISK_JSON = "wasHarvestedFromDisk";

    public static final String SAVE_FOLDER_NAME = "harvestedIndices/";
    public static final String SAVE_FILE_NAME = SAVE_FOLDER_NAME + "%s_result_%d.json";
    public static final String SAVE_FILE_NAME_PARTIAL =
        SAVE_FOLDER_NAME + "%s_partialResult_%d-%d_%d.json";
    public static final String SAVE_FAILED_EMPTY = "Could not save documents: There are no changes to save!";


    public static final String SAVE_START = "Saving documents to: %s";
    public static final String SAVE_OK = "Saving done!";
    public static final String SAVE_FAILED = "Saving failed!";
    public static final String SAVE_INTERRUPTED = "Saving interrupted unexpectedly!";


    /**
     * Private constructor, because this class just serves as a place to define
     * constants.
     */
    private SaveConstants()
    {
    }
}
