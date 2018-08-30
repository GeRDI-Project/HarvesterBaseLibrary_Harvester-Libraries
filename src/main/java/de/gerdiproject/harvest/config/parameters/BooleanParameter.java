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
 * This parameter represents a boolean flag.
 *
 * @author Robin Weiss
 */
public class BooleanParameter extends AbstractParameter<Boolean>
{
    /**
     * Constructor that assigns a category and a key that must be unique within the category.
     *
     * @param key the unique key of the parameter, which is used to change it via REST
     * @param category the category of the parameter
     *
     * @see AbstractParameter#AbstractParameter(String, ParameterCategory)
     */
    public BooleanParameter(String key, ParameterCategory category)
    {
        super(key, category);
        value = false;
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
    public BooleanParameter(String key, ParameterCategory category, boolean defaultValue)
    {
        super(key, category, defaultValue);
    }


    @Override
    public BooleanParameter copy()
    {
        return new BooleanParameter(key, category, value);
    }


    @Override
    protected String getAllowedValues()
    {
        return ConfigurationConstants.BOOLEAN_VALID_VALUES_TEXT;
    }


    @Override
    public Boolean stringToValue(String value) throws ParseException, ClassCastException
    {
        if (value == null)
            return false;

        else if (ConfigurationConstants.BOOLEAN_VALID_VALUES_LIST.contains(value))
            return value.equals(ConfigurationConstants.BOOLEAN_VALID_VALUES_LIST.get(0)) || Boolean.parseBoolean(value);

        else
            throw new ClassCastException();
    }

}
