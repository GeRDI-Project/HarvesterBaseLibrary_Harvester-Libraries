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
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractParameter.class);

    protected T value;
    protected final String key;
    protected final ParameterCategory category;

    private boolean isRegistered;


    /**
     * Constructor that assigns a category and a key that must be unique within the category.
     *
     * @param key the unique key of the parameter, which is used to change it via REST
     * @param category the category of the parameter
     *
     * @throws IllegalArgumentException thrown if the key contains invalid characters
     */
    public AbstractParameter(String key, ParameterCategory category) throws IllegalArgumentException
    {
        if (!key.matches(ConfigurationConstants.VALID_PARAM_NAME_REGEX))
            throw new IllegalArgumentException(String.format(ConfigurationConstants.INVALID_PARAMETER_KEY, key));

        this.key = key;
        this.category = category;
    }


    /**
     * Constructor that assigns all fields.
     *
     * @param key the unique key of the parameter, which is used to change it via REST
     * @param category the category of the parameter
     * @param defaultValue the default value
     */
    public AbstractParameter(String key, ParameterCategory category, T defaultValue)
    {
        this(key, category);
        this.value = defaultValue;
    }

    /**
     * Creates an unregistered copy of this parameter.
     *
     * @return a copy of this parameter
     */
    public abstract AbstractParameter<T> copy();


    /**
     * Returns a String explaining which values are allowed to be set.
     *
     * @return a String explaining which values are allowed to be set
     */
    protected abstract String getAllowedValues();


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
     * Returns a unique key consisting of the category and the parameter key.
     *
     * @return a unique key consisting of the category and the parameter key
     */
    public String getCompositeKey()
    {
        return String.format(
                   ConfigurationConstants.COMPOSITE_KEY,
                   category.getName().toLowerCase(),
                   key.toLowerCase());
    }


    /**
     * Returns the {@linkplain ParameterCategory} to which the parameter belongs.
     *
     * @return the {@linkplain ParameterCategory} to which the parameter belongs
     */
    public ParameterCategory getCategory()
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

        if (currentState != null && !category.getAllowedStates().contains(currentState.getClass()))
            returnMessage = String.format(ConfigurationConstants.CANNOT_CHANGE_PARAM_INVALID_STATE, getCompositeKey(), currentState.getName());
        else {
            try {
                this.value = stringToValue(value);
                returnMessage = String.format(ConfigurationConstants.CHANGED_PARAM, getCompositeKey(), getStringValue());
            } catch (ClassCastException e) {
                returnMessage = String.format(ConfigurationConstants.CANNOT_CHANGE_PARAM_INVALID_VALUE, getCompositeKey(), value, getAllowedValues());
            } catch (ParseException e) {
                returnMessage = e.getMessage();
            }
        }

        return returnMessage;
    }


    /**
     * Looks for an environment variable that contains the category and key of this parameter
     * and sets the value to that of the environment variable.
     */
    public void loadFromEnvironmentVariables()
    {
        String envVarName = String.format(ConfigurationConstants.ENVIRONMENT_VARIABLE, category.getName(), key);

        final Map<String, String> environmentVariables = System.getenv();

        if (environmentVariables.containsKey(envVarName))
            LOGGER.info(setValue(environmentVariables.get(envVarName), null));
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
