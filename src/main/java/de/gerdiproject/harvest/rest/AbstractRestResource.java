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
package de.gerdiproject.harvest.rest;

import java.lang.reflect.ParameterizedType;
import java.util.function.Function;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.rest.constants.RestConstants;
import de.gerdiproject.harvest.rest.events.GetRestObjectEvent;

/**
 * This abstract class represents the interface between HTTP requests
 * and a singleton object that can be manipulated by such.
 * It offers a standard plain-text and JSON getter, as well
 * as a helper function for manipulating the singleton object.
 *
 * @param T1 the type of the singleton object that is represented by the {@linkplain AbstractRestResource}
 * @param T2 the type of a {@linkplain GetRestObjectEvent} that is used to retrieve the singleton object
 *
 * @author Robin Weiss
 */
public abstract class AbstractRestResource<T1 extends AbstractRestObject<T1, ?>, T2 extends GetRestObjectEvent<T1>>
{
    protected final T1 restObject;
    protected final Gson gson;


    /**
     * This constructor constructs and dispatches an instance of the generic {@linkplain GetRestObjectEvent}
     * in order to retrieve the singleton object.
     *
     * @param gson a gson instance used to parse JSON requests and responses
     */
    @SuppressWarnings("unchecked") // this warning is suppressed, because the only generic Superclass MUST be T2. The cast will always succeed.
    public AbstractRestResource(Gson gson)
    {
        final Class<T2> getEventClass =
            (Class<T2>)((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[1];

        // try to retrieve the object
        T1 retrievedObject;

        try {
            retrievedObject = EventSystem.sendSynchronousEvent(getEventClass.newInstance());
        } catch (InstantiationException | IllegalAccessException e) {
            retrievedObject = null;
        }

        this.restObject = retrievedObject;
        this.gson = gson;
    }


    /**
     * This constructor constructs and dispatches an instance of the generic {@linkplain GetRestObjectEvent}
     * in order to retrieve the singleton object. It creates a new {@linkplain Gson} instance without custom adapters.
     */
    public AbstractRestResource()
    {
        this(new Gson());
    }


    /**
     * A HTTP GET request that returns a plain-text representation of the singleton object,
     * as well as some help text that shows which HTTP requests are possible.
     *
     * @param uriInfo an object that can be used to retrieve the path and possible query parameters.
     *
     * @return a pretty plain text that represents the singleton object or
     * an error response describing what went wrong
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getInfoText(@Context UriInfo uriInfo)
    {
        // abort if object is not initialized, yet
        if (restObject == null)
            return HttpResponseFactory.createInitResponse();

        // forward GET request to the object
        try {
            final String responseText = restObject.getAsPlainText();
            final String allowedRequests = getAllowedRequests()
                                           .replaceAll(RestConstants.LINE_START_REGEX, RestConstants.LINE_START_REPLACEMENT);
            return HttpResponseFactory.createOkResponse(responseText + allowedRequests);

        } catch (IllegalArgumentException e) {
            return HttpResponseFactory.createBadRequestResponse(e.getMessage());
        } catch (Exception e) {
            return HttpResponseFactory.createUnknownErrorResponse();
        }
    }


    /**
     * A HTTP GET request that returns a JSON representation of the singleton object.
     *
     * @param uriInfo an object that can be used to retrieve the path and possible query parameters.
     *
     * @return a JSON object that represents the singleton object or an error response describing what went wrong
     */
    @GET
    @Path(".json")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getInfoJson(@Context UriInfo uriInfo)
    {
        // abort if object is not initialized, yet
        if (restObject == null)
            return HttpResponseFactory.createInitResponse();

        // forward GET request to the object
        final Object responseObject = restObject.getAsJson(uriInfo.getQueryParameters());

        // abort if nothing could be retrieved
        if (responseObject == null)
            return HttpResponseFactory.createServerErrorResponse();

        // try to parse the JSON object
        try {
            final String responseText = responseObject instanceof String
                                        ? (String) responseObject
                                        : gson.toJson(responseObject);

            return HttpResponseFactory.createOkResponse(responseText);

        } catch (IllegalArgumentException e) {
            return HttpResponseFactory.createBadRequestResponse(e.getMessage());
        } catch (Exception e) {
            return HttpResponseFactory.createUnknownErrorResponse();
        }
    }


    /**
     * This method returns a string that offers a description of viable HTTP requests.
     * @return a string that offers a description of viable HTTP requests
     */
    protected abstract String getAllowedRequests();


    /**
     * Parses the required JSON object from a specified string and calls
     * a string returning function using the parsed object as argument.
     * Returns various HTTP error codes and explanations if the process fails.
     * This function can be used as a fail-safe manipulator of the singleton object.
     *
     * @param changeFunction a function that expects a JSON object and returns a string
     * @param jsonRaw the unparsed JSON string
     * @param jsonClass the class that is to be created from the JSON string
     *
     * @param <P> the target type of the JSON object body
     * @param <R> the return type of the changeFunction
     *
     * @return a response that describes the status of the operation
     */
    protected <P, R> Response changeObject(
        Function<P, R> changeFunction,
        String jsonRaw, Class<P> jsonClass)
    {
        // abort if object is not initialized, yet
        if (restObject == null)
            return HttpResponseFactory.createInitResponse();

        if (jsonRaw == null || jsonRaw.isEmpty())
            return HttpResponseFactory.createBadRequestResponse(RestConstants.NO_JSON_BODY_ERROR);

        // try to parse JSON object from String
        P jsonBody;

        try {
            jsonBody = gson.fromJson(jsonRaw,  jsonClass);
        } catch (JsonSyntaxException | NullPointerException e) {
            jsonBody = null;
        }

        // abort if body could not be parsed
        if (jsonBody == null)
            return HttpResponseFactory.createBadRequestResponse(
                       String.format(RestConstants.JSON_INVALID_FORMAT_ERROR, jsonRaw));

        // send request and watch out for runtime exceptions
        final R response;

        try {
            response = changeFunction.apply(jsonBody);
        } catch (IllegalArgumentException e) {
            return HttpResponseFactory.createBadRequestResponse(e.getMessage());
        } catch (IllegalStateException e) {
            return HttpResponseFactory.createServerErrorResponse();
        }

        return HttpResponseFactory.createOkResponse(response);
    }
}
