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
package de.gerdiproject.harvest.state.impl;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import de.gerdiproject.harvest.application.constants.StatusConstants;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.harvester.events.HarvesterInitializedEvent;
import de.gerdiproject.harvest.state.IState;
import de.gerdiproject.harvest.state.constants.StateConstants;
import de.gerdiproject.harvest.state.constants.StateEventHandlerConstants;

/**
 * This state represents the initialization of harvesters at the beginning of
 * the server start.
 *
 * @author Robin Weiss
 */
public class InitializationState implements IState
{
    @Override
    public void onStateEnter()
    {
        EventSystem.addListener(HarvesterInitializedEvent.class, StateEventHandlerConstants.ON_HARVESTER_INITIALIZED);
    }


    @Override
    public void onStateLeave()
    {
        EventSystem.removeListener(
            HarvesterInitializedEvent.class,
            StateEventHandlerConstants.ON_HARVESTER_INITIALIZED);
    }


    @Override
    public String getStatusString()
    {
        return StateConstants.INIT_STATUS;
    }


    @Override
    public Response startHarvest()
    {
        return createServiceUnavailableResponse(StateConstants.CANNOT_START_PREFIX);
    }


    @Override
    public Response abort()
    {
        return createServiceUnavailableResponse(
                   String.format(
                       StateConstants.CANNOT_ABORT_PREFIX,
                       StateConstants.INIT_PROCESS));
    }


    @Override
    public Response submit()
    {
        return createServiceUnavailableResponse(StateConstants.CANNOT_SUBMIT_PREFIX);
    }


    @Override
    public Response save()
    {
        return createServiceUnavailableResponse(StateConstants.CANNOT_SAVE_PREFIX);
    }


    @Override
    public Response getProgress()
    {
        return Response
               .status(Status.BAD_REQUEST)
               .entity(StatusConstants.NOT_AVAILABLE)
               .type(MediaType.TEXT_PLAIN)
               .build();
    }


    @Override
    public String getName()
    {
        return StateConstants.INIT_PROCESS;
    }


    @Override
    public Response isOutdated()
    {
        return createServiceUnavailableResponse(StateConstants.CANNOT_GET_VALUE_PREFIX);
    }


    /**
     * Creates a response, replying that the service is not available at the moment.
     *
     * @param prefix a prefix for the error response
     *
     * @return a response, replying that the service is not available at the moment
     */
    private Response createServiceUnavailableResponse(final String prefix)
    {
        return Response
               .status(Status.SERVICE_UNAVAILABLE)
               .entity(prefix + StateConstants.INIT_IN_PROGRESS)
               .type(MediaType.TEXT_PLAIN)
               .build();
    }
}
