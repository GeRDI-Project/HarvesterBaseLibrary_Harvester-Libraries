/**
 * Copyright © 2017 Robin Weiss (http://www.gerdi-project.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.gerdiproject.harvest.event;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * This singleton class provides a means to dispatch and listen to
 * {@linkplain IEvent}s.
 *
 * @author Robin Weiss
 */
public class EventSystem
{
    private final Map<Class<? extends IEvent>, List<Consumer<? extends IEvent>>> callbackMap;
    private final Map<Class<? extends ISynchronousEvent<?>>, Function<? extends ISynchronousEvent<?>, ?>> synchronousCallbackMap;
    private final Queue<IEvent> asyncEventQueue;
    private final AtomicBoolean isProcessingEvents;

    private final static EventSystem instance = new EventSystem();


    /**
     * Private constructor for a singleton instance.
     */
    private EventSystem()
    {
        callbackMap = new HashMap<>();
        synchronousCallbackMap = new HashMap<>();
        asyncEventQueue = new ConcurrentLinkedQueue<>();
        isProcessingEvents = new AtomicBoolean(false);
    }


    /**
     * Adds a callback function that is to be executed when a specified event is
     * dispatched.
     *
     * @param eventClass the class of the event
     * @param callback the callback function that is executed when the event is
     *            dispatched
     * @param <T> the type of the event
     */
    public static <T extends IEvent> void addListener(Class<T> eventClass, Consumer<T> callback)
    {
        synchronized (instance.callbackMap) {
            List<Consumer<? extends IEvent>> eventList = instance.callbackMap.get(eventClass);

            // create list, if it does not exist yet
            if (eventList == null) {
                eventList = new LinkedList<Consumer<? extends IEvent>>();
                instance.callbackMap.put(eventClass, eventList);
            }

            // add callback to list
            eventList.add(callback);
        }
    }


    /**
     * Removes a callback function for a specified event.
     *
     * @param eventClass the class of the event
     * @param callback the callback function that is to be removed from the
     *            event
     * @param <T> the type of the event
     */
    public static <T extends IEvent> void removeListener(Class<T> eventClass, Consumer<T> callback)
    {
        synchronized (instance.callbackMap) {
            List<Consumer<? extends IEvent>> eventList = instance.callbackMap.get(eventClass);

            // remove event from list, if the list exists
            if (eventList != null) {
                eventList.remove(callback);

                if (eventList.isEmpty())
                    instance.callbackMap.remove(eventClass);
            }
        }
    }


    /**
     * Removes all event listeners of a specified event.
     *
     * @param eventClass the class of the event
     * @param <T> the type of the event
     */
    public static <T extends IEvent> void removeAllListeners(Class<T> eventClass)
    {
        synchronized (instance.callbackMap) {
            final List<Consumer<? extends IEvent>> eventList = instance.callbackMap.remove(eventClass);

            // clear old list
            if (eventList != null) {
                synchronized (eventList) {
                    eventList.clear();
                }
            }
        }
    }


    /**
     * Removes all listeners and clears the queue.
     */
    public static void reset()
    {
        instance.asyncEventQueue.clear();
        instance.synchronousCallbackMap.clear();

        // remove all async events
        synchronized (instance.callbackMap) {
            Collection<List<Consumer<? extends IEvent>>> listenerLists = instance.callbackMap.values();

            for (List<Consumer<? extends IEvent>> listeners : listenerLists)
                listeners.clear();

            instance.callbackMap.clear();
        }
    }


    /**
     * Dispatches a specified asynchronous event by adding it to a queue. If the
     * event is the only one in the queue, its callback functions will be called
     * immediately. Otherwise, all other queued events will be processed first.
     *
     * @param event the event that is dispatched
     * @param <T> the type of the dispatched event
     */
    public static <T extends IEvent> void sendEvent(T event)
    {
        instance.asyncEventQueue.add(event);
        instance.processAsynchronousEventQueue();
    }


    /**
     * If no other dequeueing process is in progress, this method empties the
     * asynchronous event queue in order, executing all corresponding callbacks.
     */
    private void processAsynchronousEventQueue()
    {
        if (!isProcessingEvents.get()) {
            isProcessingEvents.set(true);

            while (!asyncEventQueue.isEmpty())
                executeAsynchronousCallbacks(asyncEventQueue.poll());

            isProcessingEvents.set(false);
        }
    }


    /**
     * Executes all functions that were to added to the specified event via the
     * addListener() function.
     *
     * @param event the event that was dispatched
     * @param <T> the type of the dispatched event
     */
    @SuppressWarnings({
        "unchecked"
    }) // this warning is suppressed, because the public functions guarantee that the consumer consumes events of the same class as the corresponding key
    private <T extends IEvent> void executeAsynchronousCallbacks(T event)
    {
        final List<Consumer<? extends IEvent>> eventList;

        // get callback list for event
        synchronized (callbackMap) {
            eventList = callbackMap.get(event.getClass());
        }

        if (eventList != null) {
            synchronized (eventList) {
                int i = eventList.size();

                // traverse list from back to front, in case a listener gets removed by a callback function
                while (i != 0)
                    ((Consumer<T>) eventList.get(--i)).accept(event);
            }
        }
    }


    /**
     * Adds a callback function that executes and returns a value when a
     * specified synchronous event is dispatched. Due to the synchronous nature,
     * only one callback function may be registered per event class.
     *
     * @param eventClass the class of the synchronous event
     * @param callback a callback function that requires the event as a parameter
     *         and returns a specified value when the event is dispatched
     * @param <T> the type of the synchronous event
     * @param <R> the type of the return value of the callback function
     */
    public static <R, T extends ISynchronousEvent<R>> void addSynchronousListener(Class<T> eventClass, Function<T, R> callback)
    {
        synchronized (instance.synchronousCallbackMap) {
            instance.synchronousCallbackMap.put(eventClass, callback);
        }
    }

    /**
     * Adds a callback function that executes and returns a value when a
     * specified synchronous event is dispatched. Due to the synchronous nature,
     * only one callback function may be registered per event class.
     *
     * @param eventClass the class of the synchronous event
     * @param callback a callback function that expects no parameters and returns when
     *            the event is dispatched
     * @param <T> the type of the synchronous event
     * @param <R> the type of the return value of the callback function
     */
    public static <R, T extends ISynchronousEvent<R>> void addSynchronousListener(Class<T> eventClass, Supplier<R> callback)
    {
        synchronized (instance.synchronousCallbackMap) {
            instance.synchronousCallbackMap.put(eventClass, (T event) -> callback.get());
        }
    }


    /**
     * Removes the callback function of a synchronous event.
     *
     * @param eventClass the class of the synchronous event
     * @param <T> the type of the synchronous event
     */
    public static <T extends ISynchronousEvent<?>> void removeSynchronousListener(Class<T> eventClass)
    {
        synchronized (instance.synchronousCallbackMap) {
            instance.synchronousCallbackMap.remove(eventClass);
        }
    }


    /**
     * Dispatches a synchronous event that executes a unique callback function
     * and returns its calculated value.
     *
     * @param event a synchronous event
     * @param <T> the type of the synchronous event
     * @param <R> the type of the return value of the callback function
     *
     * @return the return value of the callback function that is registered
     */
    @SuppressWarnings("unchecked")
    public static <R, T extends ISynchronousEvent<R>> R sendSynchronousEvent(T event)
    {
        synchronized (instance.synchronousCallbackMap) {
            Function<? extends ISynchronousEvent<?>, ?> callback =
                instance.synchronousCallbackMap.get(event.getClass());

            if (callback != null)
                return ((Function<T, R>) callback).apply(event);
            else
                return null;
        }
    }


    /**
     * Checks if there are registered event listeners.
     *
     * @return true if there is at least one registered {@linkplain IEvent}
     */
    public static boolean hasAsynchronousEventListeners()
    {
        return !instance.callbackMap.isEmpty();
    }


    /**
     * Checks if there are registered synchronous event listeners.
     *
     * @return true if there is at least one registered {@linkplain ISynchronousEvent}
     */
    public static boolean hasSynchronousEventListeners()
    {
        return !instance.synchronousCallbackMap.isEmpty();
    }
}
