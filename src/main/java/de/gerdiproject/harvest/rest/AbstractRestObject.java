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
package de.gerdiproject.harvest.rest;

import java.util.function.Consumer;

import javax.ws.rs.core.MultivaluedMap;

import de.gerdiproject.harvest.application.events.ContextDestroyedEvent;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.IEventListener;
import de.gerdiproject.harvest.rest.constants.RestConstants;
import de.gerdiproject.harvest.rest.events.GetRestObjectEvent;

/**
 * This abstract class represents a singleton that is to be manipulated by an {@linkplain AbstractRestResource}.
 *
 * @param <T> the type of the object itself, ugly but necessary Java solution
 * @param <P> the return type of the getAsJson() function used to retrieve a JSON representation of this object
 *
 * @author Robin Weiss
 *
 */
public abstract class AbstractRestObject <T extends AbstractRestObject<T, P>, P> implements IEventListener
{
    protected final String moduleName;

    private final Class<? extends GetRestObjectEvent<T>> getterEventClass;
    private final Consumer<ContextDestroyedEvent> onContextDestroyedCallback;

    /**
     * Constructor that requires the moduleName for pretty printing and the class
     * of the {@linkplain GetRestObjectEvent} that is used to retrieve the singleton instance of this object.
     *
     * @param moduleName the name of the service
     * @param getterEventClass the class of a {@linkplain GetRestObjectEvent}
     */
    public AbstractRestObject(final String moduleName, final Class<? extends GetRestObjectEvent<T>> getterEventClass)
    {
        this.moduleName = moduleName;
        this.getterEventClass = getterEventClass;
        this.onContextDestroyedCallback = this::onContextDestroyed;
    }


    @Override
    @SuppressWarnings("unchecked")
    public void addEventListeners()
    {
        EventSystem.addSynchronousListener(getterEventClass, () -> (T) this);
        EventSystem.addListener(ContextDestroyedEvent.class, onContextDestroyedCallback);
    }


    @Override
    public void removeEventListeners()
    {
        EventSystem.removeSynchronousListener(getterEventClass);
        EventSystem.removeListener(ContextDestroyedEvent.class, onContextDestroyedCallback);
    }


    /**
     * Returns a pretty print representation of this object along
     * with a title.
     *
     * @return a title and a pretty print representation of this object
     */
    public final String getAsPlainText()
    {
        return String.format(
                   RestConstants.REST_GET_TEXT,
                   moduleName,
                   getClass().getSimpleName(),
                   getPrettyPlainText());
    }


    /**
     * Returns a pretty print representation of this object.
     *
     * @return a pretty print representation of this object
     */
    protected abstract String getPrettyPlainText();


    /**
     * Returns a JSON representation of this object.
     *
     * @param query optional query parameters from the HTTP request
     *
     * @return a JSON representation of this object
     */
    public abstract P getAsJson(MultivaluedMap<String, String> query);



    //////////////////////////////
    // Event Callback Functions //
    //////////////////////////////

    /**
     * Cleans up all objects that are used by this object and removes event listeners.
     *
     * @param event the event that triggered the callback function
     */
    protected void onContextDestroyed(final ContextDestroyedEvent event)
    {
        removeEventListeners();
    }
}
