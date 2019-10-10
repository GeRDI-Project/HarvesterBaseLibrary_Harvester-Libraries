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
package de.gerdiproject.harvest.scheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.Test;

import de.gerdiproject.harvest.AbstractFileSystemUnitTest;
import de.gerdiproject.harvest.application.events.ContextDestroyedEvent;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.scheduler.Scheduler;
import de.gerdiproject.harvest.scheduler.json.ChangeSchedulerRequest;
import de.gerdiproject.harvest.scheduler.json.SchedulerResponse;

/**
 * This class contains unit tests for the {@linkplain Scheduler}.
 *
 * @author Robin Weiss
 */
public class SchedulerTest extends AbstractFileSystemUnitTest<Scheduler>
{
    private static final String INVALID_CRON = "abc";
    private static final String RANDOM_CRON_TAB = "%d 0 1 1 *";
    private static final String SOME_CRON_TAB = "0 0 1 1 *";
    private static final String ASSERT_EXCEPTION_MESSAGE = "Expected an " + IllegalArgumentException.class.getSimpleName() + " to be thrown when the same task is added twice!";

    private final File scheduleFile = new File(testFolder, "schedule.json");


    @Override
    protected Scheduler setUpTestObjects()
    {
        Scheduler scheduler = new Scheduler("", scheduleFile.toString());
        return scheduler;
    }


    /**
     * Tests if a new scheduler starts with no tasks assigned.
     */
    @Test
    public void testConstructor()
    {
        assertEquals(
            "The method size() should return 0 after the constructor was called",
            0,
            testedObject.size());
    }


    /**
     * Tests if a cache file is created when a new task is added.
     */
    @Test
    public void testSavingToDisk()
    {
        testedObject.addEventListeners();
        addTasks(1);
        assertNotEquals(
            "After adding a task, the cache file should not be empty!",
            0,
            scheduleFile.length());
    }


    /**
     * Tests if loading from disk fills up the correct
     * number of scheduled tasks.
     */
    @Test
    public void testLoadingFromDisk()
    {
        testedObject.addEventListeners();
        addRandomNumberOfTasks();

        Scheduler anotherScheduler = new Scheduler("", scheduleFile.toString());
        anotherScheduler.addEventListeners();
        anotherScheduler.loadFromDisk();
        anotherScheduler.removeEventListeners();

        assertEquals(
            "The method size() of a Scheduler that is saved to disk should return the same value as a Scheduler that was loaded from the cache file!",
            testedObject.size(),
            anotherScheduler.size());
    }


    /**
     * Tests if loading from disk causes no exceptions if
     * no cache file exists and will not change the scheduled tasks.
     */
    @Test
    public void testLoadingFromDiskNoExists()
    {
        testedObject.addEventListeners();
        addRandomNumberOfTasks();
        final int oldSize = testedObject.size();

        testedObject.loadFromDisk();

        assertEquals("The scheduled tasks should not change if a non-existing cache file is attempted to be loaded!",
                     oldSize,
                     testedObject.size());
    }


    /**
     * Tests if adding a random number of tasks affects
     * the scheduled task count accordingly.
     */
    @Test
    public void testAddingTask()
    {
        testedObject.addEventListeners();
        final int numberOfTasks = random.nextInt(10);
        addTasks(numberOfTasks);

        assertEquals("The method size() should return the same number of tasks that were added!",
                     numberOfTasks,
                     testedObject.size());
    }


    /**
     * Tests if adding the same cron tab twice throws an exception.
     */
    @Test
    public void testAddingTaskTwice()
    {
        testedObject.addEventListeners();

        try {
            testedObject.addTask(new ChangeSchedulerRequest(SOME_CRON_TAB));
            testedObject.addTask(new ChangeSchedulerRequest(SOME_CRON_TAB));

            fail("Adding the same cron tab twice should throw an exception!");
        } catch (Exception ex) {
            assertEquals(ASSERT_EXCEPTION_MESSAGE,
                         IllegalArgumentException.class,
                         ex.getClass());
        }
    }


    /**
     * Tests if adding an invalid cron tab throws an exception.
     */
    @Test
    public void testAddingTaskInvalidCronTab()
    {
        testedObject.addEventListeners();

        try {
            testedObject.addTask(new ChangeSchedulerRequest(INVALID_CRON));

            fail("Adding a cron tab with invalid syntax should throw an exception!");
        } catch (Exception ex) {
            assertEquals(ASSERT_EXCEPTION_MESSAGE,
                         IllegalArgumentException.class,
                         ex.getClass());
        }
    }


    /**
     * Tests if deleting a random number of tasks affects
     * the scheduled task count accordingly.
     */
    @Test
    public void testDeletingTask()
    {
        testedObject.addEventListeners();
        addTasks(10);
        final int numberOfDeletions = 1 + random.nextInt(10);

        for (int i = 0; i < numberOfDeletions; i++) {
            final String randomCron = String.format(RANDOM_CRON_TAB, i);
            testedObject.deleteTask(new ChangeSchedulerRequest(randomCron));
        }

        assertEquals("The method size() should return a lower number after deleting tasks!",
                     10 - numberOfDeletions,
                     testedObject.size());
    }


    /**
     * Tests if deleteing a non-existing task throws an exception.
     */
    @Test
    public void testDeletingTaskNonExisting()
    {
        testedObject.addEventListeners();

        try {
            testedObject.deleteTask(new ChangeSchedulerRequest(SOME_CRON_TAB));
            fail("Deleting a non-existing cron tab should throw an exception!");
        } catch (Exception ex) {
            assertEquals(ASSERT_EXCEPTION_MESSAGE,
                         IllegalArgumentException.class,
                         ex.getClass());
        }
    }


    /**
     * Tests if sending a delete event with a null payload removes all tasks.
     */
    @Test
    public void testDeletingAllTasks()
    {
        testedObject.addEventListeners();
        addTasks(10);

        testedObject.deleteAllTasks();
        assertEquals(
            "The method deleteAllTasks() should remove all tasks!",
            0,
            testedObject.size());
    }


    /**
     * Tests if deleting non existing tasks throws no exception when using
     * "Delete All".
     */
    @Test
    public void testDeletingAllTasksNonExisting()
    {
        testedObject.addEventListeners();

        try {
            testedObject.deleteAllTasks();
        } catch (Exception ex) {
            fail("Deleting all cron tabs should not throw an exception!");
        }
    }


    /**
     * Tests if the schedule text contains all registered schedules and has the
     * correct number of lines.
     */
    @Test
    public void testScheduleGetter()
    {
        testedObject.addEventListeners();
        addRandomNumberOfTasks();

        final String scheduleText = testedObject.getAsPlainText();

        for (int i = 0; i < testedObject.size(); i++)
            assertTrue("The method getRestText() should return all tasks!",
                       scheduleText.contains(String.format(RANDOM_CRON_TAB, i)));
    }


    /**
     * Tests if the schedule text is empty if no tasks are scheduled.
     */
    @Test
    public void testScheduleGetterEmpty()
    {
        testedObject.addEventListeners();
        final SchedulerResponse scheduleJson = testedObject.getAsJson(null);
        assertTrue("The method getRestText() should return no tasks, if none were added!",
                   scheduleJson.getScheduledHarvests().isEmpty());
    }


    /**
     * Tests if the scheduler tasks are removed when the application context is destroyed.
     */
    @Test
    public void testTaskRemovalOnContextDestruction()
    {
        testedObject.addEventListeners();
        addRandomNumberOfTasks();
        EventSystem.sendEvent(new ContextDestroyedEvent());

        assertEquals("The " + ContextDestroyedEvent.class.getSimpleName() + " should cause all tasks to be deleted!",
                     0,
                     testedObject.size());
    }


    /**
     * Tests if the scheduler no longer receives
     * events when the application context is destroyed.
     */
    @Test
    public void testForNoEventsOnContextDestruction()
    {
        testedObject.addEventListeners();
        EventSystem.sendEvent(new ContextDestroyedEvent());

        try {
            addTasks(1);
        } catch (IllegalStateException e) {
            return;
        }

        fail("Tasks must not be added after the " + ContextDestroyedEvent.class.getSimpleName() + " was sent!");
    }


    //////////////////////
    // Non-test Methods //
    //////////////////////

    /**
     * Schedules a specified number of tasks.
     *
     * @param count the number of tasks to schedule
     */
    private void addTasks(int count)
    {
        for (int i = 0; i < count; i++) {
            final String randomCron = String.format(RANDOM_CRON_TAB, i);
            testedObject.addTask(new ChangeSchedulerRequest(randomCron));
        }
    }


    /**
     * Schedules 1-10 tasks.
     */
    private void addRandomNumberOfTasks()
    {
        addTasks(1 + random.nextInt(10));
    }
}
