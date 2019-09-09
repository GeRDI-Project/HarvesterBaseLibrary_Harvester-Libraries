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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
import de.gerdiproject.harvest.etls.enums.ETLState;
import de.gerdiproject.harvest.etls.events.HarvestStartedEvent;
import de.gerdiproject.harvest.etls.loaders.DiskLoader;
import de.gerdiproject.harvest.etls.loaders.constants.DiskLoaderConstants;
import de.gerdiproject.harvest.utils.data.DiskIO;
import de.gerdiproject.json.datacite.DataCiteJson;
import de.gerdiproject.utils.examples.MockedETL;

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
        // make sure the DiskLoader writes to the 'mocked' folder
        AbstractParameter<?> filePathParam = DiskLoaderConstants.FILE_PATH_PARAM.copy();
        filePathParam.setValue(TEST_FOLDER.toString());

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
        etl.setStatus(ETLState.HARVESTING);

        // write file
        testedObject.load(docIter);
        testedObject.clear();

        // read file
        final DiskIO diskReader = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        final JsonObject fileContent = diskReader.getObject(testedObject.createTargetFile(etl.getName()), JsonObject.class);
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
        etl.setStatus(ETLState.HARVESTING);

        // write file
        testedObject.load(docIter);
        testedObject.clear();

        // read file
        final DiskIO diskReader = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        final JsonObject fileContent = diskReader.getObject(testedObject.createTargetFile(etl.getName()), JsonObject.class);

        assertEquals(String.format(ASSERT_JSON_MESSAGE, DiskLoaderConstants.SOURCE_HASH_JSON),
                     sourceHash,
                     fileContent.get(DiskLoaderConstants.SOURCE_HASH_JSON).getAsString());
    }


    /**
     * Tests if an {@linkplain IllegalStateException} is thrown if a save is attempted when there
     * are no documents to save.
     */
    @Test
    public void testSaveFailedNoDocuments()
    {
        testedObject.init(etl);

        List<DataCiteJson> emptyList = new LinkedList<>();
        final Iterator<DataCiteJson> docIter = emptyList.iterator();

        testedObject.load(docIter);
        testedObject.clear();

        assertFalse("Expected that no file was to be created when no documents were harvested!",
                    testedObject.createTargetFile(etl.getName()).exists());
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
            doc.setPublicationYear(i);
            list.add(doc);
        }

        return list.iterator();
    }
}
