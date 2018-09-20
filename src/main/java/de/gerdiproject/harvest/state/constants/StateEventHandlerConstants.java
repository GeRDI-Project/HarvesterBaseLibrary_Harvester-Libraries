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
package de.gerdiproject.harvest.state.constants;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.events.GetConfigurationEvent;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.harvester.events.HarvestFinishedEvent;
import de.gerdiproject.harvest.harvester.events.HarvestStartedEvent;
import de.gerdiproject.harvest.harvester.events.HarvesterInitializedEvent;
import de.gerdiproject.harvest.state.StateMachine;
import de.gerdiproject.harvest.state.events.AbortingFinishedEvent;
import de.gerdiproject.harvest.state.impl.ErrorState;
import de.gerdiproject.harvest.state.impl.HarvestingState;
import de.gerdiproject.harvest.state.impl.IdleState;
import de.gerdiproject.harvest.state.impl.SubmittingState;
import de.gerdiproject.harvest.submission.constants.SubmissionConstants;
import de.gerdiproject.harvest.submission.events.StartSubmissionEvent;
import de.gerdiproject.harvest.submission.events.SubmissionFinishedEvent;
import de.gerdiproject.harvest.submission.events.SubmissionStartedEvent;

/**
 * This class is a static collection of state change event handlers.
 *
 * @author Robin Weiss
 */
public class StateEventHandlerConstants
{
    private static final Logger LOGGER = LoggerFactory.getLogger(StateMachine.class);


    /**
     * Private constructor, because this class just serves as a place to define
     * static callback functions.
     */
    private StateEventHandlerConstants()
    {

    }


    /**
     * Switches the state to {@linkplain HarvestingState} when a harvest was
     * started.
     */
    public static final Consumer<HarvestStartedEvent> ON_HARVEST_STARTED = (HarvestStartedEvent e) -> {

        HarvestingState nextState = new HarvestingState(e.getEndIndex() - e.getStartIndex());
        StateMachine.setState(nextState);
    };


    /**
     * If the harvesting process is done either go to the
     * {@linkplain IdleState} or {@linkplain SubmittingState}, depending on the configuration.
     */
    public static final Consumer<HarvestFinishedEvent> ON_HARVEST_FINISHED = (HarvestFinishedEvent e) -> {
        finishHarvest(e.isSuccessful());
    };


    /**
     * Switches the state to {@linkplain SubmittingState} when a document
     * submission was started.
     */
    public static final Consumer<SubmissionStartedEvent> ON_SUBMISSION_STARTED = (SubmissionStartedEvent e) -> {

        SubmittingState nextState = new SubmittingState(e.getNumberOfDocuments());
        StateMachine.setState(nextState);
    };


    /**
     * Switches the state to {@linkplain IdleState} when an aborting-process
     * finishes.
     */
    public static final Consumer<AbortingFinishedEvent> ON_ABORTING_FINISHED = (AbortingFinishedEvent e) -> {

        IdleState nextState = new IdleState();
        StateMachine.setState(nextState);
    };


    /**
     * Switches the state to {@linkplain IdleState} if the initialization was
     * successful. Otherwise, the state is switched to the
     * {@linkplain ErrorState}.
     */
    public static final Consumer<HarvesterInitializedEvent> ON_HARVESTER_INITIALIZED =
    (HarvesterInitializedEvent e) -> {
        if (e.isSuccessful())
            StateMachine.setState(new IdleState());
        else
            StateMachine.setState(new ErrorState());
    };


    /**
     * Switches the state to {@linkplain IdleState} when a submission-process
     * finishes.
     */
    public static final Consumer<SubmissionFinishedEvent> ON_SUBMISSION_FINISHED =
        (SubmissionFinishedEvent e) -> StateMachine.setState(new IdleState());



    /**
     * The content of ON_HARVEST_FINISHED had to be put into this method to prevent a PMD bug.
     * Goes to the IDLE state and if the submission.autoSubmit parameter is true, starts a submission.
     *
     * @param isSuccessful true if the harvest finished successfully
     */
    private static void finishHarvest(boolean isSuccessful)
    {
        LOGGER.info(isSuccessful ? StateConstants.HARVEST_DONE : StateConstants.HARVEST_FAILED);

        StateMachine.setState(new IdleState());

        // was the harvest successful? then choose the next automatic post-processing state
        if (isSuccessful) {
            final Configuration config = EventSystem.sendSynchronousEvent(new GetConfigurationEvent());

            boolean isAutoSubmitEnabled = config.getParameterValue(SubmissionConstants.AUTO_SUBMIT_PARAM.getCompositeKey());

            if (isAutoSubmitEnabled) {
                String response = EventSystem.sendSynchronousEvent(new StartSubmissionEvent());
                LOGGER.info(response);
                return;
            }
        }
    }
}
