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

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.IEvent;
import de.gerdiproject.harvest.harvester.events.HarvestFinishedEvent;
import de.gerdiproject.harvest.harvester.events.HarvestStartedEvent;
import de.gerdiproject.harvest.save.events.SaveFinishedEvent;
import de.gerdiproject.harvest.save.events.SaveStartedEvent;
import de.gerdiproject.harvest.state.events.AbortingStartedEvent;
import de.gerdiproject.harvest.submission.events.SubmissionFinishedEvent;
import de.gerdiproject.harvest.submission.events.SubmissionStartedEvent;
import de.gerdiproject.harvest.utils.cache.constants.CacheConstants;
import de.gerdiproject.harvest.utils.time.HarvestTimeKeeper;
import de.gerdiproject.harvest.utils.time.ProcessTimeMeasure;
import de.gerdiproject.harvest.utils.time.ProcessTimeMeasure.ProcessStatus;

/**
 * This class provides test cases for the {@linkplain HarvestTimeKeeper}.
 *
 * @author Robin Weiss
 *
 */
public class HarvestTimeKeeperTest
{
    private static final String CACHE_PATH =
        "test/" + String.format(
            CacheConstants.HARVEST_TIME_KEEPER_CACHE_FILE_PATH,
            "timeKeeperTest");

    private HarvestTimeKeeper keeper;


    @Before
    public void before()
    {
        keeper = new HarvestTimeKeeper(CACHE_PATH);
        keeper.addEventListeners();
    }

    /**
     * Removes event listeners and deletes the cache file.
     */
    @After
    public void after()
    {
        if (keeper != null)
            keeper.removeEventListeners();

        keeper = null;

        final File cacheFile = new File(CACHE_PATH);
        cacheFile.delete();
    }

    /**
     * Tests the constructor by checking that all measures are not started after construction.
     */
    @Test
    public void testConstructor()
    {
        assertEquals(ProcessStatus.NotStarted, keeper.getHarvestMeasure().getStatus());
        assertEquals(ProcessStatus.NotStarted, keeper.getSaveMeasure().getStatus());
        assertEquals(ProcessStatus.NotStarted, keeper.getSubmissionMeasure().getStatus());
        assert(!keeper.isHarvestIncomplete());
        assert(!keeper.hasUnsubmittedChanges());
    }


    /**
     * Tests the caching functionality by sending events and reloading the {@linkplain HarvestTimeKeeper}.
     * Processes that are started should not be saved.
     */
    @Test
    public void testCaching()
    {
        // send events to change statuses
        EventSystem.sendEvent(new HarvestStartedEvent(0, 1, null));
        EventSystem.sendEvent(new HarvestFinishedEvent(true, null));
        EventSystem.sendEvent(new SaveStartedEvent(false, 1));
        EventSystem.sendEvent(new SaveFinishedEvent(false));
        EventSystem.sendEvent(new SubmissionStartedEvent(1));

        // clean up old time keeper
        keeper.removeEventListeners();

        keeper = new HarvestTimeKeeper(CACHE_PATH);
        keeper.loadFromDisk();
        keeper.addEventListeners();

        // assert that the loaded statuses are the saved ones
        assertEquals(ProcessStatus.Finished, keeper.getHarvestMeasure().getStatus());
        assertEquals(ProcessStatus.Failed, keeper.getSaveMeasure().getStatus());
        assertEquals(ProcessStatus.NotStarted, keeper.getSubmissionMeasure().getStatus());
    }


    /**
     * Tests the harvest time measure by dispatching harvesting events and asserting state changes.
     */
    @Test
    public void testHarvestMeasure()
    {
        testMeasure(
            keeper.getHarvestMeasure(),
            new HarvestStartedEvent(0, 1, null),
            new HarvestFinishedEvent(true, null),
            new HarvestFinishedEvent(false, null)
        );
    }


    /**
     * Tests the save time measure by dispatching saving events and asserting state changes.
     */
    @Test
    public void testSaveMeasure()
    {
        testMeasure(
            keeper.getSaveMeasure(),
            new SaveStartedEvent(false, 1),
            new SaveFinishedEvent(true),
            new SaveFinishedEvent(false)
        );
    }


    /**
     * Tests the submission time measure by dispatching submission events and asserting state changes.
     */
    @Test
    public void testSubmissionMeasure()
    {
        testMeasure(
            keeper.getSubmissionMeasure(),
            new SubmissionStartedEvent(1),
            new SubmissionFinishedEvent(true),
            new SubmissionFinishedEvent(false)
        );
    }


    /**
     * Tests a single time measure by dispatching events and evaluating state changes.
     *
     * @param measure the measure that is tested
     * @param startEvent an event that triggers the process to start
     * @param finishedEvent an event that triggers the process to end successfully
     * @param failedEvent an event that triggers the process to end erroneously
     */
    private void testMeasure(ProcessTimeMeasure measure, IEvent startEvent, IEvent finishedEvent, IEvent failedEvent)
    {
        assertEquals(ProcessStatus.NotStarted, measure.getStatus());

        EventSystem.sendEvent(startEvent);
        assertEquals(ProcessStatus.Started, measure.getStatus());

        EventSystem.sendEvent(finishedEvent);
        assertEquals(ProcessStatus.Finished, measure.getStatus());

        EventSystem.sendEvent(startEvent);
        assertEquals(ProcessStatus.Started, measure.getStatus());

        EventSystem.sendEvent(failedEvent);
        assertEquals(ProcessStatus.Failed, measure.getStatus());

        EventSystem.sendEvent(startEvent);
        assertEquals(ProcessStatus.Started, measure.getStatus());

        EventSystem.sendEvent(new AbortingStartedEvent());
        assertEquals(ProcessStatus.Aborted, measure.getStatus());
    }
}
