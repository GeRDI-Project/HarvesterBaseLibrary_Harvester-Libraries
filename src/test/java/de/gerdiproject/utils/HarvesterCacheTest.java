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

import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.gerdiproject.harvest.application.events.ContextDestroyedEvent;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.utils.FileUtils;
import de.gerdiproject.harvest.utils.cache.DocumentChangesCache;
import de.gerdiproject.harvest.utils.cache.DocumentVersionsCache;
import de.gerdiproject.harvest.utils.cache.HarvesterCache;
import de.gerdiproject.harvest.utils.cache.constants.CacheConstants;
import de.gerdiproject.json.datacite.DataCiteJson;
import de.gerdiproject.utils.examples.harvestercache.MockedHarvester;

/**
 * This class provides test cases for the {@linkplain HarvesterCache}.
 *
 * @author Robin Weiss
 */
public class HarvesterCacheTest
{
    private static final String CACHE_FOLDER = "mocked/harvesterCacheTestDir/";
    private static final String HARVESTER_HASH = "abc";

    private static final String DOCUMENT_1_ID = "source1";
    private static final String DOCUMENT_1_HASH = "463c6c7494bc6a5f6eb5a731f462fcfefd4288fb";
    //private static final String DOCUMENT_2_ID = "source2";
    //private static final String DOCUMENT_2_HASH = "23b65d44499bb2273f5e3633d7361ddbeefb6f11";

    @Before
    public void before()
    {
        FileUtils.deleteFile(new File(CACHE_FOLDER));
    }


    @After
    public void after()
    {
        FileUtils.deleteFile(new File(CACHE_FOLDER));
    }


    /**
     * Tests if the constructor initializes a {@linkplain DocumentVersionsCache}.
     */
    @Test
    public void testInitialVersionCache()
    {
        final MockedHarvester harvester = new MockedHarvester(CACHE_FOLDER);
        final HarvesterCache cache = new HarvesterCache(harvester);

        assertNotNull(cache.getVersionsCache());
    }

    /**
     * Tests if the constructor initializes a {@linkplain DocumentChangesCache}.
     */
    @Test
    public void testInitialChangesCache()
    {
        final MockedHarvester harvester = new MockedHarvester(CACHE_FOLDER);
        final HarvesterCache cache = new HarvesterCache(harvester);

        assertNotNull(cache.getChangesCache());
    }


    /**
     * Tests if the init function successfully creates a source hash file in
     * the temporary documents folder.
     */
    @Test
    public void testInit()
    {
        final MockedHarvester harvester = new MockedHarvester(CACHE_FOLDER);
        final File sourceHash = new File(
            String.format(CacheConstants.SOURCE_HASH_FILE_PATH,
                          harvester.getTemporaryCacheFolder() + CacheConstants.VERSIONS_FOLDER_NAME));

        final HarvesterCache cache = new HarvesterCache(harvester);
        cache.init(HARVESTER_HASH, 0, 1);

        assert sourceHash.exists();
    }


    /**
     * Tests if added event listeners are working as expected.
     * In this case, tests if temporary files are cleaned up when the server
     * undeploys the harvester service.
     */
    @Test
    public void testAddingEventListeners()
    {
        final MockedHarvester harvester = new MockedHarvester(CACHE_FOLDER);
        final HarvesterCache cache = new HarvesterCache(harvester);

        // create a temporary file via init
        cache.init(HARVESTER_HASH, 0, 1);

        // listen to the ContextDestroyedEvent
        cache.addEventListeners();

        // send the ContextDestroyedEvent
        EventSystem.sendEvent(new ContextDestroyedEvent());

        // make sure temporary files are cleaned up
        final File cacheFolder = new File(harvester.getTemporaryCacheFolder());
        assert !cacheFolder.exists();
    }


    /**
     * Tests if removing event listeners is working as expected.
     * In this case, tests if temporary files are no longer cleaned up when the server
     * undeploys the harvester service.
     */
    @Test
    public void testRemovingEventListeners()
    {
        final MockedHarvester harvester = new MockedHarvester(CACHE_FOLDER);
        final HarvesterCache cache = new HarvesterCache(harvester);

        // create a temporary file via init
        cache.init(HARVESTER_HASH, 0, 1);

        // listen to the ContextDestroyedEvent
        cache.addEventListeners();
        cache.removeEventListeners();

        // send the ContextDestroyedEvent
        EventSystem.sendEvent(new ContextDestroyedEvent());

        // make sure temporary files are cleaned up
        final File cacheFolder = new File(harvester.getTemporaryCacheFolder());
        assert cacheFolder.exists();
    }


    /**
     * Tests if
     */
    @Test
    public void testCachingDocumentVersion()
    {
        final MockedHarvester harvester = new MockedHarvester(CACHE_FOLDER);
        final HarvesterCache cache = new HarvesterCache(harvester);
        cache.init(HARVESTER_HASH, 0, 1);

        cache.cacheDocument(new DataCiteJson(DOCUMENT_1_ID), true);

        final File cachedVersion = new File(String.format(
                                                CacheConstants.DOCUMENT_HASH_FILE_PATH,
                                                harvester.getTemporaryCacheFolder() + CacheConstants.VERSIONS_FOLDER_NAME,
                                                DOCUMENT_1_HASH.substring(0, 2),
                                                DOCUMENT_1_HASH.substring(2)));
        assert cachedVersion.exists();
    }


    /**
     * Tests if
     */
    @Test
    public void testCachingDocumentChange()
    {
        final MockedHarvester harvester = new MockedHarvester(CACHE_FOLDER);
        final HarvesterCache cache = new HarvesterCache(harvester);
        cache.init(HARVESTER_HASH, 0, 1);

        cache.cacheDocument(new DataCiteJson(DOCUMENT_1_ID), true);

        final File cachedChange = new File(String.format(
                                               CacheConstants.DOCUMENT_HASH_FILE_PATH,
                                               harvester.getTemporaryCacheFolder() + CacheConstants.CHANGES_FOLDER_NAME,
                                               DOCUMENT_1_HASH.substring(0, 2),
                                               DOCUMENT_1_HASH.substring(2)));
        assert cachedChange.exists();
    }


    /**
     * Tests if
     */
    @Test
    public void testApplyingChanges()
    {

    }




    /**
     * Tests if
     */
    @Test
    public void testSkippingAllDocuments()
    {

    }
}
