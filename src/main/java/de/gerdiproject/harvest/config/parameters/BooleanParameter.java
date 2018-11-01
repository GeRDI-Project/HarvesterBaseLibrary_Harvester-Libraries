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
 * This parameter represents a boolean flag.
 *
 * @author Robin Weiss
 */
public class BooleanParameter extends AbstractParameter<Boolean>
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
    public BooleanParameter(String key, String category, boolean defaultValue, Function<String, Boolean> customMappingFunction) throws IllegalArgumentException
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
    public BooleanParameter(String key, String category, boolean defaultValue)
    {
        super(key, category, defaultValue);
    }


    @Override
    public BooleanParameter copy()
    {
        return new BooleanParameter(key, category, value, mappingFunction);
    }


    @Override
    protected Boolean stringToValue(String value) throws RuntimeException
    {
        if (value == null)
            return false;

        else if (ConfigurationConstants.BOOLEAN_VALID_VALUES_LIST.contains(value))
            return value.equals(ConfigurationConstants.BOOLEAN_VALID_VALUES_LIST.get(0)) || Boolean.parseBoolean(value);

        else
            throw new ClassCastException(ConfigurationConstants.BOOLEAN_ALLOWED_VALUES);
    }

}
