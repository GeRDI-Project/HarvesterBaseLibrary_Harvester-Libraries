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
package de.gerdiproject.harvest.config.rest;

import de.gerdiproject.harvest.config.Configuration;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Provides the option to save all configuration of the harvester service to
 * disk.
 *
 * @author row
 */
@Path("config")
public class ConfigurationFacade
{
    /**
     * Displays an info string that summerizes the current configuration.
     *
     * @return an info string
     */
    @GET
    @Produces(
            {
                MediaType.TEXT_PLAIN
            })
    public String getInfo()
    {
        return Configuration.getInfoString();
    }


    /**
     * Saves the configuration to disk.
     *
     * @return an info message that describes the status of the operation
     */
    @POST
    @Produces(
            {
                MediaType.TEXT_PLAIN
            })
    public String saveToDisk()
    {
        return Configuration.saveToDisk();
    }
}
