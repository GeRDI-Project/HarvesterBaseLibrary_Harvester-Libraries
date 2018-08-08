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
package de.gerdiproject.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.gerdiproject.event.example.TestEvent;
import de.gerdiproject.event.example.TestSynchronousEvent;
import de.gerdiproject.harvest.event.EventSystem;


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
     * are ignored while events are properly received after listeners were added.
     */
    @Test
    public void testAddingListener()
    {
        final TestEvent ignoredEvent = TEST_EVENTS.get(0);
        final TestEvent expectedEvent = TEST_EVENTS.get(1);

        assert !receivedEvents.contains(expectedEvent);

        EventSystem.sendEvent(ignoredEvent);
        assert !receivedEvents.contains(ignoredEvent);

        EventSystem.addListener(TestEvent.class, onTestEvent);

        EventSystem.sendEvent(expectedEvent);
        assert receivedEvents.contains(expectedEvent);
    }


    /**
     * Tests if an event listener is called twice if it was added twice.
     */
    @Test
    public void testAddingListenerTwice()
    {
        assertEquals(0, receivedEvents.size());

        EventSystem.addListener(TestEvent.class, onTestEvent);
        EventSystem.addListener(TestEvent.class, onTestEvent);

        EventSystem.sendEvent(SINGLE_TEST_EVENT);

        assert receivedEvents.contains(SINGLE_TEST_EVENT);
        assertEquals(2, receivedEvents.size());
    }


    /**
     * Tests if events that are sent after their listeners were removed
     * are ignored.
     */
    @Test
    public void testRemovingListener()
    {
        assertEquals(0, receivedEvents.size());

        EventSystem.addListener(TestEvent.class, onTestEvent);
        EventSystem.sendEvent(SINGLE_TEST_EVENT);
        assertEquals(1, receivedEvents.size());

        EventSystem.removeListener(TestEvent.class, onTestEvent);
        EventSystem.sendEvent(SINGLE_TEST_EVENT);
        assertEquals(1, receivedEvents.size());
    }


    /**
     * Tests if removing a non-existing event listener throw no exceptions
     * are ignored.
     */
    @Test
    public void testRemovingListenerNonExisting()
    {
        EventSystem.sendEvent(SINGLE_TEST_EVENT);
        assertEquals(0, receivedEvents.size());

        try {
            EventSystem.removeListener(TestEvent.class, onTestEvent);
        } catch (Exception e) {
            assert false;
        }
    }


    /**
     * Tests all event listeners are properly removed when calling
     * removeAllListeners().
     */
    @Test
    public void testRemovingAllListeners()
    {
        assertEquals(0, receivedEvents.size());

        EventSystem.addListener(TestEvent.class, onTestEvent);
        EventSystem.addListener(TestEvent.class, onTestEvent);
        EventSystem.addListener(TestEvent.class, onTestEvent);

        EventSystem.removeAllListeners(TestEvent.class);

        EventSystem.sendEvent(SINGLE_TEST_EVENT);
        assertEquals(0, receivedEvents.size());
    }


    /**
     * Tests if removing all listeners throws no exceptions
     * when the listeners were not added.
     */
    @Test
    public void testRemovingAllListenersNonExisting()
    {
        EventSystem.sendEvent(SINGLE_TEST_EVENT);
        assertEquals(0, receivedEvents.size());

        try {
            EventSystem.removeAllListeners(TestEvent.class);
        } catch (Exception e) {
            assert false;
        }
    }


    /**
     * Tests if synchronous events that are sent without registered listeners
     * return null while results are properly returned after a listener was added.
     */
    @Test
    public void testAddingSynchronousEvent()
    {
        final Object nullResult = EventSystem.sendSynchronousEvent(SINGLE_SYNC_TEST_EVENT);
        assertNull(nullResult);

        EventSystem.addSynchronousListener(TestSynchronousEvent.class, onTestSyncEvent);

        final Object result = EventSystem.sendSynchronousEvent(SINGLE_SYNC_TEST_EVENT);

        assertNotNull(result);
        assertEquals(SINGLE_SYNC_TEST_EVENT.getPayload(), result);
    }


    /**
     * Tests if a synchronous event listener can be added twice without throwing exceptions.
     */
    @Test
    public void testAddingSynchronousListenerTwice()
    {
        EventSystem.addSynchronousListener(TestSynchronousEvent.class, onTestSyncEvent);

        try {
            EventSystem.addSynchronousListener(TestSynchronousEvent.class, onTestSyncEvent);
        } catch (Exception e) {
            assert false;
        }
    }


    /**
     * Tests if a synchronous event listeners are replaced without throwing exceptions.
     */
    @Test
    public void testAddingSynchronousListenerOverride()
    {
        EventSystem.addSynchronousListener(TestSynchronousEvent.class, onTestSyncEvent);

        final Object result = EventSystem.sendSynchronousEvent(SINGLE_SYNC_TEST_EVENT);

        assertNotNull(result);
        assertEquals(SINGLE_SYNC_TEST_EVENT.getPayload(), result);

        try {
            EventSystem.addSynchronousListener(TestSynchronousEvent.class, onTestSyncEvent2);
        } catch (Exception e) {
            assert false;
        }

        final Object overriddenResult = EventSystem.sendSynchronousEvent(SINGLE_SYNC_TEST_EVENT);

        assertNotNull(overriddenResult);
        assertEquals(STATIC_SYNC_PAYLOAD, overriddenResult);
    }


    /**
     * Tests if synchronous events that are sent after their listener was removed
     * return null.
     */
    @Test
    public void testRemovingSynchronousListener()
    {
        EventSystem.addSynchronousListener(TestSynchronousEvent.class, onTestSyncEvent);
        final Object result = EventSystem.sendSynchronousEvent(SINGLE_SYNC_TEST_EVENT);
        assertNotNull(result);

        EventSystem.removeSynchronousListener(TestSynchronousEvent.class);
        final Object nullResult = EventSystem.sendSynchronousEvent(SINGLE_SYNC_TEST_EVENT);
        assertNull(nullResult);
    }


    /**
     * Tests if removing a non-existing event listener throw no exceptions
     * are ignored.
     */
    @Test
    public void testRemovingSynchronousListenerNonExisting()
    {
        final Object nullResult = EventSystem.sendSynchronousEvent(SINGLE_SYNC_TEST_EVENT);
        assertNull(nullResult);

        try {
            EventSystem.removeSynchronousListener(TestSynchronousEvent.class);
        } catch (Exception e) {
            assert false;
        }
    }


    /**
     * Tests if resetting the EventSystem removes all listeners.
     */
    @Test
    public void testReset()
    {
        assertEquals(0, receivedEvents.size());

        EventSystem.addListener(TestEvent.class, onTestEvent);
        EventSystem.addListener(TestEvent.class, onTestEvent);
        EventSystem.addListener(TestEvent.class, onTestEvent2);
        EventSystem.addSynchronousListener(TestSynchronousEvent.class, onTestSyncEvent);

        EventSystem.reset();

        EventSystem.sendEvent(SINGLE_TEST_EVENT);
        assertEquals(0, receivedEvents.size());

        final Object nullResult = EventSystem.sendSynchronousEvent(SINGLE_SYNC_TEST_EVENT);
        assertNull(nullResult);
    }


    /**
     * Tests if resetting the EventSystem throws no exceptions when
     * nothing is registered.
     */
    @Test
    public void testResetNonExisting()
    {
        EventSystem.sendEvent(SINGLE_TEST_EVENT);
        assertEquals(0, receivedEvents.size());

        try {
            EventSystem.reset();
        } catch (Exception e) {
            assert false;
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
            assertEquals(TEST_EVENTS.get(i), receivedEvents.get(i));
    }


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
