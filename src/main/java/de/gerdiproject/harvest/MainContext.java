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


import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gerdiproject.harvest.harvester.AbstractHarvester;


/**
 * This class provides static methods for retrieving the application name, the
 * dedicated harvester class and logger.
 *
 *
 * @author Robin Weiss
 */
public class MainContext
{
    private String moduleName;

    private static final String INIT_FINISHED = "%s is now ready!";
    private static final String INIT_START = "Initializing %s...";
    private static final String DONE = "done!";
    private static final String FAILED = "FAILED!";
    private static final Logger LOGGER = LoggerFactory.getLogger(MainContext.class);

    private AbstractHarvester harvester;
    private Charset charset;

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
            instance = new MainContext();

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
            instance = new MainContext();

        return instance.moduleName;
    }

    /**
     * Retrieves the charset used for processing strings.
     * @return the charset that is used for processing strings
     */
    public static Charset getCharset()
    {
        if (instance == null)
            instance = new MainContext();

        return instance.charset;
    }


    /**
     * Sets up global parameters and the harvester.
     *
     * @param <T>
     *            an AbstractHarvester subclass
     * @param moduleName
     *            name of this application
     * @param harvesterClass
     *            an AbstractHarvester subclass
     * @param charset
     *            the default charset for processing strings
     * @see de.gerdiproject.harvest.harvester.AbstractHarvester
     */
    public static <T extends AbstractHarvester> void init(String moduleName, Class<T> harvesterClass, Charset charset)
    {
        if (instance == null)
            instance = new MainContext();

        // set parameters
        instance.moduleName = moduleName;
        instance.charset = charset;

        // init harvester
        try {
            AbstractHarvester harvey = harvesterClass.newInstance();

            LOGGER.info(String.format(INIT_START, harvey.getName()));
            instance.harvester = harvey;
            LOGGER.info(DONE);
        } catch (InstantiationException | IllegalAccessException e) {
            LOGGER.info(FAILED);
            LOGGER.error(e.toString());
            return;
        }

        // log ready state
        LOGGER.info(String.format(INIT_FINISHED, instance.moduleName));
    }
}
