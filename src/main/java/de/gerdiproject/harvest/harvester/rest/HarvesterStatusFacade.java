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
package de.gerdiproject.harvest.harvester.rest;


import de.gerdiproject.harvest.state.StateMachine;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.harvester.events.GetProviderNameEvent;

import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;


/**
 * A restful facade for the harvester that allows to retrieve additional information
 * about the harvester service.
 *
 * @author Robin Weiss
 */
@Path("status")
public final class HarvesterStatusFacade
{
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
    public String getInfo()
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
}
