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
package de.gerdiproject.harvest.utils.cache.events;

import de.gerdiproject.harvest.event.IEvent;
import de.gerdiproject.harvest.utils.cache.HarvesterCache;
import de.gerdiproject.harvest.utils.cache.HarvesterCacheManager;

/**
 * An event that attempts to register the cache to {@linkplain HarvesterCacheManager}.
 *
 * @author Robin Weiss
 */
public class RegisterHarvesterCacheEvent implements IEvent
{
    private final HarvesterCache cache;


    /**
     * Creates an event with a specified {@linkplain HarvesterCache} as payload.
     *
     * @param cache the cache that is to be registered
     */
    public RegisterHarvesterCacheEvent(HarvesterCache cache)
    {
        super();
        this.cache = cache;
    }


    /**
     * Returns the {@linkplain HarvesterCache} that is to be registered.
     *
     * @return the {@linkplain HarvesterCache} that is to be registered
     */
    public HarvesterCache getCache()
    {
        return cache;
    }
}
