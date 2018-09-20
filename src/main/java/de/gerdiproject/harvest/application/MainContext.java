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
package de.gerdiproject.harvest.application;


import java.nio.charset.Charset;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gerdiproject.harvest.application.constants.ApplicationConstants;
import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.harvester.AbstractHarvester;
import de.gerdiproject.harvest.harvester.events.HarvesterInitializedEvent;
import de.gerdiproject.harvest.save.HarvestSaver;
import de.gerdiproject.harvest.save.constants.SaveConstants;
import de.gerdiproject.harvest.scheduler.Scheduler;
import de.gerdiproject.harvest.scheduler.constants.SchedulerConstants;
import de.gerdiproject.harvest.state.StateMachine;
import de.gerdiproject.harvest.state.impl.InitializationState;
import de.gerdiproject.harvest.submission.AbstractSubmitter;
import de.gerdiproject.harvest.submission.SubmitterManager;
import de.gerdiproject.harvest.submission.constants.SubmissionConstants;
import de.gerdiproject.harvest.utils.CancelableFuture;
import de.gerdiproject.harvest.utils.cache.HarvesterCacheManager;
import de.gerdiproject.harvest.utils.cache.constants.CacheConstants;
import de.gerdiproject.harvest.utils.cache.events.GetNumberOfHarvestedDocumentsEvent;
import de.gerdiproject.harvest.utils.logger.HarvesterLog;
import de.gerdiproject.harvest.utils.logger.constants.LoggerConstants;
import de.gerdiproject.harvest.utils.logger.events.GetMainLogEvent;
import de.gerdiproject.harvest.utils.maven.MavenUtils;
import de.gerdiproject.harvest.utils.maven.events.GetMavenUtilsEvent;
import de.gerdiproject.harvest.utils.time.HarvestTimeKeeper;
import de.gerdiproject.harvest.utils.time.ProcessTimeMeasure.ProcessStatus;


/**
 * This class provides static methods for retrieving application singleton
 * utility and configuration classes.
 *
 * @author Robin Weiss
 */
public class MainContext
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MainContext.class);
    private static volatile MainContext instance = null;

    private final HarvestSaver saver;
    private final HarvesterLog log;
    private final HarvesterCacheManager cacheManager;
    private final String moduleName;
    private final HarvestTimeKeeper timeKeeper;
    private final AbstractHarvester harvester;
    private final Configuration configuration;

    @SuppressWarnings("unused") // the submitter is connected via the event system
    private final SubmitterManager submitterManager;
    private final Scheduler scheduler;
    private final MavenUtils mavenUtils;


    /**
     * Constructs an instance with all necessary helpers and utility objects.
     *
     * @param <T> an AbstractHarvester subclass
     * @param moduleName name of this application
     * @param harvesterClass an AbstractHarvester subclass
     * @param submitterClasses a list of classes responsible for submitting documents to a
     *            search index
     * @throws IllegalAccessException can be thrown when the harvester class cannot be instantiated
     * @throws InstantiationException can be thrown when the harvester class cannot be instantiated
     *
     * @see de.gerdiproject.harvest.harvester.AbstractHarvester
     */
    private <T extends AbstractHarvester> MainContext(String moduleName,
                                                      Class<T> harvesterClass,
                                                      List<Class<? extends AbstractSubmitter>> submitterClasses) throws InstantiationException, IllegalAccessException
    {
        this.moduleName = moduleName;

        this.log = createLog(moduleName);
        EventSystem.addSynchronousListener(GetMainLogEvent.class, this::getMainLog);

        this.configuration = createConfiguration(moduleName);
        this.timeKeeper = createTimeKeeper(moduleName);

        this.mavenUtils = createMavenUtils(harvesterClass);
        EventSystem.addSynchronousListener(GetMavenUtilsEvent.class, this::getMavenUtils);

        this.cacheManager = createCacheManager();
        EventSystem.addSynchronousListener(GetNumberOfHarvestedDocumentsEvent.class, this::getNumberOfHarvestedDocuments);

        this.harvester = createHarvester(moduleName, harvesterClass);
        this.saver = createHarvestSaver(moduleName, harvester.getCharset(), timeKeeper, cacheManager);
        this.submitterManager = createSubmitterManager(submitterClasses, harvester.getCharset(), timeKeeper, cacheManager);
        this.scheduler = createScheduler(moduleName);
    }


    /**
     * Constructs an instance of the MainContext in a dedicated thread.
     *
     * @param <T> an AbstractHarvester subclass
     * @param moduleName name of this application
     * @param harvesterClass an AbstractHarvester subclass
     * @param submitterClasses a list of viable classes responsible for submitting documents to a
     *            search index
     *
     * @see de.gerdiproject.harvest.harvester.AbstractHarvester
     */
    public static <T extends AbstractHarvester> void init(String moduleName, Class<T> harvesterClass,
                                                          List<Class<? extends AbstractSubmitter>> submitterClasses)
    {
        LOGGER.info(ApplicationConstants.INIT_SERVICE);
        StateMachine.setState(new InitializationState());

        CancelableFuture<Boolean> initProcess = new CancelableFuture<>(() -> {
            // clear old instance if necessary
            destroy();

            instance = new MainContext(moduleName, harvesterClass, submitterClasses);
            return true;
        });

        initProcess
        .thenApply(MainContext::onHarvesterInitializedSuccess)
        .exceptionally(MainContext::onHarvesterInitializedFailed);
    }


    /**
     * Clears the current Singleton instance if it exists and nullifies it.
     */
    public static void destroy()
    {
        if (instance != null) {
            instance.removeEventListeners();
            instance.log.unregisterLogger();
            instance = null;
        }
    }


    /**
     * Returns the name of the application.
     *
     * @return the module name
     */
    public static String getServiceName()
    {
        return instance != null ? instance.moduleName : null;
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
     * Removes all event listeners.
     */
    private void removeEventListeners()
    {
        EventSystem.removeSynchronousListener(GetMainLogEvent.class);
        EventSystem.removeSynchronousListener(GetMavenUtilsEvent.class);
        EventSystem.removeSynchronousListener(GetNumberOfHarvestedDocumentsEvent.class);
        saver.removeEventListeners();
        timeKeeper.removeEventListeners();
        scheduler.removeEventListeners();
        configuration.removeEventListeners();
        cacheManager.removeEventListeners();
        harvester.removeEventListeners();
    }


    /**
     * Synchronous Callback function:<br>
     * Returns the number of harvested, cached documents.
     *
     * @return the number of harvested, cached documents
     */
    private int getNumberOfHarvestedDocuments()
    {
        return cacheManager.getNumberOfHarvestedDocuments();
    }


    /**
     * Synchronous Callback function:<br>
     * Returns the main log of the service.
     *
     * @return the main log of the service
     */
    private HarvesterLog getMainLog()
    {
        return log;
    }


    /**
     * Synchronous Callback function:<br>
     * Returns Maven utilities.
     *
     * @return Maven utilities
     */
    private MavenUtils getMavenUtils()
    {
        return mavenUtils;
    }


    /**
     * Creates an instance of the {@linkplain HarvestSaver}.
     *
     * @param modName the name of this service
     * @param charset the charset with wich the documents will be written
     * @param keeper the time keeper
     * @param harvesterCacheManager the cache manager
     *
     * @return an instance of the {@linkplain HarvestSaver}
     */
    private HarvestSaver createHarvestSaver(String modName, Charset charset, HarvestTimeKeeper keeper, HarvesterCacheManager harvesterCacheManager)
    {
        LOGGER.info(String.format(ApplicationConstants.INIT_FIELD, HarvestSaver.class.getSimpleName()));

        HarvestSaver saver = new HarvestSaver(
            SaveConstants.DEFAULT_SAVE_FOLDER,
            modName,
            charset,
            keeper.getHarvestMeasure(),
            harvesterCacheManager);

        saver.addEventListeners();

        LOGGER.info(String.format(ApplicationConstants.INIT_FIELD_SUCCESS, HarvestSaver.class.getSimpleName()));

        return saver;
    }


    /**
     * Creates a {@linkplain SubmitterManager}, instantiating and registering all submitter classes.
     *
     * @param submitterClass the class of the submitter that is to be instantiated
     * @param charset the charset for writing the submitted data
     * @param keeper the harvest time keeper
     * @param harvesterCacheManager the documents cache
     * @param submitterManager the submitter manager where the submitter is to be registered
     *
     * @return a {@linkplain SubmitterManager}
     */
    private SubmitterManager createSubmitterManager(List<Class<? extends AbstractSubmitter>> submitterClasses,
                                                    Charset charset, HarvestTimeKeeper keeper, HarvesterCacheManager harvesterCacheManager)
    {
        LOGGER.info(String.format(ApplicationConstants.INIT_FIELD, SubmitterManager.class.getSimpleName()));

        final SubmitterManager manager = new SubmitterManager();
        manager.addEventListeners();

        for (Class<? extends AbstractSubmitter> submitterClass : submitterClasses) {
            try {
                AbstractSubmitter newSubmitter = submitterClass.newInstance();
                newSubmitter.setCharset(charset);
                newSubmitter.setCacheManager(harvesterCacheManager);
                newSubmitter.setHarvestIncomplete(keeper.isHarvestIncomplete());
                newSubmitter.setHasSubmittedAll(keeper.getSubmissionMeasure().getStatus() == ProcessStatus.Finished);
                newSubmitter.addEventListeners();
                manager.registerSubmitter(newSubmitter);
            } catch (InstantiationException | IllegalAccessException ex) {
                LOGGER.error(String.format(SubmissionConstants.REGISTER_ERROR, submitterClass.getName()), ex);
            }
        }

        LOGGER.info(String.format(ApplicationConstants.INIT_FIELD_SUCCESS, SubmitterManager.class.getSimpleName()));

        return manager;
    }


    /**
     * Creates a {@linkplain HarvesterLog} and assigns it to this context.
     *
     * @param moduleName the name of this service
     *
     * @return a new {@linkplain HarvesterLog} for this context
     */
    private HarvesterLog createLog(String moduleName)
    {
        LOGGER.info(String.format(ApplicationConstants.INIT_FIELD, HarvesterLog.class.getSimpleName()));

        HarvesterLog serviceLog = new HarvesterLog(String.format(LoggerConstants.LOG_FILE_PATH, moduleName));
        serviceLog.registerLogger();

        LOGGER.info(String.format(ApplicationConstants.INIT_FIELD_SUCCESS, HarvesterLog.class.getSimpleName()));

        return serviceLog;
    }


    /**
     * Creates a {@linkplain MavenUtils} and assigns it to this context.
     *
     * @param harvesterClass the class of the main harvester
     *
     * @return a new {@linkplain MavenUtils} for this context
     */
    private MavenUtils createMavenUtils(Class<?> harvesterClass)
    {
        LOGGER.info(String.format(ApplicationConstants.INIT_FIELD, MavenUtils.class.getSimpleName()));

        MavenUtils utils = new MavenUtils(harvesterClass);

        LOGGER.info(String.format(ApplicationConstants.INIT_FIELD_SUCCESS, MavenUtils.class.getSimpleName()));

        return utils;
    }



    /**
     * Creates a {@linkplain HarvesterCacheManager} and assigns it to this context.
     *
     * @return a new {@linkplain HarvesterCacheManager} for this context
     */
    private HarvesterCacheManager createCacheManager()
    {
        LOGGER.info(String.format(ApplicationConstants.INIT_FIELD, HarvesterCacheManager.class.getSimpleName()));

        HarvesterCacheManager manager = new HarvesterCacheManager();
        manager.addEventListeners();

        LOGGER.info(String.format(ApplicationConstants.INIT_FIELD_SUCCESS, HarvesterCacheManager.class.getSimpleName()));
        return manager;
    }



    /**
     * Creates the main {@linkplain AbstractHarvester} and assigns it to this context.
     *
     * @param moduleName the name of this service
     * @param harvesterClass the class of the main harvester
     *
     * @return a new {@linkplain AbstractHarvester} for this context
     */
    private <T extends AbstractHarvester> T createHarvester(String moduleName, Class<T> harvesterClass)
    throws InstantiationException, IllegalAccessException
    {
        LOGGER.info(String.format(ApplicationConstants.INIT_FIELD, harvesterClass.getSimpleName()));

        T initializedHarvester = harvesterClass.newInstance();
        initializedHarvester.init(true, moduleName);
        initializedHarvester.update();
        initializedHarvester.addEventListeners();

        LOGGER.info(String.format(ApplicationConstants.INIT_FIELD_SUCCESS, harvesterClass.getSimpleName()));

        return initializedHarvester;

    }


    /**
     * Creates a {@linkplain Scheduler} and assigns it to this context.
     *
     * @param moduleName the name of this service
     *
     * @return a new {@linkplain Scheduler} for this context
     */
    private Scheduler createScheduler(String moduleName)
    {
        LOGGER.info(String.format(ApplicationConstants.INIT_FIELD, Scheduler.class.getSimpleName()));

        final String schedulerCachePath = String.format(SchedulerConstants.CACHE_PATH, moduleName);
        Scheduler sched = new Scheduler(moduleName, schedulerCachePath);
        sched.loadFromDisk();
        sched.addEventListeners();

        LOGGER.info(String.format(ApplicationConstants.INIT_FIELD_SUCCESS, Scheduler.class.getSimpleName()));
        return sched;
    }


    /**
     * Creates a {@linkplain HarvestTimeKeeper} and assigns it to this context.
     *
     * @param moduleName the name of this service
     *
     * @return a new {@linkplain HarvestTimeKeeper} for this context
     */
    private HarvestTimeKeeper createTimeKeeper(String moduleName)
    {
        LOGGER.info(String.format(ApplicationConstants.INIT_FIELD, HarvestTimeKeeper.class.getSimpleName()));

        final String timeKeeperCachePath =
            String.format(
                CacheConstants.HARVEST_TIME_KEEPER_CACHE_FILE_PATH,
                moduleName);

        HarvestTimeKeeper keeper = new HarvestTimeKeeper(timeKeeperCachePath);
        keeper.loadFromDisk();
        keeper.addEventListeners();

        LOGGER.info(String.format(ApplicationConstants.INIT_FIELD_SUCCESS, HarvestTimeKeeper.class.getSimpleName()));

        return keeper;
    }


    /**
     * Creates a {@linkplain Configuration} and assigns it to this context.
     *
     * @param moduleName the name of this service
     *
     * @return a new {@linkplain Configuration} for this context
     */
    private Configuration createConfiguration(String moduleName)
    {
        LOGGER.info(String.format(ApplicationConstants.INIT_FIELD, Configuration.class.getSimpleName()));

        Configuration config = new Configuration(moduleName);
        config.setCacheFilePath(String.format(ConfigurationConstants.CONFIG_PATH, moduleName));
        config.loadFromDisk();
        config.addEventListeners();

        Configuration.registerParameter(SubmissionConstants.AUTO_SUBMIT_PARAM);

        LOGGER.info(String.format(ApplicationConstants.INIT_FIELD_SUCCESS, Configuration.class.getSimpleName()));

        return config;
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
        // change state
        EventSystem.sendEvent(new HarvesterInitializedEvent(state));

        // log success
        LOGGER.info(String.format(ApplicationConstants.INIT_SERVICE_SUCCESS, instance.moduleName));

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
        LOGGER.error(ApplicationConstants.INIT_SERVICE_FAILED, reason.getCause());

        // change stage
        EventSystem.sendEvent(new HarvesterInitializedEvent(false));

        return false;
    };
}
