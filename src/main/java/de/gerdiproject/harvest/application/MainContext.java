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


import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gerdiproject.harvest.application.constants.ApplicationConstants;
import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.harvester.AbstractETL;
import de.gerdiproject.harvest.harvester.ETLPreconditionException;
import de.gerdiproject.harvest.harvester.events.GetRepositoryNameEvent;
import de.gerdiproject.harvest.harvester.events.HarvesterInitializedEvent;
import de.gerdiproject.harvest.harvester.loaders.ILoader;
import de.gerdiproject.harvest.harvester.loaders.utils.LoaderFactory;
import de.gerdiproject.harvest.harvester.utils.ETLRegistry;
import de.gerdiproject.harvest.scheduler.Scheduler;
import de.gerdiproject.harvest.scheduler.constants.SchedulerConstants;
import de.gerdiproject.harvest.state.StateMachine;
import de.gerdiproject.harvest.state.impl.InitializationState;
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

    private final HarvesterLog log;
    private final HarvesterCacheManager cacheManager;
    private final String moduleName;
    private final HarvestTimeKeeper timeKeeper;
    private final ETLRegistry etlRegistry;
    private final Configuration configuration;

    @SuppressWarnings("unused") // the submitter is connected via the event system
    private final LoaderFactory submitterManager;
    private final Scheduler scheduler;
    private final MavenUtils mavenUtils;


    /**
     * Constructs an instance with all necessary helpers and utility objects.
     *
     * @param callerClass the class of the object that initialized the context
     * @param repositoryNameSupplier a getter function for retrieving the name of the targeted repository
     * @param etlSupplier a function that provides all {@linkplain AbstractETL}s required for harvesting
     * @param loaderClasses a list of {@linkplain ILoader} classes for loading documents to a search index
     *
     * @throws IllegalAccessException can be thrown when the harvester class cannot be instantiated
     * @throws InstantiationException can be thrown when the harvester class cannot be instantiated
     *
     * @see de.gerdiproject.harvest.harvester.AbstractETL
     */
    private MainContext(
        Class<? extends ContextListener> callerClass,
        Supplier<String> repositoryNameSupplier,
        Supplier<List<? extends AbstractETL<?, ?>>> etlSupplier,
        List<Class<? extends ILoader<?>>> loaderClasses) throws InstantiationException, IllegalAccessException
    {
        this.moduleName = repositoryNameSupplier.get().replaceAll(" ", "") + ApplicationConstants.HARVESTER_SERVICE_NAME_SUFFIX;

        this.log = createLog(moduleName);
        EventSystem.addSynchronousListener(GetMainLogEvent.class, this::getMainLog);

        this.configuration = createConfiguration(moduleName);
        this.timeKeeper = createTimeKeeper(moduleName);

        this.mavenUtils = createMavenUtils(callerClass);
        EventSystem.addSynchronousListener(GetMavenUtilsEvent.class, this::getMavenUtils);

        this.cacheManager = createCacheManager();
        EventSystem.addSynchronousListener(GetNumberOfHarvestedDocumentsEvent.class, this::getNumberOfHarvestedDocuments);

        EventSystem.addSynchronousListener(GetRepositoryNameEvent.class, repositoryNameSupplier);

        this.submitterManager = createLoaderFactory(loaderClasses);
        this.etlRegistry = createEtlRegistry(moduleName, etlSupplier);
        this.scheduler = createScheduler(moduleName);
    }


    /**
     * Constructs an instance of the MainContext in a dedicated thread.
     *
     * @param callerClass the class of the object that called this function
     * @param repositoryNameSupplier a getter function for retrieving the name of the targeted repository
     * @param etlSupplier a function that provides all {@linkplain AbstractETL}s required for harvesting
     * @param loaderClasses a list of {@linkplain ILoader} classes for loading documents to a search index
     */
    public static void init(
        Class<? extends ContextListener> callerClass,
        Supplier<String> repositoryNameSupplier,
        Supplier<List<? extends AbstractETL<?, ?>>> etlSupplier,
        List<Class<? extends ILoader<?>>> loaderClasses)
    {
        LOGGER.info(ApplicationConstants.INIT_SERVICE);
        StateMachine.setState(new InitializationState());

        CancelableFuture<Boolean> initProcess = new CancelableFuture<>(() -> {
            // clear old instance if necessary
            destroy();

            instance = new MainContext(callerClass, repositoryNameSupplier, etlSupplier, loaderClasses);
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
        timeKeeper.removeEventListeners();
        scheduler.removeEventListeners();
        configuration.removeEventListeners();
        cacheManager.removeEventListeners();
        etlRegistry.removeEventListeners();
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
     * Creates a {@linkplain LoaderFactory}, and registers all loader classes.
     *
     * @param loaderClasses a list of {@linkplain ILoader} classes for
     *         loading documents to a search index
     *
     * @return a new {@linkplain LoaderFactory}
     */
    private LoaderFactory createLoaderFactory(List<Class<? extends ILoader<?>>> loaderClasses)
    {
        LOGGER.info(String.format(ApplicationConstants.INIT_FIELD, LoaderFactory.class.getSimpleName()));

        final LoaderFactory manager = new LoaderFactory();
        manager.addEventListeners();

        for (Class<? extends ILoader<?>> s : loaderClasses)
            manager.registerLoader(s);

        LOGGER.info(String.format(ApplicationConstants.INIT_FIELD_SUCCESS, LoaderFactory.class.getSimpleName()));

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
     * Creates the {@linkplain ETLRegistry} with all {@linkplain AbstractETL}s and assigns it to this context.
     *
     * @param moduleName the name of this service
     * @param etlSupplier all {@linkplain AbstractETL}s required for harvesting
     *
     * @return a new {@linkplain ETLRegistry} for this context
     */
    private ETLRegistry createEtlRegistry(String moduleName, Supplier<List<? extends AbstractETL<?, ?>>> etlSupplier)
    throws InstantiationException, IllegalAccessException
    {
        LOGGER.info(String.format(ApplicationConstants.INIT_FIELD, ETLRegistry.class.getSimpleName()));

        final ETLRegistry registry = new ETLRegistry(moduleName);

        // construct harvesters
        final List<? extends AbstractETL<?, ?>> etlComponents = etlSupplier.get();

        // initialize and register harvesters
        for (AbstractETL<?, ?> etl : etlComponents) {
            LOGGER.info(String.format(ApplicationConstants.INIT_FIELD, etl.getClass().getSimpleName()));

            etl.init(moduleName);

            try {
                etl.update();
            } catch (ETLPreconditionException e) { // NOPMD - Ignore exceptions, because we do not need to harvest yet
            }

            registry.register(etl);

            LOGGER.info(String.format(ApplicationConstants.INIT_FIELD_SUCCESS, etl.getClass().getSimpleName()));
        }

        registry.addEventListeners();

        LOGGER.info(String.format(ApplicationConstants.INIT_FIELD_SUCCESS, ETLRegistry.class.getSimpleName()));

        return registry;
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
