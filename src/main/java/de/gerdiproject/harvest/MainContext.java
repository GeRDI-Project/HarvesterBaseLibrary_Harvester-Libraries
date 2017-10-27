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
import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.config.parameters.AbstractParameter;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.harvester.AbstractHarvester;
import de.gerdiproject.harvest.harvester.events.HarvesterInitializedEvent;
import de.gerdiproject.harvest.utils.CancelableFuture;
import de.gerdiproject.harvest.utils.time.HarvestTimeKeeper;


/**
 * This class provides static methods for retrieving the application name, the
 * dedicated harvester class and logger.
 *
 * @author Robin Weiss
 */
public class MainContext
{
    private String moduleName;

    private static final String INIT_START = "Initializing Harvester...";
    private static final String INIT_FAILED = "Could not initialize Harvester!";
    private static final String INIT_SUCCESS = "%s initialized!";

    private static final Logger LOGGER = LoggerFactory.getLogger(MainContext.class);

    private final HarvestTimeKeeper timeKeeper;
    private AbstractHarvester harvester;
    private Charset charset;
    private Configuration configuration;

    private static MainContext instance = new MainContext();


    /**
     * Singleton constructor.
     */
    private MainContext()
    {
        timeKeeper = new HarvestTimeKeeper();
    }


    /**
     * Returns the name of the application.
     *
     * @return the module name
     */
    public static String getModuleName()
    {
        return instance.moduleName;
    }

    /**
     * Retrieves the charset used for processing strings.
     *
     * @return the charset that is used for processing strings
     */
    public static Charset getCharset()
    {
        return instance.charset;
    }


    /**
     * Retrieves the global configuration.
     *
     * @return the harvester configuration
     */
    public static Configuration getConfiguration()
    {
        return instance.configuration;
    }



    /**
     * Retrieves a timekeeper that measures certain processes.
     *
     * @return a timekeeper that measures certain processes
     */
    public static HarvestTimeKeeper getTimeKeeper()
    {
        return instance.timeKeeper;
    }


    /**
     * Sets up global parameters and the harvester.
     *
     * @param <T> an AbstractHarvester subclass
     * @param moduleName name of this application
     * @param harvesterClass an AbstractHarvester subclass
     * @param charset the default charset for processing strings
     * @param harvesterParams additional parameters, specific to the harvester, or null
     *
     * @see de.gerdiproject.harvest.harvester.AbstractHarvester
     */
    public static <T extends AbstractHarvester> void init(String moduleName, Class<T> harvesterClass, Charset charset, List<AbstractParameter<?>> harvesterParams)
    {
        // set global parameters
        instance.moduleName = moduleName;
        instance.charset = charset;

        // init harvester
        CancelableFuture<Boolean> initProcess = new CancelableFuture<>(() -> {
            LOGGER.info(INIT_START);
            instance.harvester = harvesterClass.newInstance();
            instance.harvester.setAsMainHarvester();

            // try to load the configuration from disk
            Configuration config = Configuration.createFromDisk();

            // create a new configuration
            if (config == null)
                config = new Configuration(harvesterParams);

            instance.configuration = config;

            // initialize the harvester properly (relies on the configuration)
            instance.harvester.init();

            // update the harvesting range
            config.updateParameter(ConfigurationConstants.HARVEST_START_INDEX);
            config.updateParameter(ConfigurationConstants.HARVEST_END_INDEX);

            return true;
        });

        initProcess.thenApply(onHarvesterInitializedSuccess)
        .exceptionally(onHarvesterInitializedFailed);
    }


    /**
     * This function is called when the asynchronous harvester initialization completes successfully.
     * It logs the success and changes the state machine's current state.
     */
    private static Function<Boolean, Boolean> onHarvesterInitializedSuccess = (Boolean state) -> {

        // log sucess
        LOGGER.info(String.format(INIT_SUCCESS, getModuleName()));

        // change state
        EventSystem.sendEvent(new HarvesterInitializedEvent(state));

        return state;
    };


    /**
     * This function is called when the asynchronous harvester fails to be initialized.
     * It logs the exception that caused the failure and changes the state machine's current state.
     */
    private static Function<Throwable, Boolean> onHarvesterInitializedFailed = (Throwable reason) -> {

        // log exception that caused the failure
        LOGGER.error(INIT_FAILED, reason.getCause());

        // change stage
        EventSystem.sendEvent(new HarvesterInitializedEvent(false));

        return false;
    };
}
