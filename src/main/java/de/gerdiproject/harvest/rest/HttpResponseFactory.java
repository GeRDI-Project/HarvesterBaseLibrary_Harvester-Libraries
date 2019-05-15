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

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import com.google.gson.JsonElement;

import de.gerdiproject.harvest.application.MainContext;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.ISynchronousEvent;
import de.gerdiproject.harvest.rest.constants.RestConstants;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * This factory creates common server responses to HTTP requests.
 *
 * @author Robin Weiss
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HttpResponseFactory
{
    /**
     * Creates a HTTP-503 response that should be returned during
     * the initialization state of the harvester.
     *
     * @return a HTTP-503 explaining that the service is initializing
     */
    public static Response createInitResponse()
    {
        final String message = RestConstants.CANNOT_PROCESS_PREFIX + RestConstants.WAIT_FOR_INIT;
        return Response
               .status(Status.SERVICE_UNAVAILABLE)
               .entity(refineEntity(Status.SERVICE_UNAVAILABLE, message))
               .type(MediaType.TEXT_PLAIN)
               .build();
    }


    /**
     * Creates a response, replying that the service is not available at the moment.
     * If available, a Retry-Again header is set with the remaining seconds of the process.
     *
     * @param message an error message, explaining what has failed
     * @param retryInSeconds the number of seconds until the request should be attempted
     *                          again, or -1 if this number is unknown
     *
     * @return a response, replying that the service is not available at the moment
     */
    public static Response createBusyResponse(final String message, final long retryInSeconds)
    {
        final ResponseBuilder rb = Response
                                   .status(Status.SERVICE_UNAVAILABLE)
                                   .entity(refineEntity(Status.SERVICE_UNAVAILABLE, message))
                                   .type(MediaType.TEXT_PLAIN);

        if (retryInSeconds > 0)
            rb.header(RestConstants.RETRY_AFTER_HEADER, retryInSeconds);

        return rb.build();
    }


    /**
     * Creates a HTTP-202 response for signalling that a process was started.
     * @param message a message, explaining what has started
     *
     * @return a HTTP-202 response for signalling that a process was started
     */
    public static Response createAcceptedResponse(final String message)
    {
        return Response
               .status(Status.ACCEPTED)
               .entity(refineEntity(Status.ACCEPTED, message))
               .type(MediaType.TEXT_PLAIN)
               .build();
    }


    /**
     * Creates a HTTP-500 response for the case that the service is broken
     * beyond all repair.
     *
     * @return a HTTP-500 response
     */
    public static Response createFubarResponse()
    {
        return Response
               .status(Status.INTERNAL_SERVER_ERROR)
               .entity(refineEntity(Status.INTERNAL_SERVER_ERROR, RestConstants.INIT_ERROR_DETAILED))
               .type(MediaType.TEXT_PLAIN)
               .build();
    }


    /**
     * Creates a HTTP-500 response for unknown server errors.
     *
     * @return a HTTP-500 response
     */
    public static Response createUnknownErrorResponse()
    {
        return Response
               .status(Status.INTERNAL_SERVER_ERROR)
               .entity(refineEntity(Status.INTERNAL_SERVER_ERROR, RestConstants.UNKNOWN_ERROR))
               .type(MediaType.TEXT_PLAIN)
               .build();
    }


    /**
     * Creates a HTTP-500 response for a known but unexpected server error.
     *
     * @param message the message that is passed as an entity of the response
     *
     * @return a HTTP-500 response
     */
    public static Response createKnownErrorResponse(final String message)
    {
        return Response
               .status(Status.INTERNAL_SERVER_ERROR)
               .entity(refineEntity(Status.INTERNAL_SERVER_ERROR, message))
               .type(MediaType.TEXT_PLAIN)
               .build();
    }


    /**
     * Creates a HTTP-400 response with "N/A" as message.
     *
     * @return a HTTP-400 response with "N/A" as message
     */
    public static Response createBadRequestResponse()
    {
        return createBadRequestResponse(RestConstants.NOT_AVAILABLE);
    }


    /**
     * Creates a HTTP-400 response with a specified message.
     *
     * @param message the message that is passed as an entity of the response
     *
     * @return a HTTP-400 response with a specified message
     */
    public static Response createBadRequestResponse(final String message)
    {
        return Response
               .status(Status.BAD_REQUEST)
               .entity(refineEntity(Status.BAD_REQUEST, message))
               .type(MediaType.TEXT_PLAIN)
               .build();
    }

    /**
     * Creates a HTTP-405 response.
     *
     * @return a HTTP-405 response
     */
    public static Response createMethodNotAllowedResponse()
    {
        return Response
               .status(Status.METHOD_NOT_ALLOWED)
               .entity(refineEntity(Status.METHOD_NOT_ALLOWED, RestConstants.INVALID_REQUEST_ERROR))
               .type(MediaType.TEXT_PLAIN)
               .build();
    }


    /**
     * Creates a HTTP-200 message with a specified entity.
     *
     * @param entity the entity that is passed along with the response
     *
     * @return a HTTP-200 message with a specified entity
     */
    public static Response createOkResponse(final Object entity)
    {
        return Response
               .status(Status.OK)
               .entity(refineEntity(Status.OK, entity))
               .type(MediaType.APPLICATION_JSON)
               .build();
    }


    /**
     * Creates a HTTP-200 plain text response.
     *
     * @param message the response text
     *
     * @return a plain text HTTP-200
     */
    public static Response createPlainTextOkResponse(final String message)
    {
        return Response
               .status(Status.OK)
               .entity(message.trim())
               .type(MediaType.TEXT_PLAIN)
               .build();
    }


    /**
     * Creates a response with a specified status code and entity.
     *
     * @param statusCode the HTTP return code of the response
     * @param value the entity that is passed along with the response
     *
     * @return a response with a specified status code and entity
     */
    public static Response createValueResponse(final Status statusCode, final JsonElement value)
    {
        if (value == null)
            return createBadRequestResponse();

        final String status = statusCode.getStatusCode() >= 200 && statusCode.getStatusCode() < 400
                              ? RestConstants.STATUS_OK
                              : RestConstants.STATUS_FAILED;

        final String valueEntity = String.format(RestConstants.VALUE_JSON, status, value.toString().trim());
        return Response
               .status(statusCode)
               .entity(valueEntity)
               .type(MediaType.APPLICATION_JSON)
               .build();
    }


    /**
     * Creates an error message depending on the current state of the harvester.
     *
     * @return an error message that befits the current state of the harvester
     */
    public static Response createServerErrorResponse()
    {
        if (MainContext.hasFailed())
            return createFubarResponse();
        else if (MainContext.isInitialized())
            return createUnknownErrorResponse();
        else
            return createInitResponse();
    }


    /**
     * Creates a server response for an {@linkplain ISynchronousEvent}. During initialization,
     * these events may not be registered yet, which is handled by replying with an HTTP-503 code.
     *
     * @param event the synchronous event that is to be dispatched
     *
     * @return a HTTP-200 response with the event return value as plain text entity,
     *          or a HTTP-500 response if the service is broken beyond repair,
     *          or a HTTP-503 response if the service is initializing
     */
    public static Response createSynchronousEventResponse(final ISynchronousEvent<?> event)
    {
        final Object eventResponse = EventSystem.sendSynchronousEvent(event);

        if (eventResponse == null)
            return createServerErrorResponse();

        else if (eventResponse instanceof Response)
            return (Response)eventResponse;

        else
            return createOkResponse(eventResponse);
    }


    /**
     * Converts an entity object to a proper response string.
     *
     * @param statusCode the status of the response
     * @param entity the (unrefined) entity
     *
     * @return a non-empty, non-null response JSON
     */
    private static String refineEntity(final Status statusCode, final Object entity)
    {
        final String entityString;

        if (entity == null)
            entityString = "{}";
        else if (entity instanceof String) {
            final String status = statusCode.getStatusCode() >= 200 && statusCode.getStatusCode() < 400
                                  ? RestConstants.STATUS_OK
                                  : RestConstants.STATUS_FAILED;
            entityString = String.format(RestConstants.FEEDBACK_JSON, status, ((String)entity).replaceAll("\\n", ";  ").trim());
        } else
            entityString = entity.toString();

        return entityString;
    }
}
