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
import java.util.List;

import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.state.IState;

/**
 * This parameter holds a URL.
 *
 * @author Robin Weiss
 */
public class UrlParameter extends AbstractParameter<URL>
{
    /**
     * Constructor that requires a key, valid states and a default value.
     *
     * @param key the unique key of the parameter, which is used to change it via REST
     * @param allowedStates a list of state-machine states during which the parameter may be changed
     * @param defaultValue the value with which the state is initialized
     */
    public UrlParameter(String key, List<Class<? extends IState>> allowedStates, URL defaultValue)
    {
        super(key, allowedStates, ConfigurationConstants.URL_VALID_VALUES_TEXT);
        value = defaultValue;
    }


    /**
     * Constructor for harvester parameters, that only forbid changes during harvesting.
     *
     * @param key the unique key of the parameter, which is used to change it via REST
     * @param defaultUrlString the string-value with which the state is initialized
     */
    public UrlParameter(String key, String defaultUrlString)
    {
        super(key, ConfigurationConstants.HARVESTER_PARAM_ALLOWED_STATES, ConfigurationConstants.URL_VALID_VALUES_TEXT);
        setValue(defaultUrlString, null);
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
