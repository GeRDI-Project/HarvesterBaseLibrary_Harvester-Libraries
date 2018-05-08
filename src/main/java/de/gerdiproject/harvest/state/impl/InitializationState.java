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

import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.harvester.events.HarvesterInitializedEvent;
import de.gerdiproject.harvest.state.IState;
import de.gerdiproject.harvest.state.constants.StateConstants;
import de.gerdiproject.harvest.state.constants.StateEventHandlerConstants;
import de.gerdiproject.harvest.utils.ServerResponseFactory;

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
        return ServerResponseFactory.createInitResponse();
    }


    @Override
    public Response abort()
    {
        return ServerResponseFactory.createInitResponse();
    }


    @Override
    public Response submit()
    {
        return ServerResponseFactory.createInitResponse();
    }


    @Override
    public Response save()
    {
        return ServerResponseFactory.createInitResponse();
    }


    @Override
    public Response getProgress()
    {
        return ServerResponseFactory.createBadRequestResponse();
    }


    @Override
    public String getName()
    {
        return StateConstants.INIT_PROCESS;
    }


    @Override
    public Response isOutdated()
    {
        return ServerResponseFactory.createInitResponse();
    }
}
