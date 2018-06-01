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
package de.gerdiproject.harvest.scheduler.constants;

import de.gerdiproject.harvest.utils.cache.constants.CacheConstants;

/**
 * This class offers constants that are related to scheduling.
 *
 * @author Robin Weiss
 */
public class SchedulerConstants
{
    public static final String TASK_MESSAGE = "Scheduler attempts to start a harvest: %s";
    public static final String DELETE_OK = "Removed task: %s";
    public static final String DELETE_FAILED = "Cannot remove task, because it does not exist: %s!";
    public static final String DELETE_ALL = "Deleted all %d scheduled tasks!";

    public static final String ADD_OK = "Successfully added task: %s";
    public static final String ERROR_ADD_ALREADY_EXISTS = "Cannot add task, because it already exists: %s";
    public static final String ERROR_ADD_NULL =
        "Cannot add task. You must specify a valid cron tab as a query parameter 'cron'!";

    public static final String HARVESTING_TASK = "%s Harvesting";
    public static final String TASK_ENTRY = "%d. %s%n";
    public static final String REST_INFO = "- %s Schedule -%n%n"
                                           + "Active Harvesting Schedules:%n"
                                           + "%s%n%n"
                                           + "POST?cron=XXX    Adds a new harvest task with the cron tab XXX%n"
                                           + "DELETE?cron=XXX  Deletes the harvest task with the cron tab XXX%n"
                                           + "DELETE           Deletes all harvest tasks";

    public static final String CACHE_PATH = CacheConstants.CACHE_FOLDER_PATH + "schedule.json";
    public static final String ERROR_RESCHEDULE = "Cannot re-schedule task: %s";
    public static final String LOAD_OK = "Successfully loaded schedule from disk!";
    public static final String ERROR_LOAD = "Cannot load cron tab from disk: %s";
    public static final String NEXT_DATE = "Scheduled Tak '%s' will be next executed at %s";


    /**
     * Private constructor, because this is just a collection of constants.
     */
    private SchedulerConstants()
    {
    }
}
