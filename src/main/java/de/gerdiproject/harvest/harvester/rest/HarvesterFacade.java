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
import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.harvester.constants.HarvesterConstants;

import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;


/**
 * A facade for the harvester that serves as a RESTful interface. It provides
 * REST requests that manipulate the harvester in order to prepare and send
 * search indices to Elastic Search.
 *
 * @see de.gerdiproject.harvest.harvester.AbstractHarvester
 * @author Robin Weiss
 */
@Path("")
public class HarvesterFacade
{
    /**
     * Starts a harvest using the harvester that is registered in the
     * MainContext.
     *
     * @param formParams
     *            optional parameters encompass "from" and "to" to set the
     *            harvest range
     * @return a status string that describes the success or failure of the
     *         harvest
     */
    @POST
    @Produces({
        MediaType.TEXT_PLAIN
    })
    public String startHarvest(final MultivaluedMap<String, String> formParams)
    {
        return StateMachine.getCurrentState().startHarvest();
    }


    /**
     * Displays a text that describes the status and possible REST calls.
     *
     * @return a text describing the harvesting status and available RESTful
     *         calls
     */
    @GET
    @Produces({
        MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON
    })
    public String getInfo()
    {
        String status = StateMachine.getCurrentState().getStatusString();

        // get harvesting range

        String from = "???";
        String to = "???";
        Configuration config = MainContext.getConfiguration();

        if (config != null) {
            from = config.getParameterStringValue(ConfigurationConstants.HARVEST_START_INDEX);
            to = config.getParameterStringValue(ConfigurationConstants.HARVEST_END_INDEX);
        }

        // get harvester name
        String name = MainContext.getModuleName();

        return String.format(HarvesterConstants.REST_INFO, name, status, from, to);
    }


    /**
     * Aborts an ongoing process, such as harvesting, submitting, or saving.
     *
     * @return a status message describing if the abort could be started or not
     */
    @POST
    @Path("abort")
    @Produces({
        MediaType.TEXT_PLAIN
    })
    public String abort()
    {
        return StateMachine.getCurrentState().abort();
    }


    /**
     * Saves harvested documents to disk.
     *
     * @return a status message describing if the saving could be started or not
     */
    @POST
    @Path("save")
    @Produces({
        MediaType.TEXT_PLAIN
    })
    public String saveDocuments()
    {
        return StateMachine.getCurrentState().save();
    }


    /**
     * Submits harvested documents.
     *
     * @return a status message describing if the submission could be started or not
     */
    @POST
    @Path("submit")
    @Produces({
        MediaType.TEXT_PLAIN
    })
    public String submitDocuments()
    {
        return StateMachine.getCurrentState().submit();
    }


    /*
    @GET
    @Path("isLatest")
    @Produces({
        MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON
    })
    public String isLatestVersion()
    {
        final String latestChecksum = ""; // TODO get from disk
        // prospector(?)
        final String currentChecksum = harvester.getHash(true);

        return String.valueOf(currentChecksum != null && !currentChecksum.equals(latestChecksum));
    }
    */
}
