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
package de.gerdiproject.harvest.utils;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import de.gerdiproject.harvest.application.constants.StatusConstants;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.ISynchronousEvent;
import de.gerdiproject.harvest.state.IState;
import de.gerdiproject.harvest.state.StateMachine;
import de.gerdiproject.harvest.state.constants.StateConstants;
import de.gerdiproject.harvest.state.impl.ErrorState;
import de.gerdiproject.harvest.state.impl.InitializationState;

/**
 * This factory creates common server responses to HTTP requests.
 *
 * @author Robin Weiss
 */
public class ServerResponseFactory
{
    /**
     * Private constructor because this class only provides static methods.
     */
    private ServerResponseFactory()
    {
    }


    /**
     * Creates a HTTP-503 response that should be returned during
     * the initialization state of the harvester.
     *
     * @return a HTTP-503 explaining that the service is initializing
     */
    public static Response createInitResponse()
    {
        return Response
               .status(Status.SERVICE_UNAVAILABLE)
               .entity(StateConstants.CANNOT_PROCESS_PREFIX + StateConstants.INIT_IN_PROGRESS)
               .type(MediaType.TEXT_PLAIN)
               .build();
    }


    /**
     * Creates a response, replying that the service is not available at the moment.
     * If available, a Retry-Again header is set with the remaining seconds of the process.
     *
     * @param message a the error message, explaining what has failed
     * @param retryInSeconds the number of seconds until the request should be attempted
     *                          again, or -1 if this number is unknown
     *
     * @return a response, replying that the service is not available at the moment
     */
    public static Response createBusyResponse(final String message, final long retryInSeconds)
    {
        final ResponseBuilder rb = Response
                                   .status(Status.SERVICE_UNAVAILABLE)
                                   .entity(message)
                                   .type(MediaType.TEXT_PLAIN);

        if (retryInSeconds != -1)
            rb.header(StateConstants.RETRY_AFTER_HEADER, retryInSeconds);

        return rb.build();
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
               .entity(StateConstants.ERROR_DETAILED)
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
               .entity(StateConstants.UNKNOWN_ERROR)
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
        return createBadRequestResponse(StatusConstants.NOT_AVAILABLE);
    }


    /**
     * Creates a HTTP-400 response with a specified message.
     *
     * @param message the message that is passed as an entity of the response
     *
     * @return a HTTP-400 response with a specified message
     */
    public static Response createBadRequestResponse(String message)
    {
        return Response
               .status(Status.BAD_REQUEST)
               .entity(message)
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
    public static Response createOkResponse(Object entity)
    {
        return Response
               .status(Status.OK)
               .entity(entity == null ? "" : entity.toString())
               .type(MediaType.TEXT_PLAIN)
               .build();
    }


    /**
     * Creates a response with a specified status code and entity.
     *
     * @param status the HTTP return code of the response
     * @param entity the entity that is passed along with the response
     *
     * @return a response with a specified status code and entity
     */
    public static Response createResponse(Status status, Object entity)
    {
        return Response
               .status(status)
               .entity(entity == null ? "" : entity.toString())
               .type(MediaType.TEXT_PLAIN)
               .build();
    }


    /**
     * Creates an error message depending on the current state of the harvester.
     *
     * @return an error message that befits the current state of the harvester
     */
    public static Response createServerErrorResponse()
    {
        final IState currentState = StateMachine.getCurrentState();

        if (currentState instanceof InitializationState)
            return createInitResponse();

        else if (currentState instanceof ErrorState)
            return createFubarResponse();

        else
            return createUnknownErrorResponse();
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
    public static Response createSynchronousEventResponse(ISynchronousEvent<?> event)
    {
        final Object eventResponse = EventSystem.sendSynchronousEvent(event);

        if (eventResponse == null)
            return createServerErrorResponse();

        else if (eventResponse instanceof Response)
            return (Response)eventResponse;

        else
            return createOkResponse(eventResponse);
    }
}
