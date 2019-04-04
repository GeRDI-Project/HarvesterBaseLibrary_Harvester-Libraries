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

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import de.gerdiproject.harvest.rest.AbstractRestResource;
import de.gerdiproject.harvest.rest.HttpResponseFactory;
import de.gerdiproject.harvest.scheduler.Scheduler;
import de.gerdiproject.harvest.scheduler.constants.SchedulerConstants;
import de.gerdiproject.harvest.scheduler.events.GetSchedulerEvent;
import de.gerdiproject.harvest.scheduler.json.ChangeSchedulerRequest;

/**
 * This class serves as a REST interface to the {@linkplain Scheduler}.
 *
 * @author Robin Weiss
 */
@Path("schedule")
public class SchedulerRestResource extends AbstractRestResource<Scheduler, GetSchedulerEvent>
{
    public SchedulerRestResource()
    {
        super();
    }


    /**
     * Adds a harvesting task with a specified cron tab to the schedule.
     *
     * @param crontab a cron tab describing when a harvest should take place
     *
     * @return an info message explaining if the operation was successful or not
     */
    @Path("_add")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response addTask(String crontab)
    {
        return changeObject(restObject::addTask, crontab, ChangeSchedulerRequest.class);
    }


    /**
     * Deletes a harvesting task with a specified cron tab from the schedule.
     *
     * @param crontab the cron tab that is to be removed
     *
     * @return an info message explaining if the operation was successful or not
     */
    @Path("_delete")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteTask(String crontab)
    {
        return changeObject(restObject::deleteTask, crontab, ChangeSchedulerRequest.class);
    }


    /**
     * Deletes a harvesting task with a specified cron tab from the schedule.
     *
     * @return an info message explaining if the operation was successful or not
     */

    @Path("_deleteAll")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteAllTasks()
    {
        return HttpResponseFactory.createOkResponse(restObject.deleteAllTasks());
    }


    /* (non-Javadoc)
     * @see de.gerdiproject.harvest.rest.AbstractFacade#getAllowedRequests()
     */
    @Override
    protected String getAllowedRequests()
    {
        return SchedulerConstants.ALLOWED_REQUESTS;
    }
}
