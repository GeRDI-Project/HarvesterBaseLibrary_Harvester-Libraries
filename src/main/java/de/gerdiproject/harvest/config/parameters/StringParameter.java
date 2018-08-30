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
 * This parameter holds a String value.
 *
 * @author Robin Weiss
 */
public class StringParameter extends AbstractParameter<String>
{
    /**
     * Constructor that assigns a category and a key that must be unique within the category.
     *
     * @param key the unique key of the parameter, which is used to change it via REST
     * @param category the category of the parameter
     *
     * @see AbstractParameter#AbstractParameter(String, ParameterCategory)
     */
    public StringParameter(String key, ParameterCategory category)
    {
        super(key, category);
        value = null;
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
    public StringParameter(String key, ParameterCategory category, String defaultValue)
    {
        super(key, category, defaultValue);
    }


    @Override
    public StringParameter copy()
    {
        return new StringParameter(key, category, value);
    }


    @Override
    protected String getAllowedValues()
    {
        return ConfigurationConstants.STRING_VALID_VALUES_TEXT;
    }


    @Override
    public String stringToValue(String value) throws ParseException, ClassCastException
    {
        return value;
    }

}
