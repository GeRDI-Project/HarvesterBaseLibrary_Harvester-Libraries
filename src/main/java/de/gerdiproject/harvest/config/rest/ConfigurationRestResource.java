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


import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;

import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.config.events.GetConfigurationEvent;
import de.gerdiproject.harvest.config.json.ChangeConfigurationRequest;
import de.gerdiproject.harvest.config.json.adapters.ConfigurationAdapter;
import de.gerdiproject.harvest.config.parameters.AbstractParameter;
import de.gerdiproject.harvest.rest.AbstractRestResource;
import de.gerdiproject.harvest.rest.HttpResponseFactory;


/**
 * This facade serves as an interface between REST and the {@linkplain Configuration}.
 *
 * @author Robin Weiss
 */
@Path("config")
public final class ConfigurationRestResource extends AbstractRestResource<Configuration, GetConfigurationEvent>
{
    /**
     * Constructor
     */
    public ConfigurationRestResource()
    {
        super(new GsonBuilder().registerTypeAdapter(Configuration.class, new ConfigurationAdapter()).create());
    }


    /**
     * Changes parameters of the configuration.
     *
     * @param formParams a key value map where the keys represent the parameter names and the values the new values
     *
     * @return a feedback text about parameter changes or failures to do so
     */
    @PUT
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public Response setParameters(final MultivaluedMap<String, String> formParams)
    {
        final ChangeConfigurationRequest request = new ChangeConfigurationRequest();

        for (final String key : formParams.keySet())
            request.put(key, formParams.getFirst(key));

        return setConfiguration(gson.toJson(request));
    }


    /**
     * Changes parameters of the configuration.
     *
     * @param configJson a key value map where the keys represent the parameter names and the values the new values
     *
     * @return a feedback text about parameter changes or failures to do so
     */
    @Path("_set")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response setConfiguration(final String configJson)
    {
        return changeObject(restObject::setParameters, configJson, ChangeConfigurationRequest.class);
    }


    @Override
    public Response getInfoText(final UriInfo uriInfo)
    {
        final MultivaluedMap<String, String> query = uriInfo.getQueryParameters();

        // check if only a single key is to be retrieved
        if (query.containsKey(ConfigurationConstants.QUERY_KEY)) {
            // abort if object is not initialized, yet
            if (restObject == null)
                return HttpResponseFactory.createServerErrorResponse();

            final String compositeKey = query.get(ConfigurationConstants.QUERY_KEY).get(0);
            final String value = restObject.getParameterStringValue(compositeKey);

            if (value == null) {
                final String errorMessage = String.format(
                                                ConfigurationConstants.GET_UNKNOWN_PARAM_ERROR,
                                                compositeKey);
                return HttpResponseFactory.createBadRequestResponse(errorMessage);
            } else
                return HttpResponseFactory.createValueResponse(Status.OK, new JsonPrimitive(value));
        } else
            return super.getInfoText(uriInfo);
    }


    @Override
    protected String getAllowedRequests()
    {
        // assemble valid keys
        final StringBuilder allowedValues = new StringBuilder();

        for (final AbstractParameter<?> param : restObject.getParameters()) {
            if (allowedValues.length() != 0)
                allowedValues.append(", ");

            allowedValues.append(param.getCompositeKey());
        }

        return ConfigurationConstants.ALLOWED_REQUESTS + allowedValues.toString();
    }
}
