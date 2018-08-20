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

import org.junit.Test;

import de.gerdiproject.AbstractFileSystemUnitTest;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.harvester.events.HarvestStartedEvent;
import de.gerdiproject.harvest.save.HarvestSaver;
import de.gerdiproject.harvest.save.constants.SaveConstants;
import de.gerdiproject.harvest.save.events.StartSaveEvent;
import de.gerdiproject.harvest.utils.cache.HarvesterCacheManager;
import de.gerdiproject.harvest.utils.time.ProcessTimeMeasure;
import de.gerdiproject.harvest.utils.time.ProcessTimeMeasure.ProcessStatus;

/**
 * This class contains unit tests for the {@linkplain HarvestSaver}.
 *
 * @author Robin Weiss
 */
public class HarvestSaverTest extends AbstractFileSystemUnitTest<HarvestSaver>
{
    private static final String TEST_NAME = "saveTest";

    private HarvesterCacheManager cacheManager = new HarvesterCacheManager();

    private ProcessTimeMeasure measure;


    @Override
    protected HarvestSaver setUpTestObjects()
    {
        final long startTimestamp = random.nextLong();
        final long endTimestamp = random.longs(startTimestamp + 1, startTimestamp + 99999999).findAny().getAsLong();

        this.measure = new ProcessTimeMeasure();
        this.measure.set(startTimestamp, endTimestamp, ProcessStatus.Finished);
        this.cacheManager = new HarvesterCacheManager();

        return new HarvestSaver(testFolder, TEST_NAME, StandardCharsets.UTF_8, measure, cacheManager);
    }


    @Override
    public void after()
    {
        super.after();

        cacheManager.removeEventListeners();
        cacheManager = null;

        measure.removeEventListeners();
        measure = null;
    }


    /**
     * Tests if the file name starts with the specified name and the start timestamp
     * of the specified {@linkplain ProcessTimeMeasure}.
     */
    @Test
    public void testFileName()
    {
        final String fileName = testedObject.getTargetFile().getName();
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
        final String fileNameBeforeHarvest = testedObject.getTargetFile().getName();

        testedObject.addEventListeners();
        EventSystem.sendEvent(new HarvestStartedEvent(0, 1, null));

        final String fileNameAfterHarvest = testedObject.getTargetFile().getName();

        assertNotEquals(fileNameBeforeHarvest, fileNameAfterHarvest);
    }


    @Test
    public void testFileContent()
    {
        EventSystem.sendEvent(new StartSaveEvent(false));
        // TODO
    }
}
