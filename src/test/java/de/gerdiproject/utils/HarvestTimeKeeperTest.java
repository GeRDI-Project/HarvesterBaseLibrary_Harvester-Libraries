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
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import de.gerdiproject.AbstractFileSystemUnitTest;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.IEvent;
import de.gerdiproject.harvest.harvester.events.HarvestFinishedEvent;
import de.gerdiproject.harvest.harvester.events.HarvestStartedEvent;
import de.gerdiproject.harvest.state.events.AbortingStartedEvent;
import de.gerdiproject.harvest.submission.events.SubmissionFinishedEvent;
import de.gerdiproject.harvest.submission.events.SubmissionStartedEvent;
import de.gerdiproject.harvest.utils.time.HarvestTimeKeeper;
import de.gerdiproject.harvest.utils.time.ProcessTimeMeasure;
import de.gerdiproject.harvest.utils.time.ProcessTimeMeasure.ProcessStatus;

/**
 * This class provides test cases for the {@linkplain HarvestTimeKeeper}.
 *
 * @author Robin Weiss
 */
@RunWith(Parameterized.class)
public class HarvestTimeKeeperTest extends AbstractFileSystemUnitTest<HarvestTimeKeeper>
{
    private static final String HARVEST_MEASURE = "harvest";
    private static final String SUBMISSION_MEASURE = "submission";
    private static final String ASSERT_LOAD_STATE_MESSAGE = "A measure that was saved in the %s-state should be in the %s-state when loaded!";
    private static final String ASSERT_CHANGE_STATE_MESSAGE = "Sending a %s should switch the measure from the %s- to the %s-state!";

    @Parameters(name = "tested measure: {0}")
    public static Object[] getParameters()
    {
        return new Object[] {HARVEST_MEASURE, SUBMISSION_MEASURE};
    }

    private final String cachePath = testFolder + "/processTimes.json";
    private final String testedMeasureType;

    private ProcessTimeMeasure testedMeasure;
    private IEvent startEvent;
    private IEvent finishedEvent;
    private IEvent failedEvent;


    /**
     * The constructor defines which of the three {@linkplain ProcessTimeMeasure}s
     * that are part of the {@linkplain HarvestTimeKeeper} is being tested
     *
     * @param measureType a string representing the measure being tested
     */
    public HarvestTimeKeeperTest(String testedMeasureType)
    {
        this.testedMeasureType = testedMeasureType;
    }


    @Override
    protected HarvestTimeKeeper setUpTestObjects()
    {
        final HarvestTimeKeeper timeKeeper = new HarvestTimeKeeper(cachePath);

        switch (testedMeasureType) {
            case HARVEST_MEASURE:
                testedMeasure = timeKeeper.getHarvestMeasure();
                startEvent = new HarvestStartedEvent(0, 1, null);
                finishedEvent = new HarvestFinishedEvent(true, null);
                failedEvent = new HarvestFinishedEvent(false, null);
                break;

            case SUBMISSION_MEASURE:
                testedMeasure = timeKeeper.getSubmissionMeasure();
                startEvent = new SubmissionStartedEvent(1);
                finishedEvent = new SubmissionFinishedEvent(true);
                failedEvent = new SubmissionFinishedEvent(false);
                break;

            default:
                throw new IllegalArgumentException();
        }

        return timeKeeper;
    }


    /**
     * Tests if the harvest is not considered finished and incomplete
     * while the harvest has not started.
     */
    @Test
    public void testHarvestIncompleteDuringNotStartedProcess()
    {
        assertFalse("The method isHarvestIncomplete() should return false if a harvest was not started!",
                    testedObject.isHarvestIncomplete());
    }


    /**
     * Tests if the harvest is not considered finished and incomplete
     * while the harvest is still running.
     */
    @Test
    public void testHarvestIncompleteDuringStartedProcess()
    {
        testedObject.addEventListeners();
        EventSystem.sendEvent(new HarvestStartedEvent(0, 1, null));
        assertFalse("The method isHarvestIncomplete() should return false if a harvest is in progress!",
                    testedObject.isHarvestIncomplete());
    }


    /**
     * Tests if the harvest is not considered incomplete
     * after the harvest finished successfully.
     */
    @Test
    public void testHarvestIncompleteDuringFinishedProcess()
    {
        testedObject.addEventListeners();
        EventSystem.sendEvent(new HarvestStartedEvent(0, 1, null));
        EventSystem.sendEvent(new HarvestFinishedEvent(true, null));
        assertFalse("The method isHarvestIncomplete() should return false if a harvest finished successfully!",
                    testedObject.isHarvestIncomplete());
    }


    /**
     * Tests if the harvest is considered incomplete
     * after the harvest failed.
     */
    @Test
    public void testHarvestIncompleteDuringFailedProcess()
    {
        testedObject.addEventListeners();
        EventSystem.sendEvent(new HarvestStartedEvent(0, 1, null));
        EventSystem.sendEvent(new HarvestFinishedEvent(false, null));
        assertTrue("The method isHarvestIncomplete() should return true if a harvest failed!",
                   testedObject.isHarvestIncomplete());
    }


    /**
     * Tests if the harvest is not considered incomplete
     * after the harvest was aborted.
     */
    @Test
    public void testHarvestIncompleteDuringAbortedProcess()
    {
        testedObject.addEventListeners();
        EventSystem.sendEvent(new HarvestStartedEvent(0, 1, null));
        EventSystem.sendEvent(new AbortingStartedEvent());
        assertTrue("The method isHarvestIncomplete() should return true if a harvest was aborted!",
                   testedObject.isHarvestIncomplete());
    }


    /**
     * Tests the constructor by checking that the there are no unsubmitted
     * changes.
     */
    @Test
    public void testInitialUnsubmittedChangesGetter()
    {
        assertFalse("The method hasUnsubmittedChanges() should return false if nothing was harvested, yet!",
                    testedObject.hasUnsubmittedChanges());
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
        assertEquals(String.format(ASSERT_LOAD_STATE_MESSAGE, ProcessStatus.NotStarted, ProcessStatus.NotStarted),
                     ProcessStatus.NotStarted,
                     loadedMeasure.getStatus());
    }


    /**
     * Tests if the 'started' status resets to 'notStarted' after it is loaded from cache.
     */
    @Test
    public void testCachingStartedProcess()
    {
        testedObject.addEventListeners();

        // send events to change status
        EventSystem.sendEvent(startEvent);

        final ProcessTimeMeasure loadedMeasure = loadMeasureFromCache();

        // assertTrue("",that the 'started' status resets to 'not_started'
        assertEquals(String.format(ASSERT_LOAD_STATE_MESSAGE, ProcessStatus.Started, ProcessStatus.NotStarted),
                     ProcessStatus.NotStarted,
                     loadedMeasure.getStatus());
    }


    /**
     * Tests if the 'finished' status can be loaded from cache.
     */
    @Test
    public void testCachingFinishedProcess()
    {
        testedObject.addEventListeners();

        // send events to change status
        EventSystem.sendEvent(startEvent);
        EventSystem.sendEvent(finishedEvent);

        final ProcessTimeMeasure loadedMeasure = loadMeasureFromCache();

        // assertTrue("",that the loaded status is the one that was saved
        assertEquals(String.format(ASSERT_LOAD_STATE_MESSAGE, ProcessStatus.Finished, ProcessStatus.Finished),
                     ProcessStatus.Finished,
                     loadedMeasure.getStatus());
    }


    /**
     * Tests if the 'failed' status can be loaded from cache.
     */
    @Test
    public void testCachingFailedProcess()
    {
        testedObject.addEventListeners();

        // send events to change status
        EventSystem.sendEvent(startEvent);
        EventSystem.sendEvent(failedEvent);

        final ProcessTimeMeasure loadedMeasure = loadMeasureFromCache();

        // assertTrue("",that the loaded status is the one that was saved
        assertEquals(String.format(ASSERT_LOAD_STATE_MESSAGE, ProcessStatus.Failed, ProcessStatus.Failed),
                     ProcessStatus.Failed,
                     loadedMeasure.getStatus());
    }


    /**
     * Tests if the 'aborted' status can be loaded from cache.
     */
    @Test
    public void testCachingAbortedProcess()
    {
        testedObject.addEventListeners();

        // send events to change status
        EventSystem.sendEvent(startEvent);
        EventSystem.sendEvent(new AbortingStartedEvent());

        final ProcessTimeMeasure loadedMeasure = loadMeasureFromCache();

        // assertTrue that the loaded status is the one that was saved
        assertEquals(String.format(ASSERT_LOAD_STATE_MESSAGE, ProcessStatus.Aborted, ProcessStatus.Aborted),
                     ProcessStatus.Aborted,
                     loadedMeasure.getStatus());
    }


    /**
     * Tests if the initial status of a measure is 'NotStarted'.
     */
    @Test
    public void testProcessNotStarted()
    {
        assertEquals("The initial status of a measure should be : " + ProcessStatus.NotStarted,
                     ProcessStatus.NotStarted,
                     testedMeasure.getStatus());
    }


    /**
     * Tests if a measure status progresses to 'NotStarted' if the corresponding event is sent.
     */
    @Test
    public void testProcessStart()
    {
        testedObject.addEventListeners();
        assertStateChangeOnEvent(startEvent, ProcessStatus.Started);
    }


    /**
     * Tests if a measure status progresses to 'Finished' if the corresponding event is sent
     * after the measure was started.
     */
    @Test
    public void testProcessFinished()
    {
        testedObject.addEventListeners();

        EventSystem.sendEvent(startEvent);
        assertStateChangeOnEvent(finishedEvent, ProcessStatus.Finished);
    }


    /**
     * Tests if a measure status progresses to 'Failed' if the corresponding event is sent
     * after the measure was started.
     */
    @Test
    public void testProcessFailed()
    {
        testedObject.addEventListeners();

        EventSystem.sendEvent(startEvent);
        EventSystem.sendEvent(failedEvent);
        assertStateChangeOnEvent(failedEvent, ProcessStatus.Failed);
    }


    /**
     * Tests if a measure status progresses to 'Aborted' if the corresponding event is sent
     * after the measure was started.
     */
    @Test
    public void testProcessAborted()
    {
        testedObject.addEventListeners();

        EventSystem.sendEvent(startEvent);
        assertStateChangeOnEvent(new AbortingStartedEvent(), ProcessStatus.Aborted);
    }


    /**
     * Tests if a measure status progresses to 'Started' again if the corresponding event is sent
     * after the measure was finished.
     */
    @Test
    public void testProcessRestartAfterFinished()
    {
        testedObject.addEventListeners();

        EventSystem.sendEvent(startEvent);
        EventSystem.sendEvent(finishedEvent);
        assertStateChangeOnEvent(startEvent, ProcessStatus.Started);
    }


    /**
     * Tests if a measure status progresses to 'Started' again if the corresponding event is sent
     * after the measure has failed.
     */
    @Test
    public void testProcessRestartAfterFailed()
    {
        testedObject.addEventListeners();

        EventSystem.sendEvent(startEvent);
        EventSystem.sendEvent(failedEvent);
        assertStateChangeOnEvent(startEvent, ProcessStatus.Started);
    }


    /**
     * Tests if a measure status progresses to 'Started' again if the corresponding event is sent
     * after the measure was aborted.
     */
    @Test
    public void testProcessRestartAfterAborted()
    {
        testedObject.addEventListeners();

        EventSystem.sendEvent(startEvent);
        EventSystem.sendEvent(new AbortingStartedEvent());
        assertStateChangeOnEvent(startEvent, ProcessStatus.Started);
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
        final HarvestTimeKeeper loadedKeeper = new HarvestTimeKeeper(cachePath);
        loadedKeeper.loadFromDisk();

        switch (testedMeasureType) {
            case HARVEST_MEASURE:
                return loadedKeeper.getHarvestMeasure();

            case SUBMISSION_MEASURE:
                return loadedKeeper.getSubmissionMeasure();

            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * Sends a specified event and asserts if the status of the tested measure changes as expected.
     *
     * @param event the event that is fired to trigger the status change
     * @param statusAfterEvent the expected status after the event was dispatched
     */
    private void assertStateChangeOnEvent(IEvent event, ProcessStatus statusAfterEvent)
    {
        final ProcessStatus currentStatus = testedMeasure.getStatus();

        EventSystem.sendEvent(event);

        assertEquals(String.format(ASSERT_CHANGE_STATE_MESSAGE, event.getClass().getSimpleName(), currentStatus, statusAfterEvent),
                     statusAfterEvent,
                     testedMeasure.getStatus());
    }
}
