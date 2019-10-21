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
package de.gerdiproject.harvest.etls.extractors;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

import de.gerdiproject.harvest.AbstractObjectUnitTest;
import de.gerdiproject.harvest.application.ContextListener;
import de.gerdiproject.harvest.application.ContextListenerTestWrapper;
import de.gerdiproject.harvest.application.MainContextUtils;
import de.gerdiproject.harvest.application.events.ServiceInitializedEvent;
import de.gerdiproject.harvest.etls.AbstractIteratorETL;
import de.gerdiproject.harvest.etls.EtlUnitTestUtils;
import de.gerdiproject.harvest.utils.data.constants.DataOperationConstants;
import de.gerdiproject.harvest.utils.file.FileUtils;
import de.gerdiproject.json.GsonUtils;

/**
 * This abstract class offers unit tests for concrete {@linkplain AbstractIteratorExtractor} implementations.
 *
 * @author Robin Weiss
 */
public abstract class AbstractIteratorExtractorTest <T> extends AbstractObjectUnitTest<AbstractIteratorExtractor<T>>
{
    private static final int INIT_TIMEOUT = 5000;
    private static final String WRONG_OBJECT_ERROR = "The extracted object from %s is unexpected:%n%s";


    /**
     * Returns the {@linkplain AbstractIteratorETL} to which the tested {@linkplain AbstractIteratorExtractor}
     * belongs.
     *
     * @return the {@linkplain AbstractIteratorETL} to which the tested {@linkplain AbstractIteratorExtractor}
     * belongs
     */
    protected abstract AbstractIteratorETL<T, ?> getEtl();


    /**
     * Returns an optional configuration {@linkplain File} that is
     * required to test the {@linkplain AbstractIteratorExtractor}.
     *
     * @return a configuration {@linkplain File} or null, if no parameters need to be set
     */
    protected abstract File getConfigFile();


    /**
     * Returns an object that is expected to be the transformation result
     * of the object returned by {@linkplain #getMockedInput()}.
     *
     * @return the expected transformation result
     */
    protected abstract T getExpectedOutput();


    /**
     * Returns the harvester specific {@linkplain ContextListener} subclass.
     *
     * @return the harvester specific {@linkplain ContextListener} subclass
     */
    protected abstract ContextListener getContextListener();


    @Override
    protected AbstractIteratorExtractor<T> setUpTestObjects()
    {
        final File httpResourceFolder = getMockedHttpResponseFolder();
        
        if(httpResourceFolder != null) {
            // copy mocked HTTP responses to the cache folder to drastically speed up the testing
            final File httpCacheFolder = new File(
                MainContextUtils.getCacheDirectory(getClass()),
                DataOperationConstants.CACHE_FOLDER_PATH);
            
            FileUtils.copyFile(httpResourceFolder, httpCacheFolder);
        }
        
        final ContextListenerTestWrapper<? extends AbstractIteratorETL<T, ?>> contextInitializer =
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
            INIT_TIMEOUT,
            () -> contextInitializer.initializeContext());

        // retrieve transformer from ETL
        final AbstractIteratorExtractor<T> extractor =
            (AbstractIteratorExtractor<T>) EtlUnitTestUtils.getExtractor(contextInitializer.getEtl());

        // initialize and return transformer
        extractor.init(contextInitializer.getEtl());
        return extractor;
    }
    
    
    /**
     * Returns a resource path that points to a folder that contains
     * cached HTTP responses, required for simulating an extraction.
     * 
     * @return a resource path that points to a folder that contains
     * cached HTTP responses
     */
    protected abstract File getMockedHttpResponseFolder();
    

    /**
     * Checks if the {@linkplain AbstractIteratorExtractor} implementation
     * extracts the expected output.
     */
    @Test
    public void testExtractElement()
    {
        final T actualOutput = testedObject.extract().next();
        final T expectedOutput = getExpectedOutput();
        System.out.println(GsonUtils.createGerdiDocumentGsonBuilder().create().toJson(actualOutput));

        assertEquals(String.format(
                         WRONG_OBJECT_ERROR,
                         testedObject.getClass().getSimpleName(),
                         actualOutput),
                     expectedOutput,
                     actualOutput);
    }
}
