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
import de.gerdiproject.harvest.event.IEventListener;
import de.gerdiproject.harvest.harvester.AbstractHarvester;
import de.gerdiproject.harvest.harvester.events.HarvesterInitializedEvent;
import de.gerdiproject.harvest.save.HarvestSaver;
import de.gerdiproject.harvest.scheduler.Scheduler;
import de.gerdiproject.harvest.state.StateMachine;
import de.gerdiproject.harvest.state.impl.InitializationState;
import de.gerdiproject.harvest.submission.AbstractSubmitter;
import de.gerdiproject.harvest.utils.CancelableFuture;
import de.gerdiproject.harvest.utils.logger.HarvesterLog;
import de.gerdiproject.harvest.utils.logger.constants.LoggerConstants;
import de.gerdiproject.harvest.utils.logger.events.GetMainLogEvent;
import de.gerdiproject.harvest.utils.maven.MavenUtils;
import de.gerdiproject.harvest.utils.maven.events.GetMavenUtilsEvent;
import de.gerdiproject.harvest.utils.time.HarvestTimeKeeper;


/**
 * This class provides static methods for retrieving application singleton
 * utility and configuration classes.
 *
 * @author Robin Weiss
 */
public class MainContext implements IEventListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MainContext.class);
    private static MainContext instance = null;

    private final HarvestSaver saver;
    private final HarvesterLog log;
    private final String moduleName;
    private final HarvestTimeKeeper timeKeeper;
    private final AbstractHarvester harvester;
    private final Charset charset;
    private final Configuration configuration;

    @SuppressWarnings("unused") // the submitter is connected via the event system
    private final AbstractSubmitter submitter;
    private final Scheduler scheduler;
    private final MavenUtils mavenUtils;
    private final Function<GetMainLogEvent, HarvesterLog> onGetMainLog;
    private final Function<GetMavenUtilsEvent, MavenUtils> onGetMavenUtils;


    /**
     * Constructs an instance with all necessary helpers and utility objects.
     *
     * @param <T> an AbstractHarvester subclass
     * @param moduleName name of this application
     * @param harvesterClass an AbstractHarvester subclass
     * @param charset the default charset for processing strings
     * @param harvesterParams additional parameters, specific to the harvester,
     *            or null
     * @param submitter the class responsible for submitting documents to a
     *            search index
     * @throws IllegalAccessException can be thrown when the harvester class cannot be instantiated
     * @throws InstantiationException can be thrown when the harvester class cannot be instantiated
     *
     * @see de.gerdiproject.harvest.harvester.AbstractHarvester
     */
    private <T extends AbstractHarvester> MainContext(String moduleName, Class<T> harvesterClass,
                                                      Charset charset, List<AbstractParameter<?>> harvesterParams, AbstractSubmitter submitter) throws InstantiationException, IllegalAccessException
    {
        this.moduleName = moduleName;
        this.charset = charset;

        this.log = new HarvesterLog(String.format(LoggerConstants.LOG_FILE_PATH, moduleName), charset);
        log.registerLogger();
        this.onGetMainLog = (GetMainLogEvent event) -> {return log;};

        this.timeKeeper = new HarvestTimeKeeper(moduleName);
        timeKeeper.loadFromDisk();

        this.mavenUtils = new MavenUtils(harvesterClass);
        this.onGetMavenUtils = (GetMavenUtilsEvent event) -> { return mavenUtils;};

        // initialize saver
        this.saver = new HarvestSaver(moduleName, charset, timeKeeper.getHarvestMeasure());

        // initialize submitter
        this.submitter = submitter;
        submitter.init();

        // initialize harvester
        this.harvester = harvesterClass.newInstance();
        harvester.setAsMainHarvester();

        // initialize the configuration
        this.configuration = new Configuration(harvesterParams);
        configuration.loadFromEnvironmentVariables();
        configuration.loadFromCache();

        // initialize the harvester properly (relies on the configuration)
        harvester.init();

        // update the harvesting range
        configuration.updateParameter(ConfigurationConstants.HARVEST_START_INDEX);
        configuration.updateParameter(ConfigurationConstants.HARVEST_END_INDEX);

        // init scheduler
        this.scheduler = new Scheduler();
        scheduler.init();
    }


    /**
     * Constructs an instance of the MainContext in a dedicated thread.
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
    public static <T extends AbstractHarvester> void init(String moduleName, Class<T> harvesterClass,
                                                          Charset charset, List<AbstractParameter<?>> harvesterParams, AbstractSubmitter submitter)
    {
        LOGGER.info(ApplicationConstants.INIT_HARVESTER_START);
        StateMachine.setState(new InitializationState());

        CancelableFuture<Boolean> initProcess = new CancelableFuture<>(() -> {
            // clear old instance if necessary
            if (instance != null)
                instance.clear();

            instance = new MainContext(moduleName, harvesterClass, charset, harvesterParams, submitter);
            return true;
        });

        initProcess
        .thenApply(MainContext::onHarvesterInitializedSuccess)
        .exceptionally(MainContext::onHarvesterInitializedFailed);
    }


    /**
     * Removes event listeners and clears all connections to the context.
     */
    private void clear()
    {
        removeEventListeners();
        log.unregisterLogger();
    }


    @Override
    public void addEventListeners()
    {
        EventSystem.addSynchronousListener(GetMainLogEvent.class, onGetMainLog);
        EventSystem.addSynchronousListener(GetMavenUtilsEvent.class, onGetMavenUtils);
        saver.addEventListeners();
        timeKeeper.addEventListeners();
    }


    @Override
    public void removeEventListeners()
    {
        EventSystem.removeSynchronousListener(GetMainLogEvent.class);
        EventSystem.removeSynchronousListener(GetMavenUtilsEvent.class);
        saver.removeEventListeners();
        timeKeeper.removeEventListeners();
    }


    /**
     * Returns the name of the application.
     *
     * @return the module name
     */
    public static String getModuleName()
    {
        return instance != null ? instance.moduleName : null;
    }


    /**
     * Retrieves the charset used for processing strings.
     *
     * @return the charset that is used for processing strings
     */
    public static Charset getCharset()
    {
        return instance != null ? instance.charset : null;
    }


    /**
     * Retrieves the global configuration.
     *
     * @return the harvester configuration
     */
    public static Configuration getConfiguration()
    {
        return instance != null ? instance.configuration : null;
    }


    /**
     * Retrieves a timekeeper that measures certain processes.
     *
     * @return a timekeeper that measures certain processes
     * or null, if the main context was not initialized
     */
    public static HarvestTimeKeeper getTimeKeeper()
    {
        return instance != null ? instance.timeKeeper : null;
    }


    /**
     * This function is called when the asynchronous harvester initialization
     * completes successfully. It logs the success and changes the state
     * machine's current state.
     *
     * @param state if true, the harvester was initialized successfully
     *
     * @return true, if the harvester was initialized successfully
     */
    private static Boolean onHarvesterInitializedSuccess(Boolean state)
    {
        // log sucess
        LOGGER.info(String.format(ApplicationConstants.INIT_HARVESTER_SUCCESS, getModuleName()));

        // change state
        EventSystem.sendEvent(new HarvesterInitializedEvent(state));

        instance.addEventListeners();
        return state;
    };


    /**
     * This function is called when the asynchronous harvester fails to be
     * initialized. It logs the exception that caused the failure and changes
     * the state machine's current state.
     *
     * @param reason the cause of the initialization failure
     *
     * @return false
     */
    private static Boolean onHarvesterInitializedFailed(Throwable reason)
    {
        // log exception that caused the failure
        LOGGER.error(ApplicationConstants.INIT_HARVESTER_FAILED, reason.getCause());

        // change stage
        EventSystem.sendEvent(new HarvesterInitializedEvent(false));

        instance.addEventListeners();
        return false;
    };
}
