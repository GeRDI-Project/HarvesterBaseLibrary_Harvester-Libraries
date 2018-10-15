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
package de.gerdiproject.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

import de.gerdiproject.AbstractFileSystemUnitTest;
import de.gerdiproject.harvest.etls.loaders.DiskLoader;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.utils.cache.HarvesterCache;
import de.gerdiproject.harvest.utils.cache.HarvesterCacheManager;
import de.gerdiproject.harvest.utils.cache.events.RegisterHarvesterCacheEvent;
import de.gerdiproject.json.datacite.DataCiteJson;
import de.gerdiproject.utils.examples.harvestercache.MockedETL;

/**
 * This class contains unit tests for the {@linkplain DiskLoader}.
 *
 * @author Robin Weiss
 */
public class HarvesterCacheManagerTest extends AbstractFileSystemUnitTest<HarvesterCacheManager>
{
    private static final String SOURCE_ID = "source";
    private static final String HARVESTER_ID = "harvesterId";

    @Override
    protected HarvesterCacheManager setUpTestObjects()
    {
        return new HarvesterCacheManager();
    }


    /**
     * Tests if getHarvesterCaches() returns an empty list after construction.
     */
    @Test
    public void testGetHarvesterCachesEmpty()
    {
        assertTrue("The method getHarvesterCaches() should return an empty list if registerCache() was not called before!",
                   testedObject.getHarvesterCaches().isEmpty());
    }


    /**
     * Tests if getHarvesterCaches() returns the same number of caches that
     * were registered via registerCache().
     */
    @Test
    public void testGetHarvesterCaches()
    {
        int numberOfRegisteredCaches = registerRandomNumberOfCaches();

        assertEquals("The method getHarvesterCaches() should return the correct number of registered caches!",
                     numberOfRegisteredCaches,
                     testedObject.getHarvesterCaches().size());
    }


    /**
     * Tests if a cache that is registered via a {@linkplain RegisterHarvesterCacheEvent}
     * can be retrieved via getHarvesterCaches().
     */
    @Test
    public void testRegisteringCache()
    {
        testedObject.addEventListeners();
        final HarvesterCache registeredCache = registerCache();

        assertEquals("The method registerCache() should cause the cache to be in the list that is retrieved via getHarvesterCaches()!",
                     registeredCache,
                     testedObject.getHarvesterCaches().get(0));
    }


    /**
     * Tests if the getNumberOfHarvestedDocuments() method correctly returns the total
     * number of all cached changes.
     */
    @Test
    public void testGettingNumberOfHarvestedDocuments()
    {
        registerRandomNumberOfCaches();

        int numberOfHarvestedDocs = 0;

        for (HarvesterCache cache : testedObject.getHarvesterCaches()) {
            final int addedDocs = 1 + random.nextInt(10);

            for (int i = 0; i < addedDocs; i++)
                cache.cacheDocument(new DataCiteJson(SOURCE_ID + (numberOfHarvestedDocs + i)), true);

            numberOfHarvestedDocs += addedDocs;
        }

        assertEquals("The method getNumberOfHarvestedDocuments() should return the correct number of cached document changes!",
                     numberOfHarvestedDocs,
                     testedObject.getNumberOfHarvestedDocuments());
    }


    //////////////////////
    // Non-test Methods //
    //////////////////////

    /**
     * Registers 1 to 10 {@linkplain HarvesterCache} in the tested {@linkplain HarvesterCacheManager}.
     *
     * @return the number of registered caches
     */
    private int registerRandomNumberOfCaches()
    {
        testedObject.addEventListeners();

        final int numberOfAddedCaches = 1 + random.nextInt(10);

        for (int i = 0; i < numberOfAddedCaches; i++)
            registerCache();

        return numberOfAddedCaches;
    }

    /**
     * Attempts to register a single {@linkplain HarvesterCache}.
     *
     * @return the registered {@linkplain HarvesterCache}
     */
    private HarvesterCache registerCache()
    {
        final MockedETL mockedHarvester = new MockedETL(testFolder);
        final HarvesterCache registeredCache = new HarvesterCache(
            HARVESTER_ID,
            mockedHarvester.getTemporaryCacheFolder(),
            mockedHarvester.getStableCacheFolder(),
            StandardCharsets.UTF_8);
        final RegisterHarvesterCacheEvent registerEvent = new RegisterHarvesterCacheEvent(registeredCache);

        EventSystem.sendEvent(registerEvent);

        return registeredCache;
    }
}
