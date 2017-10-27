package de.gerdiproject.harvest.config.adapter;

import java.lang.reflect.Type;
import java.util.Map.Entry;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import de.gerdiproject.json.GsonUtils;

/**
 * This adapter defines the (de-)serialization behavior of {@linkplain Configuration} objects.
 *
 * @author Robin Weiss
 */
public class ConfigurationAdapter implements JsonDeserializer<Configuration>, JsonSerializer<Configuration>
{
    /**
     * Returns a {@linkplain Gson} with an integrated {@linkplain ConfigurationAdapter}.
     *
     * @return a {@linkplain Gson} with an integrated {@linkplain ConfigurationAdapter}
     */
    public static Gson getGson()
    {
        return new GsonBuilder().registerTypeAdapter(Configuration.class, new ConfigurationAdapter()).create();
    }


    @Override
    public Configuration deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
    throws JsonParseException
    {
        Map<String, AbstractParameter<?>> globalParameters = ParameterFactory.createDefaultParameters();
        Map<String, AbstractParameter<?>> harvesterParameters = new LinkedHashMap<>();

        JsonObject configJson = json.getAsJsonObject();

        // fill global parameters
        JsonObject globalParamsJson = configJson.get("globalParameters").getAsJsonObject();
        globalParameters.forEach((String key, AbstractParameter<?> value)-> {
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
        Set<Entry<String, JsonElement>> harvesterParamsJson = configJson.get("harvesterParameters").getAsJsonObject().entrySet();

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

            // if parameter cannot be parsed, abort!
            if (param != null)
                harvesterParameters.put(key, param);
            else
                throw new JsonParseException(
                    String.format(ConfigurationConstants.PARSE_ERROR,
                                  GsonUtils.getPrettyGson().toJson(paramJson.getValue()),
                                  key));
        }

        return new Configuration(globalParameters, harvesterParameters);
    }


    @Override
    public JsonElement serialize(Configuration src, Type typeOfSrc, JsonSerializationContext context)
    {
        Map<String, AbstractParameter<?>> globalParameters = src.getGlobalParameters();
        Map<String, AbstractParameter<?>> harvesterParameters = src.getHarvesterParameters();

        JsonObject globalParamsJson = new JsonObject();
        JsonObject harvesterParamsJson = new JsonObject();

        globalParameters.forEach((String key, AbstractParameter<?> param) -> {

            if (param instanceof BooleanParameter)
                globalParamsJson.addProperty(key, (Boolean) param.getValue());

            else if (param instanceof IntegerParameter)
                globalParamsJson.addProperty(key, (Integer) param.getValue());

            else if (param instanceof StringParameter)
                globalParamsJson.addProperty(key, (String) param.getValue());

            else if (param instanceof UrlParameter)
                globalParamsJson.addProperty(key, ConfigurationConstants.URL_PREFIX + param.getValue().toString());
        });

        harvesterParameters.forEach((String key, AbstractParameter<?> param) -> {

            if (param instanceof BooleanParameter)
                harvesterParamsJson.addProperty(key, (Boolean) param.getValue());

            else if (param instanceof IntegerParameter)
                harvesterParamsJson.addProperty(key, (Integer) param.getValue());

            else if (param instanceof StringParameter)
                harvesterParamsJson.addProperty(key, (String) param.getValue());

            else if (param instanceof UrlParameter)
                globalParamsJson.addProperty(key, ConfigurationConstants.URL_PREFIX + param.getValue().toString());
        });

        // assemble config JSON file
        JsonObject configJson = new JsonObject();
        configJson.add("globalParameters", globalParamsJson);
        configJson.add("harvesterParameters", harvesterParamsJson);

        return configJson;
    }
}