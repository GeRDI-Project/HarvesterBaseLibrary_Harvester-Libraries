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


import static org.junit.Assert.fail;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;

import ch.qos.logback.classic.Level;
import de.gerdiproject.harvest.application.MainContext;
import de.gerdiproject.harvest.application.constants.ApplicationConstants;
import de.gerdiproject.harvest.application.enums.DeploymentType;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.IEvent;
import de.gerdiproject.harvest.utils.CancelableFuture;
import de.gerdiproject.harvest.utils.Procedure;
import de.gerdiproject.harvest.utils.logger.constants.LoggerConstants;

/**
 * This class serves as a base for all Harvester Library unit tests.
 * It provides common convenience functions and helper objects.
 *
 * @author Robin Weiss
 */
public abstract class AbstractUnitTest
{
    protected static final String MODULE_NAME = "UnitTestModule";
    private static final String EXPECTED_EVENT_FAIL_MESSAGE = "Expected the %s to be sent before %d ms have passed!";
    protected static final int DEFAULT_EVENT_TIMEOUT = 2000;

    private final Level initialLogLevel = LoggerConstants.ROOT_LOGGER.getLevel();
    protected final Random random = new Random();


    /**
     * Sets up a following test.
     *
     * @throws InstantiationException thrown when the test setup failed
     */
    @Before
    public void before() throws InstantiationException
    {
        System.setProperty(
            ApplicationConstants.DEPLOYMENT_TYPE,
            DeploymentType.UNIT_TEST.toString());

        setLoggerEnabled(isLoggingEnabledDuringTests());
    }


    /**
     * Removes event listeners if there are any and cleans
     * up objects that were set up throughout the test.
     */
    @After
    public void after()
    {
        EventSystem.reset();
        MainContext.destroy();

        setLoggerEnabled(true);
    }


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
