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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gerdiproject.harvest.utils.logger.HarvesterLog;


/**
 * This class provides test cases for the {@linkplain HarvesterLogTest}.
 *
 * @author Robin Weiss
 */
public class HarvesterLogTest
{
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvesterLogTest.class);
    private static final File LOG_FILE = new File("mocked/test.log");

    private HarvesterLog log;

    /**
     * Before each test, a logger is created and registered.
     */
    @Before
    public void before()
    {
        log = new HarvesterLog(LOG_FILE.getPath());
        log.registerLogger();
    }


    /**
     * After each test, the logger is unregistered and the log file removed.
     */
    @After
    public void after()
    {
        log.unregisterLogger();
        log.clearLog();
        log = null;
    }


    /**
     * Tests if a log file is created after the log is registered.
     */
    @Test
    public void testLogFileCreation()
    {
        assertTrue(LOG_FILE.exists());
        assertTrue(LOG_FILE.isFile());
    }


    /**
     * Tests if the log file size changes after logging.
     */
    @Test
    public void testLogging()
    {
        final long logSizeBefore = LOG_FILE.length();
        LOGGER.info("Logging test");
        final long logSizeAfter = LOG_FILE.length();

        assertNotEquals(logSizeBefore, logSizeAfter);
    }


    /**
     * Tests if logs are not added to the log file while the logger
     * is unregistered, and added when it is re-registered.
     */
    @Test
    public void testUnRegistering()
    {
        LOGGER.info("Logging before unregistering");
        final long logSizeBeforeUnregister = LOG_FILE.length();

        log.unregisterLogger();
        LOGGER.info("Logging after unregistering");
        final long logSizeAfterUnregister = LOG_FILE.length();

        assertEquals(logSizeBeforeUnregister, logSizeAfterUnregister);

        log.registerLogger();
        LOGGER.info("Logging after re-registering");
        final long logSizeAfterReRegister = LOG_FILE.length();
        assertNotEquals(logSizeAfterUnregister, logSizeAfterReRegister);
    }


    /**
     * Tests if the log file becomes empty after clearing it.
     * This is tested both while the logger is registered and unregistered.
     */
    @Test
    public void testLogFileClear()
    {
        log.clearLog();
        assertEquals(0L, LOG_FILE.length());

        LOGGER.info("Logging after the file was cleared");
        assertNotEquals(0L, LOG_FILE.length());

        log.unregisterLogger();

        log.clearLog();
        assertEquals(0L, LOG_FILE.length());
    }
}
