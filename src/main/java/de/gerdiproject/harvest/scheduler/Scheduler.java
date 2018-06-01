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

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import de.gerdiproject.harvest.application.events.ContextDestroyedEvent;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.IEventListener;
import de.gerdiproject.harvest.scheduler.constants.SchedulerConstants;
import de.gerdiproject.harvest.scheduler.events.AddSchedulerTaskEvent;
import de.gerdiproject.harvest.scheduler.events.DeleteSchedulerTaskEvent;
import de.gerdiproject.harvest.scheduler.events.GetScheduleEvent;
import de.gerdiproject.harvest.scheduler.events.ScheduledTaskExecutedEvent;
import de.gerdiproject.harvest.scheduler.utils.CronUtils;
import de.gerdiproject.harvest.utils.data.DiskIO;

/**
 * This class manages a schedule of cron tabs that trigger harvests. The schedule is saved to and loaded from disk.
 *
 * @author Robin Weiss
 */
public class Scheduler implements IEventListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Scheduler.class);
    private Timer timer;

    private final Map<String, TimerTask> registeredTasks;
    private final DiskIO diskIo;
    private final String cacheFilePath;


    /**
     * Constructor that initializes the timer and task registry.
     *
     * @param cacheFilePath the path to the cache file in which
     *         the JSON representation of this class is cached
     */
    public Scheduler(String cacheFilePath)
    {
        this.timer = new Timer();
        this.registeredTasks = new ConcurrentHashMap<>();
        this.diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        this.cacheFilePath = cacheFilePath;
    }


    @Override
    public void addEventListeners()
    {
        EventSystem.addSynchronousListener(AddSchedulerTaskEvent.class, this::onAddTask);
        EventSystem.addSynchronousListener(DeleteSchedulerTaskEvent.class, this::onDeleteTask);
        EventSystem.addSynchronousListener(GetScheduleEvent.class, this::onGetSchedule);
        EventSystem.addListener(ScheduledTaskExecutedEvent.class, onTaskExecuted);
        EventSystem.addListener(ContextDestroyedEvent.class, onContextDestroyed);
    }


    @Override
    public void removeEventListeners()
    {
        EventSystem.removeSynchronousListener(AddSchedulerTaskEvent.class);
        EventSystem.removeSynchronousListener(DeleteSchedulerTaskEvent.class);
        EventSystem.removeSynchronousListener(GetScheduleEvent.class);
        EventSystem.removeListener(ScheduledTaskExecutedEvent.class, onTaskExecuted);
        EventSystem.removeListener(ContextDestroyedEvent.class, onContextDestroyed);
    }


    /**
     * Returns the number of registered cron tabs.
     *
     * @return the number of registered cron tabs
     */
    public int size()
    {
        return registeredTasks.size();
    }


    /**
     * Attempts to load a schedule from disk.
     */
    public void loadFromDisk()
    {
        String[] cachedCronTabs = diskIo.getObject(cacheFilePath, String[].class);

        if (cachedCronTabs != null) {

            registeredTasks.clear();

            for (String cronTab : cachedCronTabs) {
                try {
                    scheduleTask(cronTab);
                } catch (IllegalArgumentException e) {
                    LOGGER.error(String.format(SchedulerConstants.ERROR_LOAD, cronTab), e);
                }
            }

            LOGGER.info(SchedulerConstants.LOAD_OK);
        }
    }


    /**
     * Saves all cron tabs to disk as a JSON array.
     */
    private void saveToDisk()
    {
        diskIo.writeObjectToFile(cacheFilePath, registeredTasks.keySet());
    }


    /**
     * Attempts to parse a specified cronTab and schedules a harvesting task to be executed at the earliest matching
     * date.
     *
     * @param cronTab the cron tab describing when the task is to be executed
     *
     * @throws IllegalArgumentException thrown when the cron tab could not be parsed
     */
    private void scheduleTask(String cronTab) throws IllegalArgumentException
    {
        final TimerTask oldTask = registeredTasks.get(cronTab);

        // cancel old task, just in case it is still running
        if (oldTask != null)
            oldTask.cancel();

        // calculate next date
        final Date nextDate = CronUtils.getNextMatchingDate(cronTab);

        // start and register the task
        final HarvestingTimerTask harvestingTask = new HarvestingTimerTask();
        timer.schedule(harvestingTask, nextDate);
        registeredTasks.put(cronTab, harvestingTask);

        LOGGER.debug(String.format(SchedulerConstants.NEXT_DATE, cronTab, nextDate.toString()));
    }


    /**
     * Reschedules a task, calculating a next fitting date.
     *
     * @param rescheduledTask the task that is to be rescheduled
     */
    private void rescheduleTask(TimerTask rescheduledTask)
    {
        registeredTasks.forEach((String cronTab, TimerTask task) -> {
            if (task == rescheduledTask)
            {
                try {
                    scheduleTask(cronTab);
                } catch (IllegalArgumentException e) {
                    LOGGER.error(
                        String.format(SchedulerConstants.ERROR_RESCHEDULE, cronTab),
                        e);
                }
            }
        });
    }


    /**
     * Removes event listeners, cancels the timer and removes
     * all registered tasks.
     */
    private void destroy()
    {
        removeEventListeners();

        // stop all running task threads
        timer.cancel();
        timer.purge();
        registeredTasks.clear();
    }


    //////////////////////////////
    // Event Callback Functions //
    //////////////////////////////


    /**
     * Event callback for adding a task.
     *
     * @param event the event that triggered the callback
     *
     * @throws IllegalArgumentException thrown if the cron tab is invalid or already exists
     * @return a feedback message
     */
    private String onAddTask(AddSchedulerTaskEvent event) throws IllegalArgumentException
    {
        final String cronTab = event.getCronTab();

        if (cronTab == null)
            throw new IllegalArgumentException(SchedulerConstants.ERROR_ADD_NULL);

        // check for duplicate cron tabs
        if (registeredTasks.containsKey(cronTab))
            throw new IllegalArgumentException(String.format(SchedulerConstants.ERROR_ADD_ALREADY_EXISTS, cronTab));

        scheduleTask(cronTab);

        // save the updated schedule
        saveToDisk();

        return String.format(SchedulerConstants.ADD_OK, event.getCronTab());
    }


    /**
     * Event call back for deleting a task.
     *
     * @param event the event that triggered the callback
     *
     * @throws IllegalArgumentException thrown if the cron tab is not registered
     *
     * @return a feedback message
     */
    private String onDeleteTask(DeleteSchedulerTaskEvent event) throws IllegalArgumentException
    {
        final String cronTab = event.getCronTab();

        // delete all tasks?
        if (cronTab == null) {
            final int oldNumberOfTasks = registeredTasks.size();
            timer.cancel();
            timer.purge();
            registeredTasks.clear();
            saveToDisk();
            timer = new Timer();

            return String.format(SchedulerConstants.DELETE_ALL, oldNumberOfTasks);
        }

        // check if id is within bounds
        if (!registeredTasks.containsKey(cronTab))
            throw new IllegalArgumentException(String.format(SchedulerConstants.DELETE_FAILED, cronTab));

        // remove and cancel task
        final TimerTask removedTask = registeredTasks.remove(cronTab);
        removedTask.cancel();

        // save the updated schedule
        saveToDisk();

        return String.format(SchedulerConstants.DELETE_OK, cronTab);
    }


    /**
     * Event callback for retrieving one or all cron tabs.
     *
     * @param event the event that triggered the callback
     *
     * @return one cron tab or all, separated by linebreaks
     */
    private String onGetSchedule(GetScheduleEvent event) // NOPMD event payloads must always exist
    {
        final StringBuilder sb = new StringBuilder();

        for (String cronTab : registeredTasks.keySet())
            sb.append(cronTab).append('\n');

        return sb.toString();
    }


    /**
     * Event callback that is called when a scheduled task is executed. Reschedules the excuted task with an updated
     * date.
     *
     * @param event the event that triggered the callback
     */
    private final Consumer<ScheduledTaskExecutedEvent> onTaskExecuted = (ScheduledTaskExecutedEvent event) -> {
        rescheduleTask(event.getExecutedTask());
    };


    /**
     * Event callback that is called when the harvester is undeployed from the server. Cancels all timer tasks and their
     * threads.
     *
     * @param event the event that triggered the callback
     */
    private final Consumer<ContextDestroyedEvent> onContextDestroyed = (ContextDestroyedEvent event) -> {
        destroy();
    };
}
