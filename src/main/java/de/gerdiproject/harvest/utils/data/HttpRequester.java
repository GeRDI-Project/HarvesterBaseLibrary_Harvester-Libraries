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


import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.xml.ws.http.HTTPException;

import org.jsoup.nodes.Document;

import com.google.gson.Gson;

import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.parameters.BooleanParameter;
import de.gerdiproject.harvest.utils.data.constants.DataOperationConstants;
import de.gerdiproject.harvest.utils.data.enums.RestRequestType;
import de.gerdiproject.json.GsonUtils;
import de.gerdiproject.json.geo.GeoJson;


/**
 * This class serves as a facade for HTTP requests.
 *
 * @author Robin Weiss
 */
public class HttpRequester
{
    private final DiskIO diskIO;
    private final WebDataRetriever webDataRetriever;
    private String cacheFolder;
    private final BooleanParameter readFromDisk;
    private final BooleanParameter writeToDisk;


    /**
     * Constructor that uses an UTF-8 charset, and a {@linkplain Gson}
     * implementation that can parse {@linkplain GeoJson} objects.
     */
    public HttpRequester()
    {
        this(GsonUtils.createGeoJsonGsonBuilder().create(), StandardCharsets.UTF_8);
    }


    /**
     * Constructor that allows to customize the behavior.
     *
     * @param gson the GSON (de-)serializer for reading and writing JSON objects
     * @param httpCharset the encoding charset
     */
    public HttpRequester(final Gson gson, final Charset httpCharset)
    {
        BooleanParameter readFromDiskTemp;
        BooleanParameter writeToDiskTemp;

        try {
            readFromDiskTemp = Configuration.registerParameter(DataOperationConstants.READ_FROM_DISK_PARAM);
            writeToDiskTemp = Configuration.registerParameter(DataOperationConstants.WRITE_TO_DISK_PARAM);
        } catch (final IllegalStateException e) {
            readFromDiskTemp = DataOperationConstants.READ_FROM_DISK_PARAM;
            writeToDiskTemp = DataOperationConstants.WRITE_TO_DISK_PARAM;
        }

        this.readFromDisk = readFromDiskTemp;
        this.writeToDisk = writeToDiskTemp;
        this.diskIO = new DiskIO(gson, httpCharset);
        this.webDataRetriever = new WebDataRetriever(gson, httpCharset);

        setCacheFolder(DataOperationConstants.CACHE_FOLDER_PATH);
    }


    /**
     * Constructor that copies the settings from another {@linkplain HttpRequester}.
     *
     * @param other the {@linkplain HttpRequester} from which the settings are copied
     */
    public HttpRequester(final HttpRequester other)
    {
        this.readFromDisk = other.readFromDisk;
        this.writeToDisk = other.writeToDisk;

        this.diskIO = new DiskIO(other.diskIO);
        this.webDataRetriever = new WebDataRetriever(other.webDataRetriever);

        setCacheFolder(other.cacheFolder);
    }


    /**
     * Sends a GET request to a specified URL and tries to retrieve the HTML
     * response. If the development option is enabled, the response will be read
     * from disk instead.
     *
     * @param url
     *            a URL that returns a JSON object
     * @return a JSON object, or null if the HTML document could not be retrieved
     */
    public Document getHtmlFromUrl(final String url)
    {
        Document htmlResponse = null;
        boolean isResponseReadFromWeb = false;

        // read json file from disk, if the option is enabled
        if (isReadingFromDisk())
            htmlResponse = diskIO.getHtml(urlToFilePath(url, DataOperationConstants.FILE_ENDING_HTML));

        // request json from web, if it has not been read from disk already
        if (htmlResponse == null) {
            htmlResponse = webDataRetriever.getHtml(url);
            isResponseReadFromWeb = true;
        }

        // write whole response to disk, if the option is enabled
        if (isResponseReadFromWeb && isWritingToDisk()) {
            // deliberately write an empty object to disk, if the response could
            // not be retrieved
            final String responseText = (htmlResponse == null) ? "" : htmlResponse.toString();
            diskIO.writeStringToFile(urlToFilePath(url, DataOperationConstants.FILE_ENDING_HTML), responseText);
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
     * @return a Java object, or null if the object could not be loaded or parsed
     */
    public <T> T getObjectFromUrl(final String url, final Class<T> targetClass)
    {
        T targetObject = null;
        boolean isResponseReadFromWeb = false;

        // read json file from disk, if the option is enabled
        if (isReadingFromDisk())
            targetObject = diskIO.getObject(urlToFilePath(url, DataOperationConstants.FILE_ENDING_JSON), targetClass);

        // request json from web, if it has not been read from disk already
        if (targetObject == null) {
            targetObject = webDataRetriever.getObject(url, targetClass);
            isResponseReadFromWeb = true;
        }

        // write whole response to disk, if the option is enabled
        if (isResponseReadFromWeb && isWritingToDisk()) {
            // deliberately write an empty object to disk, if the response could
            // not be retrieved
            diskIO.writeObjectToFile(urlToFilePath(url, DataOperationConstants.FILE_ENDING_JSON), targetObject);
        }

        return targetObject;
    }


    /**
     * Sends a GET request to a specified URL and tries to retrieve the JSON
     * response, mapping it to a Java object. If the development option is enabled,
     * the response will be read from disk instead.
     *
     * @param url a URL that returns a JSON object
     * @param targetType the type of the returned object
     * @param <T> the type of the returned object
     *
     * @return a Java object, or null if the object could not be loaded or parsed
     */
    public <T> T getObjectFromUrl(final String url, final Type targetType)
    {
        T targetObject = null;
        boolean isResponseReadFromWeb = false;

        // read json file from disk, if the option is enabled
        if (isReadingFromDisk())
            targetObject = diskIO.getObject(urlToFilePath(url, DataOperationConstants.FILE_ENDING_JSON), targetType);

        // request json from web, if it has not been read from disk already
        if (targetObject == null) {
            targetObject = webDataRetriever.getObject(url, targetType);
            isResponseReadFromWeb = true;
        }

        // write whole response to disk, if the option is enabled
        if (isResponseReadFromWeb && isWritingToDisk()) {
            // deliberately write an empty object to disk, if the response could
            // not be retrieved
            diskIO.writeObjectToFile(urlToFilePath(url, DataOperationConstants.FILE_ENDING_JSON), targetObject);
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
    private String urlToFilePath(final String url, final String fileEnding)
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
        return String.format(DataOperationConstants.FILE_PATH, cacheFolder, path, fileEnding);
    }


    /**
     * Sends an REST request with a plain-text body.
     *
     * @param method the request method that is being sent
     * @param url the URL to which the request is being sent
     * @param body the plain-text body of the request
     *
     * @throws HTTPException thrown if the response code is not 2xx
     * @throws IOException thrown if the response output stream could not be created
     *
     * @return the HTTP response as plain text
     */
    public String getRestResponse(final RestRequestType method, final String url, final String body) throws HTTPException, IOException
    {
        return webDataRetriever.getRestResponse(method, url, body, null, MediaType.TEXT_PLAIN);
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
    public String getRestResponse(final RestRequestType method, final String url, final String body, final String authorization, final String contentType) throws HTTPException, IOException
    {
        return webDataRetriever.getRestResponse(method, url, body, authorization, contentType);
    }


    /**
     * Sends a REST request with a plain-text body and returns the header
     * fields.
     *
     * @param method the request method that is being sent
     * @param url the URL to which the request is being sent
     * @param body the plain-text body of the request
     *
     * @throws HTTPException thrown if the response code is not 2xx
     * @throws IOException thrown if the response output stream could not be created
     *
     * @return the response header fields, or null if the response could not be parsed
     */
    public Map<String, List<String>> getRestHeader(final RestRequestType method, final String url, final String body) throws HTTPException, IOException
    {
        return webDataRetriever.getRestHeader(method, url, body, null, MediaType.TEXT_PLAIN);
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
    public Map<String, List<String>> getRestHeader(final RestRequestType method, final String url, final String body,
                                                   final String authorization, final String contentType) throws HTTPException, IOException
    {
        return webDataRetriever.getRestHeader(method, url, body, authorization, contentType);
    }


    /**
     * Returns the top folder where HTTP responses can be cached.
     *
     * @return the top folder where HTTP responses can be cached
     */
    public String getCacheFolder()
    {
        return cacheFolder;
    }


    /**
     * Changes the folder were responses are cached when the writeToDisk parameter is set.
     * @param cacheFolder the folder were responses can be cached
     */
    public void setCacheFolder(String cacheFolder)
    {
        if (cacheFolder != null && !cacheFolder.endsWith("/"))
            cacheFolder += '/';

        this.cacheFolder = cacheFolder;
    }


    /**
     * Changes the request timeout for web requests.
     *
     * @param timeout the request timeout in milliseconds
     */
    public void setTimeout(final int timeout)
    {
        this.webDataRetriever.setTimeout(timeout);
    }


    /**
     * Returns true if HTTP responses are read from a cache on disk.
     *
     * @return true if HTTP responses are read from a cache on disk
     */
    public boolean isReadingFromDisk()
    {
        return readFromDisk.getValue() && cacheFolder != null;
    }


    /**
     * Returns true if HTTP responses are written to a cache on disk.
     *
     * @return true if HTTP responses are written to a cache on disk
     */
    public boolean isWritingToDisk()
    {
        return writeToDisk.getValue() && cacheFolder != null;
    }


    /**
     * Changes the charset that is used for reading and writing responses.
     *
     * @param httpCharset the charset that is used for reading and writing responses
     */
    public void setCharset(final Charset httpCharset)
    {
        this.webDataRetriever.setCharset(httpCharset);
        this.diskIO.setCharset(httpCharset);
    }
}
