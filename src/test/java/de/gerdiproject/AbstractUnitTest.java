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

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.classic.Level;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.IEventListener;
import de.gerdiproject.harvest.utils.logger.constants.LoggerConstants;

/**
 * This class serves as a base for all unit tests that test instantiable classes.
 * It provides common convenience functions and helper objects.
 *
 * @author Robin Weiss
 */
public abstract class AbstractUnitTest<T>
{
    private static final String CLEANUP_ERROR = "Could not instantiate object: ";

    private final Level initialLogLevel = LoggerConstants.ROOT_LOGGER.getLevel();
    protected final Random random = new Random();
    protected T testedObject;


    /**
     * Creates an instance of the tested object.
     *
     * @throws IOException thrown when the test folder could not be deleted
     */
    @Before
    public void before() throws InstantiationException
    {
        testedObject = setUpTestObjects();

        if (testedObject == null)
            throw new InstantiationException(CLEANUP_ERROR + getTestedClass().getName());
    }


    /**
     * Removes event listeners if there are any and nullifies the tested object.
     */
    @After
    public void after()
    {
        EventSystem.reset();
        //testedObject = null;
    }


    /**
     * Tests if event listeners are added when the
     * addEventListeners() function is called.
     */
    @Test
    public void testAddingEventListeners()
    {
        if (testedObject instanceof IEventListener) {
            ((IEventListener) testedObject).addEventListeners();

            assert EventSystem.hasSynchronousEventListeners()
            || EventSystem.hasAsynchronousEventListeners();
        }
    }


    /**
     * Tests if there are no event listeners after the tested object
     * was constructed.
     */
    @Test
    public void testForNoInitialEventListeners()
    {
        if (testedObject instanceof IEventListener) {
            assert EventSystem.hasSynchronousEventListeners()
            || EventSystem.hasAsynchronousEventListeners();
        }
    }


    /**
     * Tests if all event listeners are removed when the
     * removeEventListeners() function is called.
     */
    @Test
    public void testRemovingEventListeners()
    {
        if (testedObject instanceof IEventListener) {
            ((IEventListener) testedObject).addEventListeners();
            ((IEventListener) testedObject).removeEventListeners();

            assert !EventSystem.hasSynchronousEventListeners()
            && !EventSystem.hasAsynchronousEventListeners();
        }
    }


    /**
     * Sets up all objects that are required for the following test
     * and returns the main tested object.
     *
     * @return the main tested object
     */
    protected abstract T setUpTestObjects();


    /**
     * Enables or disables the logger.
     *
     * @param state if true, the logger is enabled
     */
    protected void setLoggerEnabled(boolean state)
    {
        final Level newLevel = state ? initialLogLevel : Level.OFF;
        LoggerConstants.ROOT_LOGGER.setLevel(newLevel);
    }


    /**
     * Returns the class that is being tested
     *
     * @return the class that is being tested
     */
    @SuppressWarnings("unchecked")
    protected Class<T> getTestedClass()
    {
        return (Class<T>)((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }
}
