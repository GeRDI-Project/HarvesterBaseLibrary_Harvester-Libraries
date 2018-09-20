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
package de.gerdiproject.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import javax.xml.ws.http.HTTPException;

import org.jsoup.nodes.Document;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import de.gerdiproject.AbstractFileSystemUnitTest;
import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.utils.data.HttpRequester;
import de.gerdiproject.harvest.utils.data.constants.DataOperationConstants;
import de.gerdiproject.harvest.utils.data.enums.RestRequestType;

/**
 * This class provides test cases for the {@linkplain HttpRequester}.
 * Each test is executed twice. The first execution tests with disabled
 * HTTP response caching, while the second execution tests enabled caching
 * from a mocked folder on disk.
 *
 * @author Robin Weiss
 */
@RunWith(Parameterized.class)
public class HttpRequesterTest extends AbstractFileSystemUnitTest<HttpRequester>
{
    private static final String MODULE_NAME = "mocked";
    private static final String MOCKED_RESPONSE_CACHE_FOLDER = "src/test/java/de/gerdiproject/utils/examples/httpRequester/";
    private static final String JSON_OBJECT_URL = "http://fenixservices.fao.org/faostat/api/v1/en/documents/RFB/";
    private static final String DEFAULT_URL = "https://www.gerdi-project.eu/";
    private static final String UNEXPECTED_FILE_ERROR = "This file should not exist: ";
    private static final String CACHED_JSON = "response.json";
    private static final String CACHED_HTML = "response.html";
    private static final String MOCKED_RESPONSE_FOLDER = "/mocked";
    private static final String READ_FROM_DISK_KEY = DataOperationConstants.READ_FROM_DISK_PARAM.getCompositeKey();
    private static final String WRITE_TO_DISK_KEY = DataOperationConstants.WRITE_TO_DISK_PARAM.getCompositeKey();

    @Parameters(name = "caching enabled: {0}")
    public static Object[] getParameters()
    {
        return new Object[] {Boolean.FALSE, Boolean.TRUE};
    }

    private boolean isCachingEnabled;
    private String messageNotCached;


    /**
     * This constructor is called via the Parameters. It determines
     * whether the HttpRequester readFromDisk and writeToDisk flags are enabled
     * or not.
     *
     * @param isCachingEnabled if true, the readFromDisk and writeToDisk flags are enabled
     */
    public HttpRequesterTest(boolean isCachingEnabled)
    {
        this.isCachingEnabled = isCachingEnabled;
        this.messageNotCached = isCachingEnabled ? "" : "NOT";
    }


    @Override
    protected HttpRequester setUpTestObjects()
    {
        config = new Configuration(MODULE_NAME);
        config.addEventListeners();

        final HttpRequester requester = new HttpRequester(new Gson(), StandardCharsets.UTF_8);
        requester.setCacheFolder(testFolder.getPath());

        config.setParameter(READ_FROM_DISK_KEY, String.valueOf(isCachingEnabled));
        config.setParameter(WRITE_TO_DISK_KEY, String.valueOf(isCachingEnabled));

        return requester;
    }


    /**
     * Tests if the 'readFromDisk' flag is enabled/disabled when calling a constructor
     * with the specified settings.
     */
    @Test
    public void testReadFromDiskFlag()
    {
        assertEquals("The method isReadingFromDisk() should return " + isCachingEnabled + " when the " + READ_FROM_DISK_KEY + "-parameter is set to " + isCachingEnabled,
                     isCachingEnabled,
                     testedObject.isReadingFromDisk());
    }

    /**
     * Tests if the 'writeToDisk' flag is enabled/disabled when calling a constructor
     * with the specified settings.
     */
    @Test
    public void testWriteToDiskFlag()
    {
        assertEquals("The method isWritingToDisk() should return " + isCachingEnabled + " when the " + WRITE_TO_DISK_KEY + "-parameter is set to " + isCachingEnabled,
                     isCachingEnabled,
                     testedObject.isWritingToDisk());
    }


    /**
     * Tests if the caching path can be retrieved after calling the constructor.
     */
    @Test
    public void testCacheFolder()
    {
        assertNotNull("The method getCacheFolder() should not return null!",
                      testedObject.getCacheFolder());
    }


    /**
     * Tests if a HTML Java object is returned when a GET-request
     * is sent to a valid URL or its cached response.
     */
    @Test
    public void testGettingHtml()
    {
        testedObject = createReadOnlyExampleHttpRequester();

        final String requestUrl = isCachingEnabled
                                  ? DEFAULT_URL + MOCKED_RESPONSE_FOLDER
                                  : DEFAULT_URL;
        Document htmlObject = testedObject.getHtmlFromUrl(requestUrl);

        assertNotNull("The method getHtmlFromUrl(\"" + requestUrl + "\") should not return null!",
                      htmlObject);
    }


    /**
     * Tests if null is returned when trying to retrieve HTML
     * from an invalid URL.
     */
    @Test
    public void testGettingHtmlFromInvalidUrl()
    {
        testedObject = createReadOnlyExampleHttpRequester();

        setLoggerEnabled(false);
        final String requestUrl = DEFAULT_URL + "/thisDoesNotExist";
        Document htmlObject = testedObject.getHtmlFromUrl(requestUrl);
        setLoggerEnabled(true);

        assertNull("The method getHtmlFromUrl() called with the non-existing URL \"" + requestUrl + "\" should return null!",
                   htmlObject);
    }


    /**
     * Tests if a valid HTML response that attempted to be retrieved from cache
     * can be retrieved from web as a fallback.
     *
     * @throws IOException thrown when the expected response is cached on disk
     */
    @Test
    public void testGettingUncachedHtml() throws IOException
    {
        skipTestIfCachingDisabled();

        testedObject = createReadOnlyExampleHttpRequester();

        final File cachedResponse =
            new File(
            testedObject.getCacheFolder()
            + DEFAULT_URL.substring(7)
            + CACHED_HTML);

        if (cachedResponse.exists())
            throw new IOException(UNEXPECTED_FILE_ERROR + cachedResponse.getAbsolutePath());

        final Document htmlObject = testedObject.getHtmlFromUrl(DEFAULT_URL);
        assertNotNull("The method getHtmlFromUrl(\"" + DEFAULT_URL + "\") should retrieve a response from the web, if the response was not cached!",
                      htmlObject);
    }


    /**
     * Tests if a HTML response is cached on disk if the 'writeToDisk' flag is true.
     */
    @Test
    public void testCachingHtml()
    {
        final File cachedResponse = new File(
            testedObject.getCacheFolder()
            + DEFAULT_URL.substring(7)
            + CACHED_HTML);

        // no need to check if the file already exists, as this is done in before()

        // make a request, causing the response to be cached
        testedObject.getHtmlFromUrl(DEFAULT_URL);

        // verify the cache file was created (or not)
        assertEquals("The method getHtmlFromUrl() should " + messageNotCached + " cache the response on disk if the " + WRITE_TO_DISK_KEY + "-parameter is " + isCachingEnabled,
                     isCachingEnabled,
                     cachedResponse.exists());
    }


    /**
     * Tests if a JSON object can be retrieved by sending a request to the FAOSTAT rest API.
     * A JSON object is also cached on disk under the same URL, but containing only dummy data.
     */
    @Test
    public void testGettingObject()
    {
        testedObject = createReadOnlyExampleHttpRequester();
        final String requestUrl = isCachingEnabled
                                  ? JSON_OBJECT_URL + MOCKED_RESPONSE_FOLDER
                                  : JSON_OBJECT_URL;
        final JsonObject obj = testedObject.getObjectFromUrl(requestUrl, JsonObject.class);

        assertNotNull("The method getObjectFromUrl(\"" + requestUrl + "\") should not return null!",
                      obj);
    }


    /**
     * Tests if a valid JSON response that is not cached
     * can always be retrieved from web.
     *
     * @throws IOException thrown when the expected response is cached on disk
     */
    @Test
    public void testGettingUncachedObject() throws IOException
    {
        skipTestIfCachingDisabled();

        testedObject = createReadOnlyExampleHttpRequester();

        final File cachedResponse =
            new File(testedObject.getCacheFolder()
                     + JSON_OBJECT_URL.substring(7)
                     + CACHED_JSON);

        if (cachedResponse.exists())
            throw new IOException(UNEXPECTED_FILE_ERROR + cachedResponse.getAbsolutePath());

        JsonObject obj = testedObject.getObjectFromUrl(
                             JSON_OBJECT_URL,
                             JsonObject.class);

        assertNotNull("The method getObjectFromUrl(\"" + DEFAULT_URL + "\") should retrieve a response from the web, if the response was not cached!",
                      obj);
    }



    /**
     * Tests if a JSON object response is cached on disk if the 'writeToDisk' flag is true.
     */
    @Test
    public void testCachingObject() throws IOException
    {
        final File cachedResponse = new File(
            testFolder,
            JSON_OBJECT_URL.substring(7)
            + CACHED_JSON);

        // no need to check if the file already exists, as this is done in before()

        // request JSON response
        testedObject.getObjectFromUrl(JSON_OBJECT_URL, JsonObject.class);

        // verify the cache file was created (or not)
        assertEquals("The method getObjectFromUrl() should " + messageNotCached + " cache the response on disk if the " + WRITE_TO_DISK_KEY + "-parameter is " + isCachingEnabled,
                     isCachingEnabled,
                     cachedResponse.exists());
    }


    /**
     * Sends a GET request to the gerdi homepage and verifies that the response is not null.
     *
     * @throws HTTPException thrown if the website could not be reached
     * @throws IOException thrown if the response output stream could not be created
     */
    @Test
    public void testGetRequestResponse() throws IOException, HTTPException
    {
        testedObject = createReadOnlyExampleHttpRequester();
        String response = testedObject.getRestResponse(RestRequestType.GET, DEFAULT_URL, null, null, "text/html");
        assertNotNull("Sending a GET request to \"" + DEFAULT_URL + "\" should return a response!",
                      response);
    }


    /**
     * Tests if the header can be retrieved by sending a GET request to the gerdi homepage.
     *
     * @throws HTTPException thrown if the website could not be reached
     * @throws IOException thrown if the response output stream could not be created
     */
    @Test
    public void testGettingHeader() throws IOException, HTTPException
    {
        testedObject = createReadOnlyExampleHttpRequester();

        Map<String, List<String>> header = testedObject.getRestHeader(RestRequestType.GET, DEFAULT_URL, null);
        assertFalse("The map returned by getRestHeader(\"" + DEFAULT_URL + "\", null) should not be empty!",
                    header.isEmpty());
    }


    //////////////////////
    // Non-test Methods //
    //////////////////////

    /**
     * Creates a {@linkplain HttpRequester} that reads mocked up responses from the examples directory.
     *
     * @return a {@linkplain HttpRequester} that reads mocked up responses from the examples directory
     */
    private HttpRequester createReadOnlyExampleHttpRequester()
    {
        config.setParameter(DataOperationConstants.READ_FROM_DISK_PARAM.getCompositeKey(), String.valueOf(isCachingEnabled));
        config.setParameter(DataOperationConstants.WRITE_TO_DISK_PARAM.getCompositeKey(), String.valueOf(false));

        final HttpRequester requester = new HttpRequester(new Gson(), StandardCharsets.UTF_8);
        requester.setCacheFolder(MOCKED_RESPONSE_CACHE_FOLDER);

        return requester;
    }


    /**
     * Skips the test that calls this method, if isCachingEnabled is false.
     */
    private void skipTestIfCachingDisabled()
    {
        Assume.assumeTrue("Skipping test, because it only makes sense when caching is enabled.", isCachingEnabled);
    }
}
