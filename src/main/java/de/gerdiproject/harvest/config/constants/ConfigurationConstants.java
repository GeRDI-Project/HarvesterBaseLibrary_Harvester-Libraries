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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.utils.file.constants.FileConstants;

/**
 * This static class is a collection of constants, commonly used for classes
 * that deal with the {@linkplain Configuration}.
 *
 * @author Robin Weiss
 */
public class ConfigurationConstants
{
    public static final String CHANGED_PARAM = "Set parameter '%s' to '%s'.";
    public static final String REGISTERED_PARAM = "Added new %s '%s' with value '%s' to the config.";
    public static final String LOADED_PARAM = "Loaded %s '%s' with value '%s'.";

    public static final String INTEGER_VALUE_MAX = "max";
    public static final String INTEGER_VALUE_MIN = "min";

    public static final List<String> BOOLEAN_VALID_VALUES_LIST =
        Collections.unmodifiableList(Arrays.asList("1", "0", "true", "false"));

    public static final String PASSWORD_STRING_TEXT = "*****";


    public static final String CANNOT_CHANGE_PARAM =
        "Cannot change value of parameter '%s' to '%s'! %s";

    public static final String ALLOWED_VALUES = "Allowed values are: ";
    public static final String BOOLEAN_ALLOWED_VALUES = ALLOWED_VALUES + "0, 1, true, false";
    public static final String INTEGER_RANGE_ALLOWED_VALUES = ALLOWED_VALUES + INTEGER_VALUE_MAX + ", " + INTEGER_VALUE_MIN + ", 0, 1, ...";
    public static final String INTEGER_ALLOWED_VALUES = ALLOWED_VALUES + String.format("[%d, %d]", Integer.MIN_VALUE, Integer.MAX_VALUE);

    public static final String URL_PARAM_INVALID = "It must be a valid URL!";
    public static final String ETL_PARAM_INVALID_STATE = "The '%s' must be idle, but it is currently %s!";
    public static final String ETL_REGISTRY_PARAM_INVALID_STATE = "All ETLs must be idle, but they are currently %s!";

    public static final String NO_CHANGES = "No parameters were changed!";
    public static final String UNKNOWN_PARAM = "Cannot change parameter '%s'. Unknown parameter!";

    public static final String CONFIG_PATH = FileConstants.CACHE_FOLDER_PATH + "config.json";
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

    public static final String SAVE_NO_PATH_ERROR = "Could not save configuration: " + NO_PATH;
    public static final String PARSE_ERROR = "Could not read configuration parameter value '%s' from key '%s'!";
    public static final String REGISTER_ERROR = "Could not register parameter '%s', because no Configuration with event listeners exists, yet!";

    public static final String BASIC_PARAMETER_FORMAT = "%%n%%1$-%ds :  %%2$s";
    public static final String CATEGORY_FORMAT = "- %s -";

    public static final String ENVIRONMENT_VARIABLE_SET_START = "Searching for configuration from environment variables...";
    public static final String ENVIRONMENT_VARIABLE = "GERDI_HARVESTER_%S_%S";
    public static final String COMPOSITE_KEY = "%s.%s";

    public static final String DEBUG_CATEGORY = "Debug";

    public static final String KEY_FORMAT = "%s.%s";
    public static final String VALID_PARAM_NAME_REGEX = "[a-zA-Z0-9]+";
    public static final String INVALID_PARAM_NAME_REGEX = "[^a-zA-Z0-9]";
    public static final String INVALID_PARAMETER_KEY = "Invalid Parameter-Key: %s%nKeys must only consist of letters!";
    public static final String INVALID_CATEGORY_NAME = "Invalid Category-Name: %s%nNames must only consist of letters!";



    /**
     * Private constructor, because this class just serves as a place to define
     * constants.
     */
    private ConfigurationConstants()
    {
    }
}
