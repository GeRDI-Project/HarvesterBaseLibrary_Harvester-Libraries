/**
# * Copyright © 2017 Robin Weiss (http://www.gerdi-project.de)
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
import de.gerdiproject.harvest.application.events.ServiceInitializedEvent;
import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.etls.AbstractETL;
import de.gerdiproject.harvest.etls.ETLPreconditionException;
import de.gerdiproject.harvest.etls.events.GetRepositoryNameEvent;
import de.gerdiproject.harvest.etls.loaders.ILoader;
import de.gerdiproject.harvest.etls.loaders.utils.LoaderRegistry;
import de.gerdiproject.harvest.etls.utils.ETLManager;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.scheduler.Scheduler;
import de.gerdiproject.harvest.scheduler.constants.SchedulerConstants;
import de.gerdiproject.harvest.utils.CancelableFuture;
import de.gerdiproject.harvest.utils.logger.HarvesterLog;
import de.gerdiproject.harvest.utils.logger.constants.LoggerConstants;
import de.gerdiproject.harvest.utils.logger.events.GetMainLogEvent;
import de.gerdiproject.harvest.utils.maven.MavenUtils;
import de.gerdiproject.harvest.utils.maven.events.GetMavenUtilsEvent;


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

    private static boolean hasFailed;
    private static boolean isInitialized;

    private final HarvesterLog log;
    private final String moduleName;
    private final ETLManager etlManager;
    private final Configuration configuration;

    @SuppressWarnings("unused") // the loaderRegistry is used by the event system
    private final LoaderRegistry loaderRegistry;
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
     * @see de.gerdiproject.harvest.etls.AbstractETL
     */
    private MainContext(
        final Class<? extends ContextListener> callerClass,
        final Supplier<String> repositoryNameSupplier,
        final Supplier<List<? extends AbstractETL<?, ?>>> etlSupplier,
        final List<Class<? extends ILoader<?>>> loaderClasses) throws InstantiationException, IllegalAccessException
    {
        this.moduleName = repositoryNameSupplier.get().replaceAll(" ", "") + ApplicationConstants.HARVESTER_SERVICE_NAME_SUFFIX;
        EventSystem.addSynchronousListener(GetRepositoryNameEvent.class, repositoryNameSupplier);

        this.log = createLog(moduleName);
        EventSystem.addSynchronousListener(GetMainLogEvent.class, this::getMainLog);

        this.configuration = createConfiguration(moduleName);

        this.mavenUtils = createMavenUtils(callerClass);
        EventSystem.addSynchronousListener(GetMavenUtilsEvent.class, this::getMavenUtils);

        this.loaderRegistry = createLoaderFactory(loaderClasses);
        this.etlManager = createEtlManager(moduleName, etlSupplier);
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
        final Class<? extends ContextListener> callerClass,
        final Supplier<String> repositoryNameSupplier,
        final Supplier<List<? extends AbstractETL<?, ?>>> etlSupplier,
        final List<Class<? extends ILoader<?>>> loaderClasses)
    {
        LOGGER.info(ApplicationConstants.INIT_SERVICE);
        hasFailed = false;
        isInitialized = false;

        final CancelableFuture<Boolean> initProcess = new CancelableFuture<>(() -> {
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
     * Checks if the initialization phase of the MainContext has finished.
     *
     * @return true, if the initialization phase has finished
     */
    public static boolean isInitialized()
    {
        return isInitialized;
    }

    /**
     * Checks if the initialization of the MainContext has failed.
     *
     * @return true, if the initialization has failed
     */
    public static boolean hasFailed()
    {
        return hasFailed;
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

        hasFailed = false;
        isInitialized = false;
    }


    /**
     * Removes all event listeners.
     */
    private void removeEventListeners()
    {
        EventSystem.removeSynchronousListener(GetMainLogEvent.class);
        EventSystem.removeSynchronousListener(GetMavenUtilsEvent.class);
        scheduler.removeEventListeners();
        configuration.removeEventListeners();
        etlManager.removeEventListeners();
    }


    /**
     * Callback function for the {@linkplain GetMainLogEvent}.
     * Returns the main log of the service.
     *
     * @return the main log of the service
     */
    private HarvesterLog getMainLog()
    {
        return log;
    }


    /**
     * Callback function for the {@linkplain GetMavenUtilsEvent}.
     * Returns Maven utilities.
     *
     * @return Maven utilities
     */
    private MavenUtils getMavenUtils()
    {
        return mavenUtils;
    }


    /**
     * Creates a {@linkplain LoaderRegistry}, and registers all loader classes.
     *
     * @param loaderClasses a list of {@linkplain ILoader} classes for
     *         loading documents to a search index
     *
     * @return a new {@linkplain LoaderRegistry}
     */
    private LoaderRegistry createLoaderFactory(final List<Class<? extends ILoader<?>>> loaderClasses)
    {
        LOGGER.info(String.format(ApplicationConstants.INIT_FIELD, LoaderRegistry.class.getSimpleName()));

        final LoaderRegistry registry = new LoaderRegistry();
        registry.addEventListeners();

        for (final Class<? extends ILoader<?>> s : loaderClasses)
            registry.registerLoader(s);

        LOGGER.info(String.format(ApplicationConstants.INIT_FIELD_SUCCESS, LoaderRegistry.class.getSimpleName()));

        return registry;
    }


    /**
     * Creates a {@linkplain HarvesterLog} and assigns it to this context.
     *
     * @param moduleName the name of this service
     *
     * @return a new {@linkplain HarvesterLog} for this context
     */
    private HarvesterLog createLog(final String moduleName)
    {
        LOGGER.info(String.format(ApplicationConstants.INIT_FIELD, HarvesterLog.class.getSimpleName()));

        final HarvesterLog serviceLog = new HarvesterLog(String.format(LoggerConstants.LOG_FILE_PATH, moduleName));
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
    private MavenUtils createMavenUtils(final Class<?> harvesterClass)
    {
        LOGGER.info(String.format(ApplicationConstants.INIT_FIELD, MavenUtils.class.getSimpleName()));

        final MavenUtils utils = new MavenUtils(harvesterClass);

        LOGGER.info(String.format(ApplicationConstants.INIT_FIELD_SUCCESS, MavenUtils.class.getSimpleName()));

        return utils;
    }


    /**
     * Creates the {@linkplain ETLManager} with all {@linkplain AbstractETL}s and assigns it to this context.
     *
     * @param moduleName the name of this service
     * @param etlSupplier all {@linkplain AbstractETL}s required for harvesting
     *
     * @return a new {@linkplain ETLManager} for this context
     */
    private ETLManager createEtlManager(final String moduleName, final Supplier<List<? extends AbstractETL<?, ?>>> etlSupplier)
    throws InstantiationException, IllegalAccessException
    {
        LOGGER.info(String.format(ApplicationConstants.INIT_FIELD, ETLManager.class.getSimpleName()));

        final ETLManager manager = new ETLManager(moduleName);

        // construct harvesters
        final List<? extends AbstractETL<?, ?>> etlComponents = etlSupplier.get();

        // initialize and register harvesters
        for (final AbstractETL<?, ?> etl : etlComponents) {
            manager.register(etl);

            LOGGER.info(String.format(ApplicationConstants.INIT_FIELD, etl.getName()));

            etl.init(moduleName);

            try {
                etl.update();
            } catch (final ETLPreconditionException e) { // NOPMD - Ignore exceptions, because we do not need to harvest yet
            }

            LOGGER.info(String.format(ApplicationConstants.INIT_FIELD_SUCCESS, etl.getName()));
        }

        manager.loadFromDisk();
        manager.addEventListeners();

        LOGGER.info(String.format(ApplicationConstants.INIT_FIELD_SUCCESS, ETLManager.class.getSimpleName()));

        return manager;
    }


    /**
     * Creates a {@linkplain Scheduler} and assigns it to this context.
     *
     * @param moduleName the name of this service
     *
     * @return a new {@linkplain Scheduler} for this context
     */
    private Scheduler createScheduler(final String moduleName)
    {
        LOGGER.info(String.format(ApplicationConstants.INIT_FIELD, Scheduler.class.getSimpleName()));

        final String schedulerCachePath = String.format(SchedulerConstants.CACHE_PATH, moduleName);
        final Scheduler sched = new Scheduler(moduleName, schedulerCachePath);
        sched.loadFromDisk();
        sched.addEventListeners();

        LOGGER.info(String.format(ApplicationConstants.INIT_FIELD_SUCCESS, Scheduler.class.getSimpleName()));
        return sched;
    }


    /**
     * Creates a {@linkplain Configuration} and assigns it to this context.
     *
     * @param moduleName the name of this service
     *
     * @return a new {@linkplain Configuration} for this context
     */
    private Configuration createConfiguration(final String moduleName)
    {
        LOGGER.info(String.format(ApplicationConstants.INIT_FIELD, Configuration.class.getSimpleName()));

        final Configuration config = new Configuration(moduleName);
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
    private static Boolean onHarvesterInitializedSuccess(final Boolean state)
    {
        hasFailed = false;
        isInitialized = true;

        // change state
        EventSystem.sendEvent(new ServiceInitializedEvent(state));

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
    private static Boolean onHarvesterInitializedFailed(final Throwable reason)
    {
        hasFailed = true;
        isInitialized = true;

        // log exception that caused the failure
        LOGGER.error(ApplicationConstants.INIT_SERVICE_FAILED, reason.getCause());

        // change stage
        EventSystem.sendEvent(new ServiceInitializedEvent(false));

        return false;
    };
}
