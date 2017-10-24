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

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.save.events.DocumentSavedEvent;
import de.gerdiproject.harvest.save.events.SaveFinishedEvent;
import de.gerdiproject.harvest.state.AbstractProgressingState;
import de.gerdiproject.harvest.state.IState;
import de.gerdiproject.harvest.state.constants.StateConstants;
import de.gerdiproject.harvest.state.events.ChangeStateEvent;

/**
 * This state represents the process of permanently saving the harvested documents to disk.
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
            IState nextState;

            if (isAutoTriggered && MainContext.getConfiguration().getParameterValue(ConfigurationConstants.AUTO_SUBMIT, Boolean.class))
                nextState = new SubmittingState(numberOfDocsToBeSaved);
            else
                nextState = new IdleState();

            EventSystem.sendEvent(new ChangeStateEvent(nextState));
        };
    }


    @Override
    public void onStateEnter()
    {
        super.onStateEnter();
        EventSystem.addListener(DocumentSavedEvent.class, onDocumentSaved);
        EventSystem.addListener(SaveFinishedEvent.class, onSaveFinished);

    }


    @Override
    public void onStateLeave()
    {
        EventSystem.removeListener(DocumentSavedEvent.class, onDocumentSaved);
        EventSystem.removeListener(SaveFinishedEvent.class, onSaveFinished);
    }


    @Override
    public String getProgressString()
    {
        return StateConstants.INIT_STATUS;
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
    public String abort()
    {
        EventSystem.sendEvent(new ChangeStateEvent(new AbortingState(StateConstants.SAVE_PROCESS)));
        return String.format(StateConstants.ABORT_STATUS, StateConstants.SAVE_PROCESS);
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
        return String.format(
                   StateConstants.CANNOT_RESUME_PREFIX + StateConstants.SAVE_IN_PROGRESS,
                   StateConstants.SAVE_PROCESS);
    }


    @Override
    public List<String> getAllowedParameters()
    {
        return Arrays.asList(ConfigurationConstants.AUTO_SAVE,
                             ConfigurationConstants.AUTO_SUBMIT,
                             ConfigurationConstants.WRITE_HTTP_TO_DISK,
                             ConfigurationConstants.READ_HTTP_FROM_DISK,
                             ConfigurationConstants.HARVEST_START_INDEX,
                             ConfigurationConstants.HARVEST_END_INDEX,
                             ConfigurationConstants.SUBMISSION_URL,
                             ConfigurationConstants.SUBMISSION_USER_NAME,
                             ConfigurationConstants.SUBMISSION_PASSWORD,
                             ConfigurationConstants.SUBMISSION_SIZE
                            );
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
