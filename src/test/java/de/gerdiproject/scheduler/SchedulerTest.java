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
package de.gerdiproject.scheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.File;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.classic.Level;
import de.gerdiproject.harvest.application.events.ContextDestroyedEvent;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.scheduler.Scheduler;
import de.gerdiproject.harvest.scheduler.constants.SchedulerConstants;
import de.gerdiproject.harvest.scheduler.events.AddSchedulerTaskEvent;
import de.gerdiproject.harvest.scheduler.events.DeleteSchedulerTaskEvent;
import de.gerdiproject.harvest.scheduler.events.GetScheduleEvent;
import de.gerdiproject.harvest.utils.FileUtils;
import de.gerdiproject.harvest.utils.logger.constants.LoggerConstants;

/**
 * This class contains unit tests for the {@linkplain Scheduler}.
 *
 * @author Robin Weiss
 */
public class SchedulerTest
{
    private static final String INVALID_CRON = "abc";
    private static final String RANDOM_CRON_TAB = "%d 0 1 1 *";
    private static final String SOME_CRON_TAB = "0 0 1 1 *";
    private static final File CACHE_FILE = new File(
        "test/" + String.format(
            SchedulerConstants.CACHE_PATH,
            "schedulerTest"));

    private Scheduler scheduler;


    /**
     * Creates a new scheduler.
     */
    @Before
    public void before()
    {
        LoggerConstants.ROOT_LOGGER.setLevel(Level.ERROR);

        scheduler = new Scheduler(CACHE_FILE.getAbsolutePath());
        scheduler.addEventListeners();
    }


    /**
     * Deletes the cache directory of test files and clears the scheduler.
     */
    @After
    public void after()
    {
        FileUtils.deleteFile(CACHE_FILE);
        scheduler.removeEventListeners();
        scheduler = null;
        LoggerConstants.ROOT_LOGGER.setLevel(Level.DEBUG);
    }


    /**
     * Tests if a cache file is created when a new task is added.
     */
    @Test
    public void testSavingToDisk()
    {
        assert !CACHE_FILE.exists();

        addTasks(1);

        assert CACHE_FILE.exists();
        assertNotEquals(0, CACHE_FILE.length());
    }


    /**
     * Tests if loading from disk fills up the correct
     * number of scheduled tasks.
     */
    @Test
    public void testLoadingFromDisk()
    {
        // add some tasks
        addTasks(new Random().nextInt() % 10);
        int numberOfSavedTasks = scheduler.size();

        scheduler = new Scheduler(CACHE_FILE.getAbsolutePath());
        scheduler.addEventListeners();

        assertEquals(0, scheduler.size());

        scheduler.loadFromDisk();
        assertEquals(numberOfSavedTasks, scheduler.size());
    }


    /**
     * Tests if loading from disk causes no exceptions if
     * no cache file exists and will not change the scheduled tasks.
     */
    @Test
    public void testLoadingFromDiskNoExists()
    {
        // add some tasks
        addTasks(Math.abs(new Random().nextInt()) % 10);
        int oldSize = scheduler.size();

        // delete cache to guarantee it no longer exists
        FileUtils.deleteFile(CACHE_FILE);
        assert !CACHE_FILE.exists();

        try {
            scheduler.loadFromDisk();
        } catch (Exception ex) {
            assert false;
        }

        assertEquals(oldSize, scheduler.size());
    }


    /**
     * Tests if adding a random number of tasks affects
     * the scheduled task count accordingly.
     */
    @Test
    public void testAddingTask()
    {
        int numberOfTasks = Math.abs(new Random().nextInt()) % 10;
        addTasks(numberOfTasks);
        assertEquals(numberOfTasks, scheduler.size());
    }


    /**
     * Tests if adding the same cron tab twice throws an exception.
     */
    @Test
    public void testAddingTaskTwice()
    {
        try {
            EventSystem.sendSynchronousEvent(new AddSchedulerTaskEvent(SOME_CRON_TAB));
            EventSystem.sendSynchronousEvent(new AddSchedulerTaskEvent(SOME_CRON_TAB));
            assert false;
        } catch (Exception ex) {
            assert ex instanceof IllegalArgumentException;
        }
    }


    /**
     * Tests if adding an invalid cron tab throws an exception.
     */
    @Test
    public void testAddingTaskInvalidCronTab()
    {
        try {
            EventSystem.sendSynchronousEvent(new AddSchedulerTaskEvent(INVALID_CRON));
            assert false;
        } catch (Exception ex) {
            assert ex instanceof IllegalArgumentException;
        }
    }


    /**
     * Tests if deleting a random number of tasks affects
     * the scheduled task count accordingly.
     */
    @Test
    public void testDeletingTask()
    {
        addTasks(10);
        int numberOfDeletions = 1 + Math.abs(new Random().nextInt()) % 10;

        for (int i = 0; i < numberOfDeletions; i++) {
            final String randomCron = String.format(RANDOM_CRON_TAB, i);
            EventSystem.sendSynchronousEvent(new DeleteSchedulerTaskEvent(randomCron));
        }

        assertEquals(10 - numberOfDeletions, scheduler.size());
    }


    /**
     * Tests if deleteing a non-existing task throws an exception.
     */
    @Test
    public void testDeletingTaskNonExisting()
    {
        try {
            EventSystem.sendSynchronousEvent(new DeleteSchedulerTaskEvent(SOME_CRON_TAB));
            assert false;
        } catch (Exception ex) {
            assert ex instanceof IllegalArgumentException;
        }
    }


    /**
     * Tests if sending a delete event with a null payload removes all tasks.
     */
    @Test
    public void testDeletingAllTasks()
    {
        addTasks(10);
        assertEquals(10, scheduler.size());

        EventSystem.sendSynchronousEvent(new DeleteSchedulerTaskEvent(null));

        assertEquals(0, scheduler.size());
    }


    /**
     * Tests if deleting non existing tasks throws no exception when using
     * "Delete All".
     */
    @Test
    public void testDeletingAllTasksNonExisting()
    {
        assertEquals(0, scheduler.size());

        try {
            EventSystem.sendSynchronousEvent(new DeleteSchedulerTaskEvent(null));
        } catch (Exception ex) {
            assert false;
        }
    }


    /**
     * Tests if the schedule text contains all registered schedules and has the
     * correct number of lines.
     */
    @Test
    public void testScheduleGetter()
    {
        final int addedEvents = 1 + Math.abs(new Random().nextInt()) % 10;
        addTasks(addedEvents);

        final String scheduleText = EventSystem.sendSynchronousEvent(new GetScheduleEvent());

        int numberOfLines = scheduleText.split("\n").length;
        assertEquals(addedEvents, numberOfLines);

        for (int i = 0; i < addedEvents; i++)
            assert scheduleText.contains(String.format(RANDOM_CRON_TAB, i));
    }


    /**
     * Tests if the schedule text is empty if no tasks are scheduled.
     */
    @Test
    public void testScheduleGetterEmpty()
    {
        final String scheduleText = EventSystem.sendSynchronousEvent(new GetScheduleEvent());
        assert scheduleText.isEmpty();
    }


    /**
     * Tests if the scheduler is cleaned up and no longer receives
     * event when the application context is destroyed.
     */
    @Test
    public void testContextDestroyed()
    {
        addTasks(1);
        assertEquals(1, scheduler.size());

        EventSystem.sendEvent(new ContextDestroyedEvent());

        assertEquals(0, scheduler.size());

        addTasks(1);
        assertEquals(0, scheduler.size());
    }


    /**
     * Schedules a specified number of tasks.
     *
     * @param count the number of tasks to schedule
     */
    private void addTasks(int count)
    {
        for (int i = 0; i < count; i++) {
            final String randomCron = String.format(RANDOM_CRON_TAB, i);
            EventSystem.sendSynchronousEvent(new AddSchedulerTaskEvent(randomCron));
        }
    }
}
