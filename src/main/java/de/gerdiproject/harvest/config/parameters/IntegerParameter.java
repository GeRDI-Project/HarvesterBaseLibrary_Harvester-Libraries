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
 * This parameter holds an integer value.
 *
 * @author Robin Weiss
 */
public class IntegerParameter extends AbstractParameter<Integer>
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
    public IntegerParameter(String key, String category, int defaultValue, Function<String, Integer> customMappingFunction) throws IllegalArgumentException
    {
        super(key, category, defaultValue, customMappingFunction);
    }


    /**
     * Constructor that uses the default mapping function.
     *
     * @param key the unique key of the parameter, which is used to change it via REST
     * @param category the category of the parameter
     * @param defaultValue the default value
     *
     * @see AbstractParameter#AbstractParameter(String, String, Object)
     */
    public IntegerParameter(String key, String category, int defaultValue)
    {
        super(key, category, defaultValue);
    }


    @Override
    public IntegerParameter copy()
    {
        return new IntegerParameter(key, category, value, mappingFunction);
    }


    @Override
    public String getStringValue()
    {
        if (value == Integer.MAX_VALUE)
            return ConfigurationConstants.INTEGER_VALUE_MAX;

        else if (value == Integer.MIN_VALUE)
            return ConfigurationConstants.INTEGER_VALUE_MIN;

        else
            return String.valueOf(value);
    }


    @Override
    public Integer stringToValue(String value) throws RuntimeException
    {
        if (value == null)
            return 0;

        else if (value.equals(ConfigurationConstants.INTEGER_VALUE_MAX))
            return Integer.MAX_VALUE;

        else if (value.equals(ConfigurationConstants.INTEGER_VALUE_MIN))
            return Integer.MIN_VALUE;

        else {
            try {
                // try to parse the integer
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new ClassCastException(ConfigurationConstants.INTEGER_ALLOWED_VALUES);
            }
        }
    }
}
