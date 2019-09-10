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

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gerdiproject.harvest.application.constants.ApplicationConstants;
import de.gerdiproject.harvest.application.enums.DeploymentType;
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
import de.gerdiproject.harvest.utils.maven.constants.MavenConstants;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * This utility class offers static functions for initializing {@linkplain MainContext}
 * related classes.
 *
 * @author Robin Weiss
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
class MainContextUtils
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MainContextUtils.class);

    /**
     * Reads a system variable to determine the {@linkplain DeploymentType} of the
     * harvester service.
     *
     * @return the {@linkplain DeploymentType} of the harvester service
     */
    protected static DeploymentType initDeploymentType()
    {
        final String deploymentTypeSystemProperty =
            System.getProperty(ApplicationConstants.DEPLOYMENT_TYPE, null);

        DeploymentType deploymentType;

        if (deploymentTypeSystemProperty == null)
            deploymentType = DeploymentType.OTHER;
        else {
            try {
                deploymentType = DeploymentType.valueOf(deploymentTypeSystemProperty.toUpperCase(Locale.ENGLISH));
            } catch (final IllegalArgumentException e) {
                deploymentType = DeploymentType.OTHER;
            }
        }

        return deploymentType;
    }


    /**
     * Initializes a {@linkplain File} that points to the topmost directory in which
     * harvester service files are cached.
     *
     * @param deploymentType the {@linkplain DeploymentType} of the harvester service
     * @return the topmost directory in which harvester service files are cached
     */
    protected static File initCacheDirectory(final DeploymentType deploymentType)
    {
        String projectRootPath;
        String subDirPath;

        switch (deploymentType) {
            case UNIT_TEST:
                projectRootPath = getProjectRootDirectory();
                subDirPath = ApplicationConstants.CACHE_DIR_UNIT_TESTS;
                break;

            case JETTY:
                projectRootPath = getProjectRootDirectory();
                subDirPath = ApplicationConstants.CACHE_DIR_JETTY;
                break;

            default:
                projectRootPath = ApplicationConstants.CACHE_ROOT_DIR_OTHER;
                subDirPath = ApplicationConstants.CACHE_DIR_OTHER;
                break;
        }

        return new File(projectRootPath, subDirPath);
    }


    /**
     * Assembles a {@linkplain File} in which logs are to be saved.
     *
     * @param deploymentType the {@linkplain DeploymentType} of the harvester service
     * @param moduleName the name of the harvester service
     *
     * @return a {@linkplain File} in which logs are to be saved
     */
    protected static File initLogFile(final DeploymentType deploymentType, final String moduleName)
    {
        String logPathFormat;

        switch (deploymentType) {
            case UNIT_TEST:
                logPathFormat = getProjectRootDirectory() + File.separatorChar + LoggerConstants.LOG_PATH_UNIT_TESTS;
                break;

            case JETTY:
                logPathFormat = getProjectRootDirectory() + File.separatorChar + LoggerConstants.LOG_PATH_JETTY;
                break;

            case DOCKER:
                logPathFormat = LoggerConstants.LOG_PATH_DOCKER;
                break;

            default:
                logPathFormat = LoggerConstants.LOG_PATH_OTHER;
                break;
        }

        return new File(String.format(logPathFormat, moduleName));
    }


    /**
     * Returns the harvester project directory.
     *
     * @return the harvester project directory
     */
    private static String getProjectRootDirectory()
    {
        String jarPath;

        try {
            jarPath = new File(
                MainContextUtils.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI()).getPath();
        } catch (URISyntaxException e) {
            return "";
        }

        return jarPath.substring(0, jarPath.lastIndexOf(MavenConstants.TARGET_FOLDER)) ;
    }


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
     * @param logPath the path to the file to which the logs are written
     *
     * @return a new {@linkplain HarvesterLog} for this context
     */
    protected static HarvesterLog createLog(final File logPath)
    {
        LOGGER.info(String.format(ApplicationConstants.INIT_FIELD, HarvesterLog.class.getSimpleName()));

        final HarvesterLog serviceLog = new HarvesterLog(logPath.toString());
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
     * @param cacheFolder the root folder of harvester cache files
     *
     * @return a new {@linkplain ETLManager} for this context
     */
    protected static ETLManager createEtlManager(final String moduleName,
                                                 final Supplier<List<? extends AbstractETL<?, ?>>> etlSupplier,
                                                 final File cacheFolder)
    throws InstantiationException, IllegalAccessException
    {
        LOGGER.info(String.format(ApplicationConstants.INIT_FIELD, ETLManager.class.getSimpleName()));

        final ETLManager manager = new ETLManager(moduleName, cacheFolder);

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
     * @param cacheFolder the root folder of harvester cache files
     *
     * @return a new {@linkplain Scheduler} for this context
     */
    protected static Scheduler createScheduler(final String moduleName, final File cacheFolder)
    {
        LOGGER.info(String.format(ApplicationConstants.INIT_FIELD, Scheduler.class.getSimpleName()));

        final File schedulerCachePath = new File(cacheFolder, String.format(SchedulerConstants.CACHE_PATH, moduleName));
        final Scheduler sched = new Scheduler(moduleName, schedulerCachePath.toString());
        sched.loadFromDisk();
        sched.addEventListeners();

        LOGGER.info(String.format(ApplicationConstants.INIT_FIELD_SUCCESS, Scheduler.class.getSimpleName()));
        return sched;
    }


    /**
     * Creates a {@linkplain Configuration} and assigns it to this context.
     *
     * @param moduleName the name of this service
     * @param cacheFolder the root folder of harvester cache files
     *
     * @return a new {@linkplain Configuration} for this context
     */
    protected static Configuration createConfiguration(final String moduleName, final File cacheFolder)
    {
        LOGGER.info(String.format(ApplicationConstants.INIT_FIELD, Configuration.class.getSimpleName()));

        final Configuration config = new Configuration(moduleName);
        final File configCachePath = new File(cacheFolder, String.format(ConfigurationConstants.CONFIG_PATH, moduleName));
        config.setCacheFilePath(configCachePath.toString());
        config.loadFromDisk();
        config.addEventListeners();

        LOGGER.info(String.format(ApplicationConstants.INIT_FIELD_SUCCESS, Configuration.class.getSimpleName()));

        return config;
    }
}
