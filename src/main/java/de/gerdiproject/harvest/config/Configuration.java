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

import javax.ws.rs.core.MultivaluedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.gerdiproject.harvest.config.adapter.ConfigurationAdapter;
import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.config.events.GetConfigurationEvent;
import de.gerdiproject.harvest.config.events.RegisterParameterEvent;
import de.gerdiproject.harvest.config.parameters.AbstractParameter;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.rest.AbstractRestObject;
import de.gerdiproject.harvest.state.StateMachine;
import de.gerdiproject.harvest.utils.cache.ICachedObject;
import de.gerdiproject.harvest.utils.data.DiskIO;



/**
 * This class manages all application {@linkplain AbstractParameter}s.
 * It can be (de-) serialized to JSON format, saving all parameters that were registered.
 *
 * @author Robin Weiss
 */
public class Configuration extends AbstractRestObject<Configuration, String> implements ICachedObject
{
    private final Gson gson;
    private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);

    private final transient DiskIO diskIo;
    private transient String cacheFilePath;

    private final Map<String, AbstractParameter<?>> parameterMap;


    /**
     * Constructor that requires a list of parameters.
     *
     * @param moduleName the name of the service
     * @param parameters a list parameters
     */
    public Configuration(final String moduleName, AbstractParameter<?>... parameters)
    {
        super(moduleName, GetConfigurationEvent.class);

        this.parameterMap = new TreeMap<>();

        for (AbstractParameter<?> param : parameters)
            parameterMap.put(param.getCompositeKey(), param);

        this.gson = new GsonBuilder().registerTypeAdapter(Configuration.class, new ConfigurationAdapter(moduleName)).create();
        this.diskIo = new DiskIO(gson, StandardCharsets.UTF_8);
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
        super.addEventListeners();
        EventSystem.addSynchronousListener(RegisterParameterEvent.class, this::onRegisterParameter);
    }


    @Override
    public void removeEventListeners()
    {
        super.removeEventListeners();
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
            LOGGER.info(String.format(ConfigurationConstants.NO_CONFIG_FILE_EXISTS, cacheFilePath));
            return;
        }

        this.parameterMap.clear();
        this.parameterMap.putAll(loadedConfig.parameterMap);

        for (AbstractParameter<?> param : getParameters())
            LOGGER.debug(String.format(ConfigurationConstants.LOADED_PARAM,
                                       param.getClass().getSimpleName(),
                                       param.getCompositeKey(),
                                       param.getStringValue()));

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

            LOGGER.debug(String.format(ConfigurationConstants.REGISTERED_PARAM,
                                       registeredParameter.getClass().getSimpleName(),
                                       registeredParameter.getCompositeKey(),
                                       registeredParameter.getStringValue()));

            saveToDisk();
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
     * Changes multiple parameters, returning a status message about the change.
     * Also saves the configuration afterwards.
     *
     * @param values a map of key-value parameter pairs
     *
     * @return a message describing if the operations were successful, or if not, why they failed
     */
    public String setParameters(Map<String, String> values)
    {
        final StringBuilder sb = new StringBuilder();
        values.forEach(
            (String key, String value) -> sb.append(setParameter(key, value)).append('\n')
        );
        saveToDisk();

        return sb.toString();
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
    protected String getPrettyPlainText()
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
        categoryStringBuilders.forEach((String categoryName, StringBuilder categoryString) -> {
            if (combinedBuilder.length() != 0)
                combinedBuilder.append("\n\n");
            combinedBuilder.append(categoryString.toString());
        });
        return combinedBuilder.toString();
    }


    @Override
    public String getAsJson(MultivaluedMap<String, String> query)
    {
        return gson.toJson(this);
    }
}
