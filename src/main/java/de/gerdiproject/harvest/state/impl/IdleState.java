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
/*
  *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
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
package de.gerdiproject.harvest.state.impl;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.application.constants.StatusConstants;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.harvester.events.GetHarvesterOutdatedEvent;
import de.gerdiproject.harvest.harvester.events.HarvestStartedEvent;
import de.gerdiproject.harvest.harvester.events.StartHarvestEvent;
import de.gerdiproject.harvest.save.events.SaveStartedEvent;
import de.gerdiproject.harvest.save.events.StartSaveEvent;
import de.gerdiproject.harvest.state.IState;
import de.gerdiproject.harvest.state.StateMachine;
import de.gerdiproject.harvest.state.constants.StateConstants;
import de.gerdiproject.harvest.state.constants.StateEventHandlerConstants;
import de.gerdiproject.harvest.submission.events.StartSubmissionEvent;
import de.gerdiproject.harvest.submission.events.SubmissionStartedEvent;
import de.gerdiproject.harvest.utils.time.HarvestTimeKeeper;

/**
 * This state indicates it is waiting for user input.
 *
 * @author Robin Weiss
 */
public class IdleState implements IState
{
    private static final Logger LOGGER = LoggerFactory.getLogger(StateMachine.class);


    @Override
    public void onStateEnter()
    {
        EventSystem.addListener(HarvestStartedEvent.class, StateEventHandlerConstants.ON_HARVEST_STARTED);
        EventSystem.addListener(SubmissionStartedEvent.class, StateEventHandlerConstants.ON_SUBMISSION_STARTED);
        EventSystem.addListener(SaveStartedEvent.class, StateEventHandlerConstants.ON_SAVE_STARTED);

        LOGGER.info(String.format(StateConstants.READY, MainContext.getModuleName()));
    }


    @Override
    public void onStateLeave()
    {
        EventSystem.removeListener(HarvestStartedEvent.class, StateEventHandlerConstants.ON_HARVEST_STARTED);
        EventSystem.removeListener(SubmissionStartedEvent.class, StateEventHandlerConstants.ON_SUBMISSION_STARTED);
        EventSystem.removeListener(SaveStartedEvent.class, StateEventHandlerConstants.ON_SAVE_STARTED);
    }


    @Override
    public String getStatusString()
    {
        HarvestTimeKeeper timeKeeper = MainContext.getTimeKeeper();
        return String.format(
                   StateConstants.IDLE_STATUS,
                   timeKeeper.getHarvestMeasure().toString(),
                   timeKeeper.getSaveMeasure().toString(),
                   timeKeeper.getSubmissionMeasure().toString());
    }


    @Override
    public Response startHarvest()
    {
        EventSystem.sendEvent(new StartHarvestEvent());
        return Response
               .status(Status.ACCEPTED)
               .entity(StateConstants.HARVEST_STARTED)
               .type(MediaType.TEXT_PLAIN)
               .build();
    }


    @Override
    public Response abort()
    {
        final String entity = String.format(
                                  StateConstants.CANNOT_ABORT_PREFIX
                                  + StateConstants.NO_HARVEST_IN_PROGRESS,
                                  StateConstants.HARVESTING_PROCESS);

        return Response
               .status(Status.BAD_REQUEST)
               .entity(entity)
               .type(MediaType.TEXT_PLAIN)
               .build();
    }


    @Override
    public Response submit()
    {
        EventSystem.sendEvent(new StartSubmissionEvent());
        return Response
               .status(Status.ACCEPTED)
               .entity(StateConstants.SUBMITTING_STATUS)
               .type(MediaType.TEXT_PLAIN)
               .build();
    }


    @Override
    public Response save()
    {
        EventSystem.sendEvent(new StartSaveEvent(false));

        return Response
               .status(Status.ACCEPTED)
               .entity(StateConstants.SAVING_STATUS)
               .type(MediaType.TEXT_PLAIN)
               .build();
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
        return StateConstants.IDLE_PROCESS;
    }


    @Override
    public Response isOutdated()
    {
        final Boolean isOutdated;

        if (MainContext.getTimeKeeper().isHarvestIncomplete())
            isOutdated = true;
        else
            isOutdated = EventSystem.sendSynchronousEvent(new GetHarvesterOutdatedEvent());

        final String entity;
        final Status status;

        if (isOutdated == null) {
            entity = StateConstants.INIT_IN_PROGRESS;
            status = Status.SERVICE_UNAVAILABLE;
        } else {
            entity = isOutdated.toString();
            status = Status.OK;
        }

        return Response
               .status(status)
               .entity(entity)
               .type(MediaType.TEXT_PLAIN)
               .build();
    }
}
