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

import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gerdiproject.harvest.application.events.ResetContextEvent;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.rest.HttpResponseFactory;
import de.gerdiproject.harvest.state.IState;
import de.gerdiproject.harvest.state.StateMachine;
import de.gerdiproject.harvest.state.constants.StateConstants;
import de.gerdiproject.harvest.state.constants.StateEventHandlerConstants;
import de.gerdiproject.harvest.state.events.AbortingFinishedEvent;

/**
 * This state indicates some process is aborting.
 *
 * @author Robin Weiss
 */
public class AbortingState implements IState
{
    private static final Logger LOGGER = LoggerFactory.getLogger(StateMachine.class);

    private final String processName;


    /**
     * Constructs the state with the name of the aborted process.
     *
     * @param processName the name of the process that is aborted
     */
    public AbortingState(String processName)
    {
        this.processName = processName;
    }


    @Override
    public void onStateEnter()
    {
        EventSystem.addListener(AbortingFinishedEvent.class, StateEventHandlerConstants.ON_ABORTING_FINISHED);

        LOGGER.info(String.format(StateConstants.ABORT_STARTED, processName));
    }


    @Override
    public void onStateLeave()
    {
        EventSystem.removeListener(AbortingFinishedEvent.class, StateEventHandlerConstants.ON_ABORTING_FINISHED);

        LOGGER.info(String.format(StateConstants.ABORT_FINISHED, processName));
    }


    @Override
    public String getStatusString()
    {
        return String.format(StateConstants.ABORT_STATUS, processName);
    }


    @Override
    public Response startHarvest()
    {
        return createServiceUnavailableResponse();
    }


    @Override
    public Response abort()
    {
        return createServiceUnavailableResponse();
    }


    @Override
    public Response submit()
    {
        return createServiceUnavailableResponse();
    }


    @Override
    public Response save()
    {
        return createServiceUnavailableResponse();
    }


    @Override
    public Response reset()
    {
        EventSystem.sendEvent(new ResetContextEvent());
        return HttpResponseFactory.createAcceptedResponse(
                   StateConstants.RESET_STARTED_PROBLEMATIC);
    }


    @Override
    public Response getProgress()
    {
        return HttpResponseFactory.createBadRequestResponse();
    }


    @Override
    public String getName()
    {
        return StateConstants.ABORTING_PROCESS;
    }


    @Override
    public Response isOutdated()
    {
        return createServiceUnavailableResponse();
    }

    /**
     * Creates a response, replying that the service is not available at the moment.
     *
     * @return a response, replying that the service is not available at the moment
     */
    private Response createServiceUnavailableResponse()
    {
        return HttpResponseFactory.createBusyResponse(String.format(StateConstants.ABORT_DETAILED, processName), -1);
    }
}
