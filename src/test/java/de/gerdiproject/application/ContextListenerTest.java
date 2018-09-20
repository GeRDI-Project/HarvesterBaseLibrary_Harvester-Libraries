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
package de.gerdiproject.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

import de.gerdiproject.AbstractObjectUnitTest;
import de.gerdiproject.application.examples.MockedContextListener;
import de.gerdiproject.harvest.application.ContextListener;
import de.gerdiproject.harvest.application.MainContext;
import de.gerdiproject.harvest.application.events.ContextDestroyedEvent;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.harvester.events.HarvesterInitializedEvent;
import de.gerdiproject.harvest.submission.AbstractSubmitter;
import de.gerdiproject.harvest.submission.events.GetSubmitterIdsEvent;

/**
 * This class offers unit tests for the {@linkplain ContextListener}.
 *
 * @author Robin Weiss
 */
public class ContextListenerTest extends AbstractObjectUnitTest<ContextListener<?>>
{
    private static final int INIT_TIMEOUT = 5000;


    // The onResetContext() callback cannot be properly tested, because it resets the EventSystem,
    // rendering the wait for another initialization event useless, while also not being
    // able to estimate when the MainContext is initialized again.


    @Override
    protected MockedContextListener setUpTestObjects()
    {
        return new MockedContextListener();
    }


    /**
     * Tests if the contextInitialized() method ultimately triggers a
     * {@linkplain HarvesterInitializedEvent} to be sent that signifies that
     * the {@linkplain MainContext} was initialized successfully.
     */
    @Test
    public void testServiceDeployment()
    {
        HarvesterInitializedEvent initializationDoneEvent =
            waitForEvent(HarvesterInitializedEvent.class,
                         INIT_TIMEOUT,
                         () -> testedObject.contextInitialized(null));

        assertTrue("The contextInitialized() callback method should trigger a successful MainContext initialization!",
                   initializationDoneEvent.isSuccessful());
    }


    /**
     * Tests if the contextDestroyed() method sends a {@linkplain ContextDestroyedEvent}.
     */
    @Test
    public void testServiceUndeployment()
    {
        // init service
        waitForEvent(HarvesterInitializedEvent.class,
                     INIT_TIMEOUT,
                     () -> testedObject.contextInitialized(null));

        System.out.print("This following message is part of Unit Testing: "); // NOPMD this is required to add to the message of the ContextListener

        // trigger shutdown
        ContextDestroyedEvent destroyedEvent = waitForEvent(ContextDestroyedEvent.class,
                                                            DEFAULT_EVENT_TIMEOUT,
                                                            () -> testedObject.contextDestroyed(null));

        assertNotNull("The method contextInitialized() should throw an event!",
                      destroyedEvent);
    }


    /**
     * Tests if the getServiceName() method provides an auto-generated service name.
     */
    @Test
    public void testServiceName()
    {
        // trigger reset
        assertNotNull("The method getServiceName() should return a proper string!",
                      ((MockedContextListener) testedObject).getServiceName());
    }


    /**
     * Tests if the {@linkplain ContextListener} successfully passes the service name to the
     * {@linkplain MainContext} upon initialization.
     */
    @Test
    public void testServiceNameInMainContext()
    {
        // init service
        waitForEvent(HarvesterInitializedEvent.class,
                     INIT_TIMEOUT,
                     () -> testedObject.contextInitialized(null));

        // trigger reset
        assertEquals("There is supposed to be an auto-created service name after intializing the context!",
                     ((MockedContextListener) testedObject).getServiceName(),
                     MainContext.getServiceName());
    }


    /**
     * Tests if the getSubmitterClasses() method provides at least one {@linkplain AbstractSubmitter} class.
     */
    @Test
    public void testSubmitterClasses()
    {
        // trigger reset
        assertFalse("The method getSubmitterClasses() is supposed to return a non-empty list of classe!",
                    ((MockedContextListener) testedObject).getSubmitterClasses().isEmpty());
    }


    /**
     * Tests if the {@linkplain ContextListener} successfully passes the submitter classes to the
     * {@linkplain MainContext} upon initialization.
     * @throws IllegalAccessException thrown if a submitter could not be initialized
     * @throws InstantiationException thrown if a submitter could not be initialized
     */
    @Test
    public void testSubmitterIDsInMainContext() throws InstantiationException, IllegalAccessException
    {
        // init service
        waitForEvent(HarvesterInitializedEvent.class,
                     INIT_TIMEOUT,
                     () -> testedObject.contextInitialized(null));

        // get list of registered submitters
        final Set<String> registeredSubmitterIds = EventSystem.sendSynchronousEvent(new GetSubmitterIdsEvent());

        // compare registered submitters to the ones provided by the ContextListener
        for (Class<? extends AbstractSubmitter> submitterClass : ((MockedContextListener) testedObject).getSubmitterClasses()) {
            assertTrue(
                "The ID of every submitter defined by getSubmitterClasses() should be registered after initialization!",
                registeredSubmitterIds.contains(submitterClass.newInstance().getId()));
        }
    }
}
