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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.gerdiproject.harvest.config.adapter.ConfigurationAdapter;
import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.config.events.RegisterParameterEvent;
import de.gerdiproject.harvest.config.parameters.AbstractParameter;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.IEventListener;
import de.gerdiproject.harvest.state.StateMachine;
import de.gerdiproject.harvest.utils.cache.ICachedObject;
import de.gerdiproject.harvest.utils.data.DiskIO;



/**
 * This class manages all application {@linkplain AbstractParameter}s.
 * It can be (de-) serialized to JSON format, saving all parameters that were registered.
 *
 * @author Robin Weiss
 */
public class Configuration implements ICachedObject, IEventListener
{
    private static final Gson GSON = new GsonBuilder().registerTypeAdapter(Configuration.class, new ConfigurationAdapter()).create();
    private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);

    private final transient DiskIO diskIo;
    private transient String cacheFilePath;

    private final Map<String, AbstractParameter<?>> parameterMap;


    /**
     * Constructor that requires a list of parameters.
     *
     * @param parameters a list parameters
     */
    public Configuration(AbstractParameter<?>... parameters)
    {
        this.parameterMap = new TreeMap<>();

        for (AbstractParameter<?> param : parameters)
            parameterMap.put(param.getCompositeKey(), param);

        this.diskIo = new DiskIO(GSON, StandardCharsets.UTF_8);
        this.cacheFilePath = null;
    }


    /**
     * Convenience function for registering a parameter at the configuration.
     *
     * @param parameter the parameter to be registered
     * @param <T> the class of the parameter that is to be registered
     *
     * @return the registered parameter as it appears in the configuration
     */
    @SuppressWarnings("unchecked")  // the cast will succeed, because the value type is the one that is registered
    public static <T extends AbstractParameter<?>> T registerParameter(T parameter)
    {
        return (T) EventSystem.sendSynchronousEvent(new RegisterParameterEvent(parameter));
    }


    @Override
    public void addEventListeners()
    {
        EventSystem.addSynchronousListener(RegisterParameterEvent.class, this::onRegisterParameter);
    }


    @Override
    public void removeEventListeners()
    {
        EventSystem.removeSynchronousListener(RegisterParameterEvent.class);
    }


    /**
     * Finds the length of the longest string key of parameters and returns
     * a formatting String that can be used to display key value pairs
     * of the map in a pretty format.
     *
     * @return the length of the longest string key in the map
     */
    private String getParameterFormat()
    {
        int maxLength = 0;

        for (AbstractParameter<?> param : parameterMap.values()) {
            int keyLength = param.getKey().length();

            if (keyLength > maxLength)
                maxLength = keyLength;
        }

        return String.format(ConfigurationConstants.BASIC_PARAMETER_FORMAT, maxLength);
    }


    /**
     * Assembles a pretty String that summarizes all options and flags.
     *
     * @param moduleName the name of the service
     *
     * @return a pretty String that summarizes all options and flags
     */
    public String getInfoString(String moduleName)
    {
        final StringBuilder validValues = new StringBuilder();

        for (AbstractParameter<?> param : getParameters()) {
            // ignore unregistered parameters
            if (!param.isRegistered())
                continue;

            if (validValues.length() != 0)
                validValues.append(", ");

            validValues.append(param.getCompositeKey());
        }

        // return assembled string
        return String.format(ConfigurationConstants.REST_INFO, moduleName, this.toString(), validValues.toString());
    }


    /**
     * Sets the file path to which the configuration can be saved.
     *
     * @param path a path to a file
     */
    public void setCacheFilePath(String path)
    {
        this.cacheFilePath = path;
    }


    /**
     * Saves the configuration as a Json file.
     *
     */
    @Override
    public void saveToDisk()
    {
        if (cacheFilePath == null)
            LOGGER.error(ConfigurationConstants.SAVE_FAILED_NO_PATH);
        else
            diskIo.writeObjectToFile(cacheFilePath, this);
    }


    /**
     * Attempts to load a configuration file from disk, overwriting the values
     * of existing parameters, but not adding them if they did not exist before.
     * This is done in order to get rid of deprecated parameter keys.
     */
    @Override
    public void loadFromDisk()
    {
        if (cacheFilePath == null) {
            LOGGER.error(String.format(ConfigurationConstants.LOAD_FAILED, "", ConfigurationConstants.NO_PATH));
            return;
        }

        // read JSON from disk
        final Configuration loadedConfig = diskIo.getObject(cacheFilePath, Configuration.class);

        if (loadedConfig == null) {
            LOGGER.error(String.format(ConfigurationConstants.LOAD_FAILED, cacheFilePath, ConfigurationConstants.NO_EXISTS));
            return;
        }

        this.parameterMap.clear();
        this.parameterMap.putAll(loadedConfig.parameterMap);

        LOGGER.info(String.format(ConfigurationConstants.LOAD_OK, cacheFilePath));
    }


    /**
     * Synchronous Event callback:<br>
     * Registers a parameter in the configuration. If a parameter with the same key already exists
     * in the configuration, the value will not be overwritten. A newly registered parameter
     * checks if it is defined via environment variables, and will retrieve a value from there.
     *
     * @param event the event that triggered the callback
     * @return the registered parameter as it appears in the configuration
     */
    private AbstractParameter<?> onRegisterParameter(RegisterParameterEvent event)
    {
        AbstractParameter<?> registeredParameter = event.getParamToBeRegistered();
        final String compositeKey = registeredParameter.getCompositeKey();
        final AbstractParameter<?> retrievedParameter = parameterMap.get(compositeKey);

        if (retrievedParameter == null) {
            // clone parameter in order to not override constant parameters
            registeredParameter = registeredParameter.copy();

            parameterMap.put(compositeKey, registeredParameter);
            registeredParameter.loadFromEnvironmentVariables();
        } else
            registeredParameter = retrievedParameter;

        registeredParameter.setRegistered(true);
        return registeredParameter;
    }


    /**
     * Returns the value of the parameter with a specified key.
     *
     * @param compositeKey the parameter category and name, separated by a dot
     * @param <T> the value type of the parameter
     *
     * @return the value of the parameter with the specified key
     */
    @SuppressWarnings("unchecked")  // the cast will succeed, because the value type is compared to the parameterType
    public <T> T getParameterValue(String compositeKey)
    {
        final AbstractParameter<?> param = parameterMap.get(compositeKey.toLowerCase());

        // check if the parameter exists and if the value matches the parameterType
        if (param != null && param.getValue() != null)
            return (T) param.getValue();
        else
            return null;
    }


    /**
     * Returns the human readable value of the parameter with a specified key.
     *
     * @param compositeKey the parameter category and name, separated by a dot
     *
     * @return the human readable value of the parameter with a specified key
     */
    public String getParameterStringValue(String compositeKey)
    {
        final AbstractParameter<?> param = parameterMap.get(compositeKey.toLowerCase());

        // check if the parameter exists
        return (param != null) ? param.getStringValue() : null;
    }


    /**
     * Changes a configuration parameter, returning a status message about the change.
     *
     * @param compositeKey the parameter category and name, separated by a dot
     * @param value the new value of the parameter
     *
     * @return a message describing if the operation was successful, or if not, why it failed
     */
    public String setParameter(String compositeKey, String value)
    {
        final AbstractParameter<?> param = parameterMap.get(compositeKey.toLowerCase());

        // change the parameter value or return an error, if it does not exist
        if (param == null)
            return String.format(ConfigurationConstants.UNKNOWN_PARAM, compositeKey);
        else
            return param.setValue(value, StateMachine.getCurrentState());
    }


    /**
     * Retrieves a collection of all parameters.
     *
     * @return a collection of all parameters
     */
    public Collection<AbstractParameter<?>> getParameters()
    {
        return parameterMap.values();
    }


    @Override
    public String toString()
    {
        final String format = getParameterFormat();
        final Map<String, StringBuilder> categoryStringBuilders = new HashMap<>();

        for (AbstractParameter<?> param : getParameters()) {

            // ignore unregistered parameters
            if (!param.isRegistered())
                continue;

            final String categoryName = param.getCategory().getName();

            if (!categoryStringBuilders.containsKey(categoryName)) {
                final StringBuilder catBuilder = new StringBuilder();
                catBuilder.append(String.format(ConfigurationConstants.CATEGORY_FORMAT, categoryName));
                categoryStringBuilders.put(categoryName, catBuilder);
            }

            categoryStringBuilders.get(categoryName).append(String.format(format, param.getKey(), param.getStringValue()));
        }

        StringBuilder combinedBuilder = new StringBuilder();
        categoryStringBuilders.forEach((String categoryName, StringBuilder categoryString) ->
                                       combinedBuilder.append(categoryString.toString())
                                      );
        return combinedBuilder.toString();
    }
}
