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
package de.gerdiproject.save;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.nio.charset.StandardCharsets;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.harvester.events.HarvestStartedEvent;
import de.gerdiproject.harvest.save.HarvestSaver;
import de.gerdiproject.harvest.save.constants.SaveConstants;
import de.gerdiproject.harvest.save.events.StartSaveEvent;
import de.gerdiproject.harvest.utils.time.ProcessTimeMeasure;
import de.gerdiproject.harvest.utils.time.ProcessTimeMeasure.ProcessStatus;

/**
 * This class contains unit tests for the {@linkplain HarvestSaver}.
 *
 * @author Robin Weiss
 */
public class HarvestSaverTest
{
    private static final String TEST_NAME = "saveTest";

    private ProcessTimeMeasure measure;
    private HarvestSaver saver;


    /**
     * Constructs a dummy harvest {@linkplain ProcessTimeMeasure} and uses it to
     * construct a {@linkplain HarvestSaver}.
     */
    @Before
    public void before()
    {
        final Random random = new Random();
        final long startTimestamp = random.nextLong();
        final long endTimestamp = random.longs(startTimestamp + 1, startTimestamp + 99999999).findAny().getAsLong();

        measure = new ProcessTimeMeasure();
        measure.set(startTimestamp, endTimestamp, ProcessStatus.Finished);
        saver = new HarvestSaver(TEST_NAME, StandardCharsets.UTF_8, measure);
        saver.addEventListeners();
    }


    /**
     * Cleans up event listeners and temporary field values.
     */
    @After
    public void after()
    {
        saver.removeEventListeners();
        measure.removeEventListeners();

        //saver.getTargetFile().delete();

        saver = null;
        measure = null;
    }


    /**
     * Tests if the file name starts with the specified name and the start timestamp
     * of the specified {@linkplain ProcessTimeMeasure}.
     */
    @Test
    public void testFileName()
    {
        final String fileName = saver.getTargetFile().getName();
        final String expectedFileName = String.format(
                                            SaveConstants.SAVE_FILE_NAME,
                                            TEST_NAME,
                                            measure.getStartTimestamp());

        assertEquals(expectedFileName, fileName);
    }


    /**
     * Tests if the file name changes after a harvest starts, while the file name
     * prefix remains the same.
     */
    @Test
    public void testFileNameChangeAfterHarvest()
    {
        final String fileNameBeforeHarvest = saver.getTargetFile().getName();

        EventSystem.sendEvent(new HarvestStartedEvent(0, 1, null));

        final String fileNameAfterHarvest = saver.getTargetFile().getName();

        assertNotEquals(fileNameBeforeHarvest, fileNameAfterHarvest);
    }


    @Test
    public void testFileContent()
    {
        EventSystem.sendEvent(new StartSaveEvent(false));
        // TODO
    }
}
