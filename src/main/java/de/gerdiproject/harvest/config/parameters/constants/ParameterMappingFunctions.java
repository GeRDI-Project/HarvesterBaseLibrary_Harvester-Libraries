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
package de.gerdiproject.harvest.config.parameters.constants;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.function.Function;

import de.gerdiproject.harvest.config.parameters.AbstractParameter;
import de.gerdiproject.harvest.etls.AbstractETL;
import de.gerdiproject.harvest.etls.enums.ETLStatus;
import de.gerdiproject.harvest.etls.events.GetETLManagerEvent;
import de.gerdiproject.harvest.etls.utils.ETLManager;
import de.gerdiproject.harvest.event.EventSystem;

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
     * This function simply forwards the string parameter that was passed to it.
     * If any useful checks should come up in the future, they can be implemented here
     * to be used by all string parameters.
     *
     * @param value a string value
     *
     * @return a string value
     */
    public static  String mapToString(String value)
    {
        return value;
    }


    /**
     * This function checks if a string represents a boolean value and throws an
     * exception if it does not.
     *
     * @param value a string representation of a parameter value
     *
     * @return a boolean value
     *
     * @throws RuntimeException this exception is thrown when the String value cannot be mapped to the target value
     */
    public static  Boolean mapToBoolean(String value) throws RuntimeException
    {
        if (value == null)
            return false;

        else if (ParameterConstants.BOOLEAN_VALID_VALUES_LIST.contains(value))
            return value.equals(ParameterConstants.BOOLEAN_VALID_VALUES_LIST.get(0)) || Boolean.parseBoolean(value);

        else
            throw new ClassCastException(ParameterConstants.BOOLEAN_ALLOWED_VALUES);
    }


    /**
     * This function checks if a string represents an Integer value and throws an
     * exception if it does not.
     *
     * @param value a string representation of a parameter value
     *
     * @return a valid URL string
     *
     * @throws RuntimeException this exception is thrown when the String value cannot be mapped to the target value
     */
    public static Integer mapToInteger(String value) throws RuntimeException
    {
        if (value == null)
            return 0;

        else if (value.equals(ParameterConstants.INTEGER_VALUE_MAX))
            return Integer.MAX_VALUE;

        else if (value.equals(ParameterConstants.INTEGER_VALUE_MIN))
            return Integer.MIN_VALUE;

        else {
            try {
                // try to parse the integer
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new ClassCastException(ParameterConstants.INTEGER_ALLOWED_VALUES);
            }
        }
    }


    /**
     * This function checks if a string represents a positive Integer value and throws an
     * exception if it does not.
     *
     * @param value a string representation of a parameter value
     *
     * @return the integer value represented by the string
     *
     * @throws RuntimeException this exception is thrown when the String value cannot be mapped to the target value
     */
    public static Integer mapToUnsignedInteger(String value) throws RuntimeException
    {
        if (value == null)
            return 0;

        else if (value.equals(ParameterConstants.INTEGER_VALUE_MAX))
            return Integer.MAX_VALUE;

        else if (value.equals(ParameterConstants.INTEGER_VALUE_MIN))
            return 0;

        else {
            try {
                // try to parse the integer
                int intValue = Integer.parseInt(value);

                // do not accept negative numbers
                if (intValue < 0)
                    throw new IllegalArgumentException(ParameterConstants.INTEGER_RANGE_ALLOWED_VALUES);

                return intValue;
            } catch (NumberFormatException e) {
                throw new ClassCastException(ParameterConstants.INTEGER_RANGE_ALLOWED_VALUES);
            }
        }
    }


    /**
     * This function checks if a string represents a valid URL and returns the value if it does.
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
            throw new IllegalArgumentException(ParameterConstants.URL_PARAM_INVALID);
        }

        return value;
    }


    /**
     * Creates a mapping function that executes another mapping function while throwing an exception if a specified ETL
     * is currently busy.
     * The case is ignored when comparing the list values.
     *
     * @param originalMappingFunction the mapping function that is to be extended
     * @param etl the {@linkplain AbstractETL} of which the status is to be checked
     *
     * @param <VAL> the type of the parameter value
     *
     * @return a mapping function
     */
    public static <VAL> Function<String, VAL> createMapperForETL(Function<String, VAL> originalMappingFunction, AbstractETL<?, ?> etl)
    {
        return (String value) -> {
            final ETLStatus etlStatus = etl.getStatus();

            switch (etlStatus)
            {
                case QUEUED:
                case HARVESTING:
                case ABORTING:
                case CANCELLING:
                    throw new IllegalStateException(String.format(
                                                        ParameterConstants.ETL_PARAM_INVALID_STATE,
                                                        etl.getName(),
                                                        etlStatus.toString().toLowerCase()));

                default:
                    return originalMappingFunction.apply(value);
            }
        };
    }

    /**
     * Creates a mapping function that executes another mapping function while throwing an exception if any ETL
     * is currently busy.
     * The case is ignored when comparing the list values.
     *
     * @param originalMappingFunction the mapping function that is to be extended
     *
     * @param <VAL> the type of the parameter value
     *
     * @return a mapping function
     */
    public static <VAL> Function<String, VAL> createMapperForETLs(Function<String, VAL> originalMappingFunction)
    {
        return (String value) -> {
            final ETLManager registry = EventSystem.sendSynchronousEvent(new GetETLManagerEvent());
            final ETLStatus overallEtlStatus = registry == null ? ETLStatus.INITIALIZING : registry.getStatus();

            switch (overallEtlStatus)
            {
                case QUEUED:
                case HARVESTING:
                case ABORTING:
                case CANCELLING:
                    throw new IllegalStateException(String.format(
                                                        ParameterConstants.ETL_REGISTRY_PARAM_INVALID_STATE,
                                                        overallEtlStatus.toString().toLowerCase()));

                default:
                    return originalMappingFunction.apply(value);
            }
        };
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

            throw new IllegalArgumentException(ParameterConstants.ALLOWED_VALUES + allowedValuesText);
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
            throw new IllegalArgumentException(ParameterConstants.ALLOWED_VALUES + allowedValuesStringBuilder.toString());
        };
    }
}
