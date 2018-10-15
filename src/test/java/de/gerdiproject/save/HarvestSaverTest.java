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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import de.gerdiproject.AbstractFileSystemUnitTest;
import de.gerdiproject.harvest.etls.events.HarvestFinishedEvent;
import de.gerdiproject.harvest.etls.events.HarvestStartedEvent;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.save.HarvestSaver;
import de.gerdiproject.harvest.save.constants.SaveConstants;
import de.gerdiproject.harvest.save.events.SaveHarvestEvent;
import de.gerdiproject.harvest.utils.FileUtils;
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
    private static final String ASSERT_JSON_MESSAGE = "The JSON field '%s' was not properly saved or loaded!";

    private HarvesterCacheManager cacheManager = new HarvesterCacheManager();


    @Override
    protected HarvestSaver setUpTestObjects()
    {
        final long startTimestamp = random.nextLong();
        final long endTimestamp = random.longs(startTimestamp + 1, startTimestamp + 99999999).findAny().getAsLong();

        ProcessTimeMeasure measure = new ProcessTimeMeasure();
        measure.set(startTimestamp, endTimestamp, ProcessStatus.Finished);

        this.cacheManager = new HarvesterCacheManager();
        return new HarvestSaver(testFolder, TEST_NAME, StandardCharsets.UTF_8, measure, cacheManager);
    }


    @Override
    public void after()
    {
        cacheManager = null;
        super.after();
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

        // write file
        final File savedFile = EventSystem.sendSynchronousEvent(new SaveHarvestEvent());

        // read file
        final DiskIO diskReader = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        final JsonObject fileContent = diskReader.getObject(savedFile, JsonObject.class);
        final JsonArray loadedDocuments = fileContent.get(SaveConstants.DOCUMENTS_JSON).getAsJsonArray();

        // publication year is the index of the document, but JsonArrays are unsorted
        for (int i = 0; i < loadedDocuments.size(); i++)
            assertTrue("The publication year of every saved object must be lower than " + numberOfSavedDocs + ", because it equals the index of the document!",
                       loadedDocuments.get(i).getAsJsonObject().get(JSON_PUBLICATION_YEAR).getAsInt() < numberOfSavedDocs);
    }


    /**
     * Tests if the harvesting duration in seconds is correctly saved to the file.
     */
    @Test
    public void testFileContentHarvestDuration()
    {
        testedObject.addEventListeners();

        addRandomNumberOfSaveableDocuments();

        final long startTimeStamp = (1 + random.nextInt(1000)) * 1000L;
        final long endTimeStamp = startTimeStamp + random.nextInt(1000) * 1000L;

        EventSystem.sendEvent(new HarvestStartedEvent(0, 1, HARVESTER_HASH, startTimeStamp));
        EventSystem.sendEvent(new HarvestFinishedEvent(true, HARVESTER_HASH, endTimeStamp));

        // write file
        final File savedFile = EventSystem.sendSynchronousEvent(new SaveHarvestEvent());

        // read file
        final DiskIO diskReader = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        final JsonObject fileContent = diskReader.getObject(savedFile, JsonObject.class);

        assertEquals(String.format(ASSERT_JSON_MESSAGE, SaveConstants.DURATION_JSON),
                     (endTimeStamp - startTimeStamp) / 1000L,
                     fileContent.get(SaveConstants.DURATION_JSON).getAsInt());
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

        // write file
        final File savedFile = EventSystem.sendSynchronousEvent(new SaveHarvestEvent());

        // read file
        final DiskIO diskReader = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        final JsonObject fileContent = diskReader.getObject(savedFile, JsonObject.class);

        assertEquals(String.format(ASSERT_JSON_MESSAGE, SaveConstants.HARVEST_FROM_JSON),
                     harvestFrom,
                     fileContent.get(SaveConstants.HARVEST_FROM_JSON).getAsInt());
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

        // write file
        final File savedFile = EventSystem.sendSynchronousEvent(new SaveHarvestEvent());

        // read file
        final DiskIO diskReader = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        final JsonObject fileContent = diskReader.getObject(savedFile, JsonObject.class);

        assertEquals(String.format(ASSERT_JSON_MESSAGE, SaveConstants.HARVEST_TO_JSON),
                     harvestTo,
                     fileContent.get(SaveConstants.HARVEST_TO_JSON).getAsInt());
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

        // write file
        final File savedFile = EventSystem.sendSynchronousEvent(new SaveHarvestEvent());

        // read file
        final DiskIO diskReader = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        final JsonObject fileContent = diskReader.getObject(savedFile, JsonObject.class);

        assertEquals(String.format(ASSERT_JSON_MESSAGE, SaveConstants.SOURCE_HASH_JSON),
                     sourceHash,
                     fileContent.get(SaveConstants.SOURCE_HASH_JSON).getAsString());
    }


    /**
     * Tests if an {@linkplain IllegalStateException} is thrown if a save is attempted when there
     * are no documents to save.
     */
    @Test
    public void testSaveFailedNoDocuments()
    {
        testedObject.addEventListeners();

        try {
            EventSystem.sendSynchronousEvent(new SaveHarvestEvent());
        } catch (IllegalStateException e) {
            return;
        }

        fail("Expected an IllegalStateException to be thrown!");
    }


    /**
     * Tests if an {@linkplain UncheckedIOException} is thrown if a save is attempted
     * while the target file exists and cannot be overwritten.
     *
     * @throws IOException thrown if the file cannot be written to prior to saving it
     * @throws FileNotFoundException thrown if the file cannot be created
     */
    @Test
    public void testSaveFailedCannotWriteToFile() throws FileNotFoundException, IOException
    {
        testedObject.addEventListeners();
        addRandomNumberOfSaveableDocuments();

        // create the save file
        final File savedFile = new File(testFolder, String.format(SaveConstants.SAVE_FILE_NAME, TEST_NAME));
        FileUtils.createEmptyFile(savedFile);

        // open the save file, blocking the HarvestSaver from writing to it
        try
            (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(savedFile), StandardCharsets.UTF_8))) {
            // write something to the file, causing it to be not-empty
            writer.write(HARVESTER_HASH);
            writer.flush();

            EventSystem.sendSynchronousEvent(new SaveHarvestEvent());
        } catch (UncheckedIOException e) {
            return;
        }

        fail("Expected an UncheckedIOException to be thrown!");
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
        final HarvesterCache harvesterCache = new HarvesterCache(
            harvester.getId(),
            harvester.getTemporaryCacheFolder(),
            harvester.getStableCacheFolder(),
            harvester.getCharset());

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
