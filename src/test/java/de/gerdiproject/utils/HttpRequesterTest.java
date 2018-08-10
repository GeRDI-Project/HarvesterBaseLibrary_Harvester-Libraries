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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import javax.xml.ws.http.HTTPException;

import org.jsoup.nodes.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import ch.qos.logback.classic.Level;
import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.config.events.GlobalParameterChangedEvent;
import de.gerdiproject.harvest.config.parameters.BooleanParameter;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.IEvent;
import de.gerdiproject.harvest.utils.FileUtils;
import de.gerdiproject.harvest.utils.data.HttpRequester;
import de.gerdiproject.harvest.utils.data.enums.RestRequestType;
import de.gerdiproject.harvest.utils.logger.constants.LoggerConstants;

/**
 * This class provides test cases for the {@linkplain HttpRequester}.
 *
 * @author Robin Weiss
 */
@RunWith(Parameterized.class)
public class HttpRequesterTest
{
    private static final File TEST_RESPONSE_CACHE_FOLDER = new File("mocked/httpRequesterTestDir");
    private static final String MOCKED_RESPONSE_CACHE_FOLDER = "src/test/java/de/gerdiproject/utils/examples/httpRequester/";
    private static final String JSON_OBJECT_URL = "http://fenixservices.fao.org/faostat/api/v1/en/documents/RFB/";
    private static final String DEFAULT_URL = "http://www.gerdi-project.de/";
    private static final String CLEAN_UP_ERROR = "Could not remove temporary test folder!";
    private static final String UNEXPECTED_FILE_ERROR = "This file should not exist: ";
    private static final String CACHED_JSON = "response.json";
    private static final String CACHED_HTML = "response.html";
    private static final String MOCKED_RESPONSE_FOLDER = "/mocked";

    @Parameters(name = "caching enabled: {0}")
    public static Object[] getParameters()
    {
        return new Object[] {Boolean.FALSE, Boolean.TRUE};
    }

    public boolean isCachingEnabled;

    private final IEvent changeReadFromDiskEvent;
    private final IEvent changeWriteToDiskEvent;

    private HttpRequester requester = null;


    /**
     * This constructor is called via the Parameters. It determines
     * whether the HttpRequester readFromDisk and writeToDisk flags are enabled
     * or not.
     *
     * @param cachingEnabled if true, the readFromDisk and writeToDisk flags are enabled
     */
    public HttpRequesterTest(boolean cachingEnabled)
    {
        this.isCachingEnabled = cachingEnabled;
        this.changeReadFromDiskEvent = new GlobalParameterChangedEvent(
            new BooleanParameter(ConfigurationConstants.READ_HTTP_FROM_DISK, !cachingEnabled),
            null);

        this.changeWriteToDiskEvent = new GlobalParameterChangedEvent(
            new BooleanParameter(ConfigurationConstants.WRITE_HTTP_TO_DISK, !cachingEnabled),
            null);
    }


    /**
     * After each test, removes remaining event listeners from a {@linkplain HttpRequester}.
     * @throws IOException thrown when the temporary cache folder could not be removed
     */
    @Before
    public void before() throws IOException
    {
        FileUtils.deleteFile(TEST_RESPONSE_CACHE_FOLDER);

        if (TEST_RESPONSE_CACHE_FOLDER.exists())
            throw new IOException(CLEAN_UP_ERROR);
    }


    /**
     * After each test, removes remaining event listeners from a {@linkplain HttpRequester}.
     */
    @After
    public void after()
    {
        if (requester != null)
            requester.removeEventListeners();

        requester = null;

        FileUtils.deleteFile(TEST_RESPONSE_CACHE_FOLDER);
    }


    /**
     * Tests if the 'readFromDisk' flag is enabled/disabled when calling a constructor
     * with the specified settings.
     */
    @Test
    public void testReadFromDiskFlag()
    {
        requester = createHttpRequester();
        assertEquals(isCachingEnabled, requester.isReadingFromDisk());
    }

    /**
     * Tests if the 'writeToDisk' flag is enabled/disabled when calling a constructor
     * with the specified settings.
     */
    @Test
    public void testWriteToDiskFlag()
    {
        requester = createHttpRequester();
        assertEquals(isCachingEnabled, requester.isWritingToDisk());
    }


    /**
     * Tests if the caching path can be retrieved after calling the constructor.
     */
    @Test
    public void testCacheFolder()
    {
        requester = createHttpRequester();
        assertNotNull(requester.getCacheFolder());
    }


    /**
     * Tests if a {@linkplain HttpRequester} if the readFromDisk flag changes
     * its value when a GlobalParameterChangedEvent is fired after event listeners
     * were added.
     */
    @Test
    public void testChangingReadFromDiskFlagAfterAddingEventListeners()
    {
        requester = createHttpRequester();
        requester.addEventListeners();
        assertReadFromDiskFlagCanChange(true);
    }


    /**
     * Tests if a {@linkplain HttpRequester} if the readFromDisk flag does not change
     * its value when a GlobalParameterChangedEvent is fired before event listeners
     * were added.
     */
    @Test
    public void testChangingReadFromDiskFlagBeforeAddingEventListeners()
    {
        requester = createHttpRequester();
        assertReadFromDiskFlagCanChange(false);
    }


    /**
     * Tests if a {@linkplain HttpRequester} if the readFromDisk flag does not change
     * its value when a GlobalParameterChangedEvent is fired after event listeners
     * were removed.
     */
    @Test
    public void testChangingReadFromDiskFlagAfterRemovingEventListeners()
    {
        requester = createHttpRequester();
        requester.addEventListeners();
        requester.removeEventListeners();
        assertReadFromDiskFlagCanChange(false);
    }


    /**
     * Tests if a {@linkplain HttpRequester} if the writeToDisk flag changes
     * its value when a GlobalParameterChangedEvent is fired after event listeners
     * were added.
     */
    @Test
    public void testChangingWriteToDiskFlagAfterAddingEventListeners()
    {
        requester = createHttpRequester();
        requester.addEventListeners();
        assertWriteToDiskFlagCanChange(true);
    }


    /**
     * Tests if a {@linkplain HttpRequester} if the writeToDisk flag does not change
     * its value when a GlobalParameterChangedEvent is fired before event listeners
     * were added.
     */
    @Test
    public void testChangingWriteToDiskFlagBeforeAddingEventListeners()
    {
        requester = createHttpRequester();
        assertWriteToDiskFlagCanChange(false);
    }


    /**
     * Tests if a {@linkplain HttpRequester} if the writeToDisk flag does not change
     * its value when a GlobalParameterChangedEvent is fired after event listeners
     * were removed.
     */
    @Test
    public void testChangingWriteToDiskFlagAfterRemovingEventListeners()
    {
        requester = createHttpRequester();
        requester.addEventListeners();
        requester.removeEventListeners();
        assertWriteToDiskFlagCanChange(false);
    }


    /**
    * Tests if the writeToDisk flag always remains disabled if no path was set.
    */
    @Test
    public void testChangingWriteToDiskFlagWithMissingCachePath()
    {
        requester = new HttpRequester(
            StandardCharsets.UTF_8,
            new Gson(),
            isCachingEnabled,
            isCachingEnabled,
            null);

        requester.addEventListeners();

        EventSystem.sendEvent(new GlobalParameterChangedEvent(
                                  new BooleanParameter(ConfigurationConstants.WRITE_HTTP_TO_DISK, true),
                                  null));
        assert !requester.isWritingToDisk();
    }


    /**
     * Tests if the readFromDisk flag always remains disabled if no path was set.
     */
    @Test
    public void testChangingReadFromDiskFlagWithMissingCachePath()
    {
        requester = new HttpRequester(
            StandardCharsets.UTF_8,
            new Gson(),
            isCachingEnabled,
            isCachingEnabled,
            null);

        requester.addEventListeners();

        EventSystem.sendEvent(new GlobalParameterChangedEvent(
                                  new BooleanParameter(ConfigurationConstants.READ_HTTP_FROM_DISK, true),
                                  null));
        assert !requester.isWritingToDisk();
    }


    /**
     * Tests if a HTML Java object is returned when a GET-request
     * is sent to a valid URL or its cached response.
     */
    @Test
    public void testGettingHtml()
    {
        requester = createReadOnlyExampleHttpRequester();
        final String requestUrl = isCachingEnabled
                                  ? DEFAULT_URL + MOCKED_RESPONSE_FOLDER
                                  : DEFAULT_URL;
        Document htmlObject = requester.getHtmlFromUrl(requestUrl);

        assertNotNull(htmlObject);
    }


    /**
     * Tests if null is returned when trying to retrieve HTML
     * from an invalid URL.
     */
    @Test
    public void testGettingHtmlFromInvalidUrl()
    {
        requester = createReadOnlyExampleHttpRequester();

        LoggerConstants.ROOT_LOGGER.setLevel(Level.OFF);
        Document htmlObject = requester.getHtmlFromUrl(DEFAULT_URL + "/thisDoesNotExist");
        LoggerConstants.ROOT_LOGGER.setLevel(Level.DEBUG);

        assertNull(htmlObject);
    }


    /**
     * Tests if a valid HTML response that is not cached
     * can always be retrieved from web.
     *
     * @throws IOException thrown when the expected response is cached on disk
     */
    @Test
    public void testGettingUncachedHtml() throws IOException
    {
        requester = createReadOnlyExampleHttpRequester();

        final File cachedResponse =
            new File(
            requester.getCacheFolder()
            + DEFAULT_URL.substring(7)
            + CACHED_HTML);

        if (cachedResponse.exists())
            throw new IOException(UNEXPECTED_FILE_ERROR + cachedResponse.getAbsolutePath());

        final Document htmlObject = requester.getHtmlFromUrl(DEFAULT_URL);
        assertNotNull(htmlObject);
    }


    /**
     * Tests if a HTML response is cached on disk if the 'writeToDisk' flag is true.
     */
    @Test
    public void testCachingHtml()
    {
        requester = createHttpRequester();

        final File cachedResponse = new File(
            requester.getCacheFolder()
            + DEFAULT_URL.substring(7)
            + CACHED_HTML);

        // no need to check if the file already exists, as this is done in before()

        // make a request, causing the response to be cached
        requester.getHtmlFromUrl(DEFAULT_URL);

        // verify the cache file was created (or not)
        assertEquals(isCachingEnabled, cachedResponse.exists());
    }


    /**
     * Tests if a JSON object can be retrieved by sending a request to the FAOSTAT rest API.
     * A JSON object is also cached on disk under the same URL, but containing only dummy data.
     */
    @Test
    public void testGettingObject()
    {
        requester = createReadOnlyExampleHttpRequester();
        final String requestUrl = isCachingEnabled
                                  ? JSON_OBJECT_URL + MOCKED_RESPONSE_FOLDER
                                  : JSON_OBJECT_URL;
        final JsonObject obj = requester.getObjectFromUrl(requestUrl, JsonObject.class);

        assertNotNull(obj);
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
        requester = createReadOnlyExampleHttpRequester();

        final File cachedResponse =
            new File(requester.getCacheFolder()
                     + JSON_OBJECT_URL.substring(7)
                     + CACHED_JSON);

        if (cachedResponse.exists())
            throw new IOException(UNEXPECTED_FILE_ERROR + cachedResponse.getAbsolutePath());

        JsonObject obj = requester.getObjectFromUrl(
                             JSON_OBJECT_URL,
                             JsonObject.class);

        assertNotNull(obj);
    }


    /**
     * Tests if a JSON object response is cached on disk if the 'writeToDisk' flag is true.
     */
    @Test
    public void testCachingObject() throws IOException
    {
        requester = createHttpRequester();

        final File cachedResponse = new File(
            requester.getCacheFolder()
            + JSON_OBJECT_URL.substring(7)
            + CACHED_JSON);

        // no need to check if the file already exists, as this is done in before()

        // request JSON response
        requester.getObjectFromUrl(JSON_OBJECT_URL, JsonObject.class);

        // verify the cache file was created (or not)
        assertEquals(isCachingEnabled, cachedResponse.exists());
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
        requester = createReadOnlyExampleHttpRequester();
        String response = requester.getRestResponse(RestRequestType.GET, DEFAULT_URL, null, null, "text/html");
        assertNotNull(response);
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
        requester = createReadOnlyExampleHttpRequester();

        Map<String, List<String>> header = requester.getRestHeader(RestRequestType.GET, DEFAULT_URL, null);
        assert header.size() > 0;
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
        return new HttpRequester(
                   StandardCharsets.UTF_8,
                   new Gson(),
                   isCachingEnabled,
                   false,
                   MOCKED_RESPONSE_CACHE_FOLDER + "/");
    }


    /**
     * Creates a {@linkplain HttpRequester} that depends on the parameter.
     *
     * @return a {@linkplain HttpRequester} with enabled response caching
     */
    private HttpRequester createHttpRequester()
    {
        return new HttpRequester(
                   StandardCharsets.UTF_8,
                   new Gson(),
                   isCachingEnabled,
                   isCachingEnabled,
                   TEST_RESPONSE_CACHE_FOLDER.getPath() + "/");
    }


    /**
     * Checks if the readFromDisk flag can be changed via a
     * GlobalParameterChangedEvent, and asserts that the behavior is expected.
     *
     * @param expectedResult if true, the test passes if the flag changes,
     * if false, the test passes if the value does not change.
     */
    private void assertReadFromDiskFlagCanChange(boolean expectedResult)
    {
        final boolean oldValue = requester.isReadingFromDisk();

        EventSystem.sendEvent(changeReadFromDiskEvent);

        assertEquals(expectedResult, oldValue != requester.isReadingFromDisk());
    }

    /**
     * Checks if the writeToDisk flag can be changed via a
     * GlobalParameterChangedEvent, and asserts that the behavior is expected.
     *
     * @param expectedResult if true, the test passes if the flag changes,
     * if false, the test passes if the value does not change.
     */
    private void assertWriteToDiskFlagCanChange(boolean expectedResult)
    {
        final boolean oldValue = requester.isWritingToDisk();

        EventSystem.sendEvent(changeWriteToDiskEvent);

        assertEquals(expectedResult, oldValue != requester.isWritingToDisk());
    }
}
