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
import java.lang.reflect.ParameterizedType;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.jsoup.nodes.Element;
import org.junit.Test;

import de.gerdiproject.harvest.AbstractETLUnitTest;
import de.gerdiproject.harvest.application.ContextListenerTestWrapper;
import de.gerdiproject.harvest.etls.AbstractIteratorETL;
import de.gerdiproject.harvest.etls.EtlUnitTestUtils;
import de.gerdiproject.harvest.etls.extractors.ExtractorException;
import de.gerdiproject.harvest.utils.data.DiskIO;
import de.gerdiproject.json.GsonUtils;

/**
 * This abstract class offers unit tests for concrete {@linkplain AbstractIteratorTransformer} implementations.
 *
 * @author Robin Weiss
 */
public abstract class AbstractIteratorTransformerTest <T, S> extends AbstractETLUnitTest<AbstractIteratorTransformer<T, S>, T>
{
    private static final String WRONG_OBJECT_ERROR = "The transformed object from %s is not as expected!";
    private static final String NON_NULL_OBJECT_ERROR = "Expected the %s to return an empty iterator when transforming an empty iterator!";
    private static final String NULL_INPUT_ERROR = "Expected the mocked input value to not be null!";

    protected final DiskIO diskIo = new DiskIO(GsonUtils.createGerdiDocumentGsonBuilder().create(), StandardCharsets.UTF_8);


    /**
     * Returns the class of the extracted objects.
     *
     * @return the class of the extracted objects
     */
    @SuppressWarnings("unchecked") // NOPMD the cast will always succeed
    protected Class<T> getExtractedClass()
    {
        Class<?> thisClass = getClass();

        while (!(thisClass.getSuperclass().equals(AbstractIteratorTransformerTest.class)))
            thisClass = thisClass.getSuperclass();

        return (Class<T>)((ParameterizedType) thisClass.getGenericSuperclass()).getActualTypeArguments()[0];
    }


    /**
     * Returns the class of the transformed objects.
     *
     * @return the class of the transformed objects
     */
    @SuppressWarnings("unchecked") // NOPMD the cast will always succeed
    protected Class<S> getTransformedClass()
    {
        // navigate to this exact abstract class in order to retrieve the correct type
        Class<?> thisClass = getClass();

        while (!(thisClass.getSuperclass().equals(AbstractIteratorTransformerTest.class)))
            thisClass = thisClass.getSuperclass();

        return (Class<S>)((ParameterizedType) thisClass.getGenericSuperclass()).getActualTypeArguments()[1];
    }


    /**
     * Returns a mocked object that is to be transformed by the
     * tested {@linkplain AbstractIteratorTransformer}.
     *
     * @return a mocked object that is to be transformed
     */
    @SuppressWarnings("unchecked")
    protected T getMockedInput()
    {
        final Class<T> extractedClass = getExtractedClass();

        // check if object is HTML
        if (Element.class.isAssignableFrom(extractedClass)) {
            final File resource = getResource("input.html");
            return (T) diskIo.getHtml(resource.toString());
        } else {
            final File resource = getResource("input.json");
            return diskIo.getObject(resource, extractedClass);
        }
    }


    /**
     * Returns an object that is expected to be the transformation result
     * of the object returned by {@linkplain #getMockedInput()}.
     *
     * @return the expected transformation result
     */
    @SuppressWarnings("unchecked")
    protected S getExpectedOutput()
    {
        final Class<S> transformedClass = getTransformedClass();

        // check if object is HTML
        if (Element.class.isAssignableFrom(transformedClass)) {
            final File resource = getResource("output.html");
            return (S) diskIo.getHtml(resource.toString());
        } else {
            final File resource = getResource("output.json");
            return diskIo.getObject(resource, transformedClass);
        }
    }


    @Override
    @SuppressWarnings("unchecked")
    protected AbstractIteratorTransformer<T, S> setUpTestedObjectFromContextInitializer(
        ContextListenerTestWrapper<? extends AbstractIteratorETL<T, ?>> contextInitializer)
    {
        final AbstractIteratorTransformer<T, S> transformer =
            (AbstractIteratorTransformer<T, S>) EtlUnitTestUtils.getTransformer(
                (AbstractIteratorETL<T, S>)contextInitializer.getEtl());

        // initialize and return transformer
        transformer.init(contextInitializer.getEtl());
        return transformer;
    }


    /**
     * Checks if the {@linkplain AbstractIteratorTransformer} implementation
     * transforms a mocked input value to the expected output.
     */
    @Test
    public void testMockedInputNonNull()
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
