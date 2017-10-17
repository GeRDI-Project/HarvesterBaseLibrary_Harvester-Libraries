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

public class EventSystem
{
	private Map<Class<? extends IEvent>, List<Consumer<? extends IEvent>>> callbackMap;
	
	
	private static EventSystem instance = new EventSystem();
	
	public static EventSystem instance()
	{
		return instance;
	}
	
	
	private EventSystem()
	{
		callbackMap = new HashMap<>();
	}

	
	public <T extends IEvent> void addListener( Class<T> eventClass, Consumer<T> callback)
	{
		List<Consumer<? extends IEvent>> eventList = callbackMap.get( eventClass );
		
		// create list, if it does not exist yet
		if( eventList == null)
		{
			eventList = new LinkedList<Consumer<? extends IEvent>>();
			callbackMap.put( eventClass, eventList );
		}
		
		// add callback to list
		eventList.add( callback );
	}
	
	
	public <T extends IEvent> void removeListener( Class<T> eventClass, Consumer<T> callback)
	{
		List<Consumer<? extends IEvent>> eventList = callbackMap.get( eventClass );
		
		// remove event from list, if the list exists
		if( eventList != null)
		{
			eventList.remove( callback );
		}
	}
	
	
	public <T extends IEvent> void removeAllListeners( Class<T> eventClass)
	{
		callbackMap.remove( eventClass );
	}
	
	
	/*public void sendEvent2( IEvent event)
	{
		List<Consumer<? extends IEvent>> eventList = callbackMap.get( event.getClass() );
		if( eventList != null)
		{
			eventList.forEach( ( Consumer<? extends IEvent> c) -> c.accept( event ));
		}
	}*/
	
	@SuppressWarnings ("unchecked")
	public <T extends IEvent> void sendEvent( T event)
	{
		List<Consumer<? extends IEvent>> eventList = callbackMap.get( event.getClass() );
		if( eventList != null)
		{
			eventList.forEach( ( Consumer<? extends IEvent> c) -> ((Consumer<T>)c).accept( event ));
		}
	}
}
