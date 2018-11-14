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
package de.gerdiproject.harvest.config.json.adapters;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.parameters.AbstractParameter;

/**
 * This adapter defines the (de-)serialization behavior of
 * {@linkplain Configuration} objects.
 *
 * @author Robin Weiss
 */
public class ConfigurationAdapter implements JsonDeserializer<Configuration>, JsonSerializer<Configuration>
{
    private static final Type PARAM_MAP_TYPE = new TypeToken<Map<String, ParameterCategoryJson>>() {} .getType();
    private final String moduleName;


    /**
     * Constructor that requires the service name.
     *
     * @param moduleName the name of the service
     */
    public ConfigurationAdapter(String moduleName)
    {
        this.moduleName = moduleName;
    }


    @Override
    public Configuration deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
    {
        final Map<String, ParameterCategoryJson> jsonCategories = context.deserialize(json, PARAM_MAP_TYPE);

        // add all parameters to a single list
        final List<AbstractParameter<?>> parameters = new LinkedList<>();
        jsonCategories.forEach((String categoryName, ParameterCategoryJson jsonCat) ->
                               parameters.addAll(jsonCat.getParameters(categoryName)));

        // convert list to array
        final AbstractParameter<?>[] parameterArray = new AbstractParameter<?>[parameters.size()];
        parameters.toArray(parameterArray);

        return new Configuration(moduleName, parameterArray);
    }


    @Override
    public JsonElement serialize(Configuration src, Type typeOfSrc, JsonSerializationContext context)
    {
        final Map<String, ParameterCategoryJson> jsonCategoriesMap = new HashMap<>();

        for (AbstractParameter<?> param : src.getParameters()) {
            final String categoryName = param.getCategory();
            ParameterCategoryJson category = jsonCategoriesMap.get(categoryName);

            if (category == null) {
                category = new ParameterCategoryJson();
                jsonCategoriesMap.put(categoryName, category);
            }

            category.addParameter(param);
        }

        return context.serialize(jsonCategoriesMap);
    }
}
