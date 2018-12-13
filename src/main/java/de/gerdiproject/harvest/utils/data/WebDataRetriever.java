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
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.xml.ws.http.HTTPException;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import de.gerdiproject.harvest.utils.data.constants.DataOperationConstants;
import de.gerdiproject.harvest.utils.data.enums.RestRequestType;

/**
 * This class provides methods for reading files from the web.
 *
 * @author Robin Weiss
 */
public class WebDataRetriever implements IDataRetriever
{
    private static final Logger LOGGER = LoggerFactory.getLogger(WebDataRetriever.class);
    private final Gson gson;
    private Charset charset;


    /**
     * Constructor that sets the GSON (de-)serializer for reading and
     * writing JSON objects.
     *
     * @param gson the GSON (de-)serializer for reading and writing JSON objects
     * @param charset the charset of the files to be read and written
     */
    public WebDataRetriever(Gson gson, Charset charset)
    {
        this.gson = gson;
        this.charset = charset;
    }


    /**
     * Constructor that copies settings from another {@linkplain WebDataRetriever}.
     *
     * @param other the {@linkplain WebDataRetriever} of which the settings are copied
     */
    public WebDataRetriever(WebDataRetriever other)
    {
        this.gson = other.gson;
        this.charset = other.charset;
    }


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
    public <T> T getObject(String url, Class<T> targetClass)
    {
        T object = null;

        try
            (InputStreamReader reader = createWebReader(url)) {
            object = gson.fromJson(reader, targetClass);

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
            object = gson.fromJson(reader, targetType);

        } catch (IOException | IllegalStateException | JsonIOException | JsonSyntaxException e) {
            LOGGER.warn(String.format(DataOperationConstants.WEB_ERROR_JSON, url), e);
        }

        return object;
    }


    @Override
    public Document getHtml(String url)
    {
        try {
            final HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

            switch (connection.getResponseCode()) {
                case HttpURLConnection.HTTP_MOVED_TEMP :
                case HttpURLConnection.HTTP_MOVED_PERM :
                case HttpURLConnection.HTTP_SEE_OTHER :
                    connection.disconnect();
                    final String redirectedUrl = connection.getHeaderField(DataOperationConstants.REDIRECT_LOCATION_HEADER);
                    return getHtml(redirectedUrl);

                default:
                    return Jsoup.parse(connection.getInputStream(), charset.displayName(), url);
            }
        } catch (Exception e) {
            LOGGER.warn(String.format(DataOperationConstants.WEB_ERROR_JSON, url), e);
            return null;
        }
    }


    @Override
    public void setCharset(Charset charset)
    {
        this.charset = charset;
    }


    /**
     * Sends an authorized REST request with a specified body and returns the
     * response as a string.
     *
     * @param method the request method that is being sent
     * @param url the URL to which the request is being sent
     * @param body the body of the request
     * @param authorization the base-64-encoded username and password, or null if no
     *                       authorization is required
     * @param contentType the contentType of the body
     *
     * @throws HTTPException thrown if the response code is not 2xx
     * @throws IOException thrown if the response output stream could not be created
     *
     * @return the HTTP response as plain text
     */
    public String getRestResponse(RestRequestType method, String url, String body, String authorization, String contentType) throws HTTPException, IOException
    {
        HttpURLConnection connection = sendRestRequest(method, url, body, authorization, contentType);

        // create a reader for the HTTP response
        InputStream response = connection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(response, charset));

        // read the first line of the response
        String line = reader.readLine();
        String responseText = null;

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

        // combine the read lines to a single string
        return responseText;
    }


    /**
     * Sends an authorized REST request with a specified body and returns the
     * header fields.
     *
     * @param method the request method that is being sent
     * @param url the URL to which the request is being sent
     * @param body the body of the request
     * @param authorization the base-64-encoded username and password, or null if no
     *                       authorization is required
     * @param contentType the contentType of the body
     *
     * @throws HTTPException thrown if the response code is not 2xx
     * @throws IOException thrown if the response output stream could not be created
     *
     * @return the response header fields, or null if the response could not be parsed
     */
    public Map<String, List<String>> getRestHeader(RestRequestType method, String url, String body,
                                                   String authorization, String contentType) throws HTTPException, IOException
    {
        Map<String, List<String>> headerFields = null;

        HttpURLConnection connection = sendRestRequest(method, url, body, authorization, contentType);
        headerFields = connection.getHeaderFields();

        return headerFields;
    }


    /**
     * Sends a REST request with a specified body and returns the connection.
     *
     * @param method the request method that is being sent
     * @param url the URL to which the request is being sent
     * @param body the body of the request
     * @param authorization the base-64-encoded username and password, or null if no
     *                           authorization is required
     * @param contentType the contentType of the body
     *
     * @throws HTTPException thrown if the response code is not 2xx
     * @throws IOException thrown if the response output stream could not be created
     *
     * @return the connection to the host
     */
    private HttpURLConnection sendRestRequest(RestRequestType method, String url, String body, String authorization, String contentType)
    throws IOException, HTTPException
    {
        // generate a URL and open a connection
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

        // set request properties
        connection.setDoOutput(true);
        connection.setInstanceFollowRedirects(false);
        connection.setUseCaches(false);
        connection.setRequestMethod(method.toString());
        connection.setRequestProperty(HttpHeaders.CONTENT_TYPE, contentType);
        connection.setRequestProperty(DataOperationConstants.REQUEST_PROPERTY_CHARSET, charset.displayName());

        // set authentication
        if (authorization != null)
            connection.setRequestProperty(HttpHeaders.AUTHORIZATION, authorization);

        // only send date if it is specified
        if (body != null) {
            // convert body string to bytes
            byte[] bodyBytes = body.getBytes(charset);
            connection.setRequestProperty(HttpHeaders.CONTENT_LENGTH, Integer.toString(bodyBytes.length));

            // try to send body
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.write(bodyBytes);
            wr.close();
        }

        // check if we got an erroneous response
        int responseCode = connection.getResponseCode();

        if (responseCode < 200 || responseCode >= 300) {
            final String message = String.format(DataOperationConstants.WEB_ERROR_REST_HTTP, method.toString(), url, body, responseCode);
            throw new HttpStatusException(message, responseCode, url);
        }

        return connection;
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
        final HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

        switch (connection.getResponseCode()) {
            case HttpURLConnection.HTTP_MOVED_TEMP :
            case HttpURLConnection.HTTP_MOVED_PERM :
            case HttpURLConnection.HTTP_SEE_OTHER :
                connection.disconnect();
                final String redirectedUrl = connection.getHeaderField(DataOperationConstants.REDIRECT_LOCATION_HEADER);
                return createWebReader(redirectedUrl);

            default:
                return new InputStreamReader(connection.getInputStream(), charset);
        }
    }
}
