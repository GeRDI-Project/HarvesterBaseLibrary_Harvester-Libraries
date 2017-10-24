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
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.impl.HarvestStartedEvent;
import de.gerdiproject.harvest.event.impl.SaveStartedEvent;
import de.gerdiproject.harvest.event.impl.StartSaveEvent;
import de.gerdiproject.harvest.event.impl.StartHarvestEvent;
import de.gerdiproject.harvest.event.impl.StartSubmissionEvent;
import de.gerdiproject.harvest.event.impl.SubmissionStartedEvent;
import de.gerdiproject.harvest.state.StateMachine;
import de.gerdiproject.harvest.state.IState;
import de.gerdiproject.harvest.state.constants.StateConstants;
import de.gerdiproject.harvest.state.constants.StateEventHandlerConstants;

/**
 * This state indicates it is waiting for a harvest to start.
 *
 * @author Robin Weiss
 */
public class IdleState implements IState
{
    private static final Logger LOGGER = LoggerFactory.getLogger(StateMachine.class);

    private final Date harvestFinishedDate;
    private final Date docsSavedDate;
    private final Date docsSubmittedDate;


    /**
     * Constructor that represents an idle state before harvesting.
     */
    public IdleState()
    {
        this(null, null, null);
    }


    /**
     * Constructor that represents an idle state after a harvest.
     * @param harvestFinishedDate the date at which the harvest was concluded, or null if no harvest finished yet
     * @param docsSavedDate the date at which the harvested documents were saved to disk, or null if nothing was saved
     * @param docsSubmittedDate the date at which the harvest was sent to an external database, or null if nothing was submitted
     */
    public IdleState(Date harvestFinishedDate, Date docsSavedDate, Date docsSubmittedDate)
    {
        this.harvestFinishedDate = harvestFinishedDate;
        this.docsSavedDate = docsSavedDate;
        this.docsSubmittedDate = docsSubmittedDate;
    }


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
    public String getProgressString()
    {
        StringBuilder statusBuilder = new StringBuilder();

        if (harvestFinishedDate != null)
            statusBuilder.append(String.format(StateConstants.HARVEST_FINISHED_AT, harvestFinishedDate.toString()));

        if (docsSavedDate != null)
            statusBuilder.append(String.format(StateConstants.HARVEST_SAVED_AT, docsSavedDate.toString()));

        if (docsSubmittedDate != null)
            statusBuilder.append(String.format(StateConstants.HARVEST_SUBMITTED_AT, docsSubmittedDate.toString()));

        if (statusBuilder.length() == 0)
            statusBuilder.append(StateConstants.HARVEST_NOT_STARTED);

        return statusBuilder.toString();
    }


    @Override
    public String startHarvest()
    {
        EventSystem.sendEvent(new StartHarvestEvent());
        return StateConstants.HARVEST_STARTED;
    }


    @Override
    public String abort()
    {
        return String.format(
                   StateConstants.CANNOT_ABORT_PREFIX + StateConstants.NO_HARVEST_IN_PROGRESS,
                   StateConstants.HARVESTING_PROCESS);
    }


    @Override
    public String pause()
    {
        return String.format(
                   StateConstants.CANNOT_PAUSE_PREFIX + StateConstants.NO_HARVEST_IN_PROGRESS,
                   StateConstants.HARVESTING_PROCESS);
    }


    @Override
    public String resume()
    {
        return String.format(
                   StateConstants.CANNOT_RESUME_PREFIX + StateConstants.NO_HARVEST_IN_PROGRESS,
                   StateConstants.HARVESTING_PROCESS);
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
    public String submit()
    {
        EventSystem.sendEvent(new StartSubmissionEvent());
        return StateConstants.SUBMITTING_STATUS;
    }


    @Override
    public String save()
    {
        EventSystem.sendEvent(new StartSaveEvent());
        return StateConstants.SAVING_STATUS;
    }


    @Override
    public String getName()
    {
        return StateConstants.IDLE_PROCESS;
    }


}
