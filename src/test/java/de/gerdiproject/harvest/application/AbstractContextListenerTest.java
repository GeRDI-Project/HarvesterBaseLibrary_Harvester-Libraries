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

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.lang.reflect.ParameterizedType;
import java.util.Map;

import javax.servlet.annotation.WebListener;

import org.junit.Test;

import de.gerdiproject.harvest.AbstractObjectUnitTest;
import de.gerdiproject.harvest.application.events.ServiceInitializedEvent;
import de.gerdiproject.harvest.etls.AbstractETL;
import de.gerdiproject.harvest.etls.events.GetETLManagerEvent;
import de.gerdiproject.harvest.etls.events.GetRepositoryNameEvent;
import de.gerdiproject.harvest.etls.extractors.AbstractIteratorExtractor;
import de.gerdiproject.harvest.etls.json.ETLInfosJson;
import de.gerdiproject.harvest.etls.json.ETLJson;
import de.gerdiproject.harvest.etls.loaders.ILoader;
import de.gerdiproject.harvest.etls.loaders.events.CreateLoaderEvent;
import de.gerdiproject.harvest.etls.utils.ETLManager;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.utils.data.constants.DataOperationConstants;
import de.gerdiproject.harvest.utils.file.FileUtils;

/**
 * This abstract class offers unit tests for concrete {@linkplain ContextListener} implementations.
 *
 * @author Robin Weiss
 */
public abstract class AbstractContextListenerTest <T extends ContextListener> extends AbstractObjectUnitTest<T>
{
    @SuppressWarnings("unchecked")
    protected final Class<T> contextListenerClass = (Class<T>)((ParameterizedType)getClass().getGenericSuperclass()).getActualTypeArguments()[0];


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


    @Override
    protected T setUpTestObjects()
    {
        // instantiate context listener
        final T contextListener;

        try {
            contextListener = contextListenerClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalArgumentException(contextListenerClass.getSimpleName() + " must have a no-args constructor!", e);
        }

        // set up mocked HTTP responses, if they exist
        final File httpFolderResource = getMockedHttpResponseFolder();

        if (httpFolderResource != null && httpFolderResource.exists()) {
            final File httpFolderTemp = new File(
                MainContextUtils.getCacheDirectory(getClass()),
                DataOperationConstants.CACHE_FOLDER_PATH);

            FileUtils.copyFile(httpFolderResource, httpFolderTemp);
        }

        // set up config, if it exists
        final File configFileResource = getConfigFile();

        if (configFileResource != null && configFileResource.exists()) {
            final File configFile = MainContextUtils.getConfigurationFile(
                                        contextListener.getServiceName(),
                                        contextListener.getClass());
            FileUtils.copyFile(configFileResource, configFile);
        }

        return contextListener;
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


    /**
     * Initializes the {@linkplain MainContext} and returns once the initialization
     * was completed.
     */
    protected void initContext()
    {
        waitForEvent(
            ServiceInitializedEvent.class,
            getMaxInitializationTime(),
            () -> testedObject.contextInitialized(null));
    }


    /**
     * Checks if the {@linkplain ContextListener} implementation has a {@linkplain WebListener} annotation.
     */
    @Test
    public void testWebListenerAnnotation()
    {
        final WebListener webListenerAnnotation = testedObject.getClass().getAnnotation(WebListener.class);

        assertNotNull("The ContextListener implementation must have a @WebListener annotation!",
                      webListenerAnnotation);
    }


    /**
     * Checks if the {@linkplain ContextListener} implementation creates at least one
     * {@linkplain AbstractETL} implementation.
     */
    @Test
    public void testCreateEtls()
    {
        initContext();

        final ETLManager etlManager = EventSystem.sendSynchronousEvent(new GetETLManagerEvent());
        final ETLInfosJson etlInfos = etlManager.getETLsAsJson();
        final Map<String, ETLJson> etlInfoMap = etlInfos.getEtlInfos();

        assertNotEquals("Expected at least one ETL to be initializable!",
                        0,
                        etlInfoMap.size());
    }


    /**
     * Tests if the {@linkplain ContextListener} implementation successfully passes the repository
     * name to the {@linkplain MainContext}, allowing it to be retrieved via the {@linkplain GetRepositoryNameEvent}.
     */
    @Test
    public void testGetRepositoryName()
    {
        initContext();

        final String repositoryName = EventSystem.sendSynchronousEvent(new GetRepositoryNameEvent());

        assertNotEquals("Expected the method getRepositoryName() to return a non-empty String!",
                        0,
                        repositoryName.length());
    }


    /**
     * Tests if the {@linkplain ContextListener} implementation successfully passes the {@linkplain List} of
     * {@linkplain ILoader} classes to the {@linkplain MainContext}, allowing a non-null {@linkplain ILoader}
     * implementation to be created via the {@linkplain CreateLoaderEvent}.
     */
    @Test
    public void testGetLoaderClasses()
    {
        initContext();

        final ILoader<?> loader = EventSystem.sendSynchronousEvent(new CreateLoaderEvent());

        assertNotNull("Expected the method createLoaderClasses() to return a non-empty List of Loaders!",
                      loader);
    }
}
