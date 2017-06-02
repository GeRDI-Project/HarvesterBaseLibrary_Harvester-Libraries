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

import de.gerdiproject.harvest.harvester.AbstractHarvester;
import de.gerdiproject.logger.ILogger;
import de.gerdiproject.logger.impl.LoggerQueue;

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

    private AbstractHarvester harvester;
    private ILogger logger;

    private static MainContext instance;


    /**
     * Singleton constructor. Initializes the logger with a queue. Once a
     * different logger is set, everything stored within the queue will be
     * logged.
     */
    private MainContext()
    {
        logger = new LoggerQueue();
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
     * @param newLogger an ILogger implementation
     * @see de.gerdiproject.logger.ILogger
     * @see de.gerdiproject.harvest.harvester.AbstractHarvester
     */
    public static void init( String moduleName, AbstractHarvester harvester, ILogger newLogger )
    {
        if (instance == null)
        {
            instance = new MainContext();
        }
        else
        {
            // potentially log queued messages
            if (instance.logger instanceof LoggerQueue)
            {
                LoggerQueue oldLogger = (LoggerQueue) instance.logger;
                oldLogger.logQueuedMessages( newLogger );
            }
        }

        // set module name
        instance.moduleName = moduleName;

        // set logger
        newLogger.log( String.format( INIT_START, newLogger.getClass().getSimpleName() ) );
        instance.logger = newLogger;
        newLogger.log( DONE );

        // init harvester
        newLogger.log( String.format( INIT_START, harvester.getClass().getSimpleName() ) );
        harvester.init( newLogger );

        instance.harvester = harvester;
        newLogger.log( DONE );

        // log ready state
        newLogger.log( String.format( INIT_FINISHED, instance.moduleName ) );
    }


    /**
     * Returns the logger singleton instance of this application
     *
     * @return this application's logger implementation
     * @see de.gerdiproject.logger.ILogger
     */
    public static ILogger getLogger()
    {
        if (instance == null)
        {
            instance = new MainContext();
        }
        return instance.logger;
    }
}
