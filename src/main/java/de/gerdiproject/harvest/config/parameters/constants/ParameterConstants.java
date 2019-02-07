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
package de.gerdiproject.harvest.config.parameters.constants;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.gerdiproject.harvest.config.Configuration;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * This static class is a collection of constants, commonly used for classes
 * that deal with the {@linkplain Configuration}.
 *
 * @author Robin Weiss
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ParameterConstants
{
    public static final String CHANGED_PARAM = "Set parameter '%s' to '%s'.";

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

    public static final String NON_EMPTY_STRING_PARAM_INVALID = "It must be a non-empty string!";
    public static final String URL_PARAM_INVALID = "It must be a valid URL!";
    public static final String ETL_PARAM_INVALID_STATE = "The '%s' must be idle, but it is currently %s!";
    public static final String ETL_REGISTRY_PARAM_INVALID_STATE = "All ETLs must be idle, but they are currently %s!";

    public static final String COMPOSITE_KEY = "%s.%s";
    public static final String VALID_PARAM_NAME_REGEX = "[a-zA-Z0-9]+";
    public static final String INVALID_PARAM_NAME_REGEX = "[^a-zA-Z0-9]";
    public static final String INVALID_PARAMETER_KEY = "Invalid Parameter-Key: %s%nKeys must only consist of letters!";
    public static final String INVALID_CATEGORY_NAME = "Invalid Category-Name: %s%nNames must only consist of letters!";

    public static final String ENVIRONMENT_VARIABLE = "GERDI_HARVESTER_%S_%S";
}
