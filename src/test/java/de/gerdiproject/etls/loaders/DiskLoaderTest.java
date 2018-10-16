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
package de.gerdiproject.etls.loaders;

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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import de.gerdiproject.AbstractFileSystemUnitTest;
import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.parameters.AbstractParameter;
import de.gerdiproject.harvest.etls.enums.ETLStatus;
import de.gerdiproject.harvest.etls.events.HarvestStartedEvent;
import de.gerdiproject.harvest.etls.loaders.DiskLoader;
import de.gerdiproject.harvest.etls.loaders.LoaderException;
import de.gerdiproject.harvest.etls.loaders.constants.DiskLoaderConstants;
import de.gerdiproject.harvest.utils.data.DiskIO;
import de.gerdiproject.harvest.utils.file.FileUtils;
import de.gerdiproject.harvest.utils.time.ProcessTimeMeasure;
import de.gerdiproject.harvest.utils.time.ProcessTimeMeasure.ProcessStatus;
import de.gerdiproject.json.datacite.DataCiteJson;
import de.gerdiproject.utils.examples.harvestercache.MockedETL;

/**
 * This class contains unit tests for the {@linkplain DiskLoader}.
 *
 * @author Robin Weiss
 */
public class DiskLoaderTest extends AbstractFileSystemUnitTest<DiskLoader>
{
    private static final String SOURCE_ID = "source";
    private static final String HARVESTER_HASH = "ABC";
    private static final String JSON_PUBLICATION_YEAR = "publicationYear";
    private static final String ASSERT_JSON_MESSAGE = "The JSON field '%s' was not properly saved or loaded!";

    private MockedETL etl;

    @Override
    protected DiskLoader setUpTestObjects()
    {
        final long startTimestamp = random.nextLong();
        final long endTimestamp = random.longs(startTimestamp + 1, startTimestamp + 99999999).findAny().getAsLong();

        ProcessTimeMeasure measure = new ProcessTimeMeasure();
        measure.set(startTimestamp, endTimestamp, ProcessStatus.Finished);

        // make sure the DiskLoader writes to the 'mocked' folder
        AbstractParameter<?> filePathParam = DiskLoaderConstants.FILE_PATH_PARAM.copy();
        filePathParam.setValue(TEST_FOLDER.toString(), null);

        this.config = new Configuration(MODULE_NAME, filePathParam);
        this.config.addEventListeners();

        this.etl = new MockedETL();

        return new DiskLoader();
    }


    /**
     * Tests if all saved documents can be retrieved from the resulting file.
     */
    @Test
    public void testFileContentDocuments()
    {
        testedObject.init(etl);
        final Iterator<DataCiteJson> docIter = createRandomNumberOfSaveableDocuments();
        etl.setStatus(ETLStatus.HARVESTING);

        // write file
        testedObject.load(docIter, true);

        // read file
        final DiskIO diskReader = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        final JsonObject fileContent = diskReader.getObject(testedObject.getTargetFile(), JsonObject.class);
        final JsonArray loadedDocuments = fileContent.get(DiskLoaderConstants.DOCUMENTS_JSON).getAsJsonArray();

        // publication year is the index of the document, but JsonArrays are unsorted
        for (int i = 0; i < loadedDocuments.size(); i++)
            assertTrue("The publication year of every saved object must be lower than " + loadedDocuments.size() + ", because it equals the index of the document!",
                       loadedDocuments.get(i).getAsJsonObject().get(JSON_PUBLICATION_YEAR).getAsInt() < loadedDocuments.size());
    }


    /**
     * Tests if the harvester source hash is correctly set and saved when a
     * {@linkplain HarvestStartedEvent} changes it.
     */
    @Test
    public void testFileContentHarvesterHash()
    {
        final String sourceHash = HARVESTER_HASH + random.nextInt(1000);
        etl.setHash(sourceHash);
        testedObject.init(etl);

        final Iterator<DataCiteJson> docIter = createRandomNumberOfSaveableDocuments();
        etl.setStatus(ETLStatus.HARVESTING);

        // write file
        testedObject.load(docIter, true);

        // read file
        final DiskIO diskReader = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        final JsonObject fileContent = diskReader.getObject(testedObject.getTargetFile(), JsonObject.class);

        assertEquals(String.format(ASSERT_JSON_MESSAGE, DiskLoaderConstants.SOURCE_HASH_JSON),
                     sourceHash,
                     fileContent.get(DiskLoaderConstants.SOURCE_HASH_JSON).getAsString());
    }


    /**
     * Tests if an {@linkplain IllegalStateException} is thrown if a save is attempted when there
     * are no documents to save.
     */
    @Test(expected = LoaderException.class)
    public void testSaveFailedNoDocuments()
    {
        testedObject.init(etl);

        List<DataCiteJson> emptyList = new LinkedList<>();
        final Iterator<DataCiteJson> docIter = emptyList.iterator();

        testedObject.load(docIter, true);

        fail("Expected an " + LoaderException.class.getSimpleName() + " to be thrown!");
    }


    /**
     * Tests if an {@linkplain UncheckedIOException} is thrown if a save is attempted
     * while the target file exists and cannot be overwritten.
     *
     * @throws IOException thrown if the file cannot be written to prior to saving it
     * @throws FileNotFoundException thrown if the file cannot be created
     */
    @Test(expected = LoaderException.class)
    public void testSaveFailedCannotWriteToFile() throws FileNotFoundException, IOException
    {
        testedObject.init(etl);
        final Iterator<DataCiteJson> docIter = createRandomNumberOfSaveableDocuments();

        // create the save file
        final File savedFile = testedObject.getTargetFile();
        FileUtils.createEmptyFile(savedFile);

        // open the save file, blocking the DiskLoader from writing to it
        try
            (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(savedFile), StandardCharsets.UTF_8))) {
            // write something to the file, causing it to be not-empty
            writer.write(HARVESTER_HASH);
            writer.flush();

            testedObject.load(docIter, true);
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
    private Iterator<DataCiteJson> createRandomNumberOfSaveableDocuments()
    {
        final List<DataCiteJson> list = new LinkedList<>();

        // mock harvest of a random number of documents
        final int numberOfHarvestedDocuments = 1 + random.nextInt(10);

        for (int i = 0; i < numberOfHarvestedDocuments; i++) {
            final DataCiteJson doc = new DataCiteJson(SOURCE_ID + i);
            doc.setPublicationYear((short) i);
            list.add(doc);
        }

        return list.iterator();
    }
}
