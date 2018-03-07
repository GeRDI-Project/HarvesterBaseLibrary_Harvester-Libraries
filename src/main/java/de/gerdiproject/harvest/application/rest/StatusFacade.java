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
package de.gerdiproject.harvest.application.rest;


import de.gerdiproject.harvest.state.IState;
import de.gerdiproject.harvest.state.StateMachine;
import de.gerdiproject.harvest.state.impl.ErrorState;
import de.gerdiproject.harvest.utils.cache.events.GetCacheCountEvent;
import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.application.constants.StatusConstants;
import de.gerdiproject.harvest.application.enums.HealthStatus;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.harvester.events.GetMaxDocumentCountEvent;
import de.gerdiproject.harvest.harvester.events.GetProviderNameEvent;


import java.time.Instant;

import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;


/**
 * A restful facade for the harvester service that allows to retrieve additional information.
 *
 * @author Robin Weiss
 */
@Path("status")
public final class StatusFacade
{
    /**
     * Displays the possible HTTP requests of this facade.
     *
     * @return the possible HTTP requests of this facade
     */
    @GET
    @Produces({
        MediaType.TEXT_PLAIN
    })
    public String getInfo()
    {
        return String.format(StatusConstants.REST_INFO, MainContext.getModuleName());
    }

    /**
     * Displays the name of the current state of the harvester.
     *
     * @return the name of the current state of the harvester
     */
    @GET
    @Path("state")
    @Produces({
        MediaType.TEXT_PLAIN
    })
    public String getStateName()
    {
        return StateMachine.getCurrentState().getName();
    }


    /**
     * Returns the name of the data provider that is harvested.
     *
     * @return the name of the data provider that is harvested
     */
    @GET
    @Path("data-provider")
    @Produces({
        MediaType.TEXT_PLAIN
    })
    public String getDataProvider()
    {
        return EventSystem.sendSynchronousEvent(new GetProviderNameEvent());
    }


    /**
     * Calculates the maximum number of documents that can possibly be harvested.
     * If this number cannot be calculated, "N/A" is returned instead.
     *
     * @return the maximum number of documents that can possibly be harvested
     */
    @GET
    @Path("max-documents")
    @Produces({
        MediaType.TEXT_PLAIN
    })
    public String getMaxDocumentCount()
    {
        Integer maxDocs = EventSystem.sendSynchronousEvent(new GetMaxDocumentCountEvent());

        if (maxDocs == null || maxDocs < 0)
            return StatusConstants.NOT_AVAILABLE;
        else
            return maxDocs.toString();
    }

    /**
     * Retrieves the amount of documents that were harvested and are currently
     * cached.
     *
     * @return the amount of documents that were harvested
     */
    @GET
    @Path("harvested-documents")
    @Produces({
        MediaType.TEXT_PLAIN
    })
    public String getHarvestedDocumentCount()
    {
        Integer cachedDocs = EventSystem.sendSynchronousEvent(new GetCacheCountEvent());

        if (cachedDocs == null)
            return StatusConstants.NOT_AVAILABLE;
        else
            return cachedDocs.toString();
    }

    /**
     * Returns the progress of the current state.
     * If the current state has no progress, "N/A" is returned instead.
     *
     * @return the maximum number of documents that can possibly be harvested
     */
    @GET
    @Path("progress")
    @Produces({
        MediaType.TEXT_PLAIN
    })
    public String getProgress()
    {
        return StateMachine.getCurrentState().getProgress();
    }


    /**
     * Retrieves a formatted timestamp of the time at which the harvest started,
     * or "N/A" if no harvest was started yet.
     *
     * @return a formatted timestamp or "N/A" if no harvest was started yet
     */
    @GET
    @Path("harvest-timestamp")
    @Produces({
        MediaType.TEXT_PLAIN
    })
    public String getHarvestStartTimestamp()
    {
        long timestamp = MainContext.getTimeKeeper().getHarvestMeasure().getStartTimestamp();

        if (timestamp == -1L)
            return StatusConstants.NOT_AVAILABLE;
        else
            return Instant.ofEpochMilli(timestamp).toString();
    }


    /**
     * Displays the name of the current state of the harvester.
     *
     * @return the name of the current state of the harvester
     */
    @GET
    @Path("health")
    @Produces({
        MediaType.TEXT_PLAIN
    })
    public String getHealth()
    {
        final IState currentState = StateMachine.getCurrentState();
        HealthStatus health = HealthStatus.OK;

        if (currentState instanceof ErrorState)
            health = HealthStatus.FUBAR;
        else {
            final String status = currentState.getStatusString();

            final boolean hasHarvestFailed = status.contains(StatusConstants.FAILED_HARVEST_HEALTH_CHECK);

            if (hasHarvestFailed)
                health = HealthStatus.HARVEST_FAILED;
            else {
                final boolean hasSavingFailed = status.contains(StatusConstants.FAILED_SAVE_HEALTH_CHECK);
                final boolean hasSubmissionFailed = status.contains(StatusConstants.FAILED_SUBMISSION_HEALTH_CHECK);

                if (hasSavingFailed && hasSubmissionFailed)
                    health = HealthStatus.SAVING_AND_SUBMISSION_FAILED;
                else if (hasSavingFailed)
                    health = HealthStatus.SAVING_FAILED;
                else if (hasSubmissionFailed)
                    health = HealthStatus.SUBMISSION_FAILED;
            }
        }

        return health.toString();
    }
}
