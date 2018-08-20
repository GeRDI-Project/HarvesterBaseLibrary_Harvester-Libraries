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
package de.gerdiproject.save;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import de.gerdiproject.AbstractFileSystemUnitTest;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.harvester.events.HarvestStartedEvent;
import de.gerdiproject.harvest.save.HarvestSaver;
import de.gerdiproject.harvest.save.constants.SaveConstants;
import de.gerdiproject.harvest.save.events.SaveFinishedEvent;
import de.gerdiproject.harvest.save.events.SaveStartedEvent;
import de.gerdiproject.harvest.save.events.StartSaveEvent;
import de.gerdiproject.harvest.state.events.AbortingFinishedEvent;
import de.gerdiproject.harvest.state.events.AbortingStartedEvent;
import de.gerdiproject.harvest.state.events.StartAbortingEvent;
import de.gerdiproject.harvest.utils.cache.HarvesterCache;
import de.gerdiproject.harvest.utils.cache.HarvesterCacheManager;
import de.gerdiproject.harvest.utils.cache.events.RegisterHarvesterCacheEvent;
import de.gerdiproject.harvest.utils.data.DiskIO;
import de.gerdiproject.harvest.utils.time.ProcessTimeMeasure;
import de.gerdiproject.harvest.utils.time.ProcessTimeMeasure.ProcessStatus;
import de.gerdiproject.json.datacite.DataCiteJson;
import de.gerdiproject.utils.examples.harvestercache.MockedHarvester;

/**
 * This class contains unit tests for the {@linkplain HarvestSaver}.
 *
 * @author Robin Weiss
 */
public class HarvestSaverTest extends AbstractFileSystemUnitTest<HarvestSaver>
{
    private static final String TEST_NAME = "saveTest";
    private static final String SOURCE_ID = "source";
    private static final String HARVESTER_HASH = "ABC";
    private static final String JSON_PUBLICATION_YEAR = "publicationYear";

    private HarvesterCacheManager cacheManager = new HarvesterCacheManager();
    private ProcessTimeMeasure measure;


    @Override
    protected HarvestSaver setUpTestObjects()
    {
        final long startTimestamp = random.nextLong();
        final long endTimestamp = random.longs(startTimestamp + 1, startTimestamp + 99999999).findAny().getAsLong();

        this.measure = new ProcessTimeMeasure();
        this.measure.set(startTimestamp, endTimestamp, ProcessStatus.Finished);

        this.cacheManager = new HarvesterCacheManager();
        return new HarvestSaver(testFolder, TEST_NAME, StandardCharsets.UTF_8, measure, cacheManager);
    }


    @Override
    public void before() throws InstantiationException
    {
        super.before();
        setLoggerEnabled(false);
    }


    @Override
    public void after()
    {
        super.after();
        setLoggerEnabled(true);

        cacheManager = null;
        measure = null;
    }


    /**
     * Tests if the file name starts with the specified name and the start timestamp
     * of the specified {@linkplain ProcessTimeMeasure}.
     */
    @Test
    public void testFileName()
    {
        final String fileName = testedObject.getTargetFile().getName();
        final String expectedFileName = String.format(
                                            SaveConstants.SAVE_FILE_NAME,
                                            TEST_NAME,
                                            measure.getStartTimestamp());

        assertEquals(expectedFileName, fileName);
    }


    /**
     * Tests if the file name changes after a harvest starts, while the file name
     * prefix remains the same.
     */
    @Test
    public void testFileNameChangeAfterHarvest()
    {
        final String fileNameBeforeHarvest = testedObject.getTargetFile().getName();

        testedObject.addEventListeners();
        EventSystem.sendEvent(new HarvestStartedEvent(0, 1, null));

        final String fileNameAfterHarvest = testedObject.getTargetFile().getName();

        assertNotEquals(fileNameBeforeHarvest, fileNameAfterHarvest);
    }


    /**
     * Tests if the auto-save flag is correctly reflected in the {@linkplain SaveStartedEvent}
     * that is dispatched when the saving process starts.
     */
    @Test
    public void testSaveStartedEventAutoSaveDisabled()
    {
        testedObject.addEventListeners();

        SaveStartedEvent saveStartedEvent = waitForEvent(
                                                SaveStartedEvent.class,
                                                DEFAULT_EVENT_TIMEOUT,
                                                () -> EventSystem.sendEvent(new StartSaveEvent(false))
                                            );

        assert !saveStartedEvent.isAutoTriggered();
    }


    /**
     * Tests if the auto-save flag is correctly reflected in the {@linkplain SaveStartedEvent}
     * that is dispatched when the saving process starts.
     */
    @Test
    public void testSaveStartedEventAutoSaveEnabled()
    {
        testedObject.addEventListeners();

        SaveStartedEvent saveStartedEvent = waitForEvent(
                                                SaveStartedEvent.class,
                                                DEFAULT_EVENT_TIMEOUT,
                                                () -> EventSystem.sendEvent(new StartSaveEvent(true))
                                            );

        assert saveStartedEvent.isAutoTriggered();
    }


    /**
     * Tests if the getNumberOfDocuments() method of the dispatched {@linkplain SaveStartedEvent}
     * correctly represents the number of documents that are to be saved.
     */
    @Test
    public void testSaveStartedEventDocumentCount()
    {
        final int numberOfHarvestedDocuments = addRandomNumberOfSaveableDocuments();

        // trigger the saving start
        testedObject.addEventListeners();
        SaveStartedEvent saveStartedEvent = waitForEvent(
                                                SaveStartedEvent.class,
                                                DEFAULT_EVENT_TIMEOUT,
                                                () -> EventSystem.sendEvent(new StartSaveEvent(true))
                                            );

        assertEquals(numberOfHarvestedDocuments, saveStartedEvent.getNumberOfDocuments());
    }


    /**
     * Tests if the saving succeeds when there are documents to be saved.
     */
    @Test
    public void testSaveFinishedEventSuccess()
    {
        testedObject.addEventListeners();
        addRandomNumberOfSaveableDocuments();

        SaveFinishedEvent saveFinishedEvent = waitForEvent(
                                                  SaveFinishedEvent.class,
                                                  DEFAULT_EVENT_TIMEOUT,
                                                  () -> EventSystem.sendEvent(new StartSaveEvent(true))
                                              );
        assert saveFinishedEvent.isSuccessful();
    }


    /**
     * Tests if the saving fails when there are no documents to save.
     */
    @Test
    public void testSaveFinishedEventNoDocuments()
    {
        testedObject.addEventListeners();

        SaveFinishedEvent saveFinishedEvent = waitForEvent(
                                                  SaveFinishedEvent.class,
                                                  DEFAULT_EVENT_TIMEOUT,
                                                  () -> EventSystem.sendEvent(new StartSaveEvent(true))
                                              );
        assert !saveFinishedEvent.isSuccessful();
    }


    /**
     * Tests if aborting a running save process causes a {@linkplain StartAbortingEvent}
     * to be sent.
     */
    @Test
    public void testAbortStarted()
    {
        testedObject.addEventListeners();

        // add some documents so that the saving can start
        addRandomNumberOfSaveableDocuments();

        AbortingStartedEvent abortStartedEvent = waitForEvent(
                                                     AbortingStartedEvent.class,
                                                     DEFAULT_EVENT_TIMEOUT,
        () -> {
            EventSystem.sendEvent(new StartSaveEvent(true));
            EventSystem.sendEvent(new StartAbortingEvent());
        }
                                                 );
        assertNotNull(abortStartedEvent);
    }


    /**
     * Tests if after aborting a running save process, an {@linkplain AbortingFinishedEvent}
     * is sent.
     */
    @Test
    public void testAbortFinished()
    {
        testedObject.addEventListeners();

        // add some documents so that the saving can start
        addRandomNumberOfSaveableDocuments();

        AbortingFinishedEvent abortFinishedEvent = waitForEvent(
                                                       AbortingFinishedEvent.class,
                                                       DEFAULT_EVENT_TIMEOUT,
        () -> {
            EventSystem.sendEvent(new StartSaveEvent(true));
            EventSystem.sendEvent(new StartAbortingEvent());
        }
                                                   );
        assertNotNull(abortFinishedEvent);
    }

    /**
     * Tests if after aborting a running save process, no file is being generated.
     */
    @Test
    public void testAbortFileCleanup()
    {
        testedObject.addEventListeners();

        // add some documents so that the saving can start
        addRandomNumberOfSaveableDocuments();

        waitForEvent(
            AbortingFinishedEvent.class,
            DEFAULT_EVENT_TIMEOUT,
        () -> {
            EventSystem.sendEvent(new StartSaveEvent(true));
            EventSystem.sendEvent(new StartAbortingEvent());
        }
        );

        assert !testedObject.getTargetFile().exists();
    }


    /**
     * Tests if all saved documents can be retrieved from the resulting file.
     */
    @Test
    public void testFileContentDocuments()
    {
        testedObject.addEventListeners();

        final int numberOfSavedDocs =
            addRandomNumberOfSaveableDocuments();

        // wait for the save to be finished
        waitForEvent(
            SaveFinishedEvent.class,
            DEFAULT_EVENT_TIMEOUT,
            () -> EventSystem.sendEvent(new StartSaveEvent(true))
        );

        final DiskIO diskReader = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        final JsonObject fileContent = diskReader.getObject(testedObject.getTargetFile(), JsonObject.class);
        final JsonArray loadedDocuments = fileContent.get(SaveConstants.DOCUMENTS_JSON).getAsJsonArray();

        // publication year is the index of the document, but JsonArrays are unsorted
        for (int i = 0; i < loadedDocuments.size(); i++)
            assert loadedDocuments.get(i).getAsJsonObject().get(JSON_PUBLICATION_YEAR).getAsInt() < numberOfSavedDocs;
    }


    /**
     * Tests if the start index of the harvesting range is correctly set and saved when a
     * {@linkplain HarvestStartedEvent} changes it.
     */
    @Test
    public void testFileContentHarvestFrom()
    {
        testedObject.addEventListeners();

        addRandomNumberOfSaveableDocuments();

        final int harvestFrom = random.nextInt(1000);
        EventSystem.sendEvent(new HarvestStartedEvent(harvestFrom, -1, HARVESTER_HASH));

        // wait for the save to be finished
        waitForEvent(
            SaveFinishedEvent.class,
            DEFAULT_EVENT_TIMEOUT,
            () -> EventSystem.sendEvent(new StartSaveEvent(true))
        );

        final DiskIO diskReader = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        final JsonObject fileContent = diskReader.getObject(testedObject.getTargetFile(), JsonObject.class);

        assertEquals(harvestFrom, fileContent.get(SaveConstants.HARVEST_FROM_JSON).getAsInt());
    }


    /**
     * Tests if the end index of the harvesting range is correctly set and saved when a
     * {@linkplain HarvestStartedEvent} changes it.
     */
    @Test
    public void testFileContentHarvestTo()
    {
        testedObject.addEventListeners();

        addRandomNumberOfSaveableDocuments();

        final int harvestTo = random.nextInt(1000);
        EventSystem.sendEvent(new HarvestStartedEvent(-1, harvestTo, HARVESTER_HASH));

        // wait for the save to be finished
        waitForEvent(
            SaveFinishedEvent.class,
            DEFAULT_EVENT_TIMEOUT,
            () -> EventSystem.sendEvent(new StartSaveEvent(true))
        );

        final DiskIO diskReader = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        final JsonObject fileContent = diskReader.getObject(testedObject.getTargetFile(), JsonObject.class);

        assertEquals(harvestTo, fileContent.get(SaveConstants.HARVEST_TO_JSON).getAsInt());
    }


    /**
     * Tests if the harvester source hash is correctly set and saved when a
     * {@linkplain HarvestStartedEvent} changes it.
     */
    @Test
    public void testFileContentHarvesterHash()
    {
        testedObject.addEventListeners();

        addRandomNumberOfSaveableDocuments();

        final String sourceHash = HARVESTER_HASH + random.nextInt(1000);
        EventSystem.sendEvent(new HarvestStartedEvent(-1, -1, sourceHash));

        // wait for the save to be finished
        waitForEvent(
            SaveFinishedEvent.class,
            DEFAULT_EVENT_TIMEOUT,
            () -> EventSystem.sendEvent(new StartSaveEvent(true))
        );

        final DiskIO diskReader = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        final JsonObject fileContent = diskReader.getObject(testedObject.getTargetFile(), JsonObject.class);

        assertEquals(sourceHash, fileContent.get(SaveConstants.SOURCE_HASH_JSON).getAsString());
    }


    @Test
    public void testSaveFailed()
    {

    }


    //////////////////////
    // Non-test Methods //
    //////////////////////


    /**
     * Caches 1 to 10 documents in a {@linkplain HarvesterCache} that is then registered at
     * the {@linkplain HarvesterCacheManager}, allowing the documents to be saved.
     * The documents receive their index as publication year.
     *
     * @return the number of cached documents
     */
    private int addRandomNumberOfSaveableDocuments()
    {
        final MockedHarvester harvester = new MockedHarvester(testFolder);
        final HarvesterCache harvesterCache = new HarvesterCache(harvester);

        cacheManager.addEventListeners();
        EventSystem.sendEvent(new RegisterHarvesterCacheEvent(harvesterCache));

        // mock harvest of a random number of documents
        final int numberOfHarvestedDocuments = 1 + random.nextInt(10);

        for (int i = 0; i < numberOfHarvestedDocuments; i++) {
            final DataCiteJson doc = new DataCiteJson(SOURCE_ID + i);
            doc.setPublicationYear((short) i);
            harvesterCache.cacheDocument(doc, true);
        }

        harvesterCache.applyChanges(true, false);

        return numberOfHarvestedDocuments;
    }

}
