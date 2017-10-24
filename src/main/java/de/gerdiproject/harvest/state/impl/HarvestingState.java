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

import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.impl.ChangeStateEvent;
import de.gerdiproject.harvest.event.impl.DocumentHarvestedEvent;
import de.gerdiproject.harvest.event.impl.HarvestFinishedEvent;
import de.gerdiproject.harvest.event.impl.SaveStartedEvent;
import de.gerdiproject.harvest.event.impl.SubmissionStartedEvent;
import de.gerdiproject.harvest.state.AbstractProgressHarvestState;
import de.gerdiproject.harvest.state.constants.StateConstants;
import de.gerdiproject.harvest.state.constants.StateEventHandlerConstants;

public class HarvestingState extends AbstractProgressHarvestState
{
    public HarvestingState(int maxNumberOfHarvestedDocuments)
    {
        super.maxProgress = maxNumberOfHarvestedDocuments;
    }

    /**
     * If a document is harvested, add 1 to the progress.
     */
    private final Consumer<DocumentHarvestedEvent> onDocumentHarvested = (DocumentHarvestedEvent e) -> addProgress(1);



    @Override
    public void onStateEnter()
    {
        super.onStateEnter();
        EventSystem.addListener(DocumentHarvestedEvent.class, onDocumentHarvested);
        EventSystem.addListener(HarvestFinishedEvent.class, StateEventHandlerConstants.ON_HARVEST_FINISHED);
        EventSystem.addListener(SubmissionStartedEvent.class, StateEventHandlerConstants.ON_SUBMISSION_STARTED);
        EventSystem.addListener(SaveStartedEvent.class, StateEventHandlerConstants.ON_SAVE_STARTED);
    }


    @Override
    public void onStateLeave()
    {
        EventSystem.removeListener(DocumentHarvestedEvent.class, onDocumentHarvested);
        EventSystem.removeListener(HarvestFinishedEvent.class, StateEventHandlerConstants.ON_HARVEST_FINISHED);
        EventSystem.removeListener(SubmissionStartedEvent.class, StateEventHandlerConstants.ON_SUBMISSION_STARTED);
        EventSystem.removeListener(SaveStartedEvent.class, StateEventHandlerConstants.ON_SAVE_STARTED);
    }


    @Override
    public String getName()
    {
        return StateConstants.HARVESTING_PROCESS;
    }


    @Override
    public String startHarvest()
    {
        return StateConstants.CANNOT_START_PREFIX + StateConstants.HARVEST_IN_PROGRESS;
    }


    @Override
    public String abort()
    {
        EventSystem.sendEvent(new ChangeStateEvent(new AbortingState(StateConstants.HARVESTING_PROCESS)));
        return String.format(StateConstants.ABORT_STATUS, StateConstants.HARVESTING_PROCESS);
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
                   StateConstants.CANNOT_RESUME_PREFIX + StateConstants.RESUME_IN_PROGRESS,
                   StateConstants.HARVESTING_PROCESS);
    }


    @Override
    public List<String> getAllowedParameters()
    {
        return Arrays.asList(ConfigurationConstants.AUTO_SAVE,
                             ConfigurationConstants.AUTO_SUBMIT,
                             ConfigurationConstants.SUBMISSION_URL,
                             ConfigurationConstants.SUBMISSION_USER_NAME,
                             ConfigurationConstants.SUBMISSION_PASSWORD,
                             ConfigurationConstants.SUBMISSION_SIZE
                            );
    }


    @Override
    public String submit()
    {
        return StateConstants.CANNOT_SUBMIT_PREFIX + StateConstants.HARVEST_IN_PROGRESS;
    }


    @Override
    public String save()
    {
        return StateConstants.CANNOT_SAVE_PREFIX + StateConstants.HARVEST_IN_PROGRESS;
    }
}
