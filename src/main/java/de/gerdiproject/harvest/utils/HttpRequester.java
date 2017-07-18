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
package de.gerdiproject.harvest.utils;


import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.development.DevelopmentTools;
import de.gerdiproject.json.IJsonArray;
import de.gerdiproject.json.IJsonBuilder;
import de.gerdiproject.json.IJsonObject;
import de.gerdiproject.json.IJsonReader;
import de.gerdiproject.json.impl.JsonBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.HttpHeaders;


/**
 * This class serves as a facade for RESTful HTTP requests.
 *
 * @author Robin Weiss
 */
public class HttpRequester
{
    private static final String DATA_JSON = "data";
    private static final String ERROR_JSON = "Could not load and parse '%s': %s";
    private static final String SAVE_FAILED = "Could not save response of '' to disk: %s";
    private static final String FILE_PATH = "savedHttpResponses/%s/%sresponse.%s";
    private static final String FILE_ENDING_JSON = "json";
    private static final String FILE_ENDING_HTML = "html";
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpRequester.class);

    private final Charset httpCharset;
    private final DevelopmentTools devTools;
    private final IJsonBuilder jsonBuilder;

    /**
     * if true, no http error responses are being logged
     */
    public boolean suppressWarnings;


    /**
     * Standard constructor. Sets charset to UTF-8 and does not suppress
     * warnings.
     */
    public HttpRequester()
    {
        httpCharset = StandardCharsets.UTF_8;
        suppressWarnings = false;
        devTools = DevelopmentTools.instance();
        jsonBuilder = new JsonBuilder();
    }


    /**
     * Constructor that allows to change the charset.
     *
     * @param httpCharset
     *            the encoding charset
     * @param suppressWarnings
     *            if true, failed http requests will not be logged
     */
    public HttpRequester(Charset httpCharset, boolean suppressWarnings)
    {
        this.httpCharset = httpCharset;
        this.suppressWarnings = suppressWarnings;
        devTools = DevelopmentTools.instance();
        jsonBuilder = new JsonBuilder();
    }


    /**
     * Sends a GET request to a specified URL and tries to retrieve the 'data'
     * field of the JSON response as a List of JSON objects.
     *
     * @param url
     *            a URL that returns a JSON object
     * @return a List of JSON objects
     */
    public IJsonArray getJsonArrayFromUrl(String url)
    {
        IJsonObject entireObject = getRawJsonFromUrl(url);
        return (entireObject != null) ? entireObject.getJsonArray(DATA_JSON) : null;
    }


    /**
     * Sends a GET request to a specified URL and tries to retrieve the the
     * 'data' field of the JSON response as a JSON object.
     *
     * @param url
     *            a URL that returns a JSON object
     * @return a JSON object
     */
    public IJsonObject getJsonObjectFromUrl(String url)
    {
        IJsonObject entireObject = getRawJsonFromUrl(url);
        return (entireObject != null) ? entireObject.getJsonObject(DATA_JSON) : null;
    }


    /**
     * Sends a GET request to a specified URL and tries to retrieve the JSON
     * response. If the development option is enabled, the JSON will be read
     * from disk instead.
     *
     * @param url
     *            a URL that returns a JSON object
     * @return a JSON object
     */
    public IJsonObject getRawJsonFromUrl(String url)
    {
        IJsonObject jsonResponse = null;
        boolean isResponseReadFromWeb = false;

        // read json file from disk, if the option is enabled
        if (devTools.isReadingHttpFromDisk())
            jsonResponse = readJsonFromDisk(urlToFilePath(url, FILE_ENDING_JSON));

        // request json from web, if it has not been read from disk already
        if (jsonResponse == null) {
            jsonResponse = readJsonFromWeb(url);
            isResponseReadFromWeb = true;
        }

        // nullify object if it is empty anyway
        if (jsonResponse != null && jsonResponse.isEmpty())
            jsonResponse = null;

        // write whole response to disk, if the option is enabled
        if (isResponseReadFromWeb && devTools.isWritingHttpToDisk()) {
            // deliberately write an empty object to disk, if the response could
            // not
            // be retrieved
            String responseText = (jsonResponse == null) ? "{}" : jsonResponse.toString();
            saveResponseToDisk(url, responseText, FILE_ENDING_JSON);
        }

        return jsonResponse;
    }


    /**
     * Opens a file at the specified path and tries to parse the content as a
     * JsonObject.
     *
     * @param filePath
     *            the filepath to a json file
     * @return a JSON object, or null if the file could not be parsed
     */
    public IJsonObject readJsonFromDisk(String filePath)
    {
        IJsonObject jsonResponse = null;

        try {
            // try to read from disk
            File file = new File(filePath);

            // check if file exists
            if (file.exists()) {
                // parse the json object
                IJsonReader jsonReader = jsonBuilder
                                         .createReader(new InputStreamReader(new FileInputStream(file), httpCharset));
                jsonResponse = jsonReader.readObject();
                jsonReader.close();
            }
        } catch (Exception e) {
            if (!suppressWarnings)
                LOGGER.warn(String.format(ERROR_JSON, filePath, e.toString()));
        }

        return jsonResponse;
    }


    /**
     * Sends a GET request to a specified URL and tries to parse the response as
     * a JSON object.
     *
     * @param url
     *            a URL pointing to a JSON object
     * @return a JSON object, or null if the response could not be parsed
     */
    private IJsonObject readJsonFromWeb(String url)
    {
        IJsonObject jsonResponse = null;

        try {
            // send web request
            InputStream response = new URL(url).openStream();

            // parse the json object
            IJsonReader jsonReader = jsonBuilder.createReader(new InputStreamReader(response, httpCharset));
            jsonResponse = jsonReader.readObject();
            jsonReader.close();

        } catch (Exception e) {
            if (!suppressWarnings)
                LOGGER.warn(String.format(ERROR_JSON, url, e.toString()));
        }
        return jsonResponse;
    }


    /**
     * Sends a GET request to a specified URL and tries to retrieve the HTML
     * response. If the development option is enabled, the response will be read
     * from disk instead.
     *
     * @param url
     *            a URL that returns a JSON object
     * @return a JSON object
     */
    public Document getHtmlFromUrl(String url)
    {
        Document htmlResponse = null;
        boolean isResponseReadFromWeb = false;

        // read json file from disk, if the option is enabled
        if (devTools.isReadingHttpFromDisk())
            htmlResponse = readHtmlFromDisk(urlToFilePath(url, FILE_ENDING_HTML));

        // request json from web, if it has not been read from disk already
        if (htmlResponse == null) {
            htmlResponse = readHtmlFromWeb(url);
            isResponseReadFromWeb = true;
        }

        // write whole response to disk, if the option is enabled
        if (isResponseReadFromWeb && devTools.isWritingHttpToDisk()) {
            // deliberately write an empty object to disk, if the response could
            // not be retrieved
            String responseText = (htmlResponse == null) ? "" : htmlResponse.toString();
            saveResponseToDisk(url, responseText, FILE_ENDING_HTML);
        }

        return htmlResponse;
    }


    /**
     * Opens a file at the specified path and tries to parse the content as a
     * HTML document.
     *
     * @param filePath
     *            the filepath to a HTML file
     * @return a HTML document, or null if the file could not be parsed
     */
    private Document readHtmlFromDisk(String filePath)
    {
        Document htmlResponse = null;

        try {
            // try to read from disk
            File file = new File(filePath);

            // check if file exists
            if (file.exists()) {
                // parse the html object
                htmlResponse = Jsoup.parse(file, httpCharset.displayName(), filePath);
            }
        } catch (Exception e) {
            if (!suppressWarnings)
                LOGGER.warn(String.format(ERROR_JSON, filePath, e.toString()));
        }

        return htmlResponse;
    }


    /**
     * Sends a GET request to a specified URL and tries to parse the response as
     * a HTML document.
     *
     * @param url
     *            a URL pointing to HTML content
     * @return a HTML document, or null if the response could not be parsed
     */
    private Document readHtmlFromWeb(String url)
    {
        Document htmlResponse = null;

        try {
            // send web request
            InputStream response = new URL(url).openStream();

            // parse the html object
            htmlResponse = Jsoup.parse(response, httpCharset.displayName(), url);

            response.close();

        } catch (Exception e) {
            if (!suppressWarnings)
                LOGGER.warn(String.format(ERROR_JSON, url, e.toString()));
        }
        return htmlResponse;
    }


    /**
     * Converts a web URL to a path on disk from which a file can be read
     *
     * @param url
     *            the original webr equest url
     * @param fileEnding
     *            the file type
     * @return a file-path on disk
     */
    private String urlToFilePath(String url, String fileEnding)
    {
        String path = url;

        // remove the scheme
        int schemeEnd = path.indexOf("://");
        schemeEnd = (schemeEnd != -1) ? schemeEnd + 3 : 0;
        path = path.substring(schemeEnd);

        // remove double slashes
        path = path.replace("//", "/");

        // filter out :?*
        path = path.replace(":", "%colon%");
        path = path.replace("?", "%query%/");
        path = path.replace("*", "%star%");

        // add slash at the end
        if (path.charAt(path.length() - 1) != '/')
            path += '/';

        // assemble the complete file name
        return String.format(FILE_PATH, MainContext.getModuleName(), path, fileEnding);
    }


    /**
     * Saves a HTTP response to disk. This is used for debugging and development
     * purposes. Instead of harvesting the same database again and again, the
     * date can be saved and read from disk.
     *
     * @param url
     *            the original web request URL
     * @param fileEnding
     *            the extension of the file that is to be saved
     * @param response
     *            the server response to the webrequest
     */
    private void saveResponseToDisk(String url, String response, String fileEnding)
    {
        // create directories
        final File file = new File(urlToFilePath(url, fileEnding));
        file.getParentFile().mkdirs();

        // write to disk
        try {
            BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), httpCharset));

            writer.write(response);
            writer.close();

        } catch (IOException e) {
            LOGGER.error(SAVE_FAILED + e);
        }

    }


    /**
     * Sends an REST request with a plain-text body.
     *
     * @param method
     *            the request method that is being sent
     * @param url
     *            the URL to which the request is being sent
     * @param body
     *            the plain-text body of the request
     * @return the HTTP response
     */
    public String getRestResponse(RestRequestType method, String url, String body)
    {
        return getRestResponse(method, url, body, null);
    }


    /**
     * Sends an authorized REST request with a plain-text body and returns the
     * response as a string.
     *
     * @param method
     *            the request method that is being sent
     * @param url
     *            the URL to which the request is being sent
     * @param body
     *            the plain-text body of the request
     * @param authorization
     *            the base-64-encoded username and password, or null if no
     *            authorization is required
     * @return the HTTP response
     */
    public String getRestResponse(RestRequestType method, String url, String body, String authorization)
    {
        try {
            HttpURLConnection connection = sendRestRequest(method, url, body, authorization);

            // create a reader for the HTTP response
            InputStream response = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(response, httpCharset));

            // read the first line of the response
            String s = reader.readLine();
            StringBuilder responseBuilder = new StringBuilder(s);

            // read subsequent lines of the response
            s = reader.readLine();

            while (s != null) {
                // add linebreak before appending the next line
                responseBuilder.append('\n').append(s);
                s = reader.readLine();
            }

            // close the response reader
            reader.close();

            // combine the read lines to a single string
            return responseBuilder.toString();
        } catch (IOException e) {
            LOGGER.warn(e.getMessage());
            return e.getMessage();
        }
    }


    /**
     * Sends a REST request with a plain-text body and returns the header
     * fields.
     *
     * @param method
     *            the request method that is being sent
     * @param url
     *            the URL to which the request is being sent
     * @param body
     *            the plain-text body of the request
     * @return the response header fields
     */
    public Map<String, List<String>> getRestHeader(RestRequestType method, String url, String body)
    {
        return getRestHeader(method, url, body, null);
    }


    /**
     * Sends an authorized REST request with a plain-text body and returns the
     * header fields.
     *
     * @param method
     *            the request method that is being sent
     * @param url
     *            the URL to which the request is being sent
     * @param body
     *            the plain-text body of the request
     * @param authorization
     *            the base-64-encoded username and password, or null if no
     *            authorization is required
     * @return the response header fields
     */
    public Map<String, List<String>> getRestHeader(RestRequestType method, String url, String body,
                                                   String authorization)
    {
        Map<String, List<String>> headerFields = null;
        try {
            HttpURLConnection connection = sendRestRequest(method, url, body, authorization);
            headerFields = connection.getHeaderFields();

        } catch (IOException e) {
            LOGGER.warn(e.getMessage());
        }

        return headerFields;
    }


    /**
     * Sends a REST request with a plain-text body and returns the connection.
     *
     * @param method
     *            the request method that is being sent
     * @param url
     *            the URL to which the request is being sent
     * @param body
     *            the plain-text body of the request
     * @param authorization
     *            the base-64-encoded username and password, or null if no
     *            authorization is required
     * @return the connection to the host
     */
    private HttpURLConnection sendRestRequest(RestRequestType method, String url, String body, String authorization)
    throws IOException
    {
        // generate a URL and open a connection
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

        // set request properties
        connection.setDoOutput(true);
        connection.setInstanceFollowRedirects(false);
        connection.setUseCaches(false);
        connection.setRequestMethod(method.toString());
        connection.setRequestProperty(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN);
        connection.setRequestProperty("charset", httpCharset.displayName());

        // set authentication
        if (authorization != null)
            connection.setRequestProperty(HttpHeaders.AUTHORIZATION, authorization);

        // only send date if it is specified
        if (body != null) {
            // convert body string to bytes
            byte[] bodyBytes = body.getBytes(httpCharset);
            connection.setRequestProperty(HttpHeaders.CONTENT_LENGTH, Integer.toString(bodyBytes.length));

            // try to send body
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.write(bodyBytes);
            wr.close();
        }

        return connection;
    }

    /**
     * The type of REST requests that can be sent via the {@link HttpRequester}.
     *
     * @author Robin Weiss
     *
     */
    public enum RestRequestType {
        GET, POST, PUT, DELETE, HEAD, OPTIONS
    }
}
