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
package de.gerdiproject.harvest.config.adapter;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.config.parameters.AbstractParameter;
import de.gerdiproject.harvest.config.parameters.BooleanParameter;
import de.gerdiproject.harvest.config.parameters.IntegerParameter;
import de.gerdiproject.harvest.config.parameters.PasswordParameter;
import de.gerdiproject.harvest.config.parameters.StringParameter;
import de.gerdiproject.harvest.config.parameters.UrlParameter;

/**
 * This adapter defines the (de-)serialization behavior of
 * {@linkplain Configuration} objects.
 *
 * @author Robin Weiss
 */
public class ConfigurationAdapter implements JsonDeserializer<Configuration>, JsonSerializer<Configuration>
{
    @Override
    public Configuration deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
    {
        final JsonObject configJson = json.getAsJsonObject();

        // initialize global parameters with default values
        Map<String, AbstractParameter<?>> globalParameters =
            deserializeParameters(configJson.get(ConfigurationConstants.GLOBAL_PARAMETERS_JSON).getAsJsonObject());

        Map<String, AbstractParameter<?>> harvesterParameters =
            deserializeParameters(configJson.get(ConfigurationConstants.HARVESTER_PARAMETERS_JSON).getAsJsonObject());

        return new Configuration(globalParameters, harvesterParameters);
    }


    /**
     * Converts a JSON object to a parameter {@linkplain HashMap}.
     *
     * @param paramsJson the JSON object that is to be read
     *
     * @return a {@linkplain HashMap} containing parameters
     */
    private Map<String, AbstractParameter<?>> deserializeParameters(JsonObject paramsJson)
    {
        Map<String, AbstractParameter<?>> params = new HashMap<>();

        for (Entry<String, JsonElement> paramJson : paramsJson.entrySet()) {
            String key = paramJson.getKey();
            JsonPrimitive valueJson = paramJson.getValue().getAsJsonPrimitive();
            AbstractParameter<?> param = null;

            // boolean parameter
            if (valueJson.isBoolean())
                param = new BooleanParameter(key, valueJson.getAsBoolean());

            // integer parameter
            else if (valueJson.isNumber())
                param = new IntegerParameter(key, valueJson.getAsInt());

            // string parameter
            else if (valueJson.isString()) {
                final String stringVal = valueJson.getAsString();

                if (stringVal.startsWith(ConfigurationConstants.URL_PREFIX))
                    param = new UrlParameter(key, stringVal.substring(ConfigurationConstants.URL_PREFIX.length()));

                else if (stringVal.startsWith(ConfigurationConstants.PASSWORD_PREFIX))
                    param = new PasswordParameter(key, stringVal.substring(ConfigurationConstants.PASSWORD_PREFIX.length()));

                else
                    param = new StringParameter(key, stringVal);

            }

            // if parameter cannot be parsed, abort the deserialization!
            if (param != null)
                params.put(key, param);
            else
                throw new JsonParseException(
                    String.format(
                        ConfigurationConstants.PARSE_ERROR,
                        paramJson.getValue().toString(),
                        key));
        }

        return params;
    }


    @Override
    public JsonElement serialize(Configuration src, Type typeOfSrc, JsonSerializationContext context)
    {
        JsonObject globalParamsJson = serializeParameters(src.getGlobalParameters());
        JsonObject harvesterParamsJson = serializeParameters(src.getHarvesterParameters());

        // assemble config JSON file
        JsonObject configJson = new JsonObject();
        configJson.add(ConfigurationConstants.GLOBAL_PARAMETERS_JSON, globalParamsJson);
        configJson.add(ConfigurationConstants.HARVESTER_PARAMETERS_JSON, harvesterParamsJson);

        return configJson;
    }


    /**
     * Converts a {@linkplain HashMap} of parameters to a Json object.
     *
     * @param paramsJson the JSON object that is to be read
     *
     * @return a {@linkplain HashMap} containing parameters
     */
    private JsonObject serializeParameters(Map<String, AbstractParameter<?>> params)
    {
        JsonObject paramsJson = new JsonObject();

        params.forEach((String key, AbstractParameter<?> param) -> {
            if (param instanceof BooleanParameter)
                paramsJson.addProperty(key, (Boolean) param.getValue());

            else if (param instanceof IntegerParameter)
                paramsJson.addProperty(key, (Integer) param.getValue());

            else if (param instanceof UrlParameter && param.getValue() != null)
                paramsJson.addProperty(key, ConfigurationConstants.URL_PREFIX + param.getValue().toString());

            else if (param instanceof PasswordParameter && param.getValue() != null)
                paramsJson.addProperty(key, ConfigurationConstants.PASSWORD_PREFIX + param.getValue().toString());

            else if (param instanceof StringParameter)
                paramsJson.addProperty(key, (String) param.getValue());
        });

        return paramsJson;
    }
}
