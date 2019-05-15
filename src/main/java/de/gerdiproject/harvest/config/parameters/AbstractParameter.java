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

import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.parameters.constants.ParameterConstants;
import lombok.Getter;
import lombok.Setter;

/**
 * Parameters are part of the {@linkplain Configuration}. Each parameter holds some information about how and when it can be changed.
 *
 * @author Robin Weiss
 *
 * @param <V> The underlying type of the parameter value
 */
public abstract class AbstractParameter<V>
{
    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractParameter.class);

    /**
     * The parameter value.
     */
    @Getter
    protected V value;

    /**
     * The key of the parameter that must be unique within its category.
     */
    @Getter
    protected final String key;

    /**
     * The category to which the parameter belongs.
     */
    @Getter
    protected final String category;

    /**
     * This function maps a String value to the parameter value type.
     */
    @Getter @Setter
    protected Function<String, V> mappingFunction;

    /**
     * This boolean value is true if the parameter is registered at the {@linkplain Configuration}.
     */
    @Getter @Setter
    private boolean registered;


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
    public AbstractParameter(final String key, final String category, final V defaultValue, final Function<String, V> customMappingFunction) throws IllegalArgumentException
    {
        if (!key.matches(ParameterConstants.VALID_PARAM_NAME_REGEX))
            throw new IllegalArgumentException(String.format(ParameterConstants.INVALID_PARAMETER_KEY, key));

        if (!category.matches(ParameterConstants.VALID_PARAM_NAME_REGEX))
            throw new IllegalArgumentException(String.format(ParameterConstants.INVALID_CATEGORY_NAME, category));

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
    public abstract AbstractParameter<V> copy();


    /**
     * Returns a unique key consisting of the category and the parameter key.
     *
     * @return a unique key consisting of the category and the parameter key
     */
    public String getCompositeKey()
    {
        return String.format(
                   ParameterConstants.COMPOSITE_KEY,
                   category.toLowerCase(Locale.ENGLISH),
                   key.toLowerCase(Locale.ENGLISH));
    }


    /**
     * Returns a human readable String of the parameter value.
     *
     * @return a human readable String of the parameter value
     */
    public String getStringValue()
    {
        return value == null ? "" : value.toString();
    }


    /**
     * Changes the stored value by parsing a String argument.
     *
     * @param value a String representation of the new value
     *
     * @throws IllegalArgumentException if the value could not be changed
     */
    public final void setValue(final String value) throws IllegalArgumentException
    {
        // try to map the input string to the expected parameter value
        try {
            final V newValue = mappingFunction.apply(value);
            this.value = newValue;
        } catch (final RuntimeException e) { // NOPMD mappingFunction may throw any exception
            throw new IllegalArgumentException(
                String.format(
                    ParameterConstants.CANNOT_CHANGE_PARAM,
                    getCompositeKey(),
                    value, e.getMessage()),
                e);
        }
    }


    /**
     * Looks for an environment variable that contains the category and key of this parameter
     * and sets the value to that of the environment variable.
     */
    public void loadFromEnvironmentVariables()
    {
        final String envVarName = String.format(ParameterConstants.ENVIRONMENT_VARIABLE, category, key);

        final Map<String, String> environmentVariables = System.getenv();

        if (environmentVariables.containsKey(envVarName))
            setValue(environmentVariables.get(envVarName));
    }


    @Override
    public String toString()
    {
        return getKey() + " : " + getStringValue();
    }
}
