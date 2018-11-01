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
package de.gerdiproject.harvest.config.parameters;

import java.util.function.Function;

import de.gerdiproject.harvest.config.constants.ConfigurationConstants;

/**
 * This parameter holds a String value used for passwords.
 * If the value is retrieved via getStringValue(), only stars are displayed.
 *
 * @author Robin Weiss
 */
public class PasswordParameter extends StringParameter
{
    /**
     * Constructor that uses a custom mapping function.
     *
     * @param key the unique key of the parameter, which is used to change it via REST
     * @param category the category of the parameter
     * @param defaultValue the default value
     * @param customMappingFunction a function that maps strings to the parameter values
     *
     * @throws IllegalArgumentException thrown if the key contains invalid characters
     */
    public PasswordParameter(String key, String category, String defaultValue, Function<String, String> customMappingFunction) throws IllegalArgumentException
    {
        super(key, category, defaultValue, customMappingFunction);
    }


    /**
     * Constructor that uses the default mapping function.
     *
     * @param key the unique key of the parameter, which is used to change it via REST
     * @param category the category of the parameter
     * @param defaultValue the default value
     */
    public PasswordParameter(String key, String category, String defaultValue)
    {
        super(key, category, defaultValue);
    }


    @Override
    public PasswordParameter copy()
    {
        return new PasswordParameter(key, category, value, mappingFunction);
    }


    @Override
    public String getStringValue()
    {
        // always return ***** as a string value
        return ConfigurationConstants.PASSWORD_STRING_TEXT;
    }
}
