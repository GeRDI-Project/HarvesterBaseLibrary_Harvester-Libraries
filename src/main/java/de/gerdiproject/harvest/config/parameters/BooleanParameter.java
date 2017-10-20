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

import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.state.IState;

/**
 * This parameter represents a boolean flag.
 *
 * @author Robin Weiss
 */
public class BooleanParameter extends AbstractParameter<Boolean>
{
    /**
     * Constructor that requires a key, valid states and a default value.
     *
     * @param key the unique key of the parameter, which is used to change it via REST
     * @param allowedStates a list of state-machine states during which the parameter may be changed
     * @param defaultValue the value with which the state is initialized
     */
    public BooleanParameter(String key, List<Class<? extends IState>> allowedStates, boolean defaultValue)
    {
        super(key, allowedStates, ConfigurationConstants.BOOLEAN_VALID_VALUES_TEXT);
        value = defaultValue;
    }

    @Override
    public Boolean stringToValue(String value) throws ParseException, ClassCastException
    {
        if (ConfigurationConstants.BOOLEAN_VALID_VALUES_LIST.contains(value))
            return value.equals(ConfigurationConstants.BOOLEAN_VALID_VALUES_LIST.get(0)) || Boolean.parseBoolean(value);
        else
            throw new ClassCastException();
    }

}
