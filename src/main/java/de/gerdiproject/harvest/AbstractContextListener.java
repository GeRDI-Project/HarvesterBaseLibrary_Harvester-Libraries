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
package de.gerdiproject.harvest;

import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.harvester.AbstractHarvester;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * This class registers a Logger and Harvester when the server is started. A
 * subclass with the @WebListener annotation must be defined.
 *
 * @see javax.servlet.annotation.WebListener
 * @see de.gerdiproject.harvest.harvester.AbstractHarvester
 *
 * @author row
 */
public abstract class AbstractContextListener implements ServletContextListener
{
    /**
     * Creates a name of this harvester service.
     *
     * @return the name of this harvester service
     */
    abstract protected String createServiceName();
    

    /**
     * Creates a harvester for the MainContext.
     *
     * @return an instance of an AbstractHarvester sub-class
     */
    abstract protected AbstractHarvester createHarvester();


    /**
     * This method is called when the server is set up. Creates a logger and
     * harvester and sets them in the MainContext.
     *
     * @param sce the servlet context event that was initialized
     * @see de.gerdiproject.harvest.MainContext
     */
    @Override
    public void contextInitialized( ServletContextEvent sce )
    {
    	// create service name
    	String name = createServiceName();
    	
        // create singleton logger and harvester
        AbstractHarvester harvester = createHarvester();

        // init main context
        MainContext.init( name, harvester );

        // try to load configuration
        Configuration.loadFromDisk();
    }


    /**
     * This method is called when the server shuts down. Currently does nothing.
     *
     * @param sce the servlet context event that was destroyed
     */
    @Override
    public void contextDestroyed( ServletContextEvent sce )
    {
    }

}
