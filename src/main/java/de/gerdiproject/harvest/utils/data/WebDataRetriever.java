/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
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
package de.gerdiproject.harvest.utils.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URL;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.utils.data.constants.DataOperationConstants;
import de.gerdiproject.json.GsonUtils;

/**
 * This class provides methods for reading files from the web.
 *
 * @author Robin Weiss
 */
public class WebDataRetriever implements IDataRetriever
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DiskIO.class);



    @Override
    public String getString(String url)
    {
        String responseText = null;

        try {
            // send web request
            InputStream response = new URL(url).openStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(response, MainContext.getCharset()));

            // read the first line of the response
            String line = reader.readLine();

            // make sure we got a response
            if (line != null) {
                StringBuilder responseBuilder = new StringBuilder(line);

                // read subsequent lines of the response
                line = reader.readLine();

                while (line != null) {
                    // add linebreak before appending the next line
                    responseBuilder.append('\n').append(line);
                    line = reader.readLine();
                }

                responseText = responseBuilder.toString();
            }

            // close the response reader
            reader.close();

        } catch (Exception e) {
            LOGGER.warn(String.format(DataOperationConstants.WEB_ERROR_JSON, url, e.toString()));
        }

        return responseText;
    }

    @Override
    public JsonElement getJson(String url)
    {
        JsonElement jsonResponse = null;

        try {
            // send web request
            InputStream response = new URL(url).openStream();
            InputStreamReader reader = new InputStreamReader(response, MainContext.getCharset());
            JsonParser parser = new JsonParser();

            // parse the json object
            jsonResponse = parser.parse(reader);

            reader.close();

        } catch (Exception e) {
            LOGGER.warn(String.format(DataOperationConstants.WEB_ERROR_JSON, url, e.toString()));
        }

        return jsonResponse;
    }

    @Override
    public <T> T getObject(String url, Class<T> targetClass)
    {
        T object = null;

        try {
            InputStream response = new URL(url).openStream();
            InputStreamReader reader = new InputStreamReader(response, MainContext.getCharset());

            object = GsonUtils.getGson().fromJson(reader, targetClass);
            reader.close();

        } catch (IOException | IllegalStateException | JsonIOException | JsonSyntaxException e) {
            LOGGER.warn(String.format(DataOperationConstants.WEB_ERROR_JSON, url, e.toString()));
        }

        return object;
    }

    @Override
    public <T> T getObject(String url, Type targetType)
    {
        T object = null;

        try {
            InputStream response = new URL(url).openStream();
            InputStreamReader reader = new InputStreamReader(response, MainContext.getCharset());

            object = GsonUtils.getGson().fromJson(reader, targetType);
            reader.close();

        } catch (IOException | IllegalStateException | JsonIOException | JsonSyntaxException e) {
            LOGGER.warn(String.format(ERROR_JSON, url, e.toString()));
        }

        return object;
    }

    @Override
    public Document getHtml(String url)
    {
        Document htmlResponse = null;

        try {
            // send web request
            InputStream response = new URL(url).openStream();

            // parse the html object
            htmlResponse = Jsoup.parse(response, MainContext.getCharset().displayName(), url);

            response.close();

        } catch (Exception e) {
            LOGGER.warn(String.format(DataOperationConstants.WEB_ERROR_JSON, url, e.toString()));
        }

        return htmlResponse;
    }
}
