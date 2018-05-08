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

import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.application.events.ContextDestroyedEvent;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.scheduler.constants.SchedulerConstants;
import de.gerdiproject.harvest.scheduler.events.AddSchedulerTaskEvent;
import de.gerdiproject.harvest.scheduler.events.DeleteSchedulerTaskEvent;
import de.gerdiproject.harvest.scheduler.events.GetScheduleEvent;
import de.gerdiproject.harvest.scheduler.events.ScheduledTaskExecutedEvent;
import de.gerdiproject.harvest.scheduler.utils.CronUtils;
import de.gerdiproject.harvest.utils.ServerResponseFactory;
import de.gerdiproject.harvest.utils.data.DiskIO;

/**
 * This class manages a schedule of cron tabs that trigger harvests. The schedule is saved to and loaded from disk.
 *
 * @author Robin Weiss
 */
public class Scheduler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Scheduler.class);
    private Timer timer;
    private final Map<String, TimerTask> registeredTasks;


    /**
     * Constructor that initializes the timer and task registry.
     */
    public Scheduler()
    {
        this.timer = new Timer();
        this.registeredTasks = new ConcurrentHashMap<>();
    }


    /**
     * Initializes the scheduler by adding event listeners and loading cached schedules from disk.
     */
    public void init()
    {
        EventSystem.addSynchronousListener(AddSchedulerTaskEvent.class, this::onAddTask);
        EventSystem.addSynchronousListener(DeleteSchedulerTaskEvent.class, this::onDeleteSchedule);
        EventSystem.addSynchronousListener(GetScheduleEvent.class, this::onGetSchedule);
        EventSystem.addListener(ScheduledTaskExecutedEvent.class, this::onTaskExecuted);
        EventSystem.addListener(ContextDestroyedEvent.class, this::onContextDestroyed);

        loadFromDisk();
    }


    /**
     * Attempts to load a schedule from disk.
     */
    private void loadFromDisk()
    {
        final DiskIO diskReader = new DiskIO();
        String[] cachedCronTabs = diskReader.getObject(
                                      String.format(SchedulerConstants.CACHE_PATH, MainContext.getModuleName()),
                                      String[].class);

        if (cachedCronTabs != null) {
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
        final DiskIO diskWriter = new DiskIO();
        diskWriter.writeObjectToFile(
            String.format(SchedulerConstants.CACHE_PATH, MainContext.getModuleName()),
            registeredTasks.keySet());
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


    //////////////////////////////
    // Event Callback Functions //
    //////////////////////////////


    /**
     * Event callback for adding a task.
     *
     * @param event the event that triggered the callback
     *
     * @return an HTTP response
     */
    private Response onAddTask(AddSchedulerTaskEvent event)
    {
        try {
            final String cronTab = event.getCronTab();

            if (cronTab == null)
                throw new IllegalArgumentException(SchedulerConstants.ERROR_ADD_NULL);

            // check for duplicate cron tabs
            if (registeredTasks.containsKey(cronTab))
                throw new IllegalArgumentException(String.format(SchedulerConstants.ERROR_ADD_ALREADY_EXISTS, cronTab));

            scheduleTask(cronTab);

        } catch (IllegalArgumentException e) {
            return ServerResponseFactory.createBadRequestResponse(e.getMessage());
        }

        // save the updated schedule
        saveToDisk();

        return ServerResponseFactory.createResponse(
                   Status.CREATED,
                   String.format(SchedulerConstants.ADD_OK, event.getCronTab()));
    }


    /**
     * Event call back for deleting a task.
     *
     * @param event the event that triggered the callback
     *
     * @return an HTTP response to the request
     */
    private Response onDeleteSchedule(DeleteSchedulerTaskEvent event)
    {
        final String cronTab = event.getCronTab();

        // delete all tasks?
        if (cronTab == null) {
            timer.cancel();
            timer.purge();
            registeredTasks.clear();
            saveToDisk();
            timer = new Timer();

            return ServerResponseFactory.createOkResponse(SchedulerConstants.DELETE_ALL);
        }

        // check if id is within bounds
        if (!registeredTasks.containsKey(cronTab))
            return ServerResponseFactory.createBadRequestResponse(
                       String.format(SchedulerConstants.DELETE_FAILED, cronTab)
                   );

        // remove and cancel task
        final TimerTask removedTask = registeredTasks.remove(cronTab);
        removedTask.cancel();

        // save the updated schedule
        saveToDisk();

        return ServerResponseFactory.createOkResponse(
                   String.format(SchedulerConstants.DELETE_OK, cronTab)
               );
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
    private void onTaskExecuted(ScheduledTaskExecutedEvent event)
    {
        registeredTasks.forEach((String cronTab, TimerTask task) -> {
            if (task == event.getExecutedTask())
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
     * Event callback that is called when the harvester is undeployed from the server. Cancels all timer tasks and their
     * threads.
     *
     * @param event the event that triggered the callback
     */
    private void onContextDestroyed(ContextDestroyedEvent event)  // NOPMD event payloads must always exist
    {
        // stop all running task threads
        timer.cancel();
        timer.purge();
        registeredTasks.clear();
    }
}
