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
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.Consumer;

import javax.ws.rs.core.MultivaluedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.config.events.GetConfigurationEvent;
import de.gerdiproject.harvest.config.events.ParameterChangedEvent;
import de.gerdiproject.harvest.config.events.RegisterParameterEvent;
import de.gerdiproject.harvest.config.events.UnregisterParameterEvent;
import de.gerdiproject.harvest.config.json.adapters.ConfigurationAdapter;
import de.gerdiproject.harvest.config.parameters.AbstractParameter;
import de.gerdiproject.harvest.config.parameters.constants.ParameterConstants;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.rest.AbstractRestObject;
import de.gerdiproject.harvest.utils.data.DiskIO;
import de.gerdiproject.harvest.utils.file.ICachedObject;



/**
 * This class manages all application {@linkplain AbstractParameter}s.
 * It can be (de-) serialized to JSON format, saving all parameters that were registered.
 *
 * @author Robin Weiss
 */
public class Configuration extends AbstractRestObject<Configuration, Configuration> implements ICachedObject
{
    private static final Gson GSON = new GsonBuilder().registerTypeAdapter(Configuration.class, new ConfigurationAdapter()).create();
    private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);

    private final transient DiskIO diskIo;
    private transient String cacheFilePath;

    private final Map<String, AbstractParameter<?>> parameterMap;

    private final Consumer<UnregisterParameterEvent> onUnregisterParameterCallback = this::onUnregisterParameter;


    /**
     * Constructor that requires a list of parameters.
     *
     * @param moduleName the name of the service
     * @param parameters a list parameters
     */
    public Configuration(final String moduleName, final AbstractParameter<?>... parameters)
    {
        super(moduleName, GetConfigurationEvent.class);

        this.parameterMap = new TreeMap<>();

        for (final AbstractParameter<?> param : parameters)
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
     * @throws IllegalStateException thrown if no {@linkplain Configuration} exists
     *
     * @return the registered parameter as it appears in the configuration
     */
    @SuppressWarnings("unchecked")  // the cast will succeed, because the value type is the one that is registered
    public static <T extends AbstractParameter<?>> T registerParameter(final T parameter) throws IllegalStateException
    {
        final T registeredParam = (T) EventSystem.sendSynchronousEvent(new RegisterParameterEvent(parameter));

        if (registeredParam == null)
            throw new IllegalStateException(String.format(ConfigurationConstants.REGISTER_ERROR, parameter.getCompositeKey()));

        return registeredParam;
    }

    /**
     * Convenience function for unregistering a parameter from the configuration.
     *
     * @param parameter the parameter to be unregistered
     */
    public static void unregisterParameter(final AbstractParameter<?> parameter)
    {
        EventSystem.sendEvent(new UnregisterParameterEvent(parameter));
    }


    @Override
    public void addEventListeners()
    {
        super.addEventListeners();
        EventSystem.addSynchronousListener(RegisterParameterEvent.class, this::onRegisterParameter);
        EventSystem.addListener(UnregisterParameterEvent.class, onUnregisterParameterCallback);
    }


    @Override
    public void removeEventListeners()
    {
        super.removeEventListeners();
        EventSystem.removeSynchronousListener(RegisterParameterEvent.class);
        EventSystem.removeListener(UnregisterParameterEvent.class, onUnregisterParameterCallback);
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

        for (final AbstractParameter<?> param : parameterMap.values()) {
            final int keyLength = param.getKey().length();

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
    public void setCacheFilePath(final String path)
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
            LOGGER.error(ConfigurationConstants.SAVE_NO_PATH_ERROR);
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
            LOGGER.error(String.format(ConfigurationConstants.LOAD_ERROR, "", ConfigurationConstants.NO_PATH));
            return;
        }

        // read JSON from disk
        final Configuration loadedConfig = diskIo.getObject(cacheFilePath, Configuration.class);

        if (loadedConfig == null) {
            LOGGER.info(String.format(ConfigurationConstants.NO_CONFIG_FILE_ERROR, cacheFilePath));
            return;
        }

        this.parameterMap.clear();
        this.parameterMap.putAll(loadedConfig.parameterMap);

        for (final AbstractParameter<?> param : getParameters())
            LOGGER.debug(String.format(ConfigurationConstants.LOADED_PARAM,
                                       param.getClass().getSimpleName(),
                                       param.getCompositeKey(),
                                       param.getStringValue()));

        LOGGER.info(String.format(ConfigurationConstants.LOAD_OK, cacheFilePath));
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
    public <T> T getParameterValue(final String compositeKey)
    {
        final AbstractParameter<?> param = parameterMap.get(compositeKey.toLowerCase(Locale.ENGLISH));

        // check if the parameter exists and if the value matches the parameterType
        if (param == null || param.getValue() == null)
            return null;

        return (T) param.getValue();
    }


    /**
     * Returns the human readable value of the parameter with a specified key.
     *
     * @param compositeKey the parameter category and name, separated by a dot
     *
     * @return the human readable value of the parameter with a specified key
     */
    public String getParameterStringValue(final String compositeKey)
    {
        final AbstractParameter<?> param = parameterMap.get(compositeKey.toLowerCase(Locale.ENGLISH));

        // check if the parameter exists
        return param == null ? null : param.getStringValue();
    }


    /**
     * Changes a configuration parameter, returning a status message about the change.
     *
     * @param compositeKey the parameter category and name, separated by a dot
     * @param value the new value of the parameter
     *
     * @throws IllegalArgumentException if the compositeKey is empty or does not exist
     */
    public void setParameter(final String compositeKey, final String value) throws IllegalArgumentException
    {
        final AbstractParameter<?> param = parameterMap.get(compositeKey.toLowerCase(Locale.ENGLISH));

        // change the parameter value or return an error, if it does not exist
        if (param == null || !param.isRegistered())
            throw new IllegalArgumentException(String.format(ConfigurationConstants.SET_UNKNOWN_PARAM_ERROR, compositeKey));

        final Object oldValue = param.getValue();

        param.setValue(value);

        final Object newValue = param.getValue();

        if (oldValue == null && newValue != null || oldValue != null && !oldValue.equals(newValue)) {
            EventSystem.sendEvent(new ParameterChangedEvent(param, oldValue));
            LOGGER.debug(String.format(ParameterConstants.CHANGED_PARAM, param.getCompositeKey(), param.getStringValue()));
        }
    }


    /**
     * Changes multiple parameters, returning a status message about the change.
     * Also saves the configuration afterwards.
     *
     * @param values a map of key-value parameter pairs
     *
     * @throws IllegalArgumentException if the values are empty or non of them are valid
     * @return a message describing if the operations were successful, or if not, why they failed
     */
    public String changeParameters(final Map<String, String> values) throws IllegalArgumentException
    {
        if (values == null || values.isEmpty())
            throw new IllegalArgumentException(ConfigurationConstants.SET_NO_PAYLOAD_ERROR);

        boolean hasChanged = false;
        final StringBuilder sb = new StringBuilder();

        // change every defined parameter
        for (final Entry<String, String> p : values.entrySet()) {
            if (sb.length() != 0)
                sb.append('\n');

            String feedback;

            try {
                setParameter(p.getKey(), p.getValue());
                hasChanged = true;
                feedback = String.format(
                               ParameterConstants.CHANGED_PARAM,
                               p.getKey(),
                               getParameterStringValue(p.getKey()));
            } catch (final IllegalArgumentException e) {
                feedback = e.getMessage();
                LOGGER.warn("", e);
            }

            sb.append(feedback);
        }

        // if no parameter changed, the request fails
        if (!hasChanged)
            throw new IllegalArgumentException(sb.toString());

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

        for (final AbstractParameter<?> param : getParameters()) {
            // ignore unregistered parameters
            if (!param.isRegistered())
                continue;

            final String categoryName = param.getCategory();

            if (!categoryStringBuilders.containsKey(categoryName)) {
                final StringBuilder catBuilder = new StringBuilder();
                catBuilder.append(String.format(ConfigurationConstants.CATEGORY_FORMAT, categoryName));
                categoryStringBuilders.put(categoryName, catBuilder);
            }

            categoryStringBuilders.get(categoryName).append(String.format(format, param.getKey(), param.getStringValue()));
        }

        final StringBuilder combinedBuilder = new StringBuilder();
        categoryStringBuilders.forEach((final String categoryName, final StringBuilder categoryString) -> {
            if (combinedBuilder.length() != 0)
                combinedBuilder.append("\n\n");
            combinedBuilder.append(categoryString.toString());
        });
        return combinedBuilder.toString();
    }


    @Override
    public Configuration getAsJson(final MultivaluedMap<String, String> query)
    {
        return this;
    }



    //////////////////////////////
    // Event Callback Functions //
    //////////////////////////////

    /**
     * Synchronous Event callback:<br>
     * Registers a parameter in the configuration. If a parameter with the same key already exists
     * in the configuration, the value itself will not be overwritten, whereas the mapping function
     * is overwritten if the parameter was not properly registered before.<br>
     * A newly registered parameter checks if it is defined via environment variables,
     * and will retrieve a value from there.
     *
     * @param event the event that triggered the callback
     *
     * @return the registered parameter as it appears in the configuration
     */
    @SuppressWarnings("unchecked") // the cast must succeed, because the parameter must be the same
    private <T> AbstractParameter<T> onRegisterParameter(final RegisterParameterEvent event)
    {
        AbstractParameter<T> registeredParameter = (AbstractParameter<T>) event.getParameter();
        final String compositeKey = registeredParameter.getCompositeKey();
        final AbstractParameter<T> retrievedParameter = (AbstractParameter<T>) parameterMap.get(compositeKey);

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
        } else {
            // make sure to overwrite the default mapping function of parameters loaded from disk
            if (!retrievedParameter.isRegistered())
                retrievedParameter.setMappingFunction(registeredParameter.getMappingFunction());

            registeredParameter = retrievedParameter;
        }

        registeredParameter.setRegistered(true);
        return registeredParameter;
    }


    /**
     * Unregisters a parameter so it can no longer be changed via REST.
     * This is an event callback function.
     *
     * @param event the event that triggered the callback
     */
    private void onUnregisterParameter(final UnregisterParameterEvent event)
    {
        event.getParameter().setRegistered(false);
    }
}
