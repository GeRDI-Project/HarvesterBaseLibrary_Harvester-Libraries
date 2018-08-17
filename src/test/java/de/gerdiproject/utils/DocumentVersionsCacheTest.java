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
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;

import de.gerdiproject.harvest.utils.FileUtils;
import de.gerdiproject.harvest.utils.cache.DocumentChangesCache;
import de.gerdiproject.harvest.utils.cache.DocumentVersionsCache;
import de.gerdiproject.harvest.utils.cache.constants.CacheConstants;
import de.gerdiproject.harvest.utils.data.DiskIO;

/**
 * This class provides test cases for the {@linkplain DocumentChangesCache}.
 *
 * @author Robin Weiss
 */
public class DocumentVersionsCacheTest
{
    private static final File CACHE_PARENT_FOLDER = new File("mocked/harvesterCacheTestDir/");
    private static final String TEMP_CACHE_FOLDER = CACHE_PARENT_FOLDER.getPath() + "/temp/";
    private static final String STABLE_CACHE_FOLDER = CACHE_PARENT_FOLDER.getPath() + "/stable/";

    private static final String HARVESTER_HASH = "ABC";
    private static final String DOCUMENT_ID = "mockedId";
    private static final String CLEANUP_ERROR = "Could not delete temporary test diectory: " + CACHE_PARENT_FOLDER;

    private final Random random = new Random();
    private DocumentVersionsCache versionsCache;


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

        versionsCache = new DocumentVersionsCache(
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
        versionsCache = null;
        FileUtils.deleteFile(CACHE_PARENT_FOLDER);
    }


    /**
     * Tests if the init() function successfully creates a source hash file in
     * the temporary documents folder with the same source hash value that
     * was passed to the init() function.
     */
    @Test
    public void testInit()
    {
        final String sourceHash = HARVESTER_HASH + random.nextInt(1000);
        versionsCache.init(sourceHash);

        final File sourceHashFile = new File(
            String.format(CacheConstants.SOURCE_HASH_FILE_PATH,
                          TEMP_CACHE_FOLDER + CacheConstants.VERSIONS_FOLDER_NAME));

        final DiskIO diskReader = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        assertEquals(sourceHash, diskReader.getString(sourceHashFile));
    }


    /**
     * Tests if the init() function does not create a source hash file when
     * null is passed as a source hash.
     */
    @Test
    public void testInitWithNull()
    {
        versionsCache.init(null);

        final File sourceHashFile = new File(
            String.format(CacheConstants.SOURCE_HASH_FILE_PATH,
                          TEMP_CACHE_FOLDER + CacheConstants.VERSIONS_FOLDER_NAME));

        assert !sourceHashFile.exists();
    }


    /**
     * Tests if the putFile() method generates no-empty files.
     */
    @Test
    public void testPuttingFiles()
    {
        final int numberOfPutFiles = putRandomNumberOfFiles(true);

        // test if as many non-empty files were created as there were version files
        for (int i = 0; i < numberOfPutFiles; i++) {
            final File addedFile = getCachedDocument(DOCUMENT_ID + i, true);
            assert addedFile.length() != 0;
        }
    }



    /**
     * Tests if removeFile() function calls cause files to be removed from the file system.
     */
    @Test
    public void testRemovingFiles()
    {
        versionsCache.putFile(DOCUMENT_ID, DOCUMENT_ID);
        versionsCache.removeFile(DOCUMENT_ID);

        File deletedDocument = getCachedDocument(DOCUMENT_ID, true);
        assert !deletedDocument.exists();
    }


    /**
     * Tests if the getDocumentIDs() method returns all stable documentIDs.
     */
    @Test
    public void testGettingDocumentIDs()
    {
        final int numberOfPutFiles = putRandomNumberOfFiles(false);

        List<String> documentIDs = versionsCache.getDocumentIDs();

        for (int i = 0; i < numberOfPutFiles; i++)
            assert documentIDs.contains(DOCUMENT_ID + i);
    }


    /**
     * Tests if the applyChanges() method moves all temporary files to the stable folder.
     */
    @Test
    public void testApplyingChanges()
    {
        final int numberOfPutFiles = putRandomNumberOfFiles(false);

        // test if as many non-empty files were created as there were version files
        for (int i = 0; i < numberOfPutFiles; i++) {
            final File addedFile = getCachedDocument(DOCUMENT_ID + i, false);
            assert addedFile.length() != 0;
        }
    }

    /**
     * Tests if the applyChanges() method keeps old files in the stable folder.
     */
    @Test
    public void testApplyingChangesPreservingOldFiles()
    {
        versionsCache.putFile(DOCUMENT_ID + 10, DOCUMENT_ID + 10);
        putRandomNumberOfFiles(false);

        final File oldFile = getCachedDocument(DOCUMENT_ID + 10, false);
        assert oldFile.length() != 0;
    }


    /**
     * Tests if the getFileContent() function does not return file content without
     * having applied the changes beforehand.
     */
    @Test
    public void testRetrievingTemporaryFileContent()
    {
        final int numberOfPutFiles = putRandomNumberOfFiles(true);

        for (int i = 0; i < numberOfPutFiles; i++)
            assertNull(versionsCache.getFileContent(DOCUMENT_ID + i));
    }


    /**
     * Tests if the getFileContent() function returns the correct content when
     * having applied the changes beforehand.
     */
    @Test
    public void testRetrievingStableFileContent()
    {
        final int numberOfPutFiles = putRandomNumberOfFiles(false);

        for (int i = 0; i < numberOfPutFiles; i++) {
            final String retrievedValue = versionsCache.getFileContent(DOCUMENT_ID + i);
            assertEquals(DOCUMENT_ID + i, retrievedValue);
        }
    }


    /**
     * Tests if the forEach() function returns pairs of matching
     * document IDs and document objects.
     */
    @Test
    public void testForEachIntegrity()
    {
        putRandomNumberOfFiles(false);

        // test if each documentID match the documentHash
        boolean successfulIteration = versionsCache.forEach(
        (String documentId, String documentHash) -> {
            return documentId.equals(documentHash);
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
        putRandomNumberOfFiles(true);
        boolean successfulIteration = versionsCache.forEach(
        (String documentId, String documentHash) -> {
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
        final int numberOfPutFiles = putRandomNumberOfFiles(false);
        final AtomicInteger numberOfProcessedFiles = new AtomicInteger(0);

        versionsCache.forEach(
        (String documentId, String documentHash) -> {
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
        putRandomNumberOfFiles(false);

        boolean successfulIteration = versionsCache.forEach(
        (String documentId, String documentHash) -> {
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
        versionsCache.putFile(DOCUMENT_ID + 10, DOCUMENT_ID + 10);
        putRandomNumberOfFiles(false);

        final AtomicInteger numberOfProcessedFiles = new AtomicInteger(0);

        // abort forEach() after one iteration
        versionsCache.forEach(
        (String documentId, String documentHash) -> {
            if (numberOfProcessedFiles.get() == 1)
                return false;

            numberOfProcessedFiles.incrementAndGet();
            return true;
        });

        assertEquals(1, numberOfProcessedFiles.get());
    }


    /**
     * Tests if the deleteEmptyFiles() function removes stable cache files
     * if empty temporary cache files with the same document ID exist.
     */
    @Test
    public void testDeletingEmptyFilesFromStableFolder()
    {
        versionsCache.init(HARVESTER_HASH);
        putRandomNumberOfFiles(false);

        versionsCache.removeFile(DOCUMENT_ID + 0);
        versionsCache.deleteEmptyFiles();

        assert !getCachedDocument(DOCUMENT_ID + 0, false).exists();
    }


    /**
     * Tests if the deleteEmptyFiles() function removes empty temporary
     * cache files.
     */
    @Test
    public void testDeletingEmptyFilesFromTemporaryFolder()
    {
        versionsCache.init(HARVESTER_HASH);
        putRandomNumberOfFiles(false);

        versionsCache.removeFile(DOCUMENT_ID + 0);
        versionsCache.deleteEmptyFiles();

        assert !getCachedDocument(DOCUMENT_ID + 0, true).exists();
    }


    /**
     * Tests if the cache is considered outdated when the stable source hash
     * differs from the temporary one.
     */
    @Test
    public void testOutDated()
    {
        versionsCache.init(HARVESTER_HASH);
        versionsCache.applyChanges();
        versionsCache.init(HARVESTER_HASH + 2);
        assert versionsCache.isOutdated();
    }


    /**
     * Tests if the cache is considered up-to-date when the stable source hash
     * is equal to the temporary one.
     */
    @Test
    public void testOutDatedSameSourceHash()
    {
        versionsCache.init(HARVESTER_HASH);
        versionsCache.applyChanges();
        versionsCache.init(HARVESTER_HASH);
        assert !versionsCache.isOutdated();
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
    private int putRandomNumberOfFiles(boolean isInTempFolder)
    {
        final int size = 1 + random.nextInt(10);

        for (int i = 0; i < size; i++)
            versionsCache.putFile(DOCUMENT_ID + i, DOCUMENT_ID + i);

        if (!isInTempFolder)
            versionsCache.applyChanges();

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
                            topFolder + CacheConstants.VERSIONS_FOLDER_NAME,
                            documentId.substring(0, 2),
                            documentId.substring(2)));
    }
}
