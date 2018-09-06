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
import static org.junit.Assert.fail;

import java.lang.reflect.ParameterizedType;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.classic.Level;
import de.gerdiproject.harvest.application.MainContext;
import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.IEvent;
import de.gerdiproject.harvest.event.IEventListener;
import de.gerdiproject.harvest.state.StateMachine;
import de.gerdiproject.harvest.utils.CancelableFuture;
import de.gerdiproject.harvest.utils.Procedure;
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
    private static final String SKIP_EVENT_TESTS_MESSAGE = "Skipping event listener tests, because %s does not implement " + IEventListener.class.getSimpleName() + ".";
    private static final String EXPECTED_EVENT_FAIL_MESSAGE = "Expected the %s to be sent before %d ms have passed!";
    protected static final int DEFAULT_EVENT_TIMEOUT = 2000;

    private final Level initialLogLevel = LoggerConstants.ROOT_LOGGER.getLevel();
    protected final Random random = new Random();

    protected Configuration config;
    protected T testedObject;


    /**
     * Creates an instance of the tested object.
     *
     * @throws InstantiationException thrown when the test folder could not be deleted
     */
    @Before
    public void before() throws InstantiationException
    {
        setLoggerEnabled(isLoggingEnabledDuringTests());
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
        MainContext.destroy();
        StateMachine.setState(null);

        testedObject = null;

        setLoggerEnabled(true);
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
     * If true, the logger is enabled during testing.
     * @return true if logs are to be generated during testing
     */
    protected boolean isLoggingEnabledDuringTests()
    {
        return false;
    }


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


    /**
     * This method executes a procedure and then waits for an event to be dispatched before returning it.
     * If a specified timeout is reached, null is returned instead.
     *
     * @param eventClass the class of the event that is to be retrieved
     * @param timeout the timeout in milliseconds until the method returns null
     * @param startAction a procedure that ultimately causes the expected event to be sent
     *
     * @param <E> the type of the event that is expected
     *
     * @return the received event or null, if an exception or a timeout occurred
     */
    @SuppressWarnings("unchecked") // NOPMD - this function is horrible but useful. The cast will always succeed
    protected <E extends IEvent> E waitForEvent(Class<E> eventClass, int timeout, final Procedure startAction)
    {
        // add a listener that memorizes the event when it is dispatched
        final Object[] receivedEvent = new Object[1];
        final Consumer<E> onEventReceived = (E event) -> receivedEvent[0] = event;
        EventSystem.addListener(eventClass, onEventReceived);

        // execute this asynchronously
        try {
            CancelableFuture<Boolean> asyncProcess = new CancelableFuture<>(() -> {

                // execute the action that ultimately triggers the expected event
                startAction.run();

                // wait for the event to be dispatched or the timeout to be reached
                int passedTime = 0;

                while (passedTime < timeout)
                {
                    if (receivedEvent[0] != null)
                        return true;

                    Thread.sleep(100);
                    passedTime += 100;
                }
                return false;
            });

            // wait for the asynchronous process to finish
            CompletableFuture.allOf(asyncProcess).get();
        } catch (InterruptedException | ExecutionException e) {
            receivedEvent[0] = null;
        }

        // clean up the temporary listener
        EventSystem.removeListener(eventClass, onEventReceived);

        if (receivedEvent[0] == null) {
            fail(String.format(EXPECTED_EVENT_FAIL_MESSAGE, eventClass.getSimpleName(), timeout));
            return null;
        } else
            return (E) receivedEvent[0];
    }
}
