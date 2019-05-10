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

import javax.ws.rs.core.MultivaluedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.rest.AbstractRestObject;
import de.gerdiproject.harvest.scheduler.constants.SchedulerConstants;
import de.gerdiproject.harvest.scheduler.events.GetSchedulerEvent;
import de.gerdiproject.harvest.scheduler.events.ScheduledTaskExecutedEvent;
import de.gerdiproject.harvest.scheduler.json.ChangeSchedulerRequest;
import de.gerdiproject.harvest.scheduler.json.SchedulerResponse;
import de.gerdiproject.harvest.scheduler.utils.CronUtils;
import de.gerdiproject.harvest.utils.data.DiskIO;
import de.gerdiproject.harvest.utils.file.ICachedObject;

/**
 * This class manages a schedule of cron tabs that trigger harvests. The schedule is saved to and loaded from disk.
 *
 * @author Robin Weiss
 */
public class Scheduler extends AbstractRestObject<Scheduler, SchedulerResponse> implements ICachedObject
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Scheduler.class);
    private Timer timer;

    private final Map<String, TimerTask> registeredTasks;
    private final DiskIO diskIo;
    private final String cacheFilePath;


    /**
     * Constructor that initializes the timer and task registry.
     *
     * @param moduleName the name of the service
     * @param cacheFilePath the path to the cache file in which
     *         the JSON representation of this class is cached
     */
    public Scheduler(final String moduleName, final String cacheFilePath)
    {
        super(moduleName, GetSchedulerEvent.class);

        this.timer = new Timer();
        this.registeredTasks = new ConcurrentHashMap<>();
        this.diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        this.cacheFilePath = cacheFilePath;
    }


    @Override
    public void addEventListeners()
    {
        super.addEventListeners();
        EventSystem.addListener(ScheduledTaskExecutedEvent.class, onTaskExecuted);
    }


    @Override
    public void removeEventListeners()
    {
        super.removeEventListeners();
        EventSystem.removeListener(ScheduledTaskExecutedEvent.class, onTaskExecuted);
    }


    /**
     * Saves all cron tabs to disk as a JSON array.
     */
    @Override
    public void saveToDisk()
    {
        diskIo.writeObjectToFile(cacheFilePath, registeredTasks.keySet());
    }


    /**
     * Attempts to load a schedule from disk.
     */
    @Override
    public void loadFromDisk()
    {
        final String[] cachedCronTabs = diskIo.getObject(cacheFilePath, String[].class);

        if (cachedCronTabs != null) {

            registeredTasks.clear();

            for (final String cronTab : cachedCronTabs) {
                try {
                    scheduleTask(cronTab);
                } catch (final IllegalArgumentException e) {
                    LOGGER.error(String.format(SchedulerConstants.ERROR_LOAD, cronTab), e);
                }
            }

            LOGGER.info(SchedulerConstants.LOAD_OK);
        }
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
     * Returns the date of the earliest scheduled harvest.
     *
     * @return the date of the earliest scheduled harvest,
     * or -1 if nothing was scheduled
     */
    public Date getNextHarvestDate()
    {
        long nextTimestamp = Long.MAX_VALUE;

        for (final TimerTask scheduledTask : registeredTasks.values())
            nextTimestamp = Math.min(nextTimestamp, scheduledTask.scheduledExecutionTime());

        return nextTimestamp == Long.MAX_VALUE
               ? null
               : new Date(nextTimestamp);
    }


    /**
     * Attempts to parse a specified cronTab and schedules a harvesting task to be executed at the earliest matching
     * date.
     *
     * @param cronTab the cron tab describing when the task is to be executed
     *
     * @throws IllegalArgumentException thrown when the cron tab could not be parsed
     * @throws IllegalStateException thrown when the scheduler is being destroyed
     */
    private void scheduleTask(final String cronTab) throws IllegalArgumentException, IllegalStateException
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
    private void rescheduleTask(final TimerTask rescheduledTask)
    {
        registeredTasks.forEach((final String cronTab, final TimerTask task) -> {
            if (task == rescheduledTask)
            {
                try {
                    scheduleTask(cronTab);
                } catch (final IllegalArgumentException e) {
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
    @Override
    protected void destroy()
    {
        super.destroy();

        // stop all running task threads
        timer.cancel();
        timer.purge();
        registeredTasks.clear();
    }


    @Override
    protected String getPrettyPlainText()
    {
        final StringBuilder sb = new StringBuilder();

        if (registeredTasks.isEmpty())
            sb.append('-');
        else {
            for (final String cronTab : registeredTasks.keySet()) {
                if (sb.length() != 0)
                    sb.append('\n');

                sb.append(cronTab);
            }
        }

        sb.insert(0, SchedulerConstants.SCHEDULED_HARVESTS_TITLE);

        return  sb.toString();
    }


    @Override
    public SchedulerResponse getAsJson(final MultivaluedMap<String, String> query)
    {
        return new SchedulerResponse(registeredTasks.keySet());
    }


    public String addTask(final ChangeSchedulerRequest addRequest)
    {
        final String cronTab = addRequest.getCronTab();

        if (cronTab == null)
            throw new IllegalArgumentException(SchedulerConstants.ERROR_SET_NULL);

        // check for duplicate cron tabs
        if (registeredTasks.containsKey(cronTab))
            throw new IllegalArgumentException(String.format(SchedulerConstants.ERROR_ADD_ALREADY_EXISTS, cronTab));

        scheduleTask(cronTab);

        // save the updated schedule
        saveToDisk();

        return String.format(SchedulerConstants.ADD_OK, cronTab);
    }


    /**
     * This method deletes a single crontab.
     *
     * @param deleteRequest a JSON object that transports the cron tab to be deleted
     *
     * @throws IllegalArgumentException thrown if the cron tab is not registered
     *
     * @return a feedback message
     */
    public String deleteTask(final ChangeSchedulerRequest deleteRequest) throws IllegalArgumentException
    {
        final String cronTab = deleteRequest.getCronTab();

        if (cronTab == null)
            throw new IllegalArgumentException(SchedulerConstants.ERROR_SET_NULL);

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
     * Deletes all tasks.
     *
     * @return a feedback message
     */
    public String deleteAllTasks()
    {
        final int oldNumberOfTasks = registeredTasks.size();
        timer.cancel();
        timer.purge();
        registeredTasks.clear();
        saveToDisk();
        timer = new Timer();

        return String.format(SchedulerConstants.DELETE_ALL, oldNumberOfTasks);
    }


    //////////////////////////////
    // Event Callback Functions //
    //////////////////////////////

    /**
     * Event callback that is called when a scheduled task is executed. Reschedules the excuted task with an updated
     * date.
     *
     * @param event the event that triggered the callback
     */
    private final Consumer<ScheduledTaskExecutedEvent> onTaskExecuted = (final ScheduledTaskExecutedEvent event) -> {
        rescheduleTask(event.getExecutedTask());
    };
}
