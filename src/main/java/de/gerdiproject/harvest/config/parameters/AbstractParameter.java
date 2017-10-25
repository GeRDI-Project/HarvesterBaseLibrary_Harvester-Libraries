/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package de.gerdiproject.harvest.config.parameters;

import java.text.ParseException;
import java.util.List;

import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.state.IState;

/**
 * Parameters are part of the {@linkplain Configuration}. Each parameter holds some information about how and when it can be changed.
 *
 * @author Robin Weiss
 *
 * @param <T> The underlying type of the parameter value
 */
public abstract class AbstractParameter<T>
{
    protected final String key;
    protected T value;
    private final List<Class<? extends IState>> allowedStates;
    private final String allowedValues;

    /**
     * Constructor that requires all fields.
     *
     * @param key the unique key of the parameter, which is used to change it via REST
     * @param allowedStates a list of state-machine states during which the parameter may be changed
     * @param allowedValues a human readable String that describes which values can be set
     */
    public AbstractParameter(String key, List<Class<? extends IState>> allowedStates, String allowedValues)
    {
        this.key = key;
        this.allowedStates = allowedStates;
        this.allowedValues = allowedValues;
    }


    /**
     * This function attempts to convert a String value to the actual Type of the parameter.
     * @param value a String representation of the new value
     *
     * @return a converted value
     *
     * @throws ClassCastException this exception is thrown when the String value cannot be cast to the target value
     * @throws ParseException this exception is thrown when the conversion failed for a different reason
     */
    public abstract T stringToValue(String value) throws ParseException, ClassCastException;


    /**
     * Returns the unique key of the parameter, which is used to change it via REST.
     *
     * @return the unique key of the parameter, which is used to change it via REST
     */
    public String getKey()
    {
        return key;
    }


    /**
     * Returns the parameter value.
     *
     * @return the parameter value
     */
    public final T getValue()
    {
        return value;
    }


    /**
     * Returns a human readable String of the parameter value.
     *
     * @return a human readable String of the parameter value
     */
    public String getStringValue()
    {
        return value.toString();
    }


    /**
     * Changes the value by parsing a String value. A message is returned that describes
     * whether the value change was successful, or if not, why it failed.
     *
     * @param value a String representation of the new value
     * @param currentState the current state of the state machine
     *
     * @return a message that describes whether the value change was successful, or if not, why it failed
     */
    public final String setValue(String value, IState currentState)
    {
        String returnMessage;

        if (!allowedStates.contains(currentState.getClass()))
            returnMessage = String.format(ConfigurationConstants.CANNOT_CHANGE_PARAM_INVALID_STATE, key, currentState.getName());
        else {
            try {
                this.value = stringToValue(value);
                returnMessage = String.format(ConfigurationConstants.CHANGED_PARAM, key, this.value.toString());
            } catch (ClassCastException e) {
                returnMessage = String.format(ConfigurationConstants.CANNOT_CHANGE_PARAM_INVALID_VALUE, key, value, allowedValues);
            } catch (ParseException e) {
                returnMessage = e.getMessage();
            }
        }

        return returnMessage;
    }

    @Override
    public String toString()
    {
        return key + ":\t" + getStringValue();
    }
}
