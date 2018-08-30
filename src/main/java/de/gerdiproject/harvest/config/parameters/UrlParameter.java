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

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;

import de.gerdiproject.harvest.config.constants.ConfigurationConstants;

/**
 * This parameter holds a URL.
 *
 * @author Robin Weiss
 */
public class UrlParameter extends AbstractParameter<URL>
{
    /**
     * Constructor that assigns a category and a key that must be unique within the category.
     *
     * @param key the unique key of the parameter, which is used to change it via REST
     * @param category the category of the parameter
     *
     * @see AbstractParameter#AbstractParameter(String, ParameterCategory)
     */
    public UrlParameter(String key, ParameterCategory category)
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
    public UrlParameter(String key, ParameterCategory category, URL defaultValue)
    {
        super(key, category, defaultValue);
    }


    @Override
    public UrlParameter copy()
    {
        try {
            return new UrlParameter(key, category, value == null ? null : new URL(value.toString()));
        } catch (MalformedURLException e) {
            return new UrlParameter(key, category, null);
        }
    }


    @Override
    protected String getAllowedValues()
    {
        return ConfigurationConstants.URL_VALID_VALUES_TEXT;
    }


    @Override
    public URL stringToValue(String value) throws ParseException, ClassCastException
    {
        if (value == null)
            return null;

        try {
            return new URL(value);
        } catch (MalformedURLException e) {
            throw new ParseException(String.format(ConfigurationConstants.CANNOT_CHANGE_PARAM_INVALID_URL, key, value), 0);
        }
    }

}
