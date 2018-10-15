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
package de.gerdiproject.harvest.etls.loaders.constants;

import de.gerdiproject.harvest.config.parameters.StringParameter;
import de.gerdiproject.harvest.etls.loaders.DiskLoader;

/**
 * This static class is a collection of constants that are used by the
 * {@linkplain DiskLoader}.
 *
 * @author Robin Weiss
 */
public class SaveConstants
{
    public static final String HARVEST_DATE_JSON = "harvestDate";
    public static final String DURATION_JSON = "durationInSeconds";
    public static final String SOURCE_HASH_JSON = "sourceHash";

    public static final String SAVE_FOLDER_NAME =  "savedDocuments/";
    public static final String JSON_EXTENSION =  ".json";

    public static final String SAVE_START = "Saving documents to: %s";
    public static final String SAVE_OK = "Saving done!";
    public static final String SAVE_FAILED_EXCEPTION = "Could not download harvest: %s!";
    public static final String SAVE_FAILED_EMPTY = "Could not download harvest: There are no changes to save!";
    public static final String SAVE_FAILED_CANNOT_CREATE = "Could not download harvest: Could not create file '%s' on the server!";

    public static final String DOCUMENTS_JSON = "documents";

    public static final String CONTENT_HEADER = "Content-Disposition";
    public static final String CONTENT_HEADER_VALUE = "attachment; filename=\"%s\"";

    public static final StringParameter FILE_PATH_PARAM = new StringParameter(
        "saveFolder",
        LoaderConstants.PARAMETER_CATEGORY,
        SAVE_FOLDER_NAME);


    /**
     * Private constructor, because this class just serves as a place to define
     * constants.
     */
    private SaveConstants()
    {
    }
}
