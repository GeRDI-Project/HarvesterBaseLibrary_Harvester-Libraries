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

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.gerdiproject.AbstractUnitTest;
import de.gerdiproject.application.examples.MockedContextListener;
import de.gerdiproject.harvest.application.MainContext;
import de.gerdiproject.harvest.etls.events.HarvesterInitializedEvent;

/**
 * This class offers unit tests for the {@linkplain MainContext}.
 *
 * @author Robin Weiss
 */
public class MainContextTest extends AbstractUnitTest
{
    private static final int INIT_TIMEOUT = 5000;


    /**
     * Tests if the init() method ultimately sends a {@linkplain HarvesterInitializedEvent}
     * that marks the initialization as successful.
     */
    @Test
    public void testInitialization()
    {
        final MockedContextListener mockedContextListener = new MockedContextListener();

        HarvesterInitializedEvent initDoneEvent = waitForEvent(
                                                      HarvesterInitializedEvent.class,
                                                      INIT_TIMEOUT,
                                                      () -> mockedContextListener.contextInitialized(null));

        assertTrue("Expected the method init() to send an event marking the initialization as successful!",
                   initDoneEvent.isSuccessful());
    }
}
