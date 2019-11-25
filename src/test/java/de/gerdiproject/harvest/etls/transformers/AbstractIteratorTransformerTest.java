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
package de.gerdiproject.harvest.etls.transformers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import de.gerdiproject.harvest.AbstractObjectUnitTest;
import de.gerdiproject.harvest.application.ContextListener;
import de.gerdiproject.harvest.application.ContextListenerTestWrapper;
import de.gerdiproject.harvest.application.events.ServiceInitializedEvent;
import de.gerdiproject.harvest.etls.AbstractIteratorETL;
import de.gerdiproject.harvest.etls.EtlUnitTestUtils;
import de.gerdiproject.harvest.etls.extractors.ExtractorException;
import de.gerdiproject.harvest.utils.file.FileUtils;

/**
 * This abstract class offers unit tests for concrete {@linkplain AbstractIteratorTransformer} implementations.
 *
 * @author Robin Weiss
 */
public abstract class AbstractIteratorTransformerTest <T, S> extends AbstractObjectUnitTest<AbstractIteratorTransformer<T, S>>
{
    private static final int INIT_TIMEOUT = 5000;
    private static final String WRONG_OBJECT_ERROR = "The transformed object from %s is not as expected!";
    private static final String NON_NULL_OBJECT_ERROR = "Expected the %s to return an empty iterator when transforming an empty iterator!";
    private static final String NULL_INPUT_ERROR = "Expected the mocked input value to not be null!";


    /**
     * Returns the {@linkplain AbstractIteratorETL} to which the tested {@linkplain AbstractIteratorTransformer}
     * belongs.
     *
     * @return the {@linkplain AbstractIteratorETL} to which the tested {@linkplain AbstractIteratorTransformer}
     * belongs
     */
    protected abstract AbstractIteratorETL<T, S> getEtl();


    /**
     * Returns an optional configuration {@linkplain File} that is
     * required to test the {@linkplain AbstractIteratorTransformer}.
     *
     * @return a configuration {@linkplain File} or null, if no parameters need to be set
     */
    protected File getConfigFile()
    {
        return getResource("config.json");
    }


    /**
     * Returns a mocked object that is to be transformed by the
     * tested {@linkplain AbstractIteratorTransformer}.
     *
     * @return a mocked object that is to be transformed
     */
    protected abstract T getMockedInput();


    /**
     * Returns an object that is expected to be the transformation result
     * of the object returned by {@linkplain #getMockedInput()}.
     *
     * @return the expected transformation result
     */
    protected abstract S getExpectedOutput();


    /**
     * Returns the harvester specific {@linkplain ContextListener} subclass.
     *
     * @return the harvester specific {@linkplain ContextListener} subclass
     */
    protected abstract ContextListener getContextListener();


    @Override
    protected AbstractIteratorTransformer<T, S> setUpTestObjects()
    {
        final ContextListenerTestWrapper<? extends AbstractIteratorETL<T, S>> contextInitializer =
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
        final AbstractIteratorTransformer<T, S> transformer =
            (AbstractIteratorTransformer<T, S>) EtlUnitTestUtils.getTransformer(contextInitializer.getEtl());

        // initialize and return transformer
        transformer.init(contextInitializer.getEtl());
        return transformer;
    }


    /**
     * Checks if the {@linkplain AbstractIteratorTransformer} implementation
     * transforms a mocked input value to the expected output.
     */
    @Test
    public void testMockedInput()
    {
        assertNotNull(NULL_INPUT_ERROR, getMockedInput());
    }


    /**
     * Checks if the {@linkplain AbstractIteratorTransformer} implementation
     * transforms a mocked input value to the expected output.
     */
    @Test
    public void testTransformElement()
    {
        final T mockedInput = getMockedInput();
        final S actualOutput = testedObject.transformElement(mockedInput);
        final S expectedOutput = getExpectedOutput();

        assertEquals(String.format(
                         WRONG_OBJECT_ERROR,
                         testedObject.getClass().getSimpleName()),
                     expectedOutput,
                     actualOutput);
    }


    /**
     * Checks if the {@linkplain AbstractIteratorTransformer} implementation
     * transforms an empty {@linkplain Iterator} to an empty {@linkplain Iterator}.
     */
    @Test
    public void testTransformEmptyIterator()
    {
        final List<T> emptyInput = Arrays.asList();
        final Iterator<S> emptyOutput = testedObject.transform(emptyInput.iterator());

        assertFalse(String.format(NON_NULL_OBJECT_ERROR, testedObject.getClass().getSimpleName()),
                    emptyOutput.hasNext());
    }


    /**
     * Checks if the {@linkplain AbstractIteratorTransformer} implementation throws
     * an {@linkplain ExtractorException} when transforming a null value.
     */
    @Test(expected = ExtractorException.class)
    public void testTransformNull()
    {
        final Iterator<S> nullOutput = testedObject.transform(null);
        nullOutput.next();
    }
}
