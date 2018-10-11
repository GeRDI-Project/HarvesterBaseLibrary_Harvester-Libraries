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
package de.gerdiproject.harvest.harvester.extractors;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

import de.gerdiproject.harvest.harvester.AbstractETL;
import de.gerdiproject.harvest.utils.HashGenerator;
import de.gerdiproject.harvest.utils.data.HttpRequester;

/**
 * @author Robin Weiss
 *
 */
public class JsonArrayExtractor<IN> extends AbstractIteratorExtractor<IN>
{
    private final Gson gson;
    private final String jsonQuery;
    private final HttpRequester httpRequester;
    private final String url;

    private String hash;
    private List<IN> extractedList;


    /**
     * Constructor that expects a JSON array to be contained in the
     * HTTP response, navigating through the response via a specified query.
     *
     * @param url the URL that is used to retrieve the JSON response
     * @param gson used to parse the JSON response
     * @param jsonQuery a dot separated object structure at which the JSON array is expected
     */
    public JsonArrayExtractor(String url, Gson gson, String jsonQuery)
    {
        this.url = url;
        this.jsonQuery = jsonQuery;
        this.httpRequester = new HttpRequester(gson, StandardCharsets.UTF_8);
        this.gson = gson;
    }


    /**
     * Constructor that expects a JSON array to be returned directly
     * as response from the specified URL.
     *
     * @param url the URL that is used to retrieve the JSON response
     * @param gson used to parse the JSON response
     */
    public JsonArrayExtractor(String url, Gson gson)
    {
        this(url, gson, null);
    }


    @Override
    public <H extends AbstractETL<?, ?>> void init(H harvester)
    {
        super.init(harvester);
        this.httpRequester.setCharset(harvester.getCharset());

        final JsonElement jsonResponse = httpRequester.getObjectFromUrl(url, JsonElement.class);
        this.extractedList = getListFromJson(jsonResponse);
        this.hash = getHashFromJson(jsonResponse);
    }


    @Override
    public Iterator<IN> extractAll()
    {
        return extractedList.listIterator();
    }


    @Override
    public int size()
    {
        return extractedList.size();
    }


    @Override
    public String getUniqueVersionString()
    {
        return hash;
    }


    /**
     * Retrieves a hash value from a JSON object.
     *
     * @param json the JSON object of which the hash is retrieved
     *
     * @return a hash value representing the current state of the object
     */
    protected String getHashFromJson(JsonElement json)
    {
        final HashGenerator gen = new HashGenerator(StandardCharsets.UTF_8);
        return gen.getShaHash(json.toString());
    }


    /**
     * Parses a JSON object to retrieve a JSON array
     * and converts the array to a {@linkplain LinkedList}.
     *
     * @param json the JSON object of which the list is retrieved
     *
     * @return a list containing the elements of a JSON array
     */
    protected List<IN> getListFromJson(JsonElement json)
    {
        // retrieve the JsonArray from the JsonObject via the provided query
        if (jsonQuery != null) {
            for (String q : jsonQuery.split("\\."))
                json = json.getAsJsonObject().get(q);
        }

        // convert JSON array to List
        final Type listType = new TypeToken<LinkedList<IN>>() {} .getType();
        return gson.fromJson(json, listType);
    }
}
