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
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.gerdiproject.harvest.utils.FileUtils;
import de.gerdiproject.harvest.utils.cache.DocumentChangesCache;
import de.gerdiproject.harvest.utils.cache.DocumentVersionsCache;
import de.gerdiproject.harvest.utils.cache.constants.CacheConstants;
import de.gerdiproject.json.datacite.DataCiteJson;

/**
 * This class provides test cases for the {@linkplain DocumentChangesCache}.
 *
 * @author Robin Weiss
 */
public class DocumentChangesCacheTest
{
    private static final File CACHE_PARENT_FOLDER = new File("mocked/harvesterCacheTestDir/");
    private static final String TEMP_CACHE_FOLDER = CACHE_PARENT_FOLDER.getPath() + "/temp/";
    private static final String STABLE_CACHE_FOLDER = CACHE_PARENT_FOLDER.getPath() + "/stable/";

    private static final String HARVESTER_HASH = "ABC";
    private static final String DOCUMENT_ID = "mockedId";
    private static final String CLEANUP_ERROR = "Could not delete temporary test diectory: " + CACHE_PARENT_FOLDER;

    private final Random random = new Random();
    private DocumentChangesCache changesCache;


    /**
     * Removes the test folder and validates if it really
     * has been deleted.
     * Also creates an instance of a {@linkplain DocumentChangesCache}.
     *
     * @throws IOException thrown when the test folder could not be deleted
     */
    @Before
    public void before() throws IOException
    {
        FileUtils.deleteFile(CACHE_PARENT_FOLDER);

        if (CACHE_PARENT_FOLDER.exists())
            throw new IOException(CLEANUP_ERROR);

        changesCache = new DocumentChangesCache(
            TEMP_CACHE_FOLDER,
            STABLE_CACHE_FOLDER,
            StandardCharsets.UTF_8);
    }


    /**
     * Removes the test folder to free up some space.
     */
    @After
    public void after()
    {
        changesCache = null;
        FileUtils.deleteFile(CACHE_PARENT_FOLDER);
    }


    /**
     * Tests if a empty temporary change cache files are created for
     * existing stable version files when initializing the cache.
     */
    @Test
    public void testInit()
    {
        final DocumentVersionsCache versionsCache = new DocumentVersionsCache(
            TEMP_CACHE_FOLDER,
            STABLE_CACHE_FOLDER,
            StandardCharsets.UTF_8);

        versionsCache.init(HARVESTER_HASH);

        // add 1-10 version cache documents
        int size = 1 + random.nextInt(10);

        for (int i = 0; i < size; i++)
            versionsCache.putFile(DOCUMENT_ID + i, DOCUMENT_ID + i);

        versionsCache.applyChanges();

        // initialize changes cache with the version cache
        changesCache.init(versionsCache);

        // test if as many empty files were created as there were version files
        for (int i = 0; i < size; i++) {
            final File initializedFile = getCachedDocument(DOCUMENT_ID + i, true);
            assert initializedFile.exists() && initializedFile.length() == 0;
        }
    }


    /**
     * Tests if the putFile() method generates files.
     */
    @Test
    public void testPuttingFiles()
    {
        final int numberOfPutFiles = putRandomAmountOfFiles(true);

        // test if as many non-empty files were created as there were version files
        for (int i = 0; i < numberOfPutFiles; i++) {
            final File addedFile = getCachedDocument(DOCUMENT_ID + i, true);
            assert addedFile.length() != 0;
        }
    }


    /**
     * Tests if putFile() function calls correctly increase the size of the cache.
     */
    @Test
    public void testSizeWhenPuttingFiles()
    {
        final int numberOfPutFiles = putRandomAmountOfFiles(true);
        assertEquals(numberOfPutFiles, changesCache.size());
    }


    /**
     * Tests if removeFile() function calls cause files to be removed from the file system.
     */
    @Test
    public void testRemovingFiles()
    {
        changesCache.putFile(DOCUMENT_ID, new DataCiteJson(DOCUMENT_ID));
        changesCache.removeFile(DOCUMENT_ID);

        File deletedDocument = getCachedDocument(DOCUMENT_ID, true);
        assert !deletedDocument.exists();
    }


    /**
     * Tests if removeFile() function calls correctly increase the size of the cache.
     */
    @Test
    public void testSizeWhenRemovingFiles()
    {
        final int numberOfPutFiles = putRandomAmountOfFiles(true);
        final int numberOfRemovedFiles = 1 + random.nextInt(numberOfPutFiles);

        for (int i = 0; i < numberOfRemovedFiles; i++)
            changesCache.removeFile(DOCUMENT_ID + i);

        assertEquals(numberOfPutFiles - numberOfRemovedFiles, changesCache.size());
    }


    /**
     * Tests if the applyChanges() method moves all temporary files to the stable folder.
     */
    @Test
    public void testApplyingChanges()
    {
        final int numberOfPutFiles = putRandomAmountOfFiles(true);
        changesCache.applyChanges();

        // test if as many non-empty files were created as there were version files
        for (int i = 0; i < numberOfPutFiles; i++) {
            final File addedFile = getCachedDocument(DOCUMENT_ID + i, false);
            assert addedFile.length() != 0;
        }
    }


    /**
     * Tests if the getFileContent() function does not return file content without
     * having applied the changes beforehand.
     */
    @Test
    public void testRetrievingTemporaryFileContent()
    {
        final int numberOfPutFiles = putRandomAmountOfFiles(true);

        for (int i = 0; i < numberOfPutFiles; i++)
            assertNull(changesCache.getFileContent(DOCUMENT_ID + i));
    }


    /**
     * Tests if the getFileContent() function returns the correct content when
     * having applied the changes beforehand.
     */
    @Test
    public void testRetrievingStableFileContent()
    {
        final int numberOfPutFiles = putRandomAmountOfFiles(false);

        for (int i = 0; i < numberOfPutFiles; i++) {
            final DataCiteJson retrievedDocument = changesCache.getFileContent(DOCUMENT_ID + i);
            assertEquals(i, retrievedDocument.getPublicationYear());
        }
    }


    /**
     * Tests if the forEach() function returns pairs of matching
     * document IDs and document objects.
     */
    @Test
    public void testForEachIntegrity()
    {
        putRandomAmountOfFiles(false);

        // test if each document has a publication year that is equal to
        // the suffix of the document id
        boolean successfulIteration = changesCache.forEach(
        (String documentId, DataCiteJson document) -> {
            short year = document.getPublicationYear();
            return documentId.endsWith(String.valueOf(year));
        });
        assert successfulIteration;
    }


    /**
     * Tests if the forEach() returns true without processing any file,
     * if there are no files in the stable folder.
     */
    @Test
    public void testForEachTemporaryFiles()
    {
        putRandomAmountOfFiles(true);
        boolean successfulIteration = changesCache.forEach(
        (String documentId, DataCiteJson document) -> {
            return false;
        });
        assert successfulIteration;
    }


    /**
     * Tests if the forEach() function processes the correct number of documents.
     */
    @Test
    public void testForEachNumberOfProcessedFiles()
    {
        final int numberOfPutFiles = putRandomAmountOfFiles(false);
        final AtomicInteger numberOfProcessedFiles = new AtomicInteger(0);

        changesCache.forEach(
        (String documentId, DataCiteJson document) -> {
            numberOfProcessedFiles.incrementAndGet();
            return true;
        });

        assertEquals(numberOfPutFiles, numberOfProcessedFiles.get());
    }


    /**
     * Tests if the forEach() returns false when the lambda expression
     * returns false.
     */
    @Test
    public void testForEachReturningFalse()
    {
        putRandomAmountOfFiles(false);

        boolean successfulIteration = changesCache.forEach(
        (String documentId, DataCiteJson document) -> {
            return false;
        });

        assert !successfulIteration;
    }


    /**
     * Tests if the forEach() aborts when the lambda expression
     * returns false. At least two documents are added, but the lambda
     * expression will return false at the beginning of the second iteration.
     * If no further document is processed, the test succeeds.
     */
    @Test
    public void testForEachAborting()
    {
        // add 2-10 files
        changesCache.putFile(DOCUMENT_ID + 10, new DataCiteJson(DOCUMENT_ID + 10));
        putRandomAmountOfFiles(false);

        final AtomicInteger numberOfProcessedFiles = new AtomicInteger(0);

        // abort forEach() after one iteration
        changesCache.forEach(
        (String documentId, DataCiteJson document) -> {
            if (numberOfProcessedFiles.get() == 1)
                return false;

            numberOfProcessedFiles.incrementAndGet();
            return true;
        });

        assertEquals(1, numberOfProcessedFiles.get());
    }


    //////////////////////
    // Non-test Methods //
    //////////////////////

    /**
     * Adds 1 to 10 change cache files to a specified folder.
     * Each cache file has a publication year equal to their index,
     * ranging from 0-9.
     *
     * @param isInTempFolder if true, the changes are not applied,
     *         causing the files to be in the temporary folder
     *
     * @return the number of files that were added
     */
    private int putRandomAmountOfFiles(boolean isInTempFolder)
    {
        final int size = 1 + random.nextInt(10);

        for (int i = 0; i < size; i++) {
            final DataCiteJson putDocument = new DataCiteJson(DOCUMENT_ID + i);
            putDocument.setPublicationYear((short) i);
            changesCache.putFile(DOCUMENT_ID + i, putDocument);
        }

        if (!isInTempFolder)
            changesCache.applyChanges();

        return size;
    }


    /**
     * Retrieves a changes cache file.
     *
     * @param documentId the identifier of the document of which the file is retrieved
     * @param isInTempFolder if true, the file is retrieved from the temporary folder
     *
     * @return a file pointing to the specified changes cache file
     */
    private File getCachedDocument(String documentId, boolean isInTempFolder)
    {
        final String topFolder = isInTempFolder ? TEMP_CACHE_FOLDER : STABLE_CACHE_FOLDER;
        return new File(String.format(
                            CacheConstants.DOCUMENT_HASH_FILE_PATH,
                            topFolder + CacheConstants.CHANGES_FOLDER_NAME,
                            documentId.substring(0, 2),
                            documentId.substring(2)));
    }
}
