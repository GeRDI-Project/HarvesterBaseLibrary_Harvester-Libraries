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
public class MainContextUtils
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MainContextUtils.class);

    /**
     * Reads a system variable to determine the {@linkplain DeploymentType} of the
     * harvester service.
     *
     * @return the {@linkplain DeploymentType} of the harvester service
     */
    protected static DeploymentType getDeploymentType()
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
     * Retrieves a {@linkplain File} that points to the topmost directory in which
     * harvester service files are cached.
     *
     * @param projectSpecificClass a class from the project of which the cache directory is to be determined
     *
     * @return the topmost directory in which harvester service files are cached
     */
    public static File getCacheDirectory(final Class<?> projectSpecificClass)
    {
        File cacheDir;

        final DeploymentType deploymentType = getDeploymentType();

        switch (deploymentType) {
            case UNIT_TEST:
                cacheDir = new File(
                    getProjectRootDirectory(projectSpecificClass),
                    ApplicationConstants.CACHE_DIR_UNIT_TESTS);
                break;

            case JETTY:
                cacheDir = new File(
                    getProjectRootDirectory(projectSpecificClass),
                    ApplicationConstants.CACHE_DIR_JETTY);
                break;

            default:
                cacheDir = new File(
                    ApplicationConstants.CACHE_ROOT_DIR_OTHER,
                    ApplicationConstants.CACHE_DIR_OTHER);
                break;
        }

        return cacheDir;
    }


    /**
     * Returns the harvester project directory.
     *
     * @param projectSpecificClass a class from the project of which the root directory is to be determined
     *
     * @return the harvester project directory
     */
    public static File getProjectRootDirectory(final Class<?> projectSpecificClass)
    {
        String rootPath;

        try {
            final String jarPath = new File(
                projectSpecificClass
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI()).getPath();
            rootPath = jarPath.substring(0, jarPath.lastIndexOf(MavenConstants.TARGET_FOLDER));
        } catch (URISyntaxException e) {
            rootPath = "";
        }

        return new File(rootPath);
    }


    /**
     * Assembles a {@linkplain File} to which logs are written.
     *
     * @param moduleName the name of the harvester service
     * @param projectSpecificClass a class from the project of which the log file is to be determined
     *
     * @return a {@linkplain File} to which logs are written
     */
    public static File getLogFile(final String moduleName, final Class<?> projectSpecificClass)
    {
        File logFile;

        final DeploymentType deploymentType = getDeploymentType();

        switch (deploymentType) {
            case UNIT_TEST:
                logFile = new File(
                    getProjectRootDirectory(projectSpecificClass),
                    String.format(LoggerConstants.LOG_PATH_UNIT_TESTS, moduleName));
                break;

            case JETTY:
                logFile = new File(
                    getProjectRootDirectory(projectSpecificClass),
                    String.format(LoggerConstants.LOG_PATH_JETTY, moduleName));
                break;

            case DOCKER:
                logFile = new File(String.format(LoggerConstants.LOG_PATH_DOCKER, moduleName));
                break;

            default:
                logFile = new File(String.format(LoggerConstants.LOG_PATH_OTHER, moduleName));
                break;
        }

        return logFile;
    }


    /**
     * Assembles a {@linkplain File} in which the {@linkplain Configuration} can be saved.
     *
     * @param moduleName the name of the harvester service
     * @param projectSpecificClass a class from the project of which the config file is to be determined
     *
     * @return a {@linkplain File} in which the configuration is saved
     */
    public static File getConfigurationFile(final String moduleName, final Class<?> projectSpecificClass)
    {
        return new File(getCacheDirectory(projectSpecificClass), String.format(ConfigurationConstants.CONFIG_PATH, moduleName));
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
     * @param projectSpecificClass a class from the project for which the configuration is to be created
     *
     * @return a new {@linkplain Configuration} for this context
     */
    protected static Configuration createConfiguration(final String moduleName, final Class<?> projectSpecificClass)
    {
        LOGGER.info(String.format(ApplicationConstants.INIT_FIELD, Configuration.class.getSimpleName()));

        final Configuration config = new Configuration(moduleName);
        final File configCachePath = getConfigurationFile(moduleName, projectSpecificClass);
        config.setCacheFilePath(configCachePath.toString());
        config.loadFromDisk();
        config.addEventListeners();

        LOGGER.info(String.format(ApplicationConstants.INIT_FIELD_SUCCESS, Configuration.class.getSimpleName()));

        return config;
    }
}
