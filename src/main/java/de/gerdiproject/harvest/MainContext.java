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
package de.gerdiproject.harvest;


import java.nio.charset.Charset;
import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gerdiproject.harvest.application.constants.ApplicationConstants;
import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.config.parameters.AbstractParameter;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.harvester.AbstractHarvester;
import de.gerdiproject.harvest.harvester.events.HarvesterInitializedEvent;
import de.gerdiproject.harvest.save.HarvestSaver;
import de.gerdiproject.harvest.state.StateMachine;
import de.gerdiproject.harvest.state.impl.InitializationState;
import de.gerdiproject.harvest.submission.AbstractSubmitter;
import de.gerdiproject.harvest.scheduler.Scheduler;
import de.gerdiproject.harvest.utils.CancelableFuture;
import de.gerdiproject.harvest.utils.HashGenerator;
import de.gerdiproject.harvest.utils.time.HarvestTimeKeeper;


/**
 * This class provides static methods for retrieving application singleton
 * utility and configuration classes.
 *
 * @author Robin Weiss
 */
public class MainContext
{
    private String moduleName;

    private static final Logger LOGGER = LoggerFactory.getLogger(MainContext.class);

    private HarvestTimeKeeper timeKeeper;
    private AbstractHarvester harvester;
    private Charset charset;
    private Configuration configuration;
    private AbstractSubmitter submitter;
    private Scheduler scheduler;

    private static MainContext instance = new MainContext();


    /**
     * Singleton constructor.
     */
    private MainContext()
    {
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
     * @param harvesterParams additional parameters, specific to the harvester,
     *            or null
     * @param submitter the class responsible for submitting documents to a
     *            search index
     *
     * @see de.gerdiproject.harvest.harvester.AbstractHarvester
     */
    public static <T extends AbstractHarvester> void init(String moduleName, Class<T> harvesterClass, Charset charset, List<AbstractParameter<?>> harvesterParams, AbstractSubmitter submitter)
    {
        instance.moduleName = moduleName;
        instance.charset = charset;
        instance.timeKeeper = new HarvestTimeKeeper();
        instance.timeKeeper.init();

        StateMachine.setState(new InitializationState());

        // init harvester
        CancelableFuture<Boolean> initProcess = new CancelableFuture<>(() -> {
            LOGGER.info(ApplicationConstants.INIT_HARVESTER_START);

            // init HashGenerator
            HashGenerator.init(charset);

            // initialize saver and submitter
            HarvestSaver.init();
            instance.submitter = submitter;
            instance.submitter.init();

            // initialize harvester
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

            // init scheduler
            instance.scheduler = new Scheduler();
            instance.scheduler.init();

            return true;
        });

        initProcess.thenApply(onHarvesterInitializedSuccess).exceptionally(onHarvesterInitializedFailed);
    }


    /**
     * This function is called when the asynchronous harvester initialization
     * completes successfully. It logs the success and changes the state
     * machine's current state.
     */
    private static Function<Boolean, Boolean> onHarvesterInitializedSuccess = (Boolean state) -> {

        // log sucess
        LOGGER.info(String.format(ApplicationConstants.INIT_HARVESTER_SUCCESS, getModuleName()));

        // change state
        EventSystem.sendEvent(new HarvesterInitializedEvent(state));

        return state;
    };


    /**
     * This function is called when the asynchronous harvester fails to be
     * initialized. It logs the exception that caused the failure and changes
     * the state machine's current state.
     */
    private static Function<Throwable, Boolean> onHarvesterInitializedFailed = (Throwable reason) -> {

        // log exception that caused the failure
        LOGGER.error(ApplicationConstants.INIT_HARVESTER_FAILED, reason.getCause());

        // change stage
        EventSystem.sendEvent(new HarvesterInitializedEvent(false));

        return false;
    };
}
