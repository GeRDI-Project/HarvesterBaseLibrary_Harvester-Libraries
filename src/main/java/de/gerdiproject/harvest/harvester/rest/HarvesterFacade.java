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
@Path("harvest")
public class HarvesterFacade
{
    private static final String INFO = "- %s -%n%nStatus:\t\t%s%n%nRange:\t\t%s-%s%n%n"
                                       + "POST\t\t\tStarts the harvest%n"
                                       + "POST/abort\t\tAborts an ongoing harvest%n"
                                       + "POST/submit\t\tSubmits harvested documents to a DataBase%n"
                                       + "POST/save\t\tSaves harvested documents to disk";

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
        String status = StateMachine.getCurrentState().getProgressString();

        // get harvesting range
        Configuration config = MainContext.getConfiguration();
        String from = config.getParameterStringValue(ConfigurationConstants.HARVEST_START_INDEX);
        String to = config.getParameterStringValue(ConfigurationConstants.HARVEST_END_INDEX);

        // get harvester name
        String name = MainContext.getModuleName();

        return String.format(INFO, name, status, from, to);
    }


    /**
     * Displays the search index or an empty object if nothing was harvested.
     *
     * @return the search index or an empty object if nothing was harvested
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
     * Displays the search index or an empty object if nothing was harvested.
     *
     * @return the search index or an empty object if nothing was harvested
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
     * Displays the search index or an empty object if nothing was harvested.
     *
     * @return the search index or an empty object if nothing was harvested
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
