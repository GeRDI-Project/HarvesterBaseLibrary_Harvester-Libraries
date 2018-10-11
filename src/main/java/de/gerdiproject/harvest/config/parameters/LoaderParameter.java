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
import java.util.Set;

import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.harvester.loaders.ILoader;
import de.gerdiproject.harvest.submission.events.GetLoaderNamesEvent;

/**
 * This parameter holds an identifier of an {@linkplain ILoader}.
 *
 * @author Robin Weiss
 */
public class LoaderParameter extends StringParameter
{
    /**
     * Constructor that assigns a category and a key that must be unique within the category.
     *
     * @param key the unique key of the parameter, which is used to change it via REST
     * @param category the category of the parameter
     *
     * @see AbstractParameter#AbstractParameter(String, ParameterCategory)
     */
    public LoaderParameter(String key, ParameterCategory category)
    {
        super(key, category);
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
    public LoaderParameter(String key, ParameterCategory category, String defaultValue)
    {
        super(key, category, defaultValue);
    }


    @Override
    public LoaderParameter copy()
    {
        return new LoaderParameter(key, category, value);
    }


    /**
     * Retrieves the list of valid values.
     *
     * @return a string listing all valid values of this parameter
     */
    @Override
    protected String getAllowedValues()
    {
        final Set<String> validValues = EventSystem.sendSynchronousEvent(new GetLoaderNamesEvent());

        if (validValues != null) {
            String validValueString = validValues.toString();
            return validValueString.substring(1, validValueString.length() - 1);
        }

        return "";
    }


    @Override
    public String stringToValue(String value) throws ParseException, ClassCastException
    {
        final Set<String> validValues = EventSystem.sendSynchronousEvent(new GetLoaderNamesEvent());

        if (validValues == null || !validValues.contains(value))
            throw new ClassCastException();

        return super.stringToValue(value);
    }
}
