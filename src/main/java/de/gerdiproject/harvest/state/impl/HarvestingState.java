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

import javax.ws.rs.core.Response;

import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.harvester.events.DocumentsHarvestedEvent;
import de.gerdiproject.harvest.harvester.events.HarvestFinishedEvent;
import de.gerdiproject.harvest.save.events.SaveStartedEvent;
import de.gerdiproject.harvest.state.AbstractProgressingState;
import de.gerdiproject.harvest.state.constants.StateConstants;
import de.gerdiproject.harvest.state.constants.StateEventHandlerConstants;
import de.gerdiproject.harvest.submission.events.SubmissionStartedEvent;
import de.gerdiproject.harvest.utils.ServerResponseFactory;
import de.gerdiproject.harvest.utils.time.HarvestTimeKeeper;

/**
 * This state indicates that a harvest is currently in progress.
 *
 * @author Robin Weiss
 */
public class HarvestingState extends AbstractProgressingState
{
    /**
     * Constructor that requires the maximum amount of harvestable documents.
     *
     * @param maxNumberOfHarvestedDocuments the maximum amount of harvestable
     *            documents
     */
    public HarvestingState(int maxNumberOfHarvestedDocuments)
    {
        super(maxNumberOfHarvestedDocuments);
    }

    /**
     * Event callback: If a document is harvested, add 1 to the progress.
     */
    private final Consumer<DocumentsHarvestedEvent> onDocumentHarvested =
        (DocumentsHarvestedEvent e) -> addProgress(e.getDocumentCount());


    @Override
    public void onStateEnter()
    {
        super.onStateEnter();

        EventSystem.addListener(HarvestFinishedEvent.class, StateEventHandlerConstants.ON_HARVEST_FINISHED);
        EventSystem.addListener(SubmissionStartedEvent.class, StateEventHandlerConstants.ON_SUBMISSION_STARTED);
        EventSystem.addListener(SaveStartedEvent.class, StateEventHandlerConstants.ON_SAVE_STARTED);
        EventSystem.addListener(DocumentsHarvestedEvent.class, onDocumentHarvested);
    }


    @Override
    public void onStateLeave()
    {
        super.onStateLeave();

        EventSystem.removeListener(HarvestFinishedEvent.class, StateEventHandlerConstants.ON_HARVEST_FINISHED);
        EventSystem.removeListener(SubmissionStartedEvent.class, StateEventHandlerConstants.ON_SUBMISSION_STARTED);
        EventSystem.removeListener(SaveStartedEvent.class, StateEventHandlerConstants.ON_SAVE_STARTED);
        EventSystem.removeListener(DocumentsHarvestedEvent.class, onDocumentHarvested);
    }


    @Override
    public String getStatusString()
    {
        HarvestTimeKeeper timeKeeper = MainContext.getTimeKeeper();
        return String.format(
                   StateConstants.IDLE_STATUS,
                   super.getStatusString(),
                   timeKeeper.getSaveMeasure().toString(),
                   timeKeeper.getSubmissionMeasure().toString());
    }


    @Override
    public Response startHarvest()
    {
        return ServerResponseFactory.createBusyResponse(
                   StateConstants.CANNOT_START_PREFIX + StateConstants.HARVEST_IN_PROGRESS,
                   estimateRemainingSeconds());
    }


    @Override
    public Response submit()
    {
        return ServerResponseFactory.createBusyResponse(
                   StateConstants.CANNOT_SUBMIT_PREFIX + StateConstants.HARVEST_IN_PROGRESS,
                   estimateRemainingSeconds());
    }


    @Override
    public Response save()
    {
        return ServerResponseFactory.createBusyResponse(
                   StateConstants.CANNOT_SAVE_PREFIX + StateConstants.HARVEST_IN_PROGRESS,
                   estimateRemainingSeconds());
    }


    @Override
    public Response isOutdated()
    {
        return ServerResponseFactory.createBusyResponse(
                   StateConstants.CANNOT_PROCESS_PREFIX + StateConstants.HARVEST_IN_PROGRESS,
                   estimateRemainingSeconds());
    }


    @Override
    public String getName()
    {
        return StateConstants.HARVESTING_PROCESS;
    }
}
