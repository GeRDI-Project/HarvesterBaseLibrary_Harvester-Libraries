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
package de.gerdiproject.harvest.config;


import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.config.events.GlobalParameterChangedEvent;
import de.gerdiproject.harvest.config.events.HarvesterParameterChangedEvent;
import de.gerdiproject.harvest.config.parameters.AbstractParameter;
import de.gerdiproject.harvest.config.parameters.ParameterFactory;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.state.StateMachine;
import de.gerdiproject.harvest.utils.data.DiskIO;
import de.gerdiproject.json.GsonUtils;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * This class manages all application parameters.
 *
 * @author Robin Weiss
 */
public class Configuration
{
    private static final String CONFIG_PATH = "config/%sConfig.json";
    private static final String LOAD_OK = "Loaded configuration from '%s'.";
    private static final String LOAD_FAILED = "Could not load configuration from '%s': %s";
    private static final String NO_EXISTS = "No configuration exists!";
    private static final String REST_INFO = "%s Configuration:%n%s%n"
                                            + "POST\t\tSaves the current configuration to disk.%n"
                                            + "PUT \t\t\tSets x-www-form-urlencoded parameters for the harvester. Valid values: %s.%n";

    private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);

    private Map<String, AbstractParameter<?>> globalParameters;
    private Map<String, AbstractParameter<?>> harvesterParameters;


    /**
     * Constructor that requires a map of harvester specific parameters.
     *
     * @param harvesterParams a list of harvester specific parameters
     */
    public Configuration(List<AbstractParameter<?>> harvesterParams)
    {
        this.globalParameters = ParameterFactory.createDefaultParameters();
        this.harvesterParameters = ParameterFactory.createHarvesterParameters(harvesterParams);

        globalParameters.forEach((String key, AbstractParameter<?> param) ->
                                 EventSystem.sendEvent(new GlobalParameterChangedEvent(param, null))
                                );

        harvesterParameters.forEach((String key, AbstractParameter<?> param) ->
                                    EventSystem.sendEvent(new HarvesterParameterChangedEvent(param, null))
                                   );
    }


    /**
     * Assembles a pretty String that summarizes all options and flags.
     *
     * @return a pretty String that summarizes all options and flags
     */
    public String getInfoString()
    {
        String modName = MainContext.getModuleName();
        String parameters = this.toString();
        String globalParamKeys = globalParameters.keySet().toString();

        // remove brackets of the string representation
        globalParamKeys = globalParamKeys.substring(1, globalParamKeys.length() - 1);

        String harvesterParamKeys = harvesterParameters.keySet().toString();

        // remove brackets of the string representation
        harvesterParamKeys = harvesterParamKeys.substring(1, harvesterParamKeys.length() - 1);

        String validValues = harvesterParamKeys + ", " + globalParamKeys;

        // return assembled string
        return String.format(REST_INFO, modName, parameters, validValues);
    }


    /**
     * Attempts to load a configuration file from disk.<br>
     *
     * @return a configuration that was loaded from disk, or null if no config exists
     */
    public static Configuration createFromDisk()
    {
        String path = getConfigFilePath();
        Configuration config = new DiskIO().getObject(path, Configuration.class);

        if (config == null)
            LOGGER.error(String.format(LOAD_FAILED, path, NO_EXISTS));

        else
            LOGGER.info(String.format(LOAD_OK, path));

        return config;
    }


    /**
     * Returns the path to the configurationFile of this service.
     *
     * @return the path to the configurationFile of this service
     */
    private static String getConfigFilePath()
    {
        return String.format(CONFIG_PATH, MainContext.getModuleName());
    }


    /**
     * Saves the configuration as a Json file.
     *
     * @return a string describing the status of the operation
     */
    public String saveToDisk()
    {
        // assemble path
        String path = getConfigFilePath();

        // write to disk
        return  new DiskIO().writeObjectToFile(path, GsonUtils.getGson().toJson(this));
    }


    /**
     * Returns the value of the parameter with a specified key.
     *
     * @param key the key of the parameter
     * @param parameterType the class of the parameter value
     *
     * @param <T> the value type of the parameter
     *
     * @return the value of the parameter with the specified key
     */
    @SuppressWarnings("unchecked")  // the cast will succeed, because the value type is compared to the parameterType
    public <T> T getParameterValue(String key, Class<T> parameterType)
    {
        AbstractParameter<?> param = globalParameters.get(key);

        // check if the parameter exists and if the value matches the parameterType
        if (param != null && param.getValue().getClass().equals(parameterType))
            return (T) param.getValue();
        else
            return null;
    }

    /**
     * Returns the human readable value of the parameter with a specified key.
     *
     * @param key the key of the parameter
     *
     * @return the human readable value of the parameter with a specified key
     */
    public String getParameterStringValue(String key)
    {
        AbstractParameter<?> param = globalParameters.get(key);

        // check if the parameter exists
        if (param != null)
            return param.getStringValue();
        else
            return null;
    }

    /**
     * Changes a configuration parameter, returning a status message about the change.
     *
     * @param key the parameter name
     * @param value the new value of the parameter
     *
     * @return a message describing if the operation was successful, or if not, why it failed
     */
    public String setParameter(String key, String value)
    {
        boolean isHarvesterParam = false;

        // look up the key in the global parameters
        AbstractParameter<?> param = globalParameters.get(key);

        // if no global parameter with the specific name exists, look in the harvester parameters
        if (param == null) {
            param = harvesterParameters.get(key);
            isHarvesterParam = true;
        }

        // change the parameter value or return an error, if it does not exist
        if (param == null)
            return String.format(ConfigurationConstants.UNKNOWN_PARAM, key);
        else {
            Object oldValue = param.getValue();
            String message = param.setValue(value, StateMachine.getCurrentState());

            if (isHarvesterParam)
                EventSystem.sendEvent(new HarvesterParameterChangedEvent(param, oldValue));
            else
                EventSystem.sendEvent(new GlobalParameterChangedEvent(param, oldValue));

            return message;
        }
    }


    @Override
    public String toString()
    {
        final StringBuilder harvesterBuilder = new StringBuilder();
        harvesterParameters.forEach(
            (String key, AbstractParameter<?> param) ->
            harvesterBuilder.append(param).append('\n')
        );

        final StringBuilder globalBuilder = new StringBuilder();
        globalParameters.forEach(
            (String key, AbstractParameter<?> param) ->
            globalBuilder.append(param).append('\n')
        );


        return String.format(
                   ConfigurationConstants.CONFIG_PARAMETERS,
                   harvesterBuilder.toString(),
                   globalBuilder.toString());
    }
}
