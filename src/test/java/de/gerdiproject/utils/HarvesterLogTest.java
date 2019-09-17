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

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gerdiproject.AbstractFileSystemUnitTest;
import de.gerdiproject.harvest.utils.logger.HarvesterLog;


/**
 * This class provides test cases for {@linkplain HarvesterLog}s.
 *
 * @author Robin Weiss
 */
public class HarvesterLogTest extends AbstractFileSystemUnitTest<HarvesterLog>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvesterLogTest.class);
    private static final String EXAMPLE_LOG = "This log is created by the HarvesterLog unit tests.";

    private final File logFile = new File(testFolder, "test.log");


    @Override
    protected HarvesterLog setUpTestObjects()
    {
        HarvesterLog log = new HarvesterLog(logFile.getPath());
        log.registerLogger();
        return log;
    }


    @Override
    protected boolean isLoggingEnabledDuringTests()
    {
        return true;
    }


    /**
     * After each test, the logger is unregistered and the log file removed.
     */
    @Override
    public void after()
    {
        testedObject.unregisterLogger();
        
        setLoggerEnabled(false);
        testedObject.clearLog();
        setLoggerEnabled(isLoggingEnabledDuringTests());

        super.after();
    }


    /**
     * Tests if a log file is created after the log is registered.
     */
    @Test
    public void testLogFileCreation()
    {
        assertTrue("The method registerLogger() should cause the log file " + logFile + " to be created!",
                   logFile.exists() && logFile.isFile());
    }


    /**
     * Tests if the log file size changes after logging.
     */
    @Test
    public void testLogging()
    {
        final long logSizeBefore = logFile.length();
        LOGGER.info(EXAMPLE_LOG);
        final long logSizeAfter = logFile.length();

        assertNotEquals("Logging something after registerLogger() is called should cause the log file to change!",
                        logSizeBefore,
                        logSizeAfter);
    }


    /**
     * Tests if logs are not added to the log file while the logger
     * is unregistered.
     */
    @Test
    public void testLoggingWhileUnRegistered()
    {
        LOGGER.info(EXAMPLE_LOG);
        final long logSizeBeforeUnregister = logFile.length();

        testedObject.unregisterLogger();
        LOGGER.info(EXAMPLE_LOG);
        final long logSizeAfterUnregister = logFile.length();

        assertEquals("Logging something after unregisterLogger() is called should cause no changes in the log file!",
                     logSizeBeforeUnregister,
                     logSizeAfterUnregister);
    }

    /**
     * Tests if logs are added after unregistering and then registering the
     * logger again.
     */
    @Test
    public void testLoggingAfterReRegistering()
    {
        testedObject.unregisterLogger();
        testedObject.registerLogger();
        LOGGER.info(EXAMPLE_LOG);

        assertNotEquals("Logging something after an unregistered logger is registered again should cause the log file to change!",
                        0,
                        logFile.length());
    }


    /**
     * Tests if the log file becomes empty after clearing it.
     * This is tested both while the logger is registered and unregistered.
     */
    @Test
    public void testLogFileClear()
    {
        LOGGER.info(EXAMPLE_LOG);

        testedObject.unregisterLogger();
        testedObject.clearLog();

        assertEquals("Calling clearLog() should cause the log file to become empty!",
                     0L,
                     logFile.length());
    }
}
