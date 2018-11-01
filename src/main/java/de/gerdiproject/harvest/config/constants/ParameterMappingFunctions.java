/*
 *  Copyright © 2018 Robin Weiss (http://www.gerdi-project.de/)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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
package de.gerdiproject.harvest.config.constants;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.function.Function;

import de.gerdiproject.harvest.config.parameters.AbstractParameter;

/**
 * This class provides functions for mapping string values
 * to specific {@linkplain AbstractParameter} values.
 *
 * @author Robin Weiss
 */
public class ParameterMappingFunctions
{
    /**
     * Private constructor because this class only
     * provides public methods.
     */
    private ParameterMappingFunctions()
    {
    }


    /**
     * This function checks if a string represents a positive Integer value and throws an
     * exception if it is not
     *
     * @param value a string representation of a parameter value
     *
     * @return a valid URL string
     *
     * @throws RuntimeException this exception is thrown when the String value cannot be mapped to the target value
     */
    public static Integer mapToSignedInteger(String value) throws RuntimeException
    {
        if (value == null)
            return 0;

        else if (value.equals(ConfigurationConstants.INTEGER_VALUE_MAX))
            return Integer.MAX_VALUE;

        else if (value.equals(ConfigurationConstants.INTEGER_VALUE_MIN))
            return 0;

        else {
            try {
                // try to parse the integer
                int intValue = Integer.parseInt(value);

                // do not accept negative numbers
                if (intValue < 0)
                    throw new IllegalArgumentException(ConfigurationConstants.INTEGER_RANGE_ALLOWED_VALUES);

                return intValue;
            } catch (NumberFormatException e) {
                throw new ClassCastException(ConfigurationConstants.INTEGER_RANGE_ALLOWED_VALUES);
            }
        }
    }


    /**
     * This function checks if a string represents a valid URL and returns it if it is.
     *
     * @param value a string representation of a parameter value
     *
     * @return a valid URL string
     *
     * @throws RuntimeException this exception is thrown when the String value cannot be mapped to the target value
     */
    public static String mapToUrlString(String value) throws RuntimeException
    {
        if (value == null)
            return null;

        try {
            new URL(value);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(ConfigurationConstants.URL_ALLOWED_VALUES);
        }

        return value;
    }


    /**
     * Creates a mapping function that forwards input string values to the output only if they are
     * in a collection of valid values or if the input values are null.
     * The case is ignored when comparing the list values.
     *
     * @param validValues a collection of allowed string values
     *
     * @return a mapping function
     */
    public static Function<String, String> createStringListMapper(Collection<String> validValues)
    {
        return (String value) -> {
            // null values are always returned
            if (value == null)
                return null;

            // value is only valid if it is within the list
            for (String validVal : validValues)
            {
                if (value.equalsIgnoreCase(validVal))
                    return validVal;
            }

            // if the value is not in the list, throw exception
            String allowedValuesText = validValues.toString();
            allowedValuesText = allowedValuesText.substring(1, allowedValuesText.length() - 1);

            throw new IllegalArgumentException(ConfigurationConstants.ALLOWED_VALUES + allowedValuesText);
        };
    }


    /**
     * Creates a mapping function that forwards an input string if it is a simple class name
     * of a specified collection. The comparison ignores the case of the input string.
     *
     * @param validClasses a list of allowed class names
     *
     * @return a mapping function
     */
    public static Function<String, String> createClassNameListMapper(Collection<Class<?>> validClasses)
    {
        return (String value) -> {
            // null values are always returned
            if (value == null)
                return null;

            StringBuilder allowedValuesStringBuilder = new StringBuilder();

            for (Class<?> loaderClass : validClasses)
            {
                final String className = loaderClass.getSimpleName();

                if (className.equalsIgnoreCase(value))
                    return className;
                else {
                    // add class name to the list of allowed values, in case of a subsequent failure
                    if (allowedValuesStringBuilder.length() != 0)
                        allowedValuesStringBuilder.append(", ");

                    allowedValuesStringBuilder.append(className);
                }
            }

            // if the value is not in the list, throw exception
            throw new IllegalArgumentException(ConfigurationConstants.ALLOWED_VALUES + allowedValuesStringBuilder.toString());
        };
    }
}
