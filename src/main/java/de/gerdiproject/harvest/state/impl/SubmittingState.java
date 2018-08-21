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
import de.gerdiproject.harvest.state.AbstractProgressingState;
import de.gerdiproject.harvest.state.constants.StateConstants;
import de.gerdiproject.harvest.state.constants.StateEventHandlerConstants;
import de.gerdiproject.harvest.submission.events.DocumentsSubmittedEvent;
import de.gerdiproject.harvest.submission.events.SubmissionFinishedEvent;
import de.gerdiproject.harvest.utils.ServerResponseFactory;
import de.gerdiproject.harvest.utils.time.HarvestTimeKeeper;

/**
 * This state represents the submission process of harvested documents.
 *
 * @author Robin Weiss
 */
public class SubmittingState extends AbstractProgressingState
{
    /**
     * Event callback that is called when some documents are submitted.
     */
    private final Consumer<DocumentsSubmittedEvent> onDocumentsSubmitted =
        (DocumentsSubmittedEvent e) -> addProgress(e.getNumberOfSubmittedDocuments());


    /**
     * Constructor that requires the number of documents that are to be
     * submitted.
     *
     * @param numberOfDocsToBeSubmitted the number of documents that are to be
     *            submitted
     */
    public SubmittingState(int numberOfDocsToBeSubmitted)
    {
        super(numberOfDocsToBeSubmitted);
    }


    @Override
    public String getStatusString()
    {
        HarvestTimeKeeper timeKeeper = MainContext.getTimeKeeper();
        return String.format(
                   StateConstants.IDLE_STATUS,
                   timeKeeper.getHarvestMeasure().toString(),
                   super.getStatusString());
    }


    @Override
    public void onStateEnter()
    {
        super.onStateEnter();
        EventSystem.addListener(DocumentsSubmittedEvent.class, onDocumentsSubmitted);
        EventSystem.addListener(SubmissionFinishedEvent.class, StateEventHandlerConstants.ON_SUBMISSION_FINISHED);
    }


    @Override
    public void onStateLeave()
    {
        super.onStateLeave();
        EventSystem.removeListener(DocumentsSubmittedEvent.class, onDocumentsSubmitted);
        EventSystem.removeListener(SubmissionFinishedEvent.class, StateEventHandlerConstants.ON_SUBMISSION_FINISHED);
    }


    @Override
    public Response startHarvest()
    {
        return ServerResponseFactory.createBusyResponse(
                   StateConstants.CANNOT_START_PREFIX + StateConstants.SUBMIT_IN_PROGRESS,
                   estimateRemainingSeconds());
    }


    @Override
    public Response submit()
    {
        return ServerResponseFactory.createBusyResponse(
                   StateConstants.CANNOT_SUBMIT_PREFIX + StateConstants.SUBMIT_IN_PROGRESS,
                   estimateRemainingSeconds());
    }


    @Override
    public Response save()
    {
        return ServerResponseFactory.createBusyResponse(
                   StateConstants.CANNOT_SAVE_PREFIX + StateConstants.SUBMIT_IN_PROGRESS,
                   estimateRemainingSeconds());
    }


    @Override
    public Response isOutdated()
    {
        return ServerResponseFactory.createBusyResponse(
                   StateConstants.CANNOT_PROCESS_PREFIX + StateConstants.SUBMIT_IN_PROGRESS,
                   estimateRemainingSeconds());
    }


    @Override
    public String getName()
    {
        return StateConstants.SUBMIT_PROCESS;
    }
}
