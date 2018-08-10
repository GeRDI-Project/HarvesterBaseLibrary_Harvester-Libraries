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
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.IEvent;
import de.gerdiproject.harvest.harvester.events.HarvestFinishedEvent;
import de.gerdiproject.harvest.harvester.events.HarvestStartedEvent;
import de.gerdiproject.harvest.save.events.SaveFinishedEvent;
import de.gerdiproject.harvest.save.events.SaveStartedEvent;
import de.gerdiproject.harvest.state.events.AbortingStartedEvent;
import de.gerdiproject.harvest.submission.events.SubmissionFinishedEvent;
import de.gerdiproject.harvest.submission.events.SubmissionStartedEvent;
import de.gerdiproject.harvest.utils.FileUtils;
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
@RunWith(Parameterized.class)
public class HarvestTimeKeeperTest
{
    private static final String CACHE_PATH =
        "mocked/" + String.format(
            CacheConstants.HARVEST_TIME_KEEPER_CACHE_FILE_PATH,
            "timeKeeperTest");

    private static final String HARVEST_MEASURE = "harvest";
    private static final String SUBMISSION_MEASURE = "submission";
    private static final String SAVE_MEASURE = "save";

    @Parameters(name = "tested measure: {0}")
    public static Object[] getParameters()
    {
        return new Object[] {HARVEST_MEASURE, SUBMISSION_MEASURE, SAVE_MEASURE};
    }

    private final HarvestTimeKeeper keeper;
    private final String testedMeasureType;
    private final ProcessTimeMeasure testedMeasure;
    private final IEvent startEvent;
    private final IEvent finishedEvent;
    private final IEvent failedEvent;


    /**
     * The constructor defines which of the three {@linkplain ProcessTimeMeasure}s
     * that are part of the {@linkplain HarvestTimeKeeper} is being tested
     *
     * @param measureType a string representing the measure being tested
     */
    public HarvestTimeKeeperTest(String measureType)
    {
        keeper = new HarvestTimeKeeper(CACHE_PATH);
        keeper.addEventListeners();

        testedMeasureType = measureType;

        switch (measureType) {
            case HARVEST_MEASURE:
                testedMeasure = keeper.getHarvestMeasure();
                startEvent = new HarvestStartedEvent(0, 1, null);
                finishedEvent = new HarvestFinishedEvent(true, null);
                failedEvent = new HarvestFinishedEvent(false, null);
                break;

            case SUBMISSION_MEASURE:
                testedMeasure = keeper.getSubmissionMeasure();
                startEvent = new SubmissionStartedEvent(1);
                finishedEvent = new SubmissionFinishedEvent(true);
                failedEvent = new SubmissionFinishedEvent(false);
                break;

            case SAVE_MEASURE:
                testedMeasure = keeper.getSaveMeasure();
                startEvent = new SaveStartedEvent(false, 1);
                finishedEvent = new SaveFinishedEvent(true);
                failedEvent = new SaveFinishedEvent(false);
                break;

            default:
                throw new IllegalArgumentException();
        }
    }


    /**
     * Verifies that cache files are deleted and creates a {@linkplain HarvestTimeKeeper}.
     *
     * @throws IOException thrown when the temporary cache file could not be deleted
     */
    @Before
    public void before() throws IOException
    {
        final File cacheFile = new File(CACHE_PATH);
        FileUtils.deleteFile(cacheFile);

        if (cacheFile.exists())
            throw new IOException();
    }


    /**
     * Removes event listeners and deletes the cache file.
     */
    @After
    public void after()
    {
        if (keeper != null)
            keeper.removeEventListeners();

        FileUtils.deleteFile(new File(CACHE_PATH));
    }


    /**
     * Tests if the harvest is not considered finished and incomplete
     * while the harvest has not started.
     */
    @Test
    public void testHarvestIncompleteDuringNotStartedProcess()
    {
        assert !keeper.isHarvestIncomplete();
    }


    /**
     * Tests if the harvest is not considered finished and incomplete
     * while the harvest is still running.
     */
    @Test
    public void testHarvestIncompleteDuringStartedProcess()
    {
        EventSystem.sendEvent(new HarvestStartedEvent(0, 1, null));
        assert !keeper.isHarvestIncomplete();
    }


    /**
     * Tests if the harvest is not considered incomplete
     * after the harvest finished successfully.
     */
    @Test
    public void testHarvestIncompleteDuringFinishedProcess()
    {
        EventSystem.sendEvent(new HarvestStartedEvent(0, 1, null));
        EventSystem.sendEvent(new HarvestFinishedEvent(true, null));
        assert !keeper.isHarvestIncomplete();
    }


    /**
     * Tests if the harvest is considered incomplete
     * after the harvest failed.
     */
    @Test
    public void testHarvestIncompleteDuringFailedProcess()
    {
        EventSystem.sendEvent(new HarvestStartedEvent(0, 1, null));
        EventSystem.sendEvent(new HarvestFinishedEvent(false, null));
        assert keeper.isHarvestIncomplete();
    }


    /**
     * Tests if the harvest is not considered incomplete
     * after the harvest was aborted.
     */
    @Test
    public void testHarvestIncompleteDuringAbortedProcess()
    {
        EventSystem.sendEvent(new HarvestStartedEvent(0, 1, null));
        EventSystem.sendEvent(new AbortingStartedEvent());
        assert keeper.isHarvestIncomplete();
    }


    /**
     * Tests the constructor by checking that the there are no unsubmitted
     * changes.
     */
    @Test
    public void testInitialUnsubmittedChangesGetter()
    {
        assert !keeper.hasUnsubmittedChanges();
    }


    /**
     * Tests if no status is loaded from cache if the measure was not
     * started.
     */
    @Test
    public void testCachingNotStartedProcess()
    {
        final ProcessTimeMeasure loadedMeasure = loadMeasureFromCache();

        // assert that the 'started' status resets to 'not_started'
        assertEquals(ProcessStatus.NotStarted, loadedMeasure.getStatus());
    }


    /**
     * Tests if the 'started' status resets to 'notStarted' after it is loaded from cache.
     */
    @Test
    public void testCachingStartedProcess()
    {
        // send events to change status
        EventSystem.sendEvent(startEvent);

        final ProcessTimeMeasure loadedMeasure = loadMeasureFromCache();

        // assert that the 'started' status resets to 'not_started'
        assertEquals(ProcessStatus.NotStarted, loadedMeasure.getStatus());
    }


    /**
     * Tests if the 'finished' status can be loaded from cache.
     */
    @Test
    public void testCachingFinishedProcess()
    {
        // send events to change status
        EventSystem.sendEvent(startEvent);
        EventSystem.sendEvent(finishedEvent);

        final ProcessTimeMeasure loadedMeasure = loadMeasureFromCache();

        // assert that the loaded status is the one that was saved
        assertEquals(ProcessStatus.Finished, loadedMeasure.getStatus());
    }


    /**
     * Tests if the 'failed' status can be loaded from cache.
     */
    @Test
    public void testCachingFailedProcess()
    {
        // send events to change status
        EventSystem.sendEvent(startEvent);
        EventSystem.sendEvent(failedEvent);

        final ProcessTimeMeasure loadedMeasure = loadMeasureFromCache();

        // assert that the loaded status is the one that was saved
        assertEquals(ProcessStatus.Failed, loadedMeasure.getStatus());
    }


    /**
     * Tests if the 'aborted' status can be loaded from cache.
     */
    @Test
    public void testCachingAbortedProcess()
    {
        // send events to change status
        EventSystem.sendEvent(startEvent);
        EventSystem.sendEvent(new AbortingStartedEvent());

        final ProcessTimeMeasure loadedMeasure = loadMeasureFromCache();

        // assert that the loaded status is the one that was saved
        assertEquals(ProcessStatus.Aborted, loadedMeasure.getStatus());
    }


    /**
     * Tests if the initial status of a measure is 'NotStarted'.
     */
    @Test
    public void testProcessNotStarted()
    {
        assertEquals(ProcessStatus.NotStarted, testedMeasure.getStatus());
    }


    /**
     * Tests if a measure status progresses to 'NotStarted' if the corresponding event is sent.
     */
    @Test
    public void testProcessStart()
    {
        EventSystem.sendEvent(startEvent);
        assertEquals(ProcessStatus.Started, testedMeasure.getStatus());
    }


    /**
     * Tests if a measure status progresses to 'Finished' if the corresponding event is sent
     * after the measure was started.
     */
    @Test
    public void testProcessFinished()
    {
        EventSystem.sendEvent(startEvent);
        EventSystem.sendEvent(finishedEvent);
        assertEquals(ProcessStatus.Finished, testedMeasure.getStatus());
    }


    /**
     * Tests if a measure status progresses to 'Failed' if the corresponding event is sent
     * after the measure was started.
     */
    @Test
    public void testProcessFailed()
    {
        EventSystem.sendEvent(startEvent);
        EventSystem.sendEvent(failedEvent);
        assertEquals(ProcessStatus.Failed, testedMeasure.getStatus());
    }


    /**
     * Tests if a measure status progresses to 'Aborted' if the corresponding event is sent
     * after the measure was started.
     */
    @Test
    public void testProcessAborted()
    {
        EventSystem.sendEvent(startEvent);
        EventSystem.sendEvent(new AbortingStartedEvent());
        assertEquals(ProcessStatus.Aborted, testedMeasure.getStatus());
    }


    /**
     * Tests if a measure status progresses to 'Started' again if the corresponding event is sent
     * after the measure was finished.
     */
    @Test
    public void testProcessRestartAfterFinished()
    {
        EventSystem.sendEvent(startEvent);
        EventSystem.sendEvent(finishedEvent);
        EventSystem.sendEvent(startEvent);
        assertEquals(ProcessStatus.Started, testedMeasure.getStatus());
    }


    /**
     * Tests if a measure status progresses to 'Started' again if the corresponding event is sent
     * after the measure has failed.
     */
    @Test
    public void testProcessRestartAfterFailed()
    {
        EventSystem.sendEvent(startEvent);
        EventSystem.sendEvent(failedEvent);
        EventSystem.sendEvent(startEvent);
        assertEquals(ProcessStatus.Started, testedMeasure.getStatus());
    }


    /**
     * Tests if a measure status progresses to 'Started' again if the corresponding event is sent
     * after the measure was aborted.
     */
    @Test
    public void testProcessRestartAfterAborted()
    {
        EventSystem.sendEvent(startEvent);
        EventSystem.sendEvent(new AbortingStartedEvent());
        EventSystem.sendEvent(startEvent);
        assertEquals(ProcessStatus.Started, testedMeasure.getStatus());
    }


    //////////////////////
    // Non-test Methods //
    //////////////////////

    /**
     * Helper function that creates a new {@linkplain HarvestTimeKeeper}, loads the
     * cache to which the original {@linkplain HarvestTimeKeeper} has written, and
     * returns the same type of {@linkplain ProcessTimeMeasure} which is currently
     * being tested.
     *
     * @return a {@linkplain ProcessTimeMeasure} currently being tested, but loaded from cache
     */
    private ProcessTimeMeasure loadMeasureFromCache()
    {
        final HarvestTimeKeeper loadedKeeper = new HarvestTimeKeeper(CACHE_PATH);
        loadedKeeper.loadFromDisk();

        switch (testedMeasureType) {
            case HARVEST_MEASURE:
                return loadedKeeper.getHarvestMeasure();

            case SUBMISSION_MEASURE:
                return loadedKeeper.getSubmissionMeasure();

            case SAVE_MEASURE:
                return loadedKeeper.getSaveMeasure();

            default:
                throw new IllegalArgumentException();
        }
    }
}
