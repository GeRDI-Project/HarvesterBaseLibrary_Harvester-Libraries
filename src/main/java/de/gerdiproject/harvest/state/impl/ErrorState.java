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
 * This state is a dead-end that occurs when the harvester cannot be
 * initialized.
 *
 * @author Robin Weiss
 */
public class ErrorState implements IState
{
    @Override
    public String getStatusString()
    {
        return StateConstants.ERROR_STATUS;
    }


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
    public Response startHarvest()
    {
        return createServerErrorResponse();
    }


    @Override
    public Response abort()
    {
        return createServerErrorResponse();
    }


    @Override
    public Response submit()
    {
        return createServerErrorResponse();
    }


    @Override
    public Response save()
    {
        return createServerErrorResponse();
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
        return StateConstants.ERROR_PROCESS;
    }


    @Override
    public Response isOutdated()
    {
        return createServerErrorResponse();
    }


    /**
     * Creates a response, notifying the requester about the server error.
     *
     * @return a response, notifying the requester about the server error.
     */
    private Response createServerErrorResponse()
    {
        return Response
               .status(Status.INTERNAL_SERVER_ERROR)
               .entity(StateConstants.ERROR_DETAILED)
               .type(MediaType.TEXT_PLAIN)
               .build();
    }
}
