/*
 *  Copyright © 2019 Robin Weiss (http://www.gerdi-project.de/)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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
package de.gerdiproject.harvest.application;

import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gerdiproject.harvest.application.constants.ApplicationConstants;
import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.etls.AbstractETL;
import de.gerdiproject.harvest.etls.ETLPreconditionException;
import de.gerdiproject.harvest.etls.loaders.ILoader;
import de.gerdiproject.harvest.etls.loaders.utils.LoaderRegistry;
import de.gerdiproject.harvest.etls.utils.ETLManager;
import de.gerdiproject.harvest.scheduler.Scheduler;
import de.gerdiproject.harvest.scheduler.constants.SchedulerConstants;
import de.gerdiproject.harvest.utils.logger.HarvesterLog;
import de.gerdiproject.harvest.utils.logger.constants.LoggerConstants;
import de.gerdiproject.harvest.utils.maven.MavenUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author Robin Weiss
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
class MainContextUtils
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MainContextUtils.class);


    /**
     * Creates a {@linkplain LoaderRegistry}, and registers all loader classes.
     *
     * @param loaderClasses a list of {@linkplain ILoader} classes for
     *         loading documents to a search index
     *
     * @return a new {@linkplain LoaderRegistry}
     */
    protected static LoaderRegistry createLoaderRegistry(final List<Class<? extends ILoader<?>>> loaderClasses)
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
    protected static HarvesterLog createLog(final String moduleName)
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
    protected static MavenUtils createMavenUtils(final Class<?> harvesterClass)
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
    protected static ETLManager createEtlManager(final String moduleName, final Supplier<List<? extends AbstractETL<?, ?>>> etlSupplier)
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
    protected static Scheduler createScheduler(final String moduleName)
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
    protected static Configuration createConfiguration(final String moduleName)
    {
        LOGGER.info(String.format(ApplicationConstants.INIT_FIELD, Configuration.class.getSimpleName()));

        final Configuration config = new Configuration(moduleName);
        config.setCacheFilePath(String.format(ConfigurationConstants.CONFIG_PATH, moduleName));
        config.loadFromDisk();
        config.addEventListeners();

        LOGGER.info(String.format(ApplicationConstants.INIT_FIELD_SUCCESS, Configuration.class.getSimpleName()));

        return config;
    }
}
