/*
 *  Copyright © 2018 Robin Weiss (http://www.gerdi-project.de/)
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
package de.gerdiproject;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.ParameterizedType;

import org.junit.Assume;
import org.junit.Test;

import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.IEventListener;

/**
 * This class serves as a base for all unit tests that test instantiable classes.
 * It provides common convenience functions and helper objects.
 *
 * @author Robin Weiss
 */
public abstract class AbstractObjectUnitTest<T> extends AbstractUnitTest
{
    private static final String CLEANUP_ERROR = "Could not instantiate object: ";
    private static final String SKIP_EVENT_TESTS_MESSAGE = "Skipping event listener tests, because %s does not implement " + IEventListener.class.getSimpleName() + ".";


    protected Configuration config;
    protected T testedObject;


    @Override
    public void before() throws InstantiationException
    {
        super.before();
        testedObject = setUpTestObjects();

        if (testedObject == null)
            throw new InstantiationException(CLEANUP_ERROR + getTestedClass().getName());
    }


    @Override
    public void after()
    {
        super.after();
        testedObject = null;
    }


    /**
     * Tests if event listeners are added when the
     * addEventListeners() function is called.
     */
    @Test
    public void testAddingEventListeners()
    {
        Assume.assumeTrue(String.format(SKIP_EVENT_TESTS_MESSAGE, testedObject.getClass().getSimpleName()),
                          testedObject instanceof IEventListener);

        // if there is a config, remove its listeners to not falsify the result
        if (config != null)
            config.removeEventListeners();

        ((IEventListener) testedObject).addEventListeners();

        assertTrue("The method addEventListeners() is not adding any event listeners!", EventSystem.hasSynchronousEventListeners()
                   || EventSystem.hasAsynchronousEventListeners());
    }


    /**
     * Tests if there are no event listeners after the tested object
     * was constructed.
     */
    @Test
    public void testForNoInitialEventListeners()
    {
        Assume.assumeTrue(String.format(SKIP_EVENT_TESTS_MESSAGE, testedObject.getClass().getSimpleName()),
                          testedObject instanceof IEventListener);

        // if there is a config, remove its listeners to not falsify the result
        if (config != null)
            config.removeEventListeners();

        assertFalse("Event listeners should not be added in the constructor!", EventSystem.hasSynchronousEventListeners()
                    || EventSystem.hasAsynchronousEventListeners());
    }


    /**
     * Tests if all event listeners are removed when the
     * removeEventListeners() function is called.
     */
    @Test
    public void testRemovingEventListeners()
    {
        Assume.assumeTrue(String.format(SKIP_EVENT_TESTS_MESSAGE, testedObject.getClass().getSimpleName()),
                          testedObject instanceof IEventListener);

        // if there is a config, remove its listeners to not falsify the result
        if (config != null)
            config.removeEventListeners();

        ((IEventListener) testedObject).addEventListeners();
        ((IEventListener) testedObject).removeEventListeners();

        assertFalse("The method removeEventListeners() should remove all listeners!", EventSystem.hasSynchronousEventListeners()
                    || EventSystem.hasAsynchronousEventListeners());
    }


    /**
     * Sets up all objects that are required for the following test
     * and returns the main tested object.
     *
     * @return the main tested object
     */
    protected abstract T setUpTestObjects();


    /**
     * Returns the class that is being tested
     *
     * @return the class that is being tested
     */
    @SuppressWarnings("unchecked") // NOPMD the cast will always succeed
    protected Class<T> getTestedClass()
    {
        return (Class<T>)((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }
}
