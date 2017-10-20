/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
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
package de.gerdiproject.harvest.event;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * This singleton class provides a means to dispatch and listen to events.
 *
 * @author Robin Weiss
 */
public class EventSystem
{
    private Map<Class<? extends IEvent>, List<Consumer<? extends IEvent>>> callbackMap;

    private static EventSystem instance = new EventSystem();


    /**
     * Private constructor for a singleton instance.
     */
    private EventSystem()
    {
        callbackMap = new HashMap<>();
    }


    /**
     * Adds a callback function that is to be executed when a specified event is dispatched.
     *
     * @param eventClass the class of the event
     * @param callback the callback function that is executed when the event is dispatched
     */
    public static <T extends IEvent> void addListener(Class<T> eventClass, Consumer<T> callback)
    {
        List<Consumer<? extends IEvent>> eventList = instance.callbackMap.get(eventClass);

        // create list, if it does not exist yet
        if (eventList == null) {
            eventList = new LinkedList<Consumer<? extends IEvent>>();
            instance.callbackMap.put(eventClass, eventList);
        }

        // add callback to list
        eventList.add(callback);
    }


    /**
     * Removes a callback function for a specified event.
     *
     * @param eventClass the class of the event
     * @param callback the callback function that is to be removed from the event
     */
    public static <T extends IEvent> void removeListener(Class<T> eventClass, Consumer<T> callback)
    {
        List<Consumer<? extends IEvent>> eventList = instance.callbackMap.get(eventClass);

        // remove event from list, if the list exists
        if (eventList != null)
            eventList.remove(callback);
    }


    /**
     * Removes all event listeners of a specified event.
     *
     * @param eventClass the class of the event
     */
    public static <T extends IEvent> void removeAllListeners(Class<T> eventClass)
    {
        instance.callbackMap.remove(eventClass);
    }


    /**
     * Dispatches a specified event, calling all functions that were added to it via the addListener() function.
     *
     * @param event the event that is dispatched
     */
    @SuppressWarnings("unchecked") // this warning is suppressed, because the public functions guarantee that the consumer consumes events of the same class as the corresponding key
    public static <T extends IEvent> void sendEvent(T event)
    {
        List<Consumer<? extends IEvent>> eventList = instance.callbackMap.get(event.getClass());

        if (eventList != null)
            eventList.forEach((Consumer<? extends IEvent> c) -> ((Consumer<T>)c).accept(event));
    }
}
