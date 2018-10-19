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
package de.gerdiproject.harvest.etls.rest;


import java.time.Instant;
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

import de.gerdiproject.harvest.etls.constants.ETLConstants;
import de.gerdiproject.harvest.etls.enums.ETLStatus;
import de.gerdiproject.harvest.etls.events.GetETLRegistryEvent;
import de.gerdiproject.harvest.etls.utils.ETLRegistry;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.rest.AbstractRestResource;
import de.gerdiproject.harvest.rest.HttpResponseFactory;
import de.gerdiproject.harvest.state.StateMachine;
import de.gerdiproject.harvest.state.constants.StateConstants;
import de.gerdiproject.harvest.state.events.AbortingStartedEvent;
import de.gerdiproject.harvest.utils.logger.HarvesterLog;
import de.gerdiproject.harvest.utils.logger.events.GetMainLogEvent;
import de.gerdiproject.harvest.utils.maven.MavenUtils;
import de.gerdiproject.harvest.utils.maven.constants.MavenConstants;
import de.gerdiproject.harvest.utils.maven.events.GetMavenUtilsEvent;


/**
 * A facade for the harvester that serves as a RESTful interface. It provides
 * REST requests that manipulate the harvester in order to prepare and send
 * search indices to Elastic Search.
 *
 * @see de.gerdiproject.harvest.etls.AbstractETL
 * @author Robin Weiss
 */
@Path("")
public class ETLRestResource extends AbstractRestResource<ETLRegistry, GetETLRegistryEvent>
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
        try {
            // start a harvest
            restObject.harvest();
            return HttpResponseFactory.createAcceptedResponse(StateConstants.HARVEST_STARTED);
        } catch (Exception e) {
            return HttpResponseFactory.createBadRequestResponse(e.getMessage());
        }
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
        return HttpResponseFactory.createOkResponse(restObject.hasOutdatedETLs());
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
        if (restObject.getStatus() == ETLStatus.HARVESTING) {
            EventSystem.sendEvent(new AbortingStartedEvent());
            restObject.abortHarvest();


            return HttpResponseFactory.createAcceptedResponse(
                       String.format(StateConstants.ABORT_STATUS, ETLStatus.HARVESTING.toString()));
        } else {
            final String message = String.format(
                                       StateConstants.CANNOT_ABORT_PREFIX
                                       + StateConstants.NO_HARVEST_IN_PROGRESS,
                                       StateConstants.HARVESTING_PROCESS);
            return HttpResponseFactory.createBadRequestResponse(message);
        }
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
            return HttpResponseFactory.createServerErrorResponse();

        final String log = mainLog.getLog(dateFilters, levelFilters, classFilters);

        if (log == null)
            return HttpResponseFactory.createServerErrorResponse();
        else
            return HttpResponseFactory.createOkResponse(log);
    }


    /**
    * Retrieves a formatted timestamp of the time at which the harvest started,
    * or "N/A" if no harvest was started yet.
    *
    * @return a formatted timestamp or "N/A" if no harvest was started yet
    */
    @GET
    @Path("harvest-timestamp")
    @Produces({
        MediaType.TEXT_PLAIN
    })
    public Response getHarvestStartTimestamp()
    {
        long timestamp = restObject.getLatestHarvestTimestamp();

        if (timestamp == -1L)
            return HttpResponseFactory.createBadRequestResponse();

        else
            return HttpResponseFactory.createOkResponse(Instant.ofEpochMilli(timestamp).toString());
    }


    /**
     * Displays the artifactIds and versions of GeRDI Maven libraries used in this service.
     *
     * @return artifactIds and versions of GeRDI Maven libraries used in this service.
     */
    @GET
    @Path("versions")
    @Produces({
        MediaType.TEXT_PLAIN
    })
    public Response getVersions()
    {
        final String versions = getSpecifiedVersions(MavenConstants.DEFAULT_GERDI_NAMESPACE);

        if (versions == null)
            return HttpResponseFactory.createUnknownErrorResponse();
        else
            return HttpResponseFactory.createOkResponse(versions);
    }


    /**
     * Displays the artifactIds and versions of all Maven libraries used in this service.
     *
     * @return artifactIds and versions of all Maven libraries used in this service.
     */
    @GET
    @Path("versions-all")
    @Produces({
        MediaType.TEXT_PLAIN
    })
    public Response getAllVersions()
    {
        final String versions = getSpecifiedVersions(null);

        if (versions == null)
            return HttpResponseFactory.createUnknownErrorResponse();
        else
            return HttpResponseFactory.createOkResponse(versions);
    }


    /**
     * Retrieves filtered dependencies and the main jar as a linebreak separated list.
     * The first entry is always the main jar and is separated by a double linebreak.
     *
     * @param filter a groupId that can be used to filter maven dependencies
     *
     * @return dependencies and the main jar as a linebreak separated list
     */
    private String getSpecifiedVersions(String filter)
    {
        final MavenUtils utils = EventSystem.sendSynchronousEvent(new GetMavenUtilsEvent());

        if (utils == null)
            return null;

        final String mainJar = utils.getHarvesterJarName();

        if (mainJar == null)
            return null;

        final List<String> dependencyList = utils.getMavenVersionInfo(filter);

        if (dependencyList == null)
            return null;

        // remove jar from dependencies. it's dealt with in a special manner
        dependencyList.remove(mainJar);

        return String.format(
                   MavenConstants.DEPENDENCY_LIST_FORMAT,
                   mainJar,
                   String.join("\n", dependencyList));
    }


    @Override
    protected String getAllowedRequests()
    {
        return ETLConstants.ALLOWED_REQUESTS;
    }
}
