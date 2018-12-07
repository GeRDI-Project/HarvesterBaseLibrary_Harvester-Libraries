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
package de.gerdiproject.harvest.config.constants;

import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.utils.file.constants.FileConstants;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * This static class is a collection of constants, commonly used for classes
 * that deal with the {@linkplain Configuration}.
 *
 * @author Robin Weiss
 */
@NoArgsConstructor(access=AccessLevel.PRIVATE)
public class ConfigurationConstants
{
    public static final String NO_CHANGES = "No parameters were changed!";
    public static final String SET_NO_PAYLOAD_ERROR = "Cannot change parameters: Missing request body!";
    public static final String SET_UNKNOWN_PARAM_ERROR = "Cannot change parameter '%s'. Unknown parameter!";
    public static final String GET_UNKNOWN_PARAM_ERROR = "Unknown parameter '%s'!";

    public static final String CONFIG_PATH = FileConstants.CACHE_FOLDER_PATH + "config.json";

    public static final String REGISTERED_PARAM = "Added new %s '%s' with value '%s' to the config.";
    public static final String LOADED_PARAM = "Loaded %s '%s' with value '%s'.";
    public static final String LOAD_OK = "Loaded configuration from '%s'.";
    public static final String LOAD_ERROR = "Could not load configuration from '%s': %s";
    public static final String NO_CONFIG_FILE_ERROR = "Did not load configuration, because it is not cached at '%s'!";
    public static final String OUTDATED_CONFIG_FILE_ERROR = "Did not load configuration, because it is outdated!";
    public static final String NO_PATH = "You must set a path first!";

    public static final String QUERY_KEY = "key";
    public static final String ALLOWED_REQUESTS =
        "GET\n"
        + "Returns the entire configuration in pretty text.\n\n"
        + "GET ?" + QUERY_KEY + "=xxx\n"
        + "Returns the value of parameter 'xxx'.\n\n"
        + "POST _set {\"XXX.YYY\" : \"ZZZ\"}\n"
        + "Changes the value of parameter YYY of category XXX to ZZZ."
        + "PUT\n"
        + "Sets x-www-form-urlencoded parameters for the harvester.\n"
        + "Valid keys: ";

    public static final String SAVE_NO_PATH_ERROR = "Cannot save configuration: " + NO_PATH;
    public static final String PARSE_ERROR = "Cannot read configuration parameter value '%s' from key '%s'!";
    public static final String REGISTER_ERROR = "Cannot register parameter '%s', because no Configuration with event listeners exists, yet!";

    public static final String BASIC_PARAMETER_FORMAT = "%%n%%1$-%ds :  %%2$s";
    public static final String CATEGORY_FORMAT = "- %s -";

    public static final String ENVIRONMENT_VARIABLE_SET_START = "Searching for configuration from environment variables...";

    public static final String DEBUG_CATEGORY = "Debug";
}
