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
package de.gerdiproject.harvest.scheduler.rest;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.scheduler.Scheduler;
import de.gerdiproject.harvest.scheduler.constants.SchedulerConstants;
import de.gerdiproject.harvest.scheduler.events.AddSchedulerTaskEvent;
import de.gerdiproject.harvest.scheduler.events.DeleteSchedulerTaskEvent;
import de.gerdiproject.harvest.scheduler.events.GetScheduleEvent;
import de.gerdiproject.harvest.state.constants.StateConstants;

/**
 * This class serves as a REST interface to the {@linkplain Scheduler}.
 *
 * @author Robin Weiss
 */
@Path("schedule")
public class SchedulerFacade
{

    /**
     * Displays a text that describes the current schedule.
     *
     * @return a text describing the current schedule status and available RESTful calls
     */
    @GET
    @Produces({
        MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON
    })
    public String getInfo()
    {
        String schedule = EventSystem.sendSynchronousEvent(new GetScheduleEvent());

        if (schedule == null)
            schedule = StateConstants.INIT_STATUS;
        else if (schedule.isEmpty())
            schedule = "-";

        return String.format(SchedulerConstants.REST_INFO, MainContext.getModuleName(), schedule);
    }


    /**
     * Adds a harvesting task with a specified cron tab to the schedule.
     *
     * @param cronTab a cron tab describing when a harvest should take place
     *
     * @return an info message explaining if the operation was successful or not
     */
    @POST
    @Produces({
        MediaType.TEXT_PLAIN
    })
    public String addTask(@QueryParam("cron") String cronTab)
    {
        final String response = EventSystem.sendSynchronousEvent(
                                    new AddSchedulerTaskEvent(cronTab));

        if (response == null)
            return StateConstants.INIT_IN_PROGRESS;
        else
            return response;
    }


    /**
     * Deletes a harvesting task with a specified cron tab from the schedule.
     *
     * @param cronTab the cron tab that is to be removed
     *
     * @return an info message explaining if the operation was successful or not
     */
    @DELETE
    @Produces({
        MediaType.TEXT_PLAIN
    })
    public String deleteTask(@QueryParam("cron") String cronTab)
    {
        final String response = EventSystem.sendSynchronousEvent(
                                    new DeleteSchedulerTaskEvent(cronTab));

        if (response == null)
            return StateConstants.INIT_IN_PROGRESS;
        else
            return response;
    }
}
