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
package de.gerdiproject.harvest.harvester;

import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.LinkedList;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

import de.gerdiproject.harvest.utils.data.HttpRequester;

/**
 * @author Robin Weiss
 *
 */
public class JsonArrayExtractor<IN> implements IIteratorExtractor<IN>
{
    private final Gson gson;
    private final String jsonQuery;
    private final HttpRequester httpRequester;

    private int startIndex;
    private int endIndex;
    private String url;


    public JsonArrayExtractor(HttpRequester httpRequester, Gson gson, String jsonQuery)
    {
        this.jsonQuery = jsonQuery;
        this.httpRequester = httpRequester;
        this.gson = gson;
    }


    public JsonArrayExtractor(HttpRequester httpRequester, Gson gson)
    {
        this(httpRequester, gson, null);
    }


    @Override
    public void init()
    {

    }


    @Override
    public void setRange(int startIndex, int endIndex)
    {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }


    @Override
    public Iterator<IN> extract()
    {
        final Type listType = new TypeToken<LinkedList<IN>>() {} .getType();

        final LinkedList<IN> convertedArray;

        if (jsonQuery == null)
            convertedArray = httpRequester.getObjectFromUrl(url, listType);
        else {
            JsonElement obj = httpRequester.getObjectFromUrl(url, JsonElement.class);

            // retrieve the JsonArray from the JsonObject via the provided query
            for (String q : jsonQuery.split("."))
                obj = obj.getAsJsonObject().get(q);

            convertedArray = gson.fromJson(obj, listType);
        }

        return convertedArray.subList(startIndex, endIndex).iterator();
    }


    @Override
    public int size()
    {
        final JsonArray arr;

        if (jsonQuery == null)
            arr = httpRequester.getObjectFromUrl(url, JsonArray.class);
        else {
            JsonElement obj = httpRequester.getObjectFromUrl(url, JsonElement.class);

            // retrieve the JsonArray from the JsonObject via the provided query
            for (String q : jsonQuery.split("."))
                obj = obj.getAsJsonObject().get(q);

            arr = obj.getAsJsonArray();
        }

        return arr == null ? -1 : arr.size();
    }


    @Override
    public String getUniqueVersionString()
    {
        return httpRequester.getObjectFromUrl(url, String.class);
    }
}
