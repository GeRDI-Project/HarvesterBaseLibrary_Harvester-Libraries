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
package de.gerdiproject.harvest.config.rest;


import java.util.List;
import java.util.Map.Entry;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.constants.ConfigurationConstants;


/**
 * This facade serves as an interface between REST and the {@linkplain Configuration}.
 *
 * @author Robin Weiss
 */
@Path("config")
public final class ConfigurationFacade
{
    /**
     * If a key is specified in the query, the string value of the corresponding parameter is returned.
     * Is such a key not specified, the entire configuration is returned in a nice to read way.
     *
     * @param key the key of the parameter, or null if such a key is not specified
     *
     * @return a pretty formatted version of the entire configuration,
     *          or the value of the parameter with the matching query key. or an empty string,
     *          If a key is specified, but the parameter does not exist, an empty string is returned.
     */
    @GET
    @Produces({
        MediaType.TEXT_PLAIN
    })
    public Response getValue(@QueryParam("key") String key)
    {
        if (MainContext.getConfiguration() == null)
            return createServerErrorResponse();

        final String entity;

        // if there is no key, return an info string
        if (key == null)
            entity = MainContext.getConfiguration().getInfoString();
        else
            entity = MainContext.getConfiguration().getParameterStringValue(key);

        return Response.status(Status.OK)
               .entity(entity == null ? "" : entity)
               .type(MediaType.TEXT_PLAIN)
               .build();
    }


    /**
     * Saves the configuration to disk.
     *
     * @return an info message that describes the status of the operation
     */
    @POST
    @Produces({
        MediaType.TEXT_PLAIN
    })
    public Response saveToDisk()
    {
        if (MainContext.getConfiguration() == null)
            return createServerErrorResponse();
        else
            return Response.status(Status.OK)
                   .entity(MainContext.getConfiguration().saveToDisk())
                   .type(MediaType.TEXT_PLAIN)
                   .build();
    }


    /**
     * Changes parameters of the configuration.
     *
     * @param formParams a key value map where the keys represent the parameter names and the values the new values
     *
     * @return a feedback text about parameter changes or failures to do so
     */
    @PUT
    @Produces({
        MediaType.TEXT_PLAIN
    })
    @Consumes({
        MediaType.APPLICATION_FORM_URLENCODED
    })
    public Response setParameters(final MultivaluedMap<String, String> formParams)
    {
        final Configuration config = MainContext.getConfiguration();

        if (config == null)
            return createServerErrorResponse();

        // assemble response string
        final StringBuilder sb = new StringBuilder();

        if (formParams != null) {
            for (Entry<String, List<String>> entry : formParams.entrySet()) {
                String key = entry.getKey();
                List<String> values = entry.getValue();

                values.forEach((String value) -> sb.append(config.setParameter(key, value)).append('\n'));
            }
        }

        // if nothing was attempted to be changed, inform the user
        if (sb.length() == 0)
            return Response.status(Status.BAD_REQUEST)
                   .entity(ConfigurationConstants.NO_CHANGES)
                   .type(MediaType.TEXT_PLAIN)
                   .build();
        else
            return Response.status(Status.OK)
                   .entity(sb.toString())
                   .type(MediaType.TEXT_PLAIN)
                   .build();
    }


    /**
     * Creates a server error response if the configuration is unavailable.
     *
     * @return a server error response if the configuration is unavailable
     */
    private Response createServerErrorResponse()
    {
        return Response
               .status(Status.INTERNAL_SERVER_ERROR)
               .entity(ConfigurationConstants.REST_INFO_FAILED)
               .type(MediaType.TEXT_PLAIN)
               .build();
    }
}
