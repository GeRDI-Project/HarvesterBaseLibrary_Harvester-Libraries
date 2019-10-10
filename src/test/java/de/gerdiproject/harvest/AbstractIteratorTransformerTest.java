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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import de.gerdiproject.harvest.application.ContextListener;
import de.gerdiproject.harvest.application.events.ServiceInitializedEvent;
import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.events.GetConfigurationEvent;
import de.gerdiproject.harvest.etls.AbstractIteratorETL;
import de.gerdiproject.harvest.etls.ETLPreconditionException;
import de.gerdiproject.harvest.etls.extractors.ExtractorException;
import de.gerdiproject.harvest.etls.transformers.AbstractIteratorTransformer;
import de.gerdiproject.harvest.event.EventSystem;

/**
 * This abstract class offers unit tests for concrete {@linkplain AbstractIteratorTransformer} implementations.
 *
 * @author Robin Weiss
 */
public abstract class AbstractIteratorTransformerTest <T extends AbstractIteratorTransformer<R, S>, R, S> extends AbstractObjectUnitTest<T>
{
    private static final int INIT_TIMEOUT = 5000;
    private static final String WRONG_OBJECT_ERROR = "The transformed object from %s is unexpected:%n%s";
    private static final String NON_NULL_OBJECT_ERROR = "Expected the %s to return an empty iterator when transforming an empty iterator!";

    protected final AbstractIteratorETL<R, S> etl;
    protected final T transformer;


    /**
     * Constructor that requires an instance of the tested
     * transformer, as well as its corresponding ETL.
     *
     * @param etl the {@linkplain AbstractIteratorETL} to which the transformer belongs
     * @param transformer the tested transformer
     */
    public AbstractIteratorTransformerTest(final AbstractIteratorETL<R, S> etl, final T transformer)
    {
        this.etl = etl;
        this.transformer = transformer;
    }


    /**
     * Returns an optional {@linkplain String} {@linkplain Map} of
     * parameter/value pairs in order to simulate a pre-loaded {@linkplain Configuration}.
     * If no parameters are required, null is returned.
     *
     * @return a {@linkplain String} {@linkplain Map} of
     * parameter/value pairs or null, if no parameters need to be set
     */
    protected abstract Map<String, String> getParameterValues();


    /**
     * Returns the {@linkplain ContextListener} implementation of the
     * tested harvester service.
     *
     * @return  the {@linkplain ContextListener} implementation of the
     * tested harvester service
     */
    protected abstract ContextListener getContextListener();


    /**
     * Returns a mocked object that is to be transformed by the
     * tested {@linkplain AbstractIteratorTransformer}.
     *
     * @return a mocked object that is to be transformed
     */
    protected abstract R getMockedInput();


    /**
     * Returns an object that is expected to be the transformation result
     * of the object returned by {@linkplain #getMockedInput()}.
     *
     * @return the expected transformation result
     */
    protected abstract S getExpectedOutput();


    @Override
    protected T setUpTestObjects()
    {
        // initialize harvester service
        waitForEvent(
            ServiceInitializedEvent.class,
            INIT_TIMEOUT,
            () -> getContextListener().contextInitialized(null));

        etl.init("MockedHarvesterService");

        try {
            etl.update();
        } catch (final ETLPreconditionException e) { // NOPMD - Ignore exceptions, because we do not need to harvest yet
        }

        // overwrite configuration
        final Map<String, String> paramValues = getParameterValues();

        if (paramValues != null && !paramValues.isEmpty()) {
            final Configuration config = EventSystem.sendSynchronousEvent(new GetConfigurationEvent());
            config.changeParameters(paramValues);
        }

        // initialize transformer
        transformer.init(etl);
        return transformer;
    }


    /**
     * Checks if the {@linkplain AbstractIteratorTransformer} implementation
     * transforms a mocked input value to the expected output.
     */
    @Test
    public void testTransformedValue()
    {
        final Iterator<R> mockedInput = Arrays.asList(getMockedInput()).iterator();
        final S actualOutput = testedObject.transform(mockedInput).next();
        final S expectedOutput = getExpectedOutput();

        assertEquals(String.format(
                         WRONG_OBJECT_ERROR,
                         testedObject.getClass().getSimpleName(),
                         actualOutput),
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
        final List<R> emptyInput = Arrays.asList();
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
