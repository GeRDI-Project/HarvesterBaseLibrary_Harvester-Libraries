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
import static org.junit.Assert.assertNull;

import java.util.Map;

import org.junit.Test;

import de.gerdiproject.harvest.application.ContextListener;
import de.gerdiproject.harvest.application.events.ServiceInitializedEvent;
import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.events.GetConfigurationEvent;
import de.gerdiproject.harvest.etls.AbstractETL;
import de.gerdiproject.harvest.etls.extractors.IExtractor;
import de.gerdiproject.harvest.etls.transformers.ITransformer;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.json.GsonUtils;

/**
 * This abstract class offers unit tests for concrete {@linkplain ITransformer} implementations.
 *
 * @author Robin Weiss
 */
public abstract class AbstractTransformerTest <T, S, R extends ITransformer<T, S>> extends AbstractObjectUnitTest<R>
{
    private static final int INIT_TIMEOUT = 5000;
    private static final String WRONG_OBJECT_ERROR = "Expected the %s to return the following %s:%n%s";
    private static final String NON_NULL_OBJECT_ERROR = "Expected the %s to return null when transforming a null object!";


    private final AbstractETL<IExtractor<T>, R> etl;
    private final R transformer;


    public AbstractTransformerTest(final AbstractETL<IExtractor<T>, R> etl, final R transformer)
    {
        this.etl = etl;
        this.transformer = transformer;
    }


    protected abstract Map<String, String> getParameterValues();
    protected abstract T getMockedInput();
    protected abstract S getExpectedOutput();
    protected abstract ContextListener getContextListener();


    @Override
    protected R setUpTestObjects()
    {
        // initialize harvester service
        waitForEvent(
            ServiceInitializedEvent.class,
            INIT_TIMEOUT,
            () -> getContextListener().contextInitialized(null));

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
     * Checks if the {@linkplain ITransformer} implementation transforms a mocked input value to the expected output.
     */
    @Test
    public void testExtractedValue()
    {
        final T mockedInput = getMockedInput();
        final S expectedOutput = getExpectedOutput();
        final S actualOutput = testedObject.transform(mockedInput);

        assertEquals(String.format(
                         WRONG_OBJECT_ERROR,
                         testedObject.getClass().getSimpleName(),
                         expectedOutput.getClass().getSimpleName(), GsonUtils.createGerdiDocumentGsonBuilder().create().toJson(expectedOutput)),
                     expectedOutput,
                     actualOutput);
    }


    /**
     * Checks if the {@linkplain ITransformer} implementation transforms a null value to null.
     */
    @Test
    public void testExtractedNullValue()
    {
        final S nullOutput = testedObject.transform(null);

        assertNull(String.format(NON_NULL_OBJECT_ERROR, testedObject.getClass().getSimpleName()),
                   nullOutput);
    }
}
