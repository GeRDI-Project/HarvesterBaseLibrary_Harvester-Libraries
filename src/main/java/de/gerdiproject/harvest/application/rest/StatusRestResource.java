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


import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import de.gerdiproject.harvest.application.MainContext;
import de.gerdiproject.harvest.application.constants.StatusConstants;
import de.gerdiproject.harvest.etls.enums.ETLHealth;
import de.gerdiproject.harvest.rest.HttpResponseFactory;
import de.gerdiproject.harvest.state.IState;
import de.gerdiproject.harvest.state.StateMachine;
import de.gerdiproject.harvest.state.impl.ErrorState;


/**
 * A restful facade for the harvester service that allows to retrieve additional information.
 *
 * @author Robin Weiss
 */
@Path("status")
public final class StatusRestResource
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
        return String.format(StatusConstants.REST_INFO, MainContext.getServiceName());
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
    public Response getHealth()
    {
        final IState currentState = StateMachine.getCurrentState();
        ETLHealth health = ETLHealth.OK;

        if (currentState instanceof ErrorState)
            health = ETLHealth.INITIALIZATION_FAILED;
        else {
            final String status = currentState.getStatusString();

            final boolean hasHarvestFailed = status.contains(StatusConstants.FAILED_HARVEST_HEALTH_CHECK);

            if (hasHarvestFailed)
                health = ETLHealth.HARVEST_FAILED;
        }

        final Status status =  health == ETLHealth.OK
                               ? Status.OK
                               : Status.INTERNAL_SERVER_ERROR;

        return HttpResponseFactory.createResponse(status, health);
    }
}
