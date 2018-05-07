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
package de.gerdiproject.harvest.utils.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(WebDataRetriever.class);


    @Override
    public String getString(String url)
    {
        String responseText = null;

        try
            (BufferedReader reader = new BufferedReader(createWebReader(url))) {

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
        } catch (Exception e) {
            LOGGER.warn(String.format(DataOperationConstants.WEB_ERROR_JSON, url), e);
        }

        return responseText;
    }


    @Override
    public JsonElement getJson(String url)
    {
        JsonElement jsonResponse = null;

        try
            (InputStreamReader reader = createWebReader(url)) {
            JsonParser parser = new JsonParser();

            // parse the json object
            jsonResponse = parser.parse(reader);

        } catch (Exception e) {
            LOGGER.warn(String.format(DataOperationConstants.WEB_ERROR_JSON, url), e);
        }

        return jsonResponse;
    }


    @Override
    public <T> T getObject(String url, Class<T> targetClass)
    {
        T object = null;

        try
            (InputStreamReader reader = createWebReader(url)) {
            object = GsonUtils.getGson().fromJson(reader, targetClass);

        } catch (IOException | IllegalStateException | JsonIOException | JsonSyntaxException e) {
            LOGGER.warn(String.format(DataOperationConstants.WEB_ERROR_JSON, url), e);
        }

        return object;
    }


    @Override
    public <T> T getObject(String url, Type targetType)
    {
        T object = null;

        try
            (InputStreamReader reader = createWebReader(url)) {
            object = GsonUtils.getGson().fromJson(reader, targetType);

        } catch (IOException | IllegalStateException | JsonIOException | JsonSyntaxException e) {
            LOGGER.warn(String.format(DataOperationConstants.WEB_ERROR_JSON, url), e);
        }

        return object;
    }


    @Override
    public Document getHtml(String url)
    {
        Document htmlResponse = null;

        try
            (InputStream response = new URL(url).openStream()) {
            // parse the html object
            htmlResponse = Jsoup.parse(response, MainContext.getCharset().displayName(), url);

        } catch (Exception e) {
            LOGGER.warn(String.format(DataOperationConstants.WEB_ERROR_JSON, url), e);
        }

        return htmlResponse;
    }

    /**
     * Creates an input stream reader of a specified URL.
     *
     * @param url the URL of which the response is to be read
     *
     * @return a reader of the URL response
     *
     * @throws MalformedURLException thrown when the URL is malformed
     * @throws IOException thrown for various reasons when the reader is created
     */
    private InputStreamReader createWebReader(String url) throws MalformedURLException, IOException
    {
        return new InputStreamReader(new URL(url).openStream(), MainContext.getCharset());
    }
}
