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
package de.gerdiproject.harvest.utils.data.constants;

import de.gerdiproject.harvest.config.parameters.BooleanParameter;
import de.gerdiproject.harvest.config.parameters.IntegerParameter;
import de.gerdiproject.harvest.config.parameters.constants.ParameterMappingFunctions;
import de.gerdiproject.harvest.utils.data.HttpRequester;

/**
 * This static class contains constants for the {@linkplain HttpRequester} and related classes.
 *
 * @author Robin Weiss
 */
public class DataOperationConstants
{
    public static final String SAVE_OK = "Saved file: %s";
    public static final String SAVE_FAILED = "Could not write to file: %s";
    public static final String SAVE_FAILED_NO_FOLDERS = "Could not write to file '%s': Failed to create directories!";
    public static final String LOAD_FAILED = "Could not load file: %s";

    public static final String CACHE_FOLDER_PATH = "savedHttpResponses";
    public static final String FILE_PATH = "%s/%sresponse.%s";
    public static final String FILE_ENDING_JSON = "json";
    public static final String FILE_ENDING_HTML = "html";

    public static final String WEB_ERROR_JSON = "Could not load and parse from web: %s";

    public static final String WEB_ERROR_HEADER = "Could retrieve %s-request header from URL: %s";
    public static final String WEB_ERROR_REST_RESPONSE = "%s-request error for URL '%s' with body '%s'.";
    public static final String WEB_ERROR_REST_HTTP = "%s-request for URL '%s' with body '%s' returned HTTP Status-Code %d.";
    public static final String REQUEST_PROPERTY_CHARSET = "charset";
    public static final String REDIRECT_LOCATION_HEADER = "Location";
    public static final int NO_TIMEOUT = -1;


    public static final String HTTP_CATEGORY = "HttpRequests";

    public static final BooleanParameter READ_FROM_DISK_PARAM =
        new BooleanParameter(
        "readFromDisk",
        HTTP_CATEGORY,
        false);

    public static final BooleanParameter WRITE_TO_DISK_PARAM =
        new BooleanParameter(
        "writeToDisk",
        HTTP_CATEGORY,
        false);

    public static final IntegerParameter RETRIES_PARAM =
        new IntegerParameter(
        "retries",
        HTTP_CATEGORY,
        -1,
        ParameterMappingFunctions::mapToUnsignedInteger);
    public static final String RETRY = "Could not reach %s! Retrying in %d s.";


    /**
     * Private constructor, because this class just serves
     * as a place to define constants.
     */
    private DataOperationConstants()
    {
    }
}
