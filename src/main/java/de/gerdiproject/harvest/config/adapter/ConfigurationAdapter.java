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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
import de.gerdiproject.harvest.config.parameters.ParameterFactory;
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
        // initialize global parameters with default values
        Map<String, AbstractParameter<?>> globalParameters = ParameterFactory.createDefaultParameters();
        Map<String, AbstractParameter<?>> harvesterParameters = ParameterFactory.createHarvesterParameters(null);

        JsonObject configJson = json.getAsJsonObject();

        // fill global parameters
        JsonObject globalParamsJson = configJson.get(ConfigurationConstants.GLOBAL_PARAMETERS_JSON).getAsJsonObject();
        globalParameters.forEach((String key, AbstractParameter<?> value) -> {
            JsonElement valueJson = globalParamsJson.get(key);

            if (valueJson != null)
            {
                if (valueJson.getAsJsonPrimitive().isString())
                    globalParameters.get(key).setValue(valueJson.getAsString(), null);

                else
                    globalParameters.get(key).setValue(valueJson.toString(), null);
            } else
                globalParameters.get(key).setValue(null, null);
        });

        // fill harvester parameters
        Set<Entry<String, JsonElement>> harvesterParamsJson =
            configJson.get(ConfigurationConstants.HARVESTER_PARAMETERS_JSON).getAsJsonObject().entrySet();

        for (Entry<String, JsonElement> paramJson : harvesterParamsJson) {
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
                if (valueJson.getAsString().startsWith(ConfigurationConstants.URL_PREFIX))
                    param = new UrlParameter(key, valueJson.getAsString());
                else
                    param = new StringParameter(key, valueJson.getAsString());

            }

            // if parameter cannot be parsed, abort the deserialization!
            if (param != null)
                harvesterParameters.put(key, param);
            else
                throw new JsonParseException(
                    String.format(
                        ConfigurationConstants.PARSE_ERROR,
                        paramJson.getValue().toString(),
                        key));
        }

        return new Configuration(globalParameters, harvesterParameters);
    }


    @Override
    public JsonElement serialize(Configuration src, Type typeOfSrc, JsonSerializationContext context)
    {
        // serialize global parameters
        Map<String, AbstractParameter<?>> globalParameters = src.getGlobalParameters();
        JsonObject globalParamsJson = new JsonObject();

        globalParameters.forEach((String key, AbstractParameter<?> param) -> {

            if (param instanceof BooleanParameter)
                globalParamsJson.addProperty(key, (Boolean) param.getValue());

            else if (param instanceof IntegerParameter)
                globalParamsJson.addProperty(key, (Integer) param.getValue());

            else if (param instanceof StringParameter)
                globalParamsJson.addProperty(key, (String) param.getValue());

            else if (param instanceof UrlParameter && param.getValue() != null)
                globalParamsJson.addProperty(key, ConfigurationConstants.URL_PREFIX + param.getValue().toString());

        });

        // serialize harvester specific parameters
        Map<String, AbstractParameter<?>> harvesterParameters = src.getHarvesterParameters();
        JsonObject harvesterParamsJson = new JsonObject();

        harvesterParameters.forEach((String key, AbstractParameter<?> param) -> {

            if (param instanceof BooleanParameter)
                harvesterParamsJson.addProperty(key, (Boolean) param.getValue());

            else if (param instanceof IntegerParameter)
                harvesterParamsJson.addProperty(key, (Integer) param.getValue());

            else if (param instanceof StringParameter)
                harvesterParamsJson.addProperty(key, (String) param.getValue());

            else if (param instanceof UrlParameter && param.getValue() != null)
                globalParamsJson.addProperty(key, ConfigurationConstants.URL_PREFIX + param.getValue().toString());
        });

        // assemble config JSON file
        JsonObject configJson = new JsonObject();
        configJson.add(ConfigurationConstants.GLOBAL_PARAMETERS_JSON, globalParamsJson);
        configJson.add(ConfigurationConstants.HARVESTER_PARAMETERS_JSON, harvesterParamsJson);

        return configJson;
    }
}
