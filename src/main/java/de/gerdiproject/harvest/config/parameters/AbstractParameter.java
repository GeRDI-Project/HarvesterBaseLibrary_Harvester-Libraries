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

import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.parameters.constants.ParameterConstants;

/**
 * Parameters are part of the {@linkplain Configuration}. Each parameter holds some information about how and when it can be changed.
 *
 * @author Robin Weiss
 *
 * @param <T> The underlying type of the parameter value
 */
public abstract class AbstractParameter<T>
{
    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractParameter.class);

    protected T value;
    protected final String key;
    protected final String category;

    /**
     * This function maps a String value to the parameter value type.
     */
    protected Function<String, T> mappingFunction;

    private boolean isRegistered;


    /**
     * Constructor that uses a custom mapping function.
     *
     * @param key the unique key of the parameter, which is used to change it via REST
     * @param category the category of the parameter
     * @param defaultValue the default value
     * @param customMappingFunction a function that maps strings to the parameter values
     *
     * @throws IllegalArgumentException thrown if the key contains invalid characters
     */
    public AbstractParameter(String key, String category, T defaultValue, Function<String, T> customMappingFunction) throws IllegalArgumentException
    {
        if (!key.matches(ParameterConstants.VALID_PARAM_NAME_REGEX))
            throw new IllegalArgumentException(String.format(ParameterConstants.INVALID_PARAMETER_KEY, key));

        if (!category.matches(ParameterConstants.VALID_PARAM_NAME_REGEX))
            throw new IllegalArgumentException(String.format(ParameterConstants.INVALID_CATEGORY_NAME, key));

        this.key = key;
        this.category = category;
        this.value = defaultValue;
        this.mappingFunction = customMappingFunction;
    }


    /**
     * Creates an unregistered copy of this parameter.
     *
     * @return a copy of this parameter
     */
    public abstract AbstractParameter<T> copy();


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
     * Returns a unique key consisting of the category and the parameter key.
     *
     * @return a unique key consisting of the category and the parameter key
     */
    public String getCompositeKey()
    {
        return String.format(
                   ParameterConstants.COMPOSITE_KEY,
                   category.toLowerCase(),
                   key.toLowerCase());
    }


    /**
     * Returns the category to which the parameter belongs.
     *
     * @return the category to which the parameter belongs
     */
    public String getCategory()
    {
        return category;
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
        return value != null ? value.toString() : "";
    }


    /**
     * Retrieves the function that maps strings to parameter values.
     *
     * @return the function that maps strings to parameter values
     */
    public Function<String, T> getMappingFunction()
    {
        return mappingFunction;
    }


    /**
     * Changes the function that maps strings to the parameter values.
     *
     * @param mappingFunction a function that maps strings to parameter values
     */
    public void setMappingFunction(Function<String, T> mappingFunction)
    {
        this.mappingFunction = mappingFunction;
    }


    /**
     * Changes the value by parsing a String value. A message is returned that describes
     * whether the value change was successful, or if not, why it failed.
     *
     * @param value a String representation of the new value
     *
     * @return a message that describes whether the value change was successful, or if not, why it failed
     */
    public final String setValue(String value)
    {
        final T newValue;

        // try to map the string value to
        try {
            newValue = mappingFunction.apply(value);
        } catch (RuntimeException e) {
            return String.format(ParameterConstants.CANNOT_CHANGE_PARAM, getCompositeKey(), value, e.getMessage());
        }

        this.value = newValue;
        return String.format(ParameterConstants.CHANGED_PARAM, getCompositeKey(), getStringValue());
    }


    /**
     * Looks for an environment variable that contains the category and key of this parameter
     * and sets the value to that of the environment variable.
     */
    public void loadFromEnvironmentVariables()
    {
        String envVarName = String.format(ParameterConstants.ENVIRONMENT_VARIABLE, category, key);

        final Map<String, String> environmentVariables = System.getenv();

        if (environmentVariables.containsKey(envVarName))
            LOGGER.info(setValue(environmentVariables.get(envVarName)));
    }


    /**
     * Returns true if this parameter is registered at the {@linkplain Configuration}.
     *
     * @return true if this parameter is registered at the {@linkplain Configuration}
     */
    public boolean isRegistered()
    {
        return isRegistered;
    }


    /**
     * Sets a flag that signifies if this parameter is registered at the {@linkplain Configuration}.
     *
     * @param isRegistered if true, this parameter is considered to be registered
     */
    public void setRegistered(boolean isRegistered)
    {
        this.isRegistered = isRegistered;
    }


    @Override
    public String toString()
    {
        return getKey() + " : " + getStringValue();
    }
}
