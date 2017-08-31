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
import de.gerdiproject.harvest.utils.data.DiskIO;
import de.gerdiproject.harvest.utils.data.WebDataRetriever;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import javax.ws.rs.core.HttpHeaders;


/**
 * This class serves as a facade for RESTful HTTP requests.
 *
 * @author Robin Weiss
 */
public class HttpRequester
{
    private static final String FILE_PATH = "savedHttpResponses/%s/%sresponse.%s";
    private static final String FILE_ENDING_JSON = "json";
    private static final String FILE_ENDING_HTML = "html";
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpRequester.class);

    private final Charset httpCharset;
    private final DevelopmentTools devTools;
    private final DiskIO diskIO;
    private final WebDataRetriever webDataRetriever;

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
        httpCharset = MainContext.getCharset();
        suppressWarnings = false;
        devTools = DevelopmentTools.instance();
        diskIO = new DiskIO();
        webDataRetriever = new WebDataRetriever();
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
        diskIO = new DiskIO();
        webDataRetriever = new WebDataRetriever();
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
    public JsonObject getJsonFromUrl(String url)
    {
        JsonObject jsonObj  = null;
        boolean isResponseReadFromWeb = false;

        // read json file from disk, if the option is enabled
        if (devTools.isReadingHttpFromDisk()) {
            String filePath = urlToFilePath(url, FILE_ENDING_JSON);
            jsonObj = (JsonObject) diskIO.getJson(filePath);
        }

        // request json from web, if it has not been read from disk already
        if (jsonObj == null) {
            jsonObj = (JsonObject) webDataRetriever.getJson(url);
            isResponseReadFromWeb = true;
        }

        // write whole response to disk, if the option is enabled
        if (isResponseReadFromWeb && devTools.isWritingHttpToDisk())
            diskIO.writeJsonToFile(urlToFilePath(url, FILE_ENDING_JSON), jsonObj);


        // only return the object if it is not empty
        if (jsonObj != null && jsonObj.size() > 0)
            return jsonObj;
        else
            return null;
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
            htmlResponse = diskIO.getHtml(urlToFilePath(url, FILE_ENDING_HTML));

        // request json from web, if it has not been read from disk already
        if (htmlResponse == null) {
            htmlResponse = webDataRetriever.getHtml(url);
            isResponseReadFromWeb = true;
        }

        // write whole response to disk, if the option is enabled
        if (isResponseReadFromWeb && devTools.isWritingHttpToDisk()) {
            // deliberately write an empty object to disk, if the response could
            // not be retrieved
            String responseText = (htmlResponse == null) ? "" : htmlResponse.toString();
            diskIO.writeStringToFile(urlToFilePath(url, FILE_ENDING_HTML), responseText);
        }

        return htmlResponse;
    }

    /**
     * Sends a GET request to a specified URL and tries to retrieve the JSON
     * response, mapping it to a Java object. If the development option is enabled,
     * the response will be read from disk instead.
     *
     * @param url a URL that returns a JSON object
     * @param targetClass the class of the returned object
     * @param <T> the type of the returned object
     *
     * @return a Java object
     */
    public <T> T getObjectFromUrl(String url, Class<T> targetClass)
    {
        T targetObject = null;
        boolean isResponseReadFromWeb = false;

        // read json file from disk, if the option is enabled
        if (devTools.isReadingHttpFromDisk())
            targetObject = diskIO.getObject(urlToFilePath(url, FILE_ENDING_JSON), targetClass);

        // request json from web, if it has not been read from disk already
        if (targetObject == null) {
            targetObject = webDataRetriever.getObject(url, targetClass);
            isResponseReadFromWeb = true;
        }

        // write whole response to disk, if the option is enabled
        if (isResponseReadFromWeb && devTools.isWritingHttpToDisk()) {
            // deliberately write an empty object to disk, if the response could
            // not be retrieved
            diskIO.writeObjectToFile(urlToFilePath(url, FILE_ENDING_JSON), targetObject);
        }

        return targetObject;
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
