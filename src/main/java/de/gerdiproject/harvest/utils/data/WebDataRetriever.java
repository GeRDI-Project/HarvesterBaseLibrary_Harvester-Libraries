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
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.xml.ws.http.HTTPException;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.parameters.IntegerParameter;
import de.gerdiproject.harvest.rest.constants.RestConstants;
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
    private final IntegerParameter retriesParam;
    private int timeout;

    private Charset charset;


    /**
     * Constructor that sets the GSON (de-)serializer for reading and
     * writing JSON objects, as well as the charset and timeout.
     *
     * @param gson the GSON (de-)serializer for reading and writing JSON objects
     * @param charset the charset of the files to be read and written
     * @param timeout the web request timeout in milliseconds
     */
    public WebDataRetriever(Gson gson, Charset charset, int timeout)
    {
        this.gson = gson;
        this.charset = charset;
        this.timeout = timeout;

        // set up retries parameters
        IntegerParameter retriesTemp;

        try {
            retriesTemp = Configuration.registerParameter(DataOperationConstants.RETRIES_PARAM);
        } catch (IllegalStateException e) {
            retriesTemp = DataOperationConstants.RETRIES_PARAM;
        }

        this.retriesParam = retriesTemp;
    }

    /**
     * Constructor that sets the GSON (de-)serializer for reading and
     * writing JSON objects, as well as the charset.
     *
     * @param gson the GSON (de-)serializer for reading and writing JSON objects
     * @param charset the charset of the files to be read and written
     */
    public WebDataRetriever(Gson gson, Charset charset)
    {
        this(gson, charset, DataOperationConstants.NO_TIMEOUT);
    }


    /**
     * Constructor that copies settings from another {@linkplain WebDataRetriever}.
     *
     * @param other the {@linkplain WebDataRetriever} of which the settings are copied
     */
    public WebDataRetriever(WebDataRetriever other)
    {
        this(other.gson, other.charset, other.timeout);
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
            final HttpURLConnection connection = sendWebRequest(
                                                     RestRequestType.GET, url, null, null, MediaType.TEXT_PLAIN, retriesParam.getValue());

            return Jsoup.parse(this.getInputStream(connection), charset.displayName(), url);

        } catch (Exception e) {
            LOGGER.warn(String.format(DataOperationConstants.WEB_ERROR_JSON, url), e);
            return null;
        }
    }

    /**
     * Returns the correct InputStream based on the Content-Encoding header of
     * a connection. Necessary to support compression.
     *
     * @param connection the connection to be checked
     *
     * @throws IOException thrown if InputStream is corrupted
     *
     * @return an InputStream subclass
     */
    private InputStream getInputStream(HttpURLConnection connection) throws IOException
    {
        if(DataOperationConstants.GZIP_ENCODING.equals(connection.getContentEncoding())) {
            return new GZIPInputStream(connection.getInputStream());
        }
        return connection.getInputStream();
    }

    @Override
    public void setCharset(Charset charset)
    {
        this.charset = charset;
    }


    /**
     * Changes the request timeout in milliseconds.
     *
     * @param timeout the request timeout in milliseconds
     */
    public void setTimeout(int timeout)
    {
        this.timeout = timeout;
    }


    /**
     * Sends an authorized REST request with a specified body and returns the
     * response as a string.
     *
     * @param method the request method that is being sent
     * @param url the URL to which the request is being sent
     * @param body the body of the request, or null if no body is to be sent
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
        HttpURLConnection connection = sendWebRequest(method, url, body, authorization, contentType, retriesParam.getValue());

        // create a reader for the HTTP response
        InputStream response = this.getInputStream(connection);
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
     * @param body the body of the request, or null if no body is to be sent
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

        HttpURLConnection connection = sendWebRequest(method, url, body, authorization, contentType, retriesParam.getValue());
        headerFields = connection.getHeaderFields();

        return headerFields;
    }


    /**
     * Sends a REST request with a specified body and returns the connection.
     *
     * @param method the request method that is being sent
     * @param urlString the URL to which the request is being sent
     * @param body the body of the request, or null if no body is to be sent
     * @param authorization the base-64-encoded username and password, or null if no
     *                           authorization is required
     * @param contentType the contentType of the body
     * @param retries the number of retries if the request fails with a response code 5xx
     *
     * @throws HTTPException thrown if the response code is not 2xx
     * @throws IOException thrown if the response output stream could not be created
     *
     * @return the connection to the host
     */
    public HttpURLConnection sendWebRequest(RestRequestType method, String urlString, String body, String authorization, String contentType, int retries)
    throws IOException, HTTPException
    {
        // generate a URL and open a connection
        final URL url = new URL(urlString);
        final HttpURLConnection connection = createConnection(method, url, body, authorization, contentType);

        boolean mustRetry = false;

        // open the connection
        try {
            int responseCode = connection.getResponseCode();

            if (responseCode >= 300) {
                connection.disconnect();

                // handle server errors
                if (responseCode >= 500)
                    mustRetry = retries != 0;

                // handle redirection from HTTP > HTTPS
                else if (responseCode < 400) {
                    connection.disconnect();
                    final String redirectedUrl =
                        connection.getHeaderField(DataOperationConstants.REDIRECT_LOCATION_HEADER);

                    // disallow redirect from HTTPS to HTTP
                    if (redirectedUrl != null
                        && !(url.getProtocol().equalsIgnoreCase(DataOperationConstants.HTTPS)
                             && redirectedUrl.startsWith(DataOperationConstants.HTTP)))
                        return sendWebRequest(method, redirectedUrl, body, authorization, contentType, retries);
                }

                // throw an error if the request is not to be reattempted
                if (!mustRetry) {
                    final String errorMessage = String.format(
                                                    DataOperationConstants.WEB_ERROR_REST_HTTP,
                                                    method.toString(),
                                                    urlString,
                                                    body,
                                                    responseCode);
                    throw new HttpStatusException(errorMessage, responseCode, urlString);
                }
            }
        } catch (SocketTimeoutException e) {
            // if we time out, try again
            if (retries != 0)
                mustRetry = true;
            else
                throw e;
        }

        // if the request failed due to server issues, attempt to retry
        if (mustRetry) {
            // if the response header contains a retry-after field, wait for that period before retrying
            final int delayInSeconds = connection.getHeaderFieldInt(RestConstants.RETRY_AFTER_HEADER, 1);
            LOGGER.debug(String.format(DataOperationConstants.RETRY, urlString, delayInSeconds));

            try {
                Thread.sleep(delayInSeconds * 1000);
            } catch (InterruptedException e) {
                throw new IOException(e);
            }

            return sendWebRequest(method, urlString, body, authorization, contentType, Math.max(retries - 1, -1));
        } else
            return connection;
    }


    /**
     * Sets up a {@linkplain HttpURLConnection} connection with specified properties.
     *
     * @param method the request method that is being sent
     * @param url the URL to which the connection is to be established
     * @param body the body of the request, or null if no body is to be sent
     * @param authorization the base-64-encoded username and password, or null if no
     *                           authorization is required
     * @param contentType the contentType of the body
     *
     * @throws IOException thrown if the response output stream could not be created
     *
     * @return the connection to the host
     */
    private HttpURLConnection createConnection(RestRequestType method, URL url, String body, String authorization, String contentType) throws IOException
    {
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // set request properties
        connection.setDoOutput(true);
        connection.setInstanceFollowRedirects(true);
        connection.setUseCaches(false);
        connection.setRequestMethod(method.toString());
        connection.setRequestProperty(HttpHeaders.CONTENT_TYPE, contentType);
        connection.setRequestProperty(DataOperationConstants.REQUEST_PROPERTY_CHARSET, charset.displayName());
        connection.setRequestProperty("Accept-Encoding", DataOperationConstants.GZIP_ENCODING);

        // set timeout
        if (timeout != DataOperationConstants.NO_TIMEOUT) {
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
        }

        // set authentication
        if (authorization != null)
            connection.setRequestProperty(HttpHeaders.AUTHORIZATION, authorization);

        // only send data if it is specified
        if (body != null) {
            // convert body string to bytes
            final byte[] bodyBytes = body.getBytes(charset);
            connection.setRequestProperty(HttpHeaders.CONTENT_LENGTH, Integer.toString(bodyBytes.length));

            // try to send body
            final DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.write(bodyBytes);
            wr.close();
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
        final HttpURLConnection connection = sendWebRequest(
                                                 RestRequestType.GET, url, null, null, MediaType.TEXT_PLAIN, retriesParam.getValue());

        return new InputStreamReader(this.getInputStream(connection), charset);
    }
}
