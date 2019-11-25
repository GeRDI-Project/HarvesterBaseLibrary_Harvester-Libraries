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
package de.gerdiproject.harvest.event; // NOPMD JUnit 4 requires many static imports

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.examples.TestEvent;
import de.gerdiproject.harvest.event.examples.TestSynchronousEvent;


/**
 * This class provides test cases for the {@linkplain EventSystem}.
 *
 * @author Robin Weiss
 */
public class EventSystemTest
{
    private final static List<TestEvent> TEST_EVENTS = Arrays.asList(
                                                           new TestEvent("uno"),
                                                           new TestEvent("dos"),
                                                           new TestEvent("tres"));
    private final static TestEvent SINGLE_TEST_EVENT = new TestEvent("single");
    private final static TestSynchronousEvent SINGLE_SYNC_TEST_EVENT = new TestSynchronousEvent("single");
    private final static String STATIC_SYNC_PAYLOAD = "123";
    private final static String REMOVE_LISTENER_ERROR = "Removing non-existing listeners should not cause exceptions";


    private List<TestEvent> receivedEvents;


    /**
     * Cleans up the list of received events.
     */
    @Before
    public void before()
    {
        this.receivedEvents = new LinkedList<>();
    }


    /**
     * Removes all event listeners after testing.
     */
    @After
    public void after()
    {
        EventSystem.removeAllListeners(TestEvent.class);
        EventSystem.removeSynchronousListener(TestSynchronousEvent.class);
    }


    /**
     * Tests if events that are sent without registered listeners
     * are ignored.
     */
    @Test
    public void testSendingEventsWithoutAddedListener()
    {
        final TestEvent ignoredEvent = TEST_EVENTS.get(0);

        EventSystem.sendEvent(ignoredEvent);
        assertFalse("A callback function of " + ignoredEvent.getClass().getSimpleName() + " was called although no listener was added!",
                    receivedEvents.contains(ignoredEvent));
    }


    /**
     * Tests if events are properly received after listeners were added.
     */
    @Test
    public void testSendingEventsWithAddedListener()
    {
        final TestEvent expectedEvent = TEST_EVENTS.get(1);
        EventSystem.addListener(TestEvent.class, onTestEvent);
        EventSystem.sendEvent(expectedEvent);

        assertTrue("The callback function of " + expectedEvent.getClass().getSimpleName() + " was never called although a listener was added!",
                   receivedEvents.contains(expectedEvent));
    }


    /**
     * Tests if an event listener is called twice if it was added twice.
     */
    @Test
    public void testAddingListenerTwice()
    {
        EventSystem.addListener(TestEvent.class, onTestEvent);
        EventSystem.addListener(TestEvent.class, onTestEvent);

        EventSystem.sendEvent(SINGLE_TEST_EVENT);

        assertEquals("The same callback function was assigned to " + TestEvent.class.getSimpleName() + " twice, so sending the event once, should cause the callback function to be called twice!",
                     2,
                     receivedEvents.size());
    }


    /**
     * Tests if events that are sent after their listeners were removed
     * are ignored.
     */
    @Test
    public void testRemovingListener()
    {
        EventSystem.addListener(TestEvent.class, onTestEvent);
        EventSystem.removeListener(TestEvent.class, onTestEvent);

        EventSystem.sendEvent(SINGLE_TEST_EVENT);

        assertEquals("The method removeListener() should have caused the event listener to be removed!",
                     0,
                     receivedEvents.size());
    }


    /**
     * Tests if removing a non-existing event listener throws no exceptions.
     */
    @Test
    public void testRemovingNonExistingListener()
    {
        try {
            EventSystem.removeListener(TestEvent.class, onTestEvent);
        } catch (Exception e) {
            fail(REMOVE_LISTENER_ERROR);
        }
    }


    /**
     * Tests all event listeners are properly removed when calling
     * removeAllListeners().
     */
    @Test
    public void testRemovingAllListeners()
    {
        EventSystem.addListener(TestEvent.class, onTestEvent);
        EventSystem.addListener(TestEvent.class, onTestEvent);
        EventSystem.addListener(TestEvent.class, onTestEvent);
        EventSystem.removeAllListeners(TestEvent.class);

        EventSystem.sendEvent(SINGLE_TEST_EVENT);

        assertEquals("The method removeAllListeners() should have removed all listeners of " + TestEvent.class.getSimpleName() + "!",
                     0,
                     receivedEvents.size());
    }


    /**
     * Tests if removing all listeners throws no exceptions
     * when the listeners were not added.
     */
    @Test
    public void testRemovingAllListenersNonExisting()
    {
        try {
            EventSystem.removeAllListeners(TestEvent.class);
        } catch (Exception e) {
            fail(REMOVE_LISTENER_ERROR);
        }
    }


    /**
     * Tests if synchronous events that are sent without registered listeners
     * return null.
     */
    @Test
    public void testSendingSynchronousEventWithoutAddedListener()
    {
        final Object nullResult = EventSystem.sendSynchronousEvent(SINGLE_SYNC_TEST_EVENT);

        assertNull("The method sendSynchronousEvent() should return null if it was not assigned via addSynchronousListener()!",
                   nullResult);
    }


    /**
     * Tests if results are properly returned after a synchronous event listener was added.
     */
    @Test
    public void testSendingSynchronousEventWithAddedListener()
    {
        EventSystem.addSynchronousListener(TestSynchronousEvent.class, onTestSyncEvent);
        final Object result = EventSystem.sendSynchronousEvent(SINGLE_SYNC_TEST_EVENT);

        assertEquals("The method sendSynchronousEvent() did not return the value that was defined in its callback function!",
                     SINGLE_SYNC_TEST_EVENT.getPayload(),
                     result);
    }


    /**
     * Tests if a synchronous event listeners can be replaced.
     */
    @Test
    public void testAddingSynchronousListenerOverride()
    {
        EventSystem.addSynchronousListener(TestSynchronousEvent.class, onTestSyncEvent);
        final Object oldResult = EventSystem.sendSynchronousEvent(SINGLE_SYNC_TEST_EVENT);

        EventSystem.addSynchronousListener(TestSynchronousEvent.class, onTestSyncEvent2);
        final Object overriddenResult = EventSystem.sendSynchronousEvent(SINGLE_SYNC_TEST_EVENT);

        assertNotEquals("The method addSynchronousListener() should overwrite an already assigned callback function!",
                        oldResult,
                        overriddenResult);
    }


    /**
     * Tests if results are properly returned after a synchronous event listener was added that does
     * not require the event as an argument for the callback function.
     */
    @Test
    public void testSendingSynchronousEventWithoutEventArgument()
    {
        EventSystem.addSynchronousListener(TestSynchronousEvent.class, () -> STATIC_SYNC_PAYLOAD);

        final Object result = EventSystem.sendSynchronousEvent(SINGLE_SYNC_TEST_EVENT);
        assertEquals("The method sendSynchronousEvent() should return the expected payload after addSynchronousListener() was called with a Supplier as argument!",
                     STATIC_SYNC_PAYLOAD,
                     result);
    }


    /**
     * Tests if synchronous events that are sent after their listener was removed
     * return null.
     */
    @Test
    public void testRemovingSynchronousListener()
    {
        EventSystem.addSynchronousListener(TestSynchronousEvent.class, onTestSyncEvent);
        EventSystem.removeSynchronousListener(TestSynchronousEvent.class);

        final Object nullResult = EventSystem.sendSynchronousEvent(SINGLE_SYNC_TEST_EVENT);
        assertNull("The method sendSynchronousEvent() should return null after removeSynchronousListener() was called!",
                   nullResult);
    }


    /**
     * Tests if removing a non-existing event listener throw no exceptions
     * are ignored.
     */
    @Test
    public void testRemovingNonExistingSynchronousListener()
    {
        try {
            EventSystem.removeSynchronousListener(TestSynchronousEvent.class);
        } catch (Exception e) {
            fail(REMOVE_LISTENER_ERROR);
        }
    }


    /**
     * Tests if resetting the EventSystem removes all asynchronous event listeners.
     */
    @Test
    public void testSendingEventsAfterReset()
    {
        EventSystem.addListener(TestEvent.class, onTestEvent);
        EventSystem.addListener(TestEvent.class, onTestEvent);
        EventSystem.addListener(TestEvent.class, onTestEvent2);
        EventSystem.reset();

        EventSystem.sendEvent(SINGLE_TEST_EVENT);
        assertEquals("The method reset() should remove all asynchronous callback functions!",
                     0,
                     receivedEvents.size());
    }


    /**
     * Tests if resetting the EventSystem removes all synchronous listeners.
     */
    @Test
    public void testSendingSynchronousEventsAfterReset()
    {
        EventSystem.addSynchronousListener(TestSynchronousEvent.class, onTestSyncEvent);
        EventSystem.reset();

        final Object nullResult = EventSystem.sendSynchronousEvent(SINGLE_SYNC_TEST_EVENT);
        assertNull("The method reset() should remove all synchronous callback functions!",
                   nullResult);
    }


    /**
     * Tests if resetting the EventSystem throws no exceptions when
     * nothing is registered.
     */
    @Test
    public void testResetNonExisting()
    {
        try {
            EventSystem.reset();
        } catch (Exception e) {
            fail(REMOVE_LISTENER_ERROR);
        }
    }


    /**
     * Tests if events that are being sent are processed in the same order.
     */
    @Test
    public void testEventOrder()
    {
        EventSystem.addListener(TestEvent.class, onTestEvent);

        for (TestEvent event : TEST_EVENTS)
            EventSystem.sendEvent(event);

        for (int i = 0; i < TEST_EVENTS.size(); i++)
            assertEquals("Callback functions must be executed in the same order in which the events were dispatched!",
                         TEST_EVENTS.get(i),
                         receivedEvents.get(i));
    }


    /**
     * Tests if the {@linkplain EventSystem} has no initial asynchronous
     * event listeners.
     */
    @Test
    public void testEmptyEventListeners()
    {
        assertFalse("The method hasAsynchronousEventListeners() should return false if addListener() was never called!",
                    EventSystem.hasAsynchronousEventListeners());
    }


    /**
     * Tests if the {@linkplain EventSystem} has no initial synchronous
     * event listeners.
     */
    @Test
    public void testEmptySynchronousEventListeners()
    {
        assertFalse("The method hasSynchronousEventListeners() should return false if addSynchronousListener() was never called!",
                    EventSystem.hasSynchronousEventListeners());
    }


    /**
     * Tests if the hasAsynchronousEventListeners() function
     * returns true if there are registered event listeners.
     */
    @Test
    public void testNonEmptyEventListeners()
    {
        EventSystem.addListener(TestEvent.class, onTestEvent);
        assertTrue("The method hasAsynchronousEventListeners() should return true if addListener() was called!",
                   EventSystem.hasAsynchronousEventListeners());
    }


    /**
     * Tests if the hasSynchronousEventListeners() function
     * returns true if there are registered synchronous event listeners.
     */
    @Test
    public void testNonEmptySynchronousEventListeners()
    {
        EventSystem.addSynchronousListener(TestSynchronousEvent.class, onTestSyncEvent);
        assertTrue("The method hasSynchronousEventListeners() should return true if addSynchronousListener() was called!",
                   EventSystem.hasSynchronousEventListeners());
    }

    /**
     * Tests if the {@linkplain EventSystem} has no initial asynchronous
     * event listeners.
     */
    @Test
    public void testEmptyEventListenersAfterRemoval()
    {
        EventSystem.addListener(TestEvent.class, onTestEvent);
        EventSystem.removeListener(TestEvent.class, onTestEvent);

        assertFalse("The method hasAsynchronousEventListeners() should return false after all asynchronous listeners were removed!",
                    EventSystem.hasAsynchronousEventListeners());
    }


    /**
     * Tests if the {@linkplain EventSystem} has no initial synchronous
     * event listeners.
     */
    @Test
    public void testEmptySynchronousEventListenersAfterRemoval()
    {
        EventSystem.addSynchronousListener(TestSynchronousEvent.class, onTestSyncEvent);
        EventSystem.removeSynchronousListener(TestSynchronousEvent.class);

        assertFalse("The method hasSynchronousEventListeners() should return false after all synchronous listeners were removed!",
                    EventSystem.hasSynchronousEventListeners());
    }


    //////////////////////
    // Non-test Methods //
    //////////////////////

    /**
     * Exemplary event callback function that adds the received event to a list.
     */
    private final Consumer<TestEvent> onTestEvent = (TestEvent event) -> {
        receivedEvents.add(event);
    };


    /**
     * Exemplary event callback function that adds the received event to a list.
     */
    private final Consumer<TestEvent> onTestEvent2 = (TestEvent event) -> {
        receivedEvents.add(event);
    };


    /**
     * Exemplary synchronous event callback function that returns the event payload.
     */
    private final Function<TestSynchronousEvent, Object> onTestSyncEvent = (TestSynchronousEvent event) -> {
        return event.getPayload();
    };


    /**
     * Exemplary synchronous event callback function that returns the event payload.
     */
    private final Function<TestSynchronousEvent, Object> onTestSyncEvent2 = (TestSynchronousEvent event) -> {
        return STATIC_SYNC_PAYLOAD;
    };
}
