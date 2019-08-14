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
import de.gerdiproject.harvest.etls.AbstractETL;
import de.gerdiproject.harvest.etls.events.GetRepositoryNameEvent;
import de.gerdiproject.harvest.etls.loaders.ILoader;
import de.gerdiproject.harvest.etls.loaders.utils.LoaderRegistry;
import de.gerdiproject.harvest.etls.utils.ETLManager;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.scheduler.Scheduler;
import de.gerdiproject.harvest.utils.CancelableFuture;
import de.gerdiproject.harvest.utils.logger.HarvesterLog;
import de.gerdiproject.harvest.utils.logger.events.GetMainLogEvent;
import de.gerdiproject.harvest.utils.maven.MavenUtils;
import de.gerdiproject.harvest.utils.maven.events.GetMavenUtilsEvent;
import lombok.Value;


/**
 * This class provides static methods for retrieving application singleton
 * utility and configuration classes.
 *
 * @author Robin Weiss
 */
@Value
public final class MainContext
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MainContext.class);
    private static volatile MainContext instance;

    private static boolean failed;
    private static boolean initialized;

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

        this.log = MainContextUtils.createLog(moduleName);
        EventSystem.addSynchronousListener(GetMainLogEvent.class, this::getLog);

        this.configuration = MainContextUtils.createConfiguration(moduleName);

        this.mavenUtils = MainContextUtils.createMavenUtils(callerClass);
        EventSystem.addSynchronousListener(GetMavenUtilsEvent.class, this::getMavenUtils);

        this.loaderRegistry = MainContextUtils.createLoaderRegistry(loaderClasses);
        this.etlManager = MainContextUtils.createEtlManager(moduleName, etlSupplier);
        this.scheduler = MainContextUtils.createScheduler(moduleName);
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
        failed = false;
        initialized = false;

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
        return initialized;
    }

    /**
     * Checks if the initialization of the MainContext has failed.
     *
     * @return true, if the initialization has failed
     */
    public static boolean hasFailed()
    {
        return failed;
    }


    /**
     * Clears the current Singleton instance if it exists and nullifies it.
     */
    public static synchronized void destroy() // NOPMD synchronized fixes another issue here
    {
        if (instance != null) {
            instance.removeEventListeners();
            instance.log.unregisterLogger();
            instance = null;
        }

        failed = false;
        initialized = false;
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
        failed = false;
        initialized = true;

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
        failed = true;
        initialized = true;

        // log exception that caused the failure
        LOGGER.error(ApplicationConstants.INIT_SERVICE_FAILED, reason.getCause());

        // change stage
        EventSystem.sendEvent(new ServiceInitializedEvent(false));

        return false;
    };
}
