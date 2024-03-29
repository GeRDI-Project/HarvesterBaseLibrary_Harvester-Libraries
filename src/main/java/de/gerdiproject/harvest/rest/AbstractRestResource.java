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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.function.Function;

import javax.ws.rs.GET;
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
 * @param <T> the type of the singleton object that is represented by the {@linkplain AbstractRestResource}
 * @param <S> the type of a {@linkplain GetRestObjectEvent} that is used to retrieve the singleton object
 *
 * @author Robin Weiss
 */
public abstract class AbstractRestResource<T extends AbstractRestObject<T, ?>, S extends GetRestObjectEvent<T>>
{
    protected final T restObject;
    protected final Gson gson;


    /**
     * This constructor constructs and dispatches an instance of the generic {@linkplain GetRestObjectEvent}
     * in order to retrieve the singleton object.
     *
     * @param gson a gson instance used to parse JSON requests and responses
     */
    @SuppressWarnings("unchecked") // this warning is suppressed, because the only generic Superclass MUST be T2. The cast will always succeed.
    public AbstractRestResource(final Gson gson)
    {
        final Class<S> getEventClass =
            (Class<S>)((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[1];

        // try to retrieve the object
        T retrievedObject;

        try {
            retrievedObject = EventSystem.sendSynchronousEvent(getEventClass.getDeclaredConstructor().newInstance());
        } catch (InstantiationException
                     | IllegalAccessException
                     | IllegalArgumentException
                     | InvocationTargetException
                     | NoSuchMethodException
                     | SecurityException e) {
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
    @Produces({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON})
    public Response getInfoText(@Context final UriInfo uriInfo)
    {
        // abort if object is not initialized, yet
        if (restObject == null)
            return HttpResponseFactory.createServerErrorResponse();

        // check if ?pretty=true or simply ?pretty
        boolean isPlainText = false;

        if (uriInfo.getQueryParameters().get(RestConstants.PRETTY_QUERY_PARAM) != null) {
            final String prettyValue = uriInfo.getQueryParameters().get(RestConstants.PRETTY_QUERY_PARAM).get(0);
            isPlainText = prettyValue.isEmpty() || prettyValue.equals(String.valueOf(true));
        }

        // forward GET request to the object
        try {
            if (isPlainText) {
                final String responseText = restObject.getAsPlainText();
                final String allowedRequests = getAllowedRequests()
                                               .replaceAll(RestConstants.LINE_START_REGEX, RestConstants.LINE_START_REPLACEMENT);
                return HttpResponseFactory.createPlainTextOkResponse(responseText + allowedRequests);
            } else {
                final Object responseObject = restObject.getAsJson(uriInfo.getQueryParameters());

                // abort if nothing could be retrieved
                if (responseObject == null)
                    return HttpResponseFactory.createServerErrorResponse();

                // try to parse the JSON object
                return HttpResponseFactory.createOkResponse(gson.toJsonTree(responseObject));
            }
        } catch (final IllegalArgumentException e) {
            return HttpResponseFactory.createBadRequestResponse(e.getMessage());

        } catch (final RuntimeException e) { // NOPMD it's up to the implementation which exceptions to throw, but they are all treated equally
            return HttpResponseFactory.createKnownErrorResponse(e.getMessage());
        }
    }


    /**
     * This method returns a string that offers a description of viable HTTP requests.
     *
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
        final Function<P, R> changeFunction,
        final String jsonRaw, final Class<P> jsonClass)
    {
        // abort if object is not initialized, yet
        if (restObject == null)
            return HttpResponseFactory.createServerErrorResponse();

        if (jsonRaw == null || jsonRaw.isEmpty())
            return HttpResponseFactory.createBadRequestResponse(RestConstants.NO_JSON_BODY_ERROR);

        // try to parse JSON object from String
        P jsonBody;

        try {
            jsonBody = gson.fromJson(jsonRaw,  jsonClass);
        } catch (JsonSyntaxException | NullPointerException e) { // NOPMD NPEs can be thrown due to invalid JSON strings
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
        } catch (final IllegalArgumentException e) {
            return HttpResponseFactory.createBadRequestResponse(e.getMessage());
        } catch (final IllegalStateException e) {
            return HttpResponseFactory.createServerErrorResponse();
        }

        return HttpResponseFactory.createOkResponse(response);
    }
}
