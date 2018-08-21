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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import org.junit.Test;

import com.google.gson.Gson;

import de.gerdiproject.AbstractFileSystemUnitTest;
import de.gerdiproject.harvest.application.events.ContextDestroyedEvent;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.utils.cache.HarvesterCache;
import de.gerdiproject.harvest.utils.cache.constants.CacheConstants;
import de.gerdiproject.harvest.utils.data.DiskIO;
import de.gerdiproject.json.datacite.DataCiteJson;
import de.gerdiproject.utils.examples.harvestercache.MockedHarvester;

/**
 * This class provides test cases for the {@linkplain HarvesterCache}.
 *
 * @author Robin Weiss
 */
public class HarvesterCacheTest extends AbstractFileSystemUnitTest<HarvesterCache>
{
    private static final String HARVESTER_HASH = "abc";
    private static final String HARVESTER_ID = "harvesterId";

    private static final String SOURCE_ID = "source1";
    private static final String DOCUMENT_ID = "381f236a968160abe96cfb223a43bbf2e917c14c";

    private MockedHarvester harvester;
    private String tempFolder;


    @Override
    protected HarvesterCache setUpTestObjects()
    {
        harvester = new MockedHarvester(testFolder);
        tempFolder = harvester.getTemporaryCacheFolder();

        String stableFolder = harvester.getStableCacheFolder();
        HarvesterCache cache = new HarvesterCache(HARVESTER_ID, tempFolder, stableFolder, StandardCharsets.UTF_8);
        cache.init(HARVESTER_HASH, 0, 1);

        return cache;
    }


    /**
     * Tests if the constructor initializes a document changes cache.
     */
    @Test
    public void testGettingChangesCache()
    {
        assertNotNull(testedObject.getChangesCache());
    }


    /**
     * Tests if the constructor initializes a versions cache.
     */
    @Test
    public void testGettingVersionsCache()
    {
        assertNotNull(testedObject.getVersionsCache());
    }


    /**
     * Tests if the init function creates sourceHashes that depend on the harvesting range.
     */
    @Test
    public void testInitSourceHashHarvestingRange()
    {
        final File sourceHashFile = new File(
            String.format(CacheConstants.SOURCE_HASH_FILE_PATH,
                          tempFolder + CacheConstants.VERSIONS_FOLDER_NAME));

        final DiskIO diskReader = new DiskIO(new Gson(), StandardCharsets.UTF_8);

        final String sourceHash01 = diskReader.getString(sourceHashFile);
        testedObject.init(HARVESTER_HASH, 0, 2);
        final String sourceHash02 = diskReader.getString(sourceHashFile);

        assertNotEquals(sourceHash01, sourceHash02);
    }


    /**
     * Tests if added event listeners are working as expected.
     * In this case, tests if temporary files are cleaned up when the server
     * undeploys the harvester service.
     */
    @Test
    public void testContextDestroyedEvent()
    {
        testedObject.addEventListeners();

        // send the ContextDestroyedEvent
        EventSystem.sendEvent(new ContextDestroyedEvent());

        // make sure temporary files are cleaned up
        final File cacheFolder = new File(tempFolder);
        assert !cacheFolder.exists();
    }


    /**
     * Tests if caching a document causes a temporary version cache file to be created.
     */
    @Test
    public void testCachingNewDocumentVersionFile()
    {
        testedObject.cacheDocument(new DataCiteJson(SOURCE_ID), true);

        File versionFile = new File(String.format(
                                        CacheConstants.DOCUMENT_HASH_FILE_PATH,
                                        tempFolder + CacheConstants.VERSIONS_FOLDER_NAME,
                                        DOCUMENT_ID.substring(0, 2),
                                        DOCUMENT_ID.substring(2)));

        assert versionFile.exists();
    }


    /**
     * Tests if caching a document causes a temporary changes cache file to be created.
     */
    @Test
    public void testCachingNewDocumentChangesFile()
    {
        testedObject.cacheDocument(new DataCiteJson(SOURCE_ID), true);

        File changesFile = new File(String.format(
                                        CacheConstants.DOCUMENT_HASH_FILE_PATH,
                                        tempFolder + CacheConstants.CHANGES_FOLDER_NAME,
                                        DOCUMENT_ID.substring(0, 2),
                                        DOCUMENT_ID.substring(2)));

        assert changesFile.exists();
    }


    /**
     * Tests if caching a document causes and applying, causes
     * the changes cache file to be in the stable directory.
     */
    @Test
    public void testApplyingChangesFile()
    {
        testedObject.cacheDocument(new DataCiteJson(SOURCE_ID), true);
        testedObject.applyChanges(true, false);

        assertNotNull(testedObject.getChangesCache().getFileContent(DOCUMENT_ID));
    }


    /**
     * Tests if caching a document causes and applying, causes
     * the version cache file to be in the stable directory.
     */
    @Test
    public void testApplyingVersionFile()
    {
        testedObject.cacheDocument(new DataCiteJson(SOURCE_ID), true);
        testedObject.applyChanges(true, false);

        assertNotNull(testedObject.getVersionsCache().getFileContent(DOCUMENT_ID));
    }


    /**
     * Tests if caching a document that was already cached, writes the new
     * version to the temporary changes cache.
     */
    @Test
    public void testApplyingChangedDocument()
    {
        final boolean forcedHarvest = false;

        final DataCiteJson document = new DataCiteJson(SOURCE_ID);
        document.setPublicationYear((short)42);

        final DataCiteJson documentNew = new DataCiteJson(SOURCE_ID);
        documentNew.setPublicationYear((short)1337);

        assert addDocumentAfterApply(document, documentNew, forcedHarvest);
    }


    /**
     * Tests if caching a document that was already cached, skips the document,
     * causing it to be removed from the temporary changes cache.
     */
    @Test
    public void testApplyingUnchangedDocument()
    {
        final boolean forcedHarvest = false;
        final DataCiteJson document = new DataCiteJson(SOURCE_ID);
        assertEquals(forcedHarvest, addDocumentAfterApply(document, document, forcedHarvest));
    }


    /**
     * Tests if caching a document that was already cached, adds the document
     * to the temporary changes cache when the forcedHarvest flag is enabled.
     */
    @Test
    public void testApplyingUnchangedDocumentForced()
    {
        final boolean forcedHarvest = true;
        final DataCiteJson document = new DataCiteJson(SOURCE_ID);
        assertEquals(forcedHarvest, addDocumentAfterApply(document, document, forcedHarvest));
    }


    /**
     * Tests if stable document version files are removed when they were not applied
     * a second time.
     */
    @Test
    public void testApplyingDeletedDocument()
    {
        final boolean wasHarvestSuccessful = true;
        final boolean wasHarvestAborted = false;

        assert !doesVersionFileExistAfterApplying(
            wasHarvestSuccessful,
            wasHarvestAborted);
    }


    /**
     * Tests if stable document version files remain even when they were not applied
     * a second time, while the harvest that retrieved the documents was aborted.
     */
    @Test
    public void testApplyingDeletedDocumentWithAbortedHarvest()
    {
        final boolean wasHarvestSuccessful = false;
        final boolean wasHarvestAborted = true;

        assert doesVersionFileExistAfterApplying(
            wasHarvestSuccessful,
            wasHarvestAborted);
    }


    /**
     * Tests if stable document version files remain even when they were not applied
     * a second time, while the harvest that retrieved the documents has failed.
     */
    @Test
    public void testApplyingDeletedDocumentWithFailedHarvest()
    {
        final boolean wasHarvestSuccessful = false;
        final boolean wasHarvestAborted = false;

        assert doesVersionFileExistAfterApplying(
            wasHarvestSuccessful,
            wasHarvestAborted);
    }


    /**
     * Tests if stable document version files remain even when they were not applied
     * a second time, while the harvest that retrieved the documents was aborted and has failed.
     * (actually this is impossible during normal execution)
     */
    @Test
    public void testApplyingDeletedDocumentWithAbortedFailedHarvest()
    {
        final boolean wasHarvestSuccessful = true;
        final boolean wasHarvestAborted = true;

        assert doesVersionFileExistAfterApplying(
            wasHarvestSuccessful,
            wasHarvestAborted);
    }


    /**
     * Tests if calling the skipAllDocuments() function causes all temporary changes cache files to be
     * deleted.
     */
    @Test
    public void testSkippingAllDocuments()
    {
        // add random number of documents
        final int numberOfSkippedDocuments = 2 + new Random().nextInt(8);

        for (int i = 0; i < numberOfSkippedDocuments; i++)
            testedObject.cacheDocument(new DataCiteJson(SOURCE_ID + i), true);

        testedObject.applyChanges(true, false);
        testedObject.init(HARVESTER_HASH + 1, 0, 1);

        testedObject.skipAllDocuments();
        File tempChangesFolder = new File(tempFolder, CacheConstants.CHANGES_FOLDER_NAME);

        assertEquals(0, tempChangesFolder.listFiles().length);
    }


    //////////////////////
    // Non-test Methods //
    //////////////////////

    /**
     * Adds a document to the cache and applies the changes. Afterwards,
     * a second document is added and the cache is applied again.
     * If the size of the changes cache increases, the second document was added successfully.
     *
     * @param isForced represents the forcedHarvest flag
     * @param firstDoc the first document to be added
     * @param secondDoc the second document to be added
     *
     * @return true if the second document was added to the stable changes cache
     */
    private boolean addDocumentAfterApply(DataCiteJson firstDoc, DataCiteJson secondDoc, boolean isForced)
    {
        final int oldSize = testedObject.getChangesCache().size();
        testedObject.cacheDocument(firstDoc, isForced);
        testedObject.applyChanges(true, false);

        testedObject.init(HARVESTER_HASH + 1, 0, 1);
        testedObject.cacheDocument(secondDoc, isForced);
        testedObject.applyChanges(true, false);

        return testedObject.getChangesCache().size() > oldSize;
    }


    /**
     * Adds and applies one document. Afterwards, the cache is re-initialized and applied without
     * adding documents. Depending on the flags of the second applyChanges call, the
     * @param isSuccessful if true, the harvest applying the changes is considered successful
     * @param isAborting if true, the harvest applying the changes is considered aborted
     *
     * @return true if the stable version file of the document exists
     */
    private boolean doesVersionFileExistAfterApplying(boolean isSuccessful, boolean isAborting)
    {
        // add one document to the stable cache
        testedObject.cacheDocument(new DataCiteJson(SOURCE_ID), true);
        testedObject.applyChanges(true, false);

        // apply with zero documents, causing the old document to be removed
        testedObject.init(HARVESTER_HASH + 2, 0, 1);
        testedObject.applyChanges(isSuccessful, isAborting);

        return testedObject.getVersionsCache().getFileContent(DOCUMENT_ID) != null;
    }
}
