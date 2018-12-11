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


import java.util.Arrays;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;

import de.gerdiproject.harvest.application.events.ResetContextEvent;
import de.gerdiproject.harvest.etls.constants.ETLConstants;
import de.gerdiproject.harvest.etls.enums.ETLHealth;
import de.gerdiproject.harvest.etls.enums.ETLState;
import de.gerdiproject.harvest.etls.events.GetETLManagerEvent;
import de.gerdiproject.harvest.etls.json.ETLInfosJson;
import de.gerdiproject.harvest.etls.json.ETLJson;
import de.gerdiproject.harvest.etls.utils.ETLManager;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.rest.AbstractRestResource;
import de.gerdiproject.harvest.rest.HttpResponseFactory;
import de.gerdiproject.harvest.rest.constants.RestConstants;
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
public class ETLRestResource extends AbstractRestResource<ETLManager, GetETLManagerEvent>
{
    /**
     * A HTTP GET request that returns a JSON representation of a single ETL.
     *
     * @param uriInfo an object that can be used to retrieve the path and possible query parameters.
     *
     * @return a JSON object that represents the ETL or an error response describing what went wrong
     */
    @GET
    @Path("etl")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getETLInfo(@Context UriInfo uriInfo)
    {
        // abort if object is not initialized, yet
        if (restObject == null)
            return HttpResponseFactory.createServerErrorResponse();

        // forward GET request to the object
        try {
            final ETLJson responseObject = restObject.getETLAsJson(uriInfo.getQueryParameters());

            return HttpResponseFactory.createOkResponse(gson.toJsonTree(responseObject));
        } catch (Exception e) {
            return HttpResponseFactory.createBadRequestResponse(e.getMessage());
        }
    }


    /**
     * A HTTP GET request that returns a JSON representation of a all ETLs.
     *
     * @return a JSON object that represents all ETLs or an error response describing what went wrong
     */
    @GET
    @Path("etls")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllETLInfos()
    {
        // abort if object is not initialized, yet
        if (restObject == null)
            return HttpResponseFactory.createServerErrorResponse();

        // forward GET request to the object
        try {
            final ETLInfosJson responseObject = restObject.getETLsAsJson();

            return HttpResponseFactory.createOkResponse(gson.toJsonTree(responseObject));
        } catch (Exception e) {
            return HttpResponseFactory.createBadRequestResponse(e.getMessage());
        }
    }


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
        MediaType.APPLICATION_JSON
    })
    public Response startHarvest(final MultivaluedMap<String, String> formParams)
    {
        // abort if object is not initialized, yet
        if (restObject == null)
            return HttpResponseFactory.createServerErrorResponse();

        try {
            // start a harvest
            restObject.harvest();
            return HttpResponseFactory.createAcceptedResponse(ETLConstants.HARVEST_STARTED);
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
        MediaType.APPLICATION_JSON
    })
    public Response isOutdated()
    {
        // abort if object is not initialized, yet
        if (restObject == null)
            return HttpResponseFactory.createServerErrorResponse();

        return HttpResponseFactory.createValueResponse(Status.OK, new JsonPrimitive(restObject.hasOutdatedETLs()));
    }


    /**
     * Aborts an ongoing harvest.
     *
     * @return a status message describing if the abort could be started or not
     */
    @POST
    @Path("abort")
    @Produces({
        MediaType.APPLICATION_JSON
    })
    public Response abort()
    {
        // abort if object is not initialized, yet
        if (restObject == null)
            return HttpResponseFactory.createServerErrorResponse();

        if (restObject.getState() == ETLState.HARVESTING) {
            restObject.abortHarvest();

            return HttpResponseFactory.createAcceptedResponse(ETLConstants.ABORT_START);
        } else
            return HttpResponseFactory.createBadRequestResponse(ETLConstants.ABORT_HARVEST_FAILED_NO_HARVEST);
    }


    /**
     * Attempts to completely reset the harvester service
     *
     * @return a status message
     */
    @POST
    @Path("reset")
    @Produces({
        MediaType.APPLICATION_JSON
    })
    public Response reset()
    {
        EventSystem.sendEvent(new ResetContextEvent());
        return HttpResponseFactory.createAcceptedResponse(
                   RestConstants.RESET_STARTED);
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
            return HttpResponseFactory.createPlainTextOkResponse(log);
    }


    /**
     * Checks the health of the harvester.
     *
     * @return the health of the harvester
     */
    @GET
    @Path("health")
    @Produces({
        MediaType.APPLICATION_JSON
    })
    public Response getHealth()
    {
        // abort if object is not initialized, yet
        if (restObject == null)
            return HttpResponseFactory.createValueResponse(
                       Status.INTERNAL_SERVER_ERROR,
                       new JsonPrimitive(ETLHealth.INITIALIZATION_FAILED.toString()));

        final ETLHealth health = restObject.getHealth();
        final Status status =  health == ETLHealth.OK
                               ? Status.OK
                               : Status.INTERNAL_SERVER_ERROR;

        return HttpResponseFactory.createValueResponse(status, new JsonPrimitive(health.toString()));
    }


    /**
     * Displays the artifactIds and versions of GeRDI Maven libraries used in this service.
     *
     * @return artifactIds and versions of GeRDI Maven libraries used in this service.
     */
    @GET
    @Path("versions")
    @Produces({
        MediaType.APPLICATION_JSON
    })
    public Response getVersions()
    {
        return HttpResponseFactory.createValueResponse(
                   Status.OK,
                   getSpecifiedVersions(MavenConstants.DEFAULT_GERDI_NAMESPACE));
    }


    /**
     * Displays the artifactIds and versions of all Maven libraries used in this service.
     *
     * @return artifactIds and versions of all Maven libraries used in this service.
     */
    @GET
    @Path("versions-all")
    @Produces({
        MediaType.APPLICATION_JSON
    })
    public Response getAllVersions()
    {
        return HttpResponseFactory.createValueResponse(
                   Status.OK,
                   getSpecifiedVersions(null));
    }


    /**
     * Retrieves filtered dependencies and the main jar as a JsonArray.
     * The first entry is always the main jar and is separated by a double linebreak.
     *
     * @param filter a groupId that can be used to filter maven dependencies
     *
     * @return dependencies and the main jar as a JsonArray
     */
    private JsonArray getSpecifiedVersions(String filter)
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

        final JsonArray array = new JsonArray();
        dependencyList.forEach((String d) -> array.add(d));

        return array;
    }


    @Override
    protected String getAllowedRequests()
    {
        return ETLConstants.ALLOWED_REQUESTS;
    }
}
