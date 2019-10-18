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
package de.gerdiproject.harvest.application;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.gerdiproject.harvest.AbstractObjectUnitTest;
import de.gerdiproject.harvest.application.events.ContextDestroyedEvent;
import de.gerdiproject.harvest.application.events.ServiceInitializedEvent;
import de.gerdiproject.harvest.application.examples.MockedContextListener;
import de.gerdiproject.harvest.etls.loaders.AbstractURLLoader;

/**
 * This class offers unit tests for the {@linkplain ContextListener}.
 *
 * @author Robin Weiss
 */
public final class ContextListenerTest extends AbstractObjectUnitTest<ContextListener>
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
     * Tests if the {@linkplain ContextListener#contextInitialized(javax.servlet.ServletContextEvent)}
     * method ultimately triggers a {@linkplain ServiceInitializedEvent} to be sent that signifies that
     * the {@linkplain MainContext} was initialized successfully.
     */
    @Test
    public void testServiceDeployment()
    {
        ServiceInitializedEvent initializationDoneEvent =
            waitForEvent(ServiceInitializedEvent.class,
                         INIT_TIMEOUT,
                         () -> testedObject.contextInitialized(null));

        assertTrue("The contextInitialized() callback method should trigger a successful MainContext initialization!",
                   initializationDoneEvent.isSuccessful());
    }


    /**
     * Tests if the {@linkplain ContextListener#contextDestroyed(javax.servlet.ServletContextEvent)} method
     * sends a {@linkplain ContextDestroyedEvent}.
     */
    @Test
    public void testServiceUndeployment()
    {
        // init service
        waitForEvent(ServiceInitializedEvent.class,
                     INIT_TIMEOUT,
                     () -> testedObject.contextInitialized(null));

        // suppress logging
        System.out.close();

        // trigger shutdown
        ContextDestroyedEvent destroyedEvent = waitForEvent(ContextDestroyedEvent.class,
                                                            DEFAULT_EVENT_TIMEOUT,
                                                            () -> testedObject.contextDestroyed(null));

        assertNotNull("The method contextInitialized() should throw an event!",
                      destroyedEvent);
    }


    /**
     * Tests if the {@linkplain ContextListener#getServiceName} method provides an auto-generated service name.
     */
    @Test
    public void testServiceName()
    {
        // trigger reset
        assertNotNull("The method getServiceName() should return a proper string!",
                      ((MockedContextListener) testedObject).getServiceName());
    }


    /**
     * Tests if the {@linkplain ContextListener#getLoaderClasses} method provides at least one {@linkplain AbstractURLLoader} class.
     */
    @Test
    public void testSubmitterClasses()
    {
        // trigger reset
        assertFalse("The method getSubmitterClasses() is supposed to return a non-empty list of classe!",
                    ((MockedContextListener) testedObject).getLoaderClasses().isEmpty());
    }
}
