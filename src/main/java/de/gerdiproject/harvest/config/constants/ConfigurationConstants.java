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
import de.gerdiproject.harvest.state.IState;
import de.gerdiproject.harvest.state.impl.ErrorState;
import de.gerdiproject.harvest.state.impl.IdleState;
import de.gerdiproject.harvest.state.impl.InitializationState;
import de.gerdiproject.harvest.state.impl.SavingState;
import de.gerdiproject.harvest.state.impl.SubmittingState;
import de.gerdiproject.harvest.utils.cache.constants.CacheConstants;

/**
 * This static class is a collection of constants, commonly used for classes
 * that deal with the {@linkplain Configuration}.
 *
 * @author Robin Weiss
 */
public class ConfigurationConstants
{
    public static final String AUTO_SAVE = "autoSave";
    public static final String AUTO_SUBMIT = "autoSubmit";
    public static final String WRITE_HTTP_TO_DISK = "writeToDisk";
    public static final String READ_HTTP_FROM_DISK = "readFromDisk";
    public static final String HARVEST_START_INDEX = "harvestFrom";
    public static final String HARVEST_END_INDEX = "harvestTo";
    public static final String SUBMISSION_URL = "submissionUrl";
    public static final String SUBMISSION_USER_NAME = "submissionUserName";
    public static final String SUBMISSION_PASSWORD = "submissionPassword";
    public static final String SUBMISSION_SIZE = "submissionSize";
    public static final String SUBMIT_INCOMPLETE = "submitIncomplete";
    public static final String SUBMIT_FORCED = "submitOutdated";
    public static final String FORCE_HARVEST = "forceHarvest";
    public static final String DELETE_UNFINISHED_SAVE = "deleteFailedSaves";

    public static final String CHANGED_PARAM = "Set parameter '%s' to '%s'.";

    public static final String CANNOT_CHANGE_PARAM_INVALID_STATE =
        "Cannot change parameter '%s' during the %s-process.";

    public static final String CANNOT_CHANGE_PARAM_INVALID_VALUE =
        "Cannot change parameter '%s' to '%s'. Allowed values are: %s";

    public static final String CANNOT_CHANGE_PARAM_INVALID_URL =
        "Cannot change parameter '%s'. '%s' is not a valid URL!";

    public static final String NO_CHANGES = "No parameters were changed!";
    public static final String UNKNOWN_PARAM = "Cannot change parameter '%s'. Unknown parameter!";

    public static final String BOOLEAN_VALID_VALUES_TEXT = "0, 1, true, false";
    public static final List<String> BOOLEAN_VALID_VALUES_LIST =
        Collections.unmodifiableList(Arrays.asList("1", "0", "true", "false"));

    public static final String INTEGER_VALUE_MAX = "max";
    public static final String INTEGER_VALUE_MIN = "min";
    public static final String INTEGER_VALID_VALUES_TEXT = INTEGER_VALUE_MAX + ", " + INTEGER_VALUE_MIN + ", 0, 1, ...";

    public static final String PASSWORD_STRING_TEXT = "*****";
    public static final String STRING_VALID_VALUES_TEXT = "<anything>";
    public static final String URL_VALID_VALUES_TEXT = "<a valid URL>";

    public static final String CONFIG_PARAMETERS = "Harvester Parameters:%n%s%nGlobal Parameters:%n%s";
    public static final String CONFIG_PATH = CacheConstants.CACHE_FOLDER_PATH + "config.json";
    public static final String LOAD_OK = "Loaded configuration from '%s'.";
    public static final String LOAD_FAILED = "Could not load configuration from '%s': %s";
    public static final String NO_EXISTS = "No configuration exists!";
    public static final String REST_INFO = "- %s Configuration -%n%n%s%n"
                                           + "GET   Returns either the entire configuration in pretty text, or%n"
                                           + "      if '?key=xxx' is added, returns the value of parameter 'xxx'.%n"
                                           + "POST  Saves the current configuration to disk.%n"
                                           + "PUT   Sets x-www-form-urlencoded parameters for the harvester.%n"
                                           + "      Valid values: %s.%n";

    public static final String PARSE_ERROR = "Could not read configuration parameter value '%s' from key '%s'!";

    public static final List<Class<? extends IState>> HARVESTER_PARAM_ALLOWED_STATES =
        Collections.unmodifiableList(
            Arrays.asList(
                InitializationState.class,
                ErrorState.class,
                IdleState.class,
                SavingState.class,
                SubmittingState.class));

    public static final String URL_PREFIX = "%URL_PARAMETER%";
    public static final String GLOBAL_PARAMETERS_JSON = "globalParameters";
    public static final String HARVESTER_PARAMETERS_JSON = "harvesterParameters";
    public static final String BASIC_PARAMETER_FORMAT = "%%1$-%ds :  %%2$s%%n";

    public static final String ENVIRONMENT_VARIABLE_SET_START = "Searching for configuration from environment variables...";
    public static final String ENVIRONMENT_VARIABLE_SET_END = "Set %d parameter(s) from environment variables.";
    public static final String ENVIRONMENT_VARIABLE = "GERDI_%S_%S";


    /**
     * Private constructor, because this class just serves as a place to define
     * constants.
     */
    private ConfigurationConstants()
    {
    }
}
