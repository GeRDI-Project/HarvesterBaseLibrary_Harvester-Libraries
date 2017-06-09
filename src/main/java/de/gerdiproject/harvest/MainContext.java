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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gerdiproject.harvest.harvester.AbstractHarvester;

/**
 * This class provides static methods for retrieving the application name, the
 * dedicated harvester class and logger.
 *
 *
 * @author row
 */
public class MainContext
{
    private String moduleName;

    private static final String INIT_FINISHED = "%s is now ready!";
    private static final String INIT_START = "Initializing %s...";
    private static final String DONE = "done!";

	private static final Logger LOGGER = LoggerFactory.getLogger( MainContext.class );
	
    private AbstractHarvester harvester;

    private static MainContext instance;


    /**
     * Singleton constructor.
     */
    private MainContext()
    {
    }


    /**
     * Returns the harvester singleton instance of this application
     *
     * @return this application's harvester class
     * @see de.gerdiproject.harvest.harvester.AbstractHarvester
     */
    public static AbstractHarvester getHarvester()
    {
        if (instance == null)
        {
            instance = new MainContext();
        }
        return instance.harvester;
    }


    /**
     * Returns the name of the application.
     *
     * @return the module name
     */
    public static String getModuleName()
    {
        if (instance == null)
        {
            instance = new MainContext();
        }
        return instance.moduleName;
    }


    /**
     * Sets the global harvester and logger instances.
     *
     * @param moduleName name of this application
     * @param harvester an AbstractHarvester subclass
     * @see de.gerdiproject.harvest.harvester.AbstractHarvester
     */
    public static void init( String moduleName, AbstractHarvester harvester )
    {
        if (instance == null)
        {
            instance = new MainContext();
        }

        // set module name
        instance.moduleName = moduleName;

        // init harvester
        LOGGER.info( String.format( INIT_START, harvester.getName() ) );
        harvester.init();

        instance.harvester = harvester;
        LOGGER.info( DONE );

        // log ready state
        LOGGER.info( String.format( INIT_FINISHED, instance.moduleName ) );
    }
}
