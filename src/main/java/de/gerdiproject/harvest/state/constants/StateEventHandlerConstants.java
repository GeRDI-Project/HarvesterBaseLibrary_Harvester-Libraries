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
package de.gerdiproject.harvest.state.constants;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.impl.ChangeStateEvent;
import de.gerdiproject.harvest.event.impl.HarvestFinishedEvent;
import de.gerdiproject.harvest.event.impl.HarvestStartedEvent;
import de.gerdiproject.harvest.event.impl.SaveStartedEvent;
import de.gerdiproject.harvest.event.impl.StartSaveEvent;
import de.gerdiproject.harvest.event.impl.StartSubmissionEvent;
import de.gerdiproject.harvest.event.impl.SubmissionStartedEvent;
import de.gerdiproject.harvest.state.StateMachine;
import de.gerdiproject.harvest.state.impl.HarvestingState;
import de.gerdiproject.harvest.state.impl.IdleState;
import de.gerdiproject.harvest.state.impl.SavingState;
import de.gerdiproject.harvest.state.impl.SubmittingState;


/**
 * This class is a static collection of state change event handlers.
 *
 * @author Robin Weiss
 */
public class StateEventHandlerConstants
{
    private static final Logger LOGGER = LoggerFactory.getLogger(StateMachine.class);

    /**
     * Private constructor, because this is a static class.
     */
    private StateEventHandlerConstants()
    {

    }


    /**
     * Switches the state to {@linkplain HarvestingState} when a harvest was started.
     */
    public static final Consumer<HarvestStartedEvent> ON_HARVEST_STARTED =
    (HarvestStartedEvent e) -> {

        HarvestingState nextState = new HarvestingState(e.getEndIndex() - e.getStartIndex());
        EventSystem.sendEvent(new ChangeStateEvent(nextState));
    };


    /**
     * If the harvesting process is done either go to the {@linkplain IdleState}, {@linkplain SavingState},
     * or {@linkplain SubmittingState}, depending on the configuration.
     */
    public static final Consumer<HarvestFinishedEvent> ON_HARVEST_FINISHED = (HarvestFinishedEvent e) -> {
        // was the harvest successful? then choose the next automatic post-processing state
        if (e.isSuccessful())
        {
            LOGGER.info(StateConstants.HARVEST_DONE);

            final Configuration config = MainContext.getConfiguration();

            if (config.getParameterValue(ConfigurationConstants.AUTO_SAVE, Boolean.class)) {
                EventSystem.sendEvent(new StartSaveEvent());
                return;
            }

            else if (config.getParameterValue(ConfigurationConstants.AUTO_SUBMIT, Boolean.class)) {
                EventSystem.sendEvent(new StartSubmissionEvent());
                return;
            }
        } else
            LOGGER.info(StateConstants.HARVEST_FAILED);

        EventSystem.sendEvent(new ChangeStateEvent(new IdleState()));
    };


    /**
     * Switches the state to {@linkplain SubmittingState} when a document submission was started.
     */
    public static final Consumer<SubmissionStartedEvent> ON_SUBMISSION_STARTED =
    (SubmissionStartedEvent e) -> {

        SubmittingState nextState = new SubmittingState();
        EventSystem.sendEvent(new ChangeStateEvent(nextState));
    };


    /**
     * Switches the state to {@linkplain SavingState} when a document saving process was started.
     */
    public static final Consumer<SaveStartedEvent> ON_SAVE_STARTED =
    (SaveStartedEvent e) -> {

        SavingState nextState = new SavingState(false);
        EventSystem.sendEvent(new ChangeStateEvent(nextState));
    };
}
