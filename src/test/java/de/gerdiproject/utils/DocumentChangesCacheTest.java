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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import de.gerdiproject.AbstractFileSystemUnitTest;
import de.gerdiproject.harvest.utils.cache.DocumentChangesCache;
import de.gerdiproject.harvest.utils.cache.constants.CacheConstants;
import de.gerdiproject.json.datacite.DataCiteJson;

/**
 * This class provides test cases for the {@linkplain DocumentChangesCache}.
 *
 * @author Robin Weiss
 */
public class DocumentChangesCacheTest extends AbstractFileSystemUnitTest<DocumentChangesCache>
{
    private static final String DOCUMENT_ID = "mockedId";

    private String wipFolder = testFolder + "/documents_temp/";
    private String stableFolder = testFolder + "/documents/";


    @Override
    protected DocumentChangesCache setUpTestObjects()
    {
        return new DocumentChangesCache(
                   wipFolder,
                   stableFolder,
                   StandardCharsets.UTF_8);
    }


    /**
     * Tests if a empty temporary change cache files are created for
     * existing stable version files when initializing the cache.
     */
    @Test
    public void testInit()
    {
        // add 1-10 version cache documents
        final int size = 1 + random.nextInt(10);

        List<String> documentIDs = new LinkedList<>();

        for (int i = 0; i < size; i++)
            documentIDs.add(DOCUMENT_ID + i);


        // initialize changes cache with the version cache
        testedObject.init(documentIDs);

        // test if as many empty files were created as there were version files
        for (int i = 0; i < size; i++) {
            final File initializedFile = getCachedDocument(DOCUMENT_ID + i, true);
            assertTrue("The method init() should create an empty file!",
                       initializedFile.exists() && initializedFile.length() == 0);
        }
    }


    /**
     * Tests if the putFile() method generates files.
     */
    @Test
    public void testPuttingFiles()
    {
        final int numberOfPutFiles = putRandomNumberOfFiles(true);

        // test if as many non-empty files were created as there were version files
        for (int i = 0; i < numberOfPutFiles; i++) {
            final File addedFile = getCachedDocument(DOCUMENT_ID + i, true);
            assertNotEquals("The method putFile() should create a non-empty file!",
                            0, addedFile.length());
        }
    }


    /**
     * Tests if putFile() function calls correctly increase the size of the cache.
     */
    @Test
    public void testSizeWhenPuttingFiles()
    {
        final int numberOfPutFiles = putRandomNumberOfFiles(true);
        assertEquals("The method size() should reflect the number of files generated via putFile()!",
                     numberOfPutFiles, testedObject.size());
    }


    /**
     * Tests if removeFile() function calls cause files to be removed from the file system.
     */
    @Test
    public void testRemovingFiles()
    {
        testedObject.putFile(DOCUMENT_ID, new DataCiteJson(DOCUMENT_ID));
        testedObject.removeFile(DOCUMENT_ID);

        File deletedDocument = getCachedDocument(DOCUMENT_ID, true);
        assertFalse("The method removeFile() should cause the corresponding file to be deleted!",
                    deletedDocument.exists());
    }


    /**
     * Tests if removeFile() function calls correctly increase the size of the cache.
     */
    @Test
    public void testSizeWhenRemovingFiles()
    {
        final int numberOfPutFiles = putRandomNumberOfFiles(true);
        final int numberOfRemovedFiles = 1 + random.nextInt(numberOfPutFiles);

        for (int i = 0; i < numberOfRemovedFiles; i++)
            testedObject.removeFile(DOCUMENT_ID + i);

        assertEquals("The method removeFile() should cause the size() method to reflect the changes!",
                     numberOfPutFiles - numberOfRemovedFiles, testedObject.size());
    }


    /**
     * Tests if the getDocumentIDs() method returns all stable documentIDs.
     */
    @Test
    public void testGettingDocumentIDs()
    {
        final int numberOfPutFiles = putRandomNumberOfFiles(false);

        List<String> documentIDs = testedObject.getDocumentIDs();

        for (int i = 0; i < numberOfPutFiles; i++)
            assertTrue("The method getDocumentIDs() should return all IDs that were added via putFile()!",
                       documentIDs.contains(DOCUMENT_ID + i));
    }


    /**
     * Tests if the applyChanges() method moves all temporary files to the stable folder.
     */
    @Test
    public void testApplyingChanges()
    {
        final int numberOfPutFiles = putRandomNumberOfFiles(true);
        testedObject.applyChanges();

        // test if as many non-empty files were created as there were version files
        for (int i = 0; i < numberOfPutFiles; i++) {
            final File addedFile = getCachedDocument(DOCUMENT_ID + i, false);
            assertNotEquals("The method applyChanges() should cause every file that was created via putFile() to now be retrievable via getCachedDocument()!",
                            0, addedFile.length());
        }
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
            assertNull("The method getFileContent() should return null if applyChanges() was not called on the document that is requested! ",
                       testedObject.getFileContent(DOCUMENT_ID + i));
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
            final DataCiteJson retrievedDocument = testedObject.getFileContent(DOCUMENT_ID + i);
            assertEquals("The document identifier does not match the file content!",
                         i,
                         retrievedDocument.getPublicationYear());
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

        // test if each document has a publication year that is equal to
        // the suffix of the document id
        boolean successfulIteration = testedObject.forEach(
        (String documentId, DataCiteJson document) -> {
            short year = document.getPublicationYear();
            return documentId.endsWith(String.valueOf(year));
        });
        assertTrue("The document identifier does not match the document in the forEach() function!",
                   successfulIteration);
    }


    /**
     * Tests if the forEach() returns true without processing any file,
     * if there are no files in the stable folder.
     */
    @Test
    public void testForEachTemporaryFiles()
    {
        putRandomNumberOfFiles(true);
        boolean successfulIteration = testedObject.forEach(
        (String documentId, DataCiteJson document) -> {
            return false;
        });
        assertTrue("The method forEach() should return true if there are no documents to process!",
                   successfulIteration);
    }


    /**
     * Tests if the forEach() function processes the correct number of documents.
     */
    @Test
    public void testForEachNumberOfProcessedFiles()
    {
        final int numberOfPutFiles = putRandomNumberOfFiles(false);
        final AtomicInteger numberOfProcessedFiles = new AtomicInteger(0);

        testedObject.forEach(
        (String documentId, DataCiteJson document) -> {
            numberOfProcessedFiles.incrementAndGet();
            return true;
        });

        assertEquals("The method forEach() must process the same number of documents that were putFile() and applied!",
                     numberOfPutFiles,
                     numberOfProcessedFiles.get());
    }


    /**
     * Tests if the forEach() returns false when the lambda expression
     * returns false.
     */
    @Test
    public void testForEachReturningFalse()
    {
        putRandomNumberOfFiles(false);

        boolean successfulIteration = testedObject.forEach(
        (String documentId, DataCiteJson document) -> {
            return false;
        });

        assertFalse("The method forEach() must return false if its lambda expression evaluates to false!",
                    successfulIteration);
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
        testedObject.putFile(DOCUMENT_ID + 10, new DataCiteJson(DOCUMENT_ID + 10));
        putRandomNumberOfFiles(false);

        final AtomicInteger numberOfProcessedFiles = new AtomicInteger(0);

        // abort forEach() after one iteration
        testedObject.forEach(
        (String documentId, DataCiteJson document) -> {
            if (numberOfProcessedFiles.get() == 1)
                return false;

            numberOfProcessedFiles.incrementAndGet();
            return true;
        });

        assertEquals("The method forEach() must interrupt when its lambda expression returns false!",
                     1,
                     numberOfProcessedFiles.get());
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

        for (int i = 0; i < size; i++) {
            final DataCiteJson putDocument = new DataCiteJson(DOCUMENT_ID + i);
            putDocument.setPublicationYear((short) i);
            testedObject.putFile(DOCUMENT_ID + i, putDocument);
        }

        if (!isInTempFolder)
            testedObject.applyChanges();

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
        final String topFolder = isInTempFolder ? wipFolder : stableFolder;
        return new File(String.format(
                            CacheConstants.DOCUMENT_HASH_FILE_PATH,
                            topFolder + CacheConstants.CHANGES_FOLDER_NAME,
                            documentId.substring(0, 2),
                            documentId.substring(2)));
    }
}
