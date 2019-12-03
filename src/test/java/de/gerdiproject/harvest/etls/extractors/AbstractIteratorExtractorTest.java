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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.ParameterizedType;
import java.nio.charset.StandardCharsets;

import org.jsoup.nodes.Element;
import org.junit.Test;

import de.gerdiproject.harvest.AbstractETLUnitTest;
import de.gerdiproject.harvest.application.ContextListenerTestWrapper;
import de.gerdiproject.harvest.etls.AbstractIteratorETL;
import de.gerdiproject.harvest.etls.EtlUnitTestUtils;
import de.gerdiproject.harvest.utils.data.DiskIO;
import de.gerdiproject.json.GsonUtils;

/**
 * This abstract class offers unit tests for concrete {@linkplain AbstractIteratorExtractor} implementations.
 *
 * @author Robin Weiss
 */
public abstract class AbstractIteratorExtractorTest <T> extends AbstractETLUnitTest<AbstractIteratorExtractor<T>, T>
{
    protected static final String WRONG_OBJECT_ERROR = "The extracted object from %s is not as expected!";

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

        while (!(thisClass.getSuperclass().equals(AbstractIteratorExtractorTest.class)))
            thisClass = thisClass.getSuperclass();

        return (Class<T>)((ParameterizedType) thisClass.getGenericSuperclass()).getActualTypeArguments()[0];
    }


    /**
     * Returns an object that is expected to be the expected result
     * of the extraction process.
     *
     * @return the expected extraction result
     */
    @SuppressWarnings("unchecked")
    protected T getExpectedOutput()
    {
        final Class<T> extractedClass = getExtractedClass();

        // check if object is HTML
        if (Element.class.isAssignableFrom(extractedClass)) {
            final File resource = getResource("output.html");
            return (T) diskIo.getHtml(resource.toString());
        } else {
            final File resource = getResource("output.json");
            return diskIo.getObject(resource, extractedClass);
        }
    }


    /**
     * Checks if the {@linkplain AbstractIteratorExtractor} implementation
     * extracts the non-null output.
     */
    @Test
    public void testExtractElementNonNull()
    {
        final T actualOutput = testedObject.extract().next();
        assertNotNull(actualOutput);
    }


    /**
     * Checks if the {@linkplain AbstractIteratorExtractor} implementation
     * extracts the expected output.
     */
    @Test
    public void testExtractElement()
    {
        final T actualOutput = testedObject.extract().next();
        final T expectedOutput = getExpectedOutput();

        assertExpectedOutput(expectedOutput, actualOutput);
    }


    @Override
    protected AbstractIteratorExtractor<T> setUpTestedObjectFromContextInitializer(
        ContextListenerTestWrapper<? extends AbstractIteratorETL<T, ?>> contextInitializer)
    {
        final AbstractIteratorExtractor<T> extractor =
            (AbstractIteratorExtractor<T>) EtlUnitTestUtils.getExtractor(contextInitializer.getEtl());

        // initialize and return extractor
        extractor.init(contextInitializer.getEtl());
        return extractor;
    }


    /**
     * Asserts that the extracted output equals the expected output.
     *
     * @param expectedOutput the object that was extracted
     * @param actualOutput the object that was expected to be extracted
     */
    protected void assertExpectedOutput(final T expectedOutput, final T actualOutput)
    {
        // determine assertion by the extracted type
        if (Element.class.isAssignableFrom(getExtractedClass())) {
            assertTrue(String.format(WRONG_OBJECT_ERROR, testedObject.getClass().getSimpleName()),
                       ((Element)expectedOutput).hasSameValue(actualOutput));
        } else {
            assertEquals(String.format(WRONG_OBJECT_ERROR, testedObject.getClass().getSimpleName()),
                         expectedOutput,
                         actualOutput);
        }
    }
}
