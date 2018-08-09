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
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import ch.qos.logback.classic.Level;
import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.config.events.GlobalParameterChangedEvent;
import de.gerdiproject.harvest.config.parameters.BooleanParameter;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.IEvent;
import de.gerdiproject.harvest.utils.data.HttpRequester;
import de.gerdiproject.harvest.utils.data.enums.RestRequestType;
import de.gerdiproject.harvest.utils.logger.constants.LoggerConstants;

/**
 * This class provides test cases for the {@linkplain HttpRequester}.
 *
 * @author Robin Weiss
 */
public class HttpRequesterTest
{
    private static final String MOCKED_RESPONSE_CACHE_FOLDER = "src/test/java/de/gerdiproject/utils/examples/httpRequester/";
    private static final String FAOSTAT_JSON_OBJECT_URL = "http://fenixservices.fao.org/faostat/api/v1/en/documents/RFB/";
    private static final String DEFAULT_URL = "http://www.gerdi-project.de/";

    private static final IEvent DISABLE_READ_EVENT = new GlobalParameterChangedEvent(
        new BooleanParameter(ConfigurationConstants.READ_HTTP_FROM_DISK, false),
        null);

    private static final IEvent DISABLE_WRITE_EVENT = new GlobalParameterChangedEvent(
        new BooleanParameter(ConfigurationConstants.WRITE_HTTP_TO_DISK, false),
        null);

    private static final IEvent ENABLE_READ_EVENT = new GlobalParameterChangedEvent(
        new BooleanParameter(ConfigurationConstants.READ_HTTP_FROM_DISK, true),
        null);

    private static final IEvent ENABLE_WRITE_EVENT = new GlobalParameterChangedEvent(
        new BooleanParameter(ConfigurationConstants.WRITE_HTTP_TO_DISK, true),
        null);


    private HttpRequester requester = null;


    /**
     * After each test, removes remaining event listeners from a {@linkplain HttpRequester}.
     */
    @After
    public void after()
    {
        if (requester != null)
            requester.removeEventListeners();

        requester = null;
    }


    /**
     * Tests if flags are enabled when calling a constructor
     * without specifying a cache path.
     */
    @Test
    public void testConstructorWithCaching()
    {
        requester = createHttpRequesterWithCaching();

        assertNotNull(requester.getCacheFolder());
        assert requester.isReadingFromDisk();
        assert requester.isWritingToDisk();
    }


    /**
     * Tests if flags are set up correctly when calling a constructor
     * without specifying a cache path.
     */
    @Test
    public void testConstructorWithoutCaching()
    {
        requester = createHttpRequesterWithoutCaching();

        assertNull(requester.getCacheFolder());
        assert ! requester.isReadingFromDisk();
        assert ! requester.isWritingToDisk();
    }


    /**
     * Tests if a {@linkplain HttpRequester} receives events properly after the
     * EventListeners are added.
     */
    @Test
    public void testAddingEventListeners()
    {
        requester = createHttpRequesterWithCaching();

        // start listening now
        requester.addEventListeners();

        // check if global parameter changes affect the HttpRequester
        assert requester.isReadingFromDisk();
        EventSystem.sendEvent(DISABLE_READ_EVENT);
        assert !requester.isReadingFromDisk();

        assert requester.isWritingToDisk();
        EventSystem.sendEvent(DISABLE_WRITE_EVENT);
        assert !requester.isWritingToDisk();
    }


    /**
     * Tests if a {@linkplain HttpRequester} no longer receives events after the
     * EventListeners are removed.
     */
    @Test
    public void testRemovingEventListeners()
    {
        requester = createHttpRequesterWithCaching();

        // add and remove listening
        requester.addEventListeners();
        requester.removeEventListeners();

        // check if global parameter changes do not affect the HttpRequester
        EventSystem.sendEvent(DISABLE_READ_EVENT);
        assert requester.isReadingFromDisk();

        EventSystem.sendEvent(DISABLE_WRITE_EVENT);
        assert requester.isWritingToDisk();
    }


    /**
    * Tests if caching always remains disabled if no path was set.
    */
    @Test
    public void testMissingCachePath()
    {
        requester = new HttpRequester(
            StandardCharsets.UTF_8,
            new Gson(),
            true,
            true,
            null);

        // read and write must be disabled, because there is no path
        assert !requester.isReadingFromDisk();
        assert !requester.isWritingToDisk();

        EventSystem.sendEvent(ENABLE_READ_EVENT);
        assert !requester.isReadingFromDisk();

        EventSystem.sendEvent(ENABLE_WRITE_EVENT);
        assert !requester.isWritingToDisk();
    }


    /**
     * Tests if an HTML Java object is returned when a GET-request
     * is sent to a valid URL.
     */
    @Test
    public void testGettingHtmlFromUrl()
    {
        requester = createHttpRequesterWithoutCaching();
        Document htmlObject = requester.getHtmlFromUrl(DEFAULT_URL);

        assertNotNull(htmlObject);
    }


    /**
     * Tests if null is returned when a GET-request
     * is sent to an invalid URL.
     */
    @Test
    public void testGettingHtmlFromInvalidUrl()
    {
        requester = createHttpRequesterWithoutCaching();

        LoggerConstants.ROOT_LOGGER.setLevel(Level.OFF);
        Document htmlObject = requester.getHtmlFromUrl(DEFAULT_URL + "/thisDoesNotExist");
        LoggerConstants.ROOT_LOGGER.setLevel(Level.DEBUG);

        assertNull(htmlObject);
    }


    /**
     * Tests if a HTML response can be read from disk if the corresponding flag is set.
     */
    @Test
    public void testGettingHtmlFromDisk()
    {
        requester = createReadOnlyExampleHttpRequester();
        Document htmlObject = requester.getHtmlFromUrl(DEFAULT_URL + "/mocked");

        assertNotNull(htmlObject);
    }


    /**
     * Tests if a HTML response that is supposed to be read from disk,
     * is read from web if the response was not cached.
     */
    @Test
    public void testGettingNonExistingHtmlFromDisk()
    {
        final File cachedResponse =
            new File(
            MOCKED_RESPONSE_CACHE_FOLDER
            + DEFAULT_URL.substring(7)
            + "response.html");

        assert !cachedResponse.exists();

        requester = createReadOnlyExampleHttpRequester();
        Document htmlObject = requester.getHtmlFromUrl(DEFAULT_URL);

        assertNotNull(htmlObject);
    }


    /**
     * Tests if a JSON object can be retrieved by sending a request to the FAOSTAT rest API.
     * If FAOSTAT do not change their existing datasets, the response should contain a "metadata"
     * JSON object and a "data" JSON array.
     */
    @Test
    public void testGettingObjectFromUrl()
    {
        requester = createHttpRequesterWithoutCaching();
        JsonObject obj = requester.getObjectFromUrl(
                             FAOSTAT_JSON_OBJECT_URL,
                             JsonObject.class);

        assertNotNull(obj.get("metadata").getAsJsonObject());
        assertNotNull(obj.get("data").getAsJsonArray());
    }


    /**
     * Tests if a JSON object is read and deserialized when the read-from-disk flag is enabled.
     */
    @Test
    public void testGettingObjectFromDisk()
    {
        requester = createReadOnlyExampleHttpRequester();

        final JsonObject obj = requester.getObjectFromUrl(
                                   DEFAULT_URL + "/mocked",
                                   JsonObject.class);

        assertEquals("foo", obj.get("myString").getAsString());
        assertEquals(1337, obj.get("myInt").getAsInt());
        assertEquals(true, obj.get("myBool").getAsBoolean());
    }


    /**
     * Tests if a JSON object that is supposed to be read from disk,
     * is read from web if the response was not cached.
     */
    @Test
    public void testGettingNonExistingObjectFromDisk()
    {
        final File cachedResponse =
            new File(MOCKED_RESPONSE_CACHE_FOLDER
                     + FAOSTAT_JSON_OBJECT_URL.substring(7)
                     + "response.html");

        assert !cachedResponse.exists();

        requester = createReadOnlyExampleHttpRequester();

        JsonObject obj = requester.getObjectFromUrl(
                             FAOSTAT_JSON_OBJECT_URL,
                             JsonObject.class);

        assertNotNull(obj);
    }


    /**
     * Sends a GET request to the gerdi homepage and verifies that the response is not null.
     */
    @Test
    public void testGetRequestResponse()
    {
        requester = createHttpRequesterWithoutCaching();

        try {
            String response = requester.getRestResponse(RestRequestType.GET, DEFAULT_URL, null, null, "text/html");
            assertNotNull(response);

        } catch (HTTPException | IOException e) {
            assert false;
        }
    }


    /**
     * Tests if the header can be retrieved by sending a GET request to the gerdi homepage.
     */
    @Test
    public void testGettingHeader()
    {
        requester = createHttpRequesterWithoutCaching();

        try {
            Map<String, List<String>> header = requester.getRestHeader(RestRequestType.GET, DEFAULT_URL, null);

            assert header.size() > 0;

        } catch (HTTPException | IOException e) {
            assert false;
        }

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
                   true,
                   false,
                   MOCKED_RESPONSE_CACHE_FOLDER);
    }


    /**
     * Creates a {@linkplain HttpRequester} with enabled response caching.
     *
     * @return a {@linkplain HttpRequester} with enabled response caching
     */
    private HttpRequester createHttpRequesterWithCaching()
    {
        return new HttpRequester(
                   StandardCharsets.UTF_8,
                   new Gson(),
                   true,
                   true,
                   "mocked/httpRequesterTestDir");
    }


    /**
     * Creates a {@linkplain HttpRequester} with disabled response caching.
     *
     * @return a {@linkplain HttpRequester} with disabled response caching
     */
    private HttpRequester createHttpRequesterWithoutCaching()
    {
        return new HttpRequester(
                   StandardCharsets.UTF_8,
                   new Gson());
    }
}
