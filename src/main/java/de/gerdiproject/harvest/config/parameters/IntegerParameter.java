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

import java.text.ParseException;

import de.gerdiproject.harvest.config.constants.ConfigurationConstants;

/**
 * This parameter holds an integer value.
 *
 * @author Robin Weiss
 */
public class IntegerParameter extends AbstractParameter<Integer>
{
    /**
     * Constructor that assigns a category and a key that must be unique within the category.
     *
     * @param key the unique key of the parameter, which is used to change it via REST
     * @param category the category of the parameter
     *
     * @see AbstractParameter#AbstractParameter(String, ParameterCategory)
     */
    public IntegerParameter(String key, ParameterCategory category)
    {
        super(key, category);
        value = 0;
    }


    /**
     * Constructor that assigns all fields.
     *
     * @param key the unique key of the parameter, which is used to change it via REST
     * @param category the category of the parameter
     * @param defaultValue the default value
     *
     * @see AbstractParameter#AbstractParameter(String, ParameterCategory, Object)
     */
    public IntegerParameter(String key, ParameterCategory category, int defaultValue)
    {
        super(key, category, defaultValue);
    }


    @Override
    public IntegerParameter copy()
    {
        return new IntegerParameter(key, category, value);
    }


    @Override
    protected String getAllowedValues()
    {
        return ConfigurationConstants.INTEGER_VALID_VALUES_TEXT;
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
    public Integer stringToValue(String value) throws ParseException, ClassCastException
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
                int intValue = Integer.parseInt(value);

                // do not accept negative numbers
                if (intValue < 0)
                    throw new ClassCastException();

                return intValue;
            } catch (NumberFormatException e) {
                throw new ClassCastException();
            }
        }
    }

}
