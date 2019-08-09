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
package de.gerdiproject.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import de.gerdiproject.AbstractUnitTest;
import de.gerdiproject.application.examples.MockedContextListener;
import de.gerdiproject.harvest.application.MainContext;
import de.gerdiproject.harvest.application.events.ServiceInitializedEvent;
import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.utils.Procedure;
import de.gerdiproject.harvest.utils.file.FileUtils;

/**
 * This class offers unit tests for the {@linkplain MainContext}.
 * It tests various functionalities when the MainContext is initialized
 * successfully, or when the initialization fails.
 *
 * @author Robin Weiss
 */
@RunWith(Parameterized.class)
public class MainContextTest extends AbstractUnitTest
{
    private static final int INIT_TIMEOUT = 5000;
    private static final File CACHE_FOLDER = new File("cache");
    private static final String CLEANUP_ERROR = "Could not delete temporary test directory for MainContext tests: " + CACHE_FOLDER;

    @Parameters(name = "successful init: {0}")
    public static Object[] getParameters()
    {
        return new Object[] {Boolean.FALSE, Boolean.TRUE};
    }

    private final MockedContextListener mockedContextListener;
    private final boolean shouldInitBeSuccessful;
    private final Procedure initFunction;
    private final File configFile;


    /**
     * This constructor is called via the Parameters. It determines
     * whether the MainContext init() method should be successful
     * or not.
     *
     * @param shouldInitBeSuccessful if true, the init() function should succeed
     */
    public MainContextTest(boolean shouldInitBeSuccessful)
    {
        this.shouldInitBeSuccessful = shouldInitBeSuccessful;
        this.mockedContextListener = new MockedContextListener();

        this.configFile = new File(
            String.format(
                ConfigurationConstants.CONFIG_PATH,
                mockedContextListener.getServiceName()));

        this.initFunction = shouldInitBeSuccessful
                            ? mockedContextListener::initializeMainContext
                            : mockedContextListener::failMainContextInitialization;
    }


    @Override
    public void before() throws InstantiationException
    {
        // remove cache folder that is generated by the MainContext
        FileUtils.deleteFile(CACHE_FOLDER);
        FileUtils.deleteFile(configFile);

        if (CACHE_FOLDER.exists() || configFile.exists())
            throw new InstantiationException(CLEANUP_ERROR);

        super.before();
    }


    @Override
    public void after()
    {
        super.after();

        // clean up temp files after tests to free up some space
        FileUtils.deleteFile(CACHE_FOLDER);
        FileUtils.deleteFile(configFile);
    }


    /**
     * Tests if the init() method ultimately sends a {@linkplain ServiceInitializedEvent}
     * with a corresponding payload.
     */
    @Test
    public void testInitializationEvent()
    {
        final ServiceInitializedEvent initDoneEvent =
            waitForEvent(ServiceInitializedEvent.class, INIT_TIMEOUT, initFunction);

        assertEquals("Expected the method init() to send an event carrying the initialization success as payload!",
                     shouldInitBeSuccessful,
                     initDoneEvent.isSuccessful());
    }


    /**
     * Tests if the static isInitialized() method returns false before the initialization.
     */
    @Test
    public void testInitializationStateBefore()
    {
        assertFalse("Expected the method isInitialized() to return false before initialization!",
                    MainContext.isInitialized());
    }


    /**
     * Tests if the static isInitialized() method returns false after destroying the MainContext.
     */
    @Test
    public void testInitializationStateAfterDestroy()
    {
        waitForEvent(ServiceInitializedEvent.class, INIT_TIMEOUT, initFunction);
        MainContext.destroy();

        assertFalse("Expected the method isInitialized() to return false after calling destroy()!",
                    MainContext.isInitialized());
    }


    /**
     * Tests if the static isInitialized() method returns true after initialization.
     */
    @Test
    public void testInitializationState()
    {
        waitForEvent(ServiceInitializedEvent.class, INIT_TIMEOUT, initFunction);

        assertTrue("Expected the method isInitialized() to return true after initialization!",
                   MainContext.isInitialized());
    }


    /**
     * Tests if the static hasFailed() method returns false before initialization.
     */
    @Test
    public void testFailedStateBefore()
    {
        assertFalse("Expected the method hasFailed() to return false before the initialization!",
                    MainContext.hasFailed());
    }


    /**
     * Tests if the static hasFailed() method returns false after destroying the MainContext.
     */
    @Test
    public void testFailedStateAfterDestroy()
    {
        waitForEvent(ServiceInitializedEvent.class, INIT_TIMEOUT, initFunction);
        MainContext.destroy();

        assertFalse("Expected the method hasFailed() to return false after calling destroy()!",
                    MainContext.hasFailed());
    }


    /**
     * Tests if the static hasFailed() method returns the corresponding value after initialization.
     */
    @Test
    public void testFailedState()
    {
        waitForEvent(ServiceInitializedEvent.class, INIT_TIMEOUT, initFunction);

        assertNotEquals("Expected the method hasFailed() to return " + shouldInitBeSuccessful + " after initialization!",
                        shouldInitBeSuccessful,
                        MainContext.hasFailed());
    }
}
