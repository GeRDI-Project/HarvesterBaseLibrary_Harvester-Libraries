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
package de.gerdiproject.harvest;

import java.io.File;

import de.gerdiproject.harvest.application.ContextListener;
import de.gerdiproject.harvest.application.ContextListenerTestWrapper;
import de.gerdiproject.harvest.application.MainContext;
import de.gerdiproject.harvest.application.MainContextUtils;
import de.gerdiproject.harvest.application.events.ServiceInitializedEvent;
import de.gerdiproject.harvest.config.events.GetConfigurationEvent;
import de.gerdiproject.harvest.etls.AbstractIteratorETL;
import de.gerdiproject.harvest.etls.extractors.AbstractIteratorExtractor;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.utils.data.constants.DataOperationConstants;
import de.gerdiproject.harvest.utils.file.FileUtils;

/**
 * This class serves as a Unit Test template for ETL related tests.
 * It initializes a {@linkplain MainContext} with a single {@linkplain AbstractIteratorETL}.
 *
 * @author Robin Weiss
 */
public abstract class AbstractETLUnitTest<T, R> extends AbstractObjectUnitTest<T>
{
    /**
     * Returns the {@linkplain AbstractIteratorETL} to which the tested class belongs, if any.
     *
     * @return the {@linkplain AbstractIteratorETL} to which the tested class belongs, or
     * null if no ETL needs to be created.
     */
    protected abstract AbstractIteratorETL<R, ?> getEtl();


    /**
     * Retrieves the tested object from the {@linkplain ContextListenerTestWrapper}.
     * @param contextInitializer a wrapper class that allows to retrieve ETL related
     * classes
     *
     * @return the object that is to be tested
     */
    protected abstract T setUpTestedObjectFromContextInitializer(final ContextListenerTestWrapper<? extends AbstractIteratorETL<R, ?>> contextInitializer);


    @Override
    protected T setUpTestObjects()
    {
        final File httpResourceFolder = getMockedHttpResponseFolder();

        if (httpResourceFolder != null && httpResourceFolder.exists()) {
            // copy mocked HTTP responses to the cache folder to drastically speed up the testing
            final File httpCacheFolder = new File(
                MainContextUtils.getCacheDirectory(getClass()),
                DataOperationConstants.CACHE_FOLDER_PATH);

            FileUtils.copyFile(httpResourceFolder, httpCacheFolder);
        }

        final ContextListenerTestWrapper<? extends AbstractIteratorETL<R, ?>> contextInitializer =
            new ContextListenerTestWrapper<>(getContextListener(), this::getEtl);

        // copy over configuration file
        final File configFileResource = getConfigFile();

        if (configFileResource != null && configFileResource.exists()) {
            final File configFile = contextInitializer.getConfigFile();
            FileUtils.copyFile(configFileResource, configFile);
        }

        // initialize harvester service
        waitForEvent(
            ServiceInitializedEvent.class,
            getMaxInitializationTime(),
            () -> contextInitializer.initializeContext());

        // get configuration
        this.config = EventSystem.sendSynchronousEvent(new GetConfigurationEvent());

        // get tested object from contextInitializer
        return setUpTestedObjectFromContextInitializer(contextInitializer);
    }


    /**
     * Returns the harvester specific {@linkplain ContextListener} subclass.
     *
     * @return the harvester specific {@linkplain ContextListener} subclass
     */
    protected abstract ContextListener getContextListener();


    /**
     * Returns an optional configuration {@linkplain File} that is
     * required to test the {@linkplain AbstractIteratorExtractor}.
     *
     * @return a configuration {@linkplain File} or null, if no parameters need to be set
     */
    protected File getConfigFile()
    {
        return getResource("config.json");
    }


    /**
     * Returns a resource path that points to a folder that contains
     * cached HTTP responses, required for simulating an extraction.
     *
     * @return a resource path that points to a folder that contains
     * cached HTTP responses
     */
    protected File getMockedHttpResponseFolder()
    {
        return getResource("mockedHttpResponses");
    }


    /**
     * Retrieves the maximum number of milliseconds that the initialization
     * process should require.
     *
     * @return the maximum number of milliseconds of the initialization process
     */
    protected int getMaxInitializationTime()
    {
        return 5000;
    }
}
