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

import de.gerdiproject.harvest.config.parameters.constants.ParameterConstants;
import de.gerdiproject.harvest.config.parameters.constants.ParameterMappingFunctions;

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
    public IntegerParameter(final String key, final String category, final int defaultValue, final Function<String, Integer> customMappingFunction) throws IllegalArgumentException
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
    public IntegerParameter(final String key, final String category, final int defaultValue)
    {
        super(key, category, defaultValue, ParameterMappingFunctions::mapToInteger);
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
            return ParameterConstants.INTEGER_VALUE_MAX;

        else if (value == Integer.MIN_VALUE)
            return ParameterConstants.INTEGER_VALUE_MIN;

        else
            return String.valueOf(value);
    }
}
