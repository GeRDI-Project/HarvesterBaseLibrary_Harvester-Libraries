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
package de.gerdiproject.harvest.config;


import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.application.constants.ApplicationConstants;
import de.gerdiproject.harvest.config.adapter.ConfigurationAdapter;
import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.config.events.GlobalParameterChangedEvent;
import de.gerdiproject.harvest.config.events.HarvesterParameterChangedEvent;
import de.gerdiproject.harvest.config.parameters.AbstractParameter;
import de.gerdiproject.harvest.config.parameters.ParameterFactory;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.state.StateMachine;
import de.gerdiproject.harvest.utils.data.DiskIO;



/**
 * This class manages all application {@linkplain AbstractParameter}s.
 * It is stored by the {@linkplain MainContext}.
 *
 * @author Robin Weiss
 */
public class Configuration
{
    private static final Gson GSON =  new GsonBuilder().registerTypeAdapter(Configuration.class, new ConfigurationAdapter()).create();
    private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);

    private Map<String, AbstractParameter<?>> globalParameters;
    String globalParameterFormat;

    private Map<String, AbstractParameter<?>> harvesterParameters;
    String harvesterParameterFormat;

    private final transient DiskIO diskIo;


    /**
     * This constructor is used by the JSON deserialization (see {@linkplain ConfigurationAdapter}.
     * It does not call parameter update events.
     *
     * @param globalParameters a map of global parameters
     * @param harvesterParameters a map of harvester specific parameters
     */
    public Configuration(Map<String, AbstractParameter<?>> globalParameters, Map<String, AbstractParameter<?>> harvesterParameters)
    {
        this.globalParameters = globalParameters;
        this.harvesterParameters = harvesterParameters;

        this.globalParameterFormat = getPaddedKeyFormat(globalParameters);
        this.harvesterParameterFormat = getPaddedKeyFormat(harvesterParameters);

        this.diskIo = new DiskIO(GSON, StandardCharsets.UTF_8);
    }


    /**
     * Constructor that requires a map of harvester specific parameters.
     *
     * @param harvesterParams a list of harvester specific parameters
     */
    public Configuration(List<AbstractParameter<?>> harvesterParams)
    {
        this(
            ParameterFactory.createDefaultParameters(),
            ParameterFactory.createHarvesterParameters(harvesterParams)
        );
        updateAllParameters();
    }


    /**
     * Finds the length of the longest string key of a map and returns
     * a formatting String that can be used to display key value pairs
     * of the map in a pretty format.
     *
     * @param map the map of which the key lengths are compared
     *
     * @return the length of the longest string key in the map
     */
    private String getPaddedKeyFormat(Map<String, ?> map)
    {
        final AtomicInteger maxLength = new AtomicInteger(0);

        map.forEach((String key, Object value) -> {
            int keyLength = key.length();

            if (keyLength > maxLength.get())
                maxLength.set(keyLength);
        });

        return String.format(ConfigurationConstants.BASIC_PARAMETER_FORMAT, maxLength.get());
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
        return String.format(ConfigurationConstants.REST_INFO, modName, parameters, validValues);
    }


    /**
     * Attempts to load a configuration file from disk.
     */
    public void loadFromCache()
    {
        // read JSON from disk
        final String path = getConfigFilePath();
        final Configuration configJson = diskIo.getObject(path, Configuration.class);

        if (configJson == null)
            LOGGER.error(String.format(ConfigurationConstants.LOAD_FAILED, path, ConfigurationConstants.NO_EXISTS));
        else {
            try {

                // copy harvester parameters
                configJson.harvesterParameters.forEach((String key, AbstractParameter<?> param) -> {
                    if (harvesterParameters.containsKey(key))
                        setParameter(key, param.getStringValue());
                });

                // copy global parameters
                configJson.globalParameters.forEach((String key, AbstractParameter<?> param) -> {
                    if (globalParameters.containsKey(key))
                        setParameter(key, param.getStringValue());
                });

                LOGGER.info(String.format(ConfigurationConstants.LOAD_OK, path));

            } catch (JsonParseException e) {
                LOGGER.error(String.format(ConfigurationConstants.LOAD_FAILED, path, e.toString()));
            }
        }
    }


    /**
     * Attempts to load configuration parameters from environment variables.
     */
    public void loadFromEnvironmentVariables()
    {
        LOGGER.info(ConfigurationConstants.ENVIRONMENT_VARIABLE_SET_START);

        int suffixIndex = MainContext.getModuleName().indexOf(ApplicationConstants.HARVESTER_SERVICE_NAME_SUFFIX);
        final String moduleName = (suffixIndex != -1)
                                  ? MainContext.getModuleName().substring(0, suffixIndex)
                                  : MainContext.getModuleName();

        final AtomicInteger changeCount = new AtomicInteger(0);
        final Map<String, String> environmentVariables = System.getenv();

        // copy harvester parameters
        harvesterParameters.forEach((String key, AbstractParameter<?> param) -> {
            final String envVal = environmentVariables.get(
                String.format(ConfigurationConstants.ENVIRONMENT_VARIABLE, moduleName, key));

            if (envVal != null)
            {
                LOGGER.debug(setParameter(key, envVal));
                changeCount.incrementAndGet();
            }
        });

        // copy global parameters
        globalParameters.forEach((String key, AbstractParameter<?> param) -> {
            final String envVal = environmentVariables.get(
                String.format(ConfigurationConstants.ENVIRONMENT_VARIABLE, moduleName, key));

            if (envVal != null)
            {
                LOGGER.debug(setParameter(key, envVal));
                changeCount.incrementAndGet();
            }
        });

        LOGGER.info(String.format(ConfigurationConstants.ENVIRONMENT_VARIABLE_SET_END, changeCount.get()));
    }


    /**
     * Saves the configuration as a Json file.
     *
     * @return a string describing the status of the operation
     */
    public String saveToDisk()
    {
        return diskIo.writeObjectToFile(getConfigFilePath(), this);
    }


    /**
     * Returns the path to the configurationFile of this service.
     *
     * @return the path to the configurationFile of this service
     */
    private static String getConfigFilePath()
    {
        return String.format(ConfigurationConstants.CONFIG_PATH, MainContext.getModuleName());
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

        // if no global parameter with the specific name exists, look in the harvester parameters
        if (param == null)
            param = harvesterParameters.get(key);

        // check if the parameter exists and if the value matches the parameterType
        if (param != null && param.getValue() != null && param.getValue().getClass().equals(parameterType))
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

        // if no global parameter with the specific name exists, look in the harvester parameters
        if (param == null)
            param = harvesterParameters.get(key);

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
        // look up the key in the global parameters
        AbstractParameter<?> param = globalParameters.get(key);
        boolean isHarvesterParam = false;

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


    /**
     * Sends out a parameter changed event for a specified parameter.
     * @param key the key of the parameter
     */
    public void updateParameter(String key)
    {
        // look up the key in the global parameters
        AbstractParameter<?> param = globalParameters.get(key);
        boolean isHarvesterParam = false;

        // if no global parameter with the specific name exists, look in the harvester parameters
        if (param == null) {
            param = harvesterParameters.get(key);
            isHarvesterParam = true;
        }

        if (param != null) {
            if (isHarvesterParam)
                EventSystem.sendEvent(new HarvesterParameterChangedEvent(param, null));
            else
                EventSystem.sendEvent(new GlobalParameterChangedEvent(param, null));
        }
    }


    /**
     * Sends out a parameter changed events for all parameters.
     */
    public void updateAllParameters()
    {
        globalParameters.forEach((String key, AbstractParameter<?> param) ->
                                 EventSystem.sendEvent(new GlobalParameterChangedEvent(param, null))
                                );

        harvesterParameters.forEach((String key, AbstractParameter<?> param) ->
                                    EventSystem.sendEvent(new HarvesterParameterChangedEvent(param, null))
                                   );
    }


    @Override
    public String toString()
    {
        final StringBuilder harvesterBuilder = new StringBuilder();
        harvesterParameters.forEach(
            (String key, AbstractParameter<?> param) ->
            harvesterBuilder.append(String.format(harvesterParameterFormat, key, param.getStringValue()))
        );

        final StringBuilder globalBuilder = new StringBuilder();
        globalParameters.forEach(
            (String key, AbstractParameter<?> param) ->
            globalBuilder.append(String.format(globalParameterFormat, key, param.getStringValue()))
        );

        return String.format(
                   ConfigurationConstants.CONFIG_PARAMETERS,
                   harvesterBuilder.toString(),
                   globalBuilder.toString());
    }


    /**
     * Returns read-only access of all global parameters.
     *
     * @return a read-only map of all global parameters
     */
    public Map<String, AbstractParameter<?>> getGlobalParameters()
    {
        return Collections.unmodifiableMap(globalParameters);
    }


    /**
     * Returns read-only access of all harvester specific parameters.
     *
     * @return a read-only map of all harvester specific parameters
     */
    public Map<String, AbstractParameter<?>> getHarvesterParameters()
    {
        return Collections.unmodifiableMap(harvesterParameters);
    }
}
