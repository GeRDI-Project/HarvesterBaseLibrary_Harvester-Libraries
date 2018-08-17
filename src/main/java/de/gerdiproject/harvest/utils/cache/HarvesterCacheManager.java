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
package de.gerdiproject.harvest.utils.cache;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.IEventListener;
import de.gerdiproject.harvest.utils.cache.events.RegisterHarvesterCacheEvent;

/**
 * This class manages a list of all {@linkplain HarvesterCache}s. It provides
 * utility functions and a means to retrieve all caches.
 *
 * @author Robin Weiss
 */
public class HarvesterCacheManager implements IEventListener
{
    private final List<HarvesterCache> cacheList;


    /**
     * Private constructor for singleton usage.
     */
    public HarvesterCacheManager()
    {
        cacheList = Collections.synchronizedList(new LinkedList<>());
    }


    @Override
    public void addEventListeners()
    {
        EventSystem.addListener(RegisterHarvesterCacheEvent.class, onRegisterHarvesterCache);

    }


    @Override
    public void removeEventListeners()
    {
        EventSystem.removeListener(RegisterHarvesterCacheEvent.class, onRegisterHarvesterCache);
    }


    /**
     * Registers a cache, adding it to the internal list of caches.
     *
     * @param cache the cache to be registered
     */
    public void registerCache(final HarvesterCache cache)
    {
        cacheList.add(cache);
    }


    /**
     * Returns an unmodifiable list of registered caches.
     *
     * @return an unmodifiable list of registered caches
     */
    public List<HarvesterCache> getHarvesterCaches()
    {
        return Collections.unmodifiableList(cacheList);
    }


    /**
     * Returns the number of cached changes.
     *
     * @return the number of cached changes
     */
    public int getNumberOfHarvestedDocuments()
    {
        int harvestedCount = 0;

        for (final HarvesterCache cache : cacheList)
            harvestedCount += cache.getChangesCache().size();

        return harvestedCount;
    }


    //////////////////////////////
    // Event Callback Functions //
    //////////////////////////////

    /**
     * Callback function for registering a {@linkplain HarvesterCache}.
     */
    private Consumer<RegisterHarvesterCacheEvent> onRegisterHarvesterCache =
        (RegisterHarvesterCacheEvent event) -> registerCache(event.getCache());
}
