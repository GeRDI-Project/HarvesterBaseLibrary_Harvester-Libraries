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

import java.util.function.Consumer;

import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.save.events.DocumentSavedEvent;
import de.gerdiproject.harvest.save.events.SaveFinishedEvent;
import de.gerdiproject.harvest.state.AbstractProgressingState;
import de.gerdiproject.harvest.state.constants.StateConstants;
import de.gerdiproject.harvest.state.constants.StateEventHandlerConstants;
import de.gerdiproject.harvest.state.events.ChangeStateEvent;
import de.gerdiproject.harvest.submission.events.StartSubmissionEvent;
import de.gerdiproject.harvest.submission.events.SubmissionStartedEvent;
import de.gerdiproject.harvest.utils.time.HarvestTimeKeeper;

/**
 * This state represents the process of saving the harvested documents to disk.
 *
 * @author Robin Weiss
 */
public class SavingState extends AbstractProgressingState
{
    /**
     * If a document is saved, add 1 to the progress.
     */
    private final Consumer<DocumentSavedEvent> onDocumentSaved = (DocumentSavedEvent e) -> addProgress(1);


    /**
     * If all documents were saved, change the state to {@linkplain IdleState},
     * or to {@linkplain SubmittingState} if auto-submission is enabled.
     */
    private final Consumer<SaveFinishedEvent> onSaveFinished;


    /**
     * Constructor for the saving state.
     *
     * @param numberOfDocsToBeSaved the number of documents that are to be saved
     * @param isAutoTriggered if true, this state was not triggered via REST
     */
    public SavingState(int numberOfDocsToBeSaved, boolean isAutoTriggered)
    {
        super(numberOfDocsToBeSaved);

        this.onSaveFinished =
        (SaveFinishedEvent e) -> {
            if (isAutoTriggered && MainContext.getConfiguration().getParameterValue(ConfigurationConstants.AUTO_SUBMIT, Boolean.class))
                EventSystem.sendEvent(new StartSubmissionEvent());
            else
                EventSystem.sendEvent(new ChangeStateEvent(new IdleState()));
        };
    }


    @Override
    public void onStateEnter()
    {
        super.onStateEnter();
        EventSystem.addListener(DocumentSavedEvent.class, onDocumentSaved);
        EventSystem.addListener(SaveFinishedEvent.class, onSaveFinished);
        EventSystem.addListener(SubmissionStartedEvent.class, StateEventHandlerConstants.ON_SUBMISSION_STARTED);

    }


    @Override
    public void onStateLeave()
    {
        super.onStateLeave();
        EventSystem.removeListener(DocumentSavedEvent.class, onDocumentSaved);
        EventSystem.removeListener(SaveFinishedEvent.class, onSaveFinished);
        EventSystem.removeListener(SubmissionStartedEvent.class, StateEventHandlerConstants.ON_SUBMISSION_STARTED);
    }


    @Override
    public String getStatusString()
    {
        HarvestTimeKeeper timeKeeper = MainContext.getTimeKeeper();
        return String.format(
                   StateConstants.IDLE_STATUS,
                   timeKeeper.getHarvestMeasure().toString(),
                   super.getStatusString(),
                   timeKeeper.getSubmissionMeasure().toString()

               );
    }


    @Override
    public String getName()
    {
        return StateConstants.SAVE_PROCESS;
    }


    @Override
    public String startHarvest()
    {
        return StateConstants.CANNOT_START_PREFIX + StateConstants.SAVE_IN_PROGRESS;
    }


    @Override
    public String pause()
    {
        // TODO implement pause
        return null;
    }


    @Override
    public String resume()
    {
        // TODO implement pause
        return String.format(
                   StateConstants.CANNOT_RESUME_PREFIX + StateConstants.SAVE_IN_PROGRESS,
                   StateConstants.SAVE_PROCESS);
    }


    @Override
    public String save()
    {
        return StateConstants.CANNOT_SAVE_PREFIX + StateConstants.SAVE_IN_PROGRESS;
    }


    @Override
    public String submit()
    {
        return StateConstants.CANNOT_SUBMIT_PREFIX + StateConstants.SAVE_IN_PROGRESS;
    }
}
