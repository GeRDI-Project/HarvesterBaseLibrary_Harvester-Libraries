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
import java.util.List;
import java.util.Set;

import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.state.IState;
import de.gerdiproject.harvest.submission.AbstractSubmitter;
import de.gerdiproject.harvest.submission.events.GetSubmitterIdsEvent;

/**
 * This parameter holds an identifier of an {@linkplain AbstractSubmitter}.
 *
 * @author Robin Weiss
 */
public class SubmitterParameter extends StringParameter
{
    /**
     * Constructor that requires a key, valid states and a default value.
     *
     * @param key the unique key of the parameter, which is used to change it via REST
     * @param allowedStates a list of state-machine states during which the parameter may be changed
     * @param defaultValue the value with which the state is initialized
     */
    public SubmitterParameter(String key, List<Class<? extends IState>> allowedStates, String defaultValue)
    {
        super(key, allowedStates, defaultValue);
    }


    /**
     * Retrieves the list of valid values.
     *
     * @return a string listing all valid values of this parameter
     */
    @Override
    protected String getAllowedValues()
    {
        final Set<String> validValues = EventSystem.sendSynchronousEvent(new GetSubmitterIdsEvent());

        if (validValues != null) {
            String validValueString = validValues.toString();
            return validValueString.substring(1, validValueString.length() - 1);
        }

        return "";
    }


    /**
     * Constructor for harvester parameters, that only forbid changes during harvesting.
     *
     * @param key the unique key of the parameter, which is used to change it via REST
     * @param defaultSubmitterId the string-value with which the state is initialized
     */
    public SubmitterParameter(String key, String defaultSubmitterId)
    {
        super(key, ConfigurationConstants.HARVESTER_PARAM_ALLOWED_STATES, ConfigurationConstants.URL_VALID_VALUES_TEXT);
        setValue(defaultSubmitterId, null);
    }


    @Override
    public String stringToValue(String value) throws ParseException, ClassCastException
    {
        final Set<String> validValues = EventSystem.sendSynchronousEvent(new GetSubmitterIdsEvent());

        if (validValues == null || !validValues.contains(value))
            throw new ClassCastException();

        return super.stringToValue(value);
    }




}
