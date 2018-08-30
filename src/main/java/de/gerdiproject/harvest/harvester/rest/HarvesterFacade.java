/**
 * Copyright © 2017 Robin Weiss (http://www.gerdi-project.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.gerdiproject.harvest.harvester.rest;


import java.util.Arrays;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import de.gerdiproject.harvest.application.MainContext;
import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.harvester.constants.HarvesterConstants;
import de.gerdiproject.harvest.harvester.events.GetMaxDocumentCountEvent;
import de.gerdiproject.harvest.state.StateMachine;
import de.gerdiproject.harvest.utils.ServerResponseFactory;
import de.gerdiproject.harvest.utils.logger.HarvesterLog;
import de.gerdiproject.harvest.utils.logger.events.GetMainLogEvent;


/**
 * A facade for the harvester that serves as a RESTful interface. It provides
 * REST requests that manipulate the harvester in order to prepare and send
 * search indices to Elastic Search.
 *
 * @see de.gerdiproject.harvest.harvester.AbstractHarvester
 * @author Robin Weiss
 */
@Path("")
public class HarvesterFacade
{
    /**
     * Starts a harvest using the harvester that is registered in the
     * MainContext.
     *
     * @param formParams optional parameters encompass "from" and "to" to set
     *            the harvest range
     * @return a status string that describes the success or failure of the
     *         harvest
     */
    @POST
    @Produces({
        MediaType.TEXT_PLAIN
    })
    public Response startHarvest(final MultivaluedMap<String, String> formParams)
    {
        return StateMachine.getCurrentState().startHarvest();
    }


    /**
     * Displays a text that describes the status and possible REST calls.
     *
     * @return a text describing the harvesting status and available RESTful
     *         calls
     */
    @GET
    @Produces({
        MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON
    })
    public String getInfo()
    {
        final String status = StateMachine.getCurrentState().getStatusString();

        // get harvesting range
        String from = HarvesterConstants.UNKNOWN_NUMBER;
        String to = HarvesterConstants.UNKNOWN_NUMBER;
        final Configuration config = MainContext.getConfiguration();

        if (config != null) {
            // get the range specified in the config
            from = config.getParameterStringValue(HarvesterConstants.START_INDEX_PARAM.getCompositeKey());
            to = config.getParameterStringValue(HarvesterConstants.END_INDEX_PARAM.getCompositeKey());

            // add the real expected number of max documents from the main harvester
            if (to.equals(ConfigurationConstants.INTEGER_VALUE_MAX)) {
                Integer maxDocs = EventSystem.sendSynchronousEvent(new GetMaxDocumentCountEvent());

                if (maxDocs != null && maxDocs > 0) {
                    try {
                        int maxRange = Integer.parseInt(from) + maxDocs;
                        to = String.format(HarvesterConstants.MAX_RANGE_NUMBER, maxRange);
                    } catch (NumberFormatException e) { // NOPMD - just leave the String how it was before

                    }
                }
            }
        }

        // get harvester name
        final String name = MainContext.getModuleName();

        return String.format(HarvesterConstants.REST_INFO, name, status, from, to);
    }


    /**
     * Checks if the harvester should be triggered again.
     *
     * @return true if the harvested data is outdated
     */
    @GET
    @Path("outdated")
    @Produces({
        MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON
    })
    public Response isOutdated()
    {
        return StateMachine.getCurrentState().isOutdated();
    }


    /**
     * Aborts an ongoing process, such as harvesting, submitting, or saving.
     *
     * @return a status message describing if the abort could be started or not
     */
    @POST
    @Path("abort")
    @Produces({
        MediaType.TEXT_PLAIN
    })
    public Response abort()
    {
        return StateMachine.getCurrentState().abort();
    }


    /**
     * Saves harvested documents to disk.
     *
     * @return a status message describing if the saving could be started or not
     */
    @GET
    @Path("download")
    @Produces({
        MediaType.TEXT_PLAIN
    })
    public Response saveDocuments()
    {
        return StateMachine.getCurrentState().save();
    }


    /**
     * Submits harvested documents.
     *
     * @return a status message describing if the submission could be started or
     *         not
     */
    @POST
    @Path("submit")
    @Produces({
        MediaType.TEXT_PLAIN
    })
    public Response submitDocuments()
    {
        return StateMachine.getCurrentState().submit();
    }


    /**
     * Attempts to completely reset the harvester service
     *
     * @return a status message
     */
    @POST
    @Path("reset")
    @Produces({
        MediaType.TEXT_PLAIN
    })
    public Response reset()
    {
        return StateMachine.getCurrentState().reset();
    }


    /**
     * Attempts to retrieve the log of the harvester service.
     *
     * @param dateString the log dates in YYYY-MM-DD format of the log messages as comma
     *         separated string, or null if this filter should not be applied
     * @param levelString the log levels of the log messages as comma separated string,
     *         or null if this filter should not be applied
     * @param classString the logger names of the log messages as comma separated string,
     *         or null if this filter should not be applied
     *
     * @return a the log of the harvester service
     */
    @GET
    @Path("log")
    @Produces({
        MediaType.TEXT_PLAIN
    })
    public Response getLog(@QueryParam("date") String dateString, @QueryParam("level") String levelString, @QueryParam("class") String classString)
    {
        final List<String> dateFilters = dateString == null ? null : Arrays.asList(dateString.split(","));
        final List<String> levelFilters = levelString == null ? null : Arrays.asList(levelString.split(","));
        final List<String> classFilters = classString == null ? null : Arrays.asList(classString.split(","));

        final HarvesterLog mainLog = EventSystem.sendSynchronousEvent(new GetMainLogEvent());

        if (mainLog == null)
            return ServerResponseFactory.createServerErrorResponse();

        final String log = mainLog.getLog(dateFilters, levelFilters, classFilters);

        if (log == null)
            return ServerResponseFactory.createServerErrorResponse();
        else
            return ServerResponseFactory.createOkResponse(log);
    }
}
