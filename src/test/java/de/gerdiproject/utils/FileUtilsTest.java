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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.junit.After;
import org.junit.Test;

import com.google.gson.Gson;

import de.gerdiproject.harvest.utils.FileUtils;
import de.gerdiproject.harvest.utils.cache.constants.CacheConstants;
import de.gerdiproject.harvest.utils.data.DiskIO;

/**
 * This class provides test cases for the {@linkplain FileUtils}.
 *
 * @author Robin Weiss
 */
public class FileUtilsTest
{
    private static final File TEST_FILE = new File("mocked/fileUtilsTestDir/fileUtilsTest.file");
    private static final File COPY_TEST_SOURCE_FILE = new File("mocked/fileUtilsTestDir/fileUtilsCopyTestSource.file");
    private static final File COPY_TEST_TARGET_FILE = new File("mocked/fileUtilsTestDir/copyTest/fileUtilsCopyTestTarget.file");
    private static final String COPY_TEST_TEXT = "Milch macht müde Männer munter.";
    private static final String COPY_TEST_OVERWRITE_TEXT = "Ohne Krimi geht die Mimi nie ins Bett.";


    private static final File TEST_DIRECTORY = new File("mocked/fileUtilsTestDir");
    private static final File TEST_MULTI_DIRECTORY = new File("mocked/fileUtilsTestDir/moarTests/moar");


    /**
     * Cleans up the entire directory of test files after each test.
     */
    @After
    public void after()
    {
        FileUtils.deleteFile(TEST_DIRECTORY);
    }


    /**
     * Tests if creating files is reflected in the file system.
     */
    @Test
    public void testFileCreation()
    {
        assertFalse(TEST_FILE.exists());
        FileUtils.createEmptyFile(TEST_FILE);

        assertTrue(TEST_FILE.exists());
        assertTrue(TEST_FILE.isFile());
        assertEquals(TEST_FILE.length(), 0L);
    }


    /**
     * Tests if creating files overwrites existing files.
     */
    @Test
    public void testFileCreationWithTarget()
    {
        final DiskIO diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        diskIo.writeStringToFile(TEST_FILE, COPY_TEST_TEXT);

        assertTrue(TEST_FILE.exists());
        FileUtils.createEmptyFile(TEST_FILE);

        assertTrue(TEST_FILE.exists());
        assertTrue(TEST_FILE.isFile());
        assertEquals(TEST_FILE.length(), 0L);
    }


    /**
     * Tests if deleting files is reflected in the file system.
     */
    @Test
    public void testFileDeletion()
    {
        FileUtils.createEmptyFile(TEST_FILE);
        assertTrue(TEST_FILE.exists());
        FileUtils.deleteFile(TEST_FILE);
        assertFalse(TEST_FILE.exists());
    }


    /**
     * Tests if deleting non-existing files causes no exception.
     */
    @Test
    public void testFileDeletionNonExisting()
    {
        assertFalse(TEST_FILE.exists());

        try {
            FileUtils.deleteFile(TEST_FILE);
        } catch (Exception e) {
            assert(false);
        }

        assertFalse(TEST_FILE.exists());
    }


    /**
     * Tests if multiple directories can be created in the
     * file system.
     */
    @Test
    public void testDirectoryCreation()
    {
        FileUtils.createDirectories(TEST_MULTI_DIRECTORY);
        assertTrue(TEST_MULTI_DIRECTORY.exists());
        assertTrue(TEST_MULTI_DIRECTORY.isDirectory());
    }


    /**
     * Tests if creating the same directory structure twice
     * throws no exceptions.
     */
    @Test
    public void testDirectoryCreationExisting()
    {
        assertFalse(TEST_MULTI_DIRECTORY.exists());
        FileUtils.createDirectories(TEST_MULTI_DIRECTORY);

        try {
            FileUtils.createDirectories(TEST_MULTI_DIRECTORY);
        } catch (Exception e) {
            assert(false);
        }

        assertTrue(TEST_MULTI_DIRECTORY.exists());
    }


    /**
     * Tests if a directory structure can be deleted
     * from the filesystem.
     */
    @Test
    public void testDirectoryDeletion()
    {
        FileUtils.createDirectories(TEST_MULTI_DIRECTORY);
        assertTrue(TEST_MULTI_DIRECTORY.exists());

        FileUtils.deleteFile(TEST_DIRECTORY);

        assertFalse(TEST_MULTI_DIRECTORY.exists());
        assertFalse(TEST_DIRECTORY.exists());
    }


    /**
     * Tests if deleting a non-existing directory
     * throws no exceptions.
     */
    @Test
    public void testDirectoryDeletionNonExisting()
    {
        assertFalse(TEST_DIRECTORY.exists());

        try {
            FileUtils.deleteFile(TEST_DIRECTORY);
        } catch (Exception e) {
            assert(false);
        }

        assertFalse(TEST_DIRECTORY.exists());
    }


    /**
     * Tests if copying a file to a non-existing destination
     * causes the corresponding changes in the filesystem.
     */
    @Test
    public void testFileCopyingWithoutTarget()
    {
        // create source file
        final DiskIO diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        diskIo.writeStringToFile(COPY_TEST_SOURCE_FILE, COPY_TEST_TEXT);

        assertTrue(COPY_TEST_SOURCE_FILE.exists());
        assertFalse(COPY_TEST_TARGET_FILE.exists());

        FileUtils.copyFile(COPY_TEST_SOURCE_FILE, COPY_TEST_TARGET_FILE);

        assertTrue(COPY_TEST_SOURCE_FILE.exists());
        assertTrue(COPY_TEST_TARGET_FILE.exists());
        assertEquals(COPY_TEST_TEXT, diskIo.getString(COPY_TEST_TARGET_FILE));
    }


    /**
     * Tests if copying a non-existing file causes no changes in the
     * filesystem.
     */
    @Test
    public void testFileCopyingWithoutSource()
    {
        assertFalse(COPY_TEST_SOURCE_FILE.exists());
        assertFalse(COPY_TEST_TARGET_FILE.exists());

        FileUtils.copyFile(COPY_TEST_SOURCE_FILE, COPY_TEST_TARGET_FILE);

        assertFalse(COPY_TEST_SOURCE_FILE.exists());
        assertFalse(COPY_TEST_TARGET_FILE.exists());
    }


    /**
     * Tests if copying a file to a destination that already exists
     * will properly overwrite the existing target file.
     */
    @Test
    public void testFileCopyingWithTarget()
    {
        // create two files
        final DiskIO diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        diskIo.writeStringToFile(COPY_TEST_SOURCE_FILE, COPY_TEST_TEXT);
        diskIo.writeStringToFile(COPY_TEST_TARGET_FILE, COPY_TEST_OVERWRITE_TEXT);

        assertEquals(COPY_TEST_TEXT, diskIo.getString(COPY_TEST_SOURCE_FILE));
        assertEquals(COPY_TEST_OVERWRITE_TEXT, diskIo.getString(COPY_TEST_TARGET_FILE));

        FileUtils.copyFile(COPY_TEST_SOURCE_FILE, COPY_TEST_TARGET_FILE);

        assertEquals(COPY_TEST_TEXT, diskIo.getString(COPY_TEST_SOURCE_FILE));
        assertEquals(COPY_TEST_TEXT, diskIo.getString(COPY_TEST_TARGET_FILE));
    }


    /**
     * Tests if copying a file to a destination that already exists
     * will cause no changes in the filesystem, if the target file is currently
     * opened.
     */
    @Test
    public void testFileCopyingWithOpenStream()
    {
        // create source file
        final DiskIO diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        diskIo.writeStringToFile(COPY_TEST_SOURCE_FILE, COPY_TEST_TEXT);
        assertTrue(COPY_TEST_SOURCE_FILE.exists());

        // block target file
        FileUtils.createEmptyFile(COPY_TEST_TARGET_FILE);
        assertTrue(COPY_TEST_TARGET_FILE.exists());

        try
            (InputStreamReader reader = new InputStreamReader(new FileInputStream(COPY_TEST_TARGET_FILE))) {
            FileUtils.copyFile(COPY_TEST_SOURCE_FILE, COPY_TEST_TARGET_FILE);

        } catch (IOException e) {
            assert(false);
        }

        assertEquals(COPY_TEST_TEXT, diskIo.getString(COPY_TEST_SOURCE_FILE));
        assertEquals(0L, COPY_TEST_TARGET_FILE.length());
    }


    /**
     * Tests if file replacement properly overwrites an existing target file,
     * while deleting the source file without leaving behind temporary backup files.
     */
    @Test
    public void testFileReplacementWithTarget()
    {
        // create two files
        final DiskIO diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        diskIo.writeStringToFile(COPY_TEST_SOURCE_FILE, COPY_TEST_TEXT);
        diskIo.writeStringToFile(COPY_TEST_TARGET_FILE, COPY_TEST_OVERWRITE_TEXT);

        assertEquals(COPY_TEST_TEXT, diskIo.getString(COPY_TEST_SOURCE_FILE));
        assertEquals(COPY_TEST_OVERWRITE_TEXT, diskIo.getString(COPY_TEST_TARGET_FILE));

        FileUtils.replaceFile(COPY_TEST_TARGET_FILE, COPY_TEST_SOURCE_FILE);

        assertEquals(COPY_TEST_TEXT, diskIo.getString(COPY_TEST_TARGET_FILE));
        assertFalse(COPY_TEST_SOURCE_FILE.exists());

        // check if backup file has been removed
        assertFalse(new File(COPY_TEST_TARGET_FILE.getPath() + CacheConstants.TEMP_FILE_EXTENSION).exists());
    }


    /**
     * Tests if file replacement properly creates a previously non-existing target file.
     */
    @Test
    public void testFileReplacementWithoutTarget()
    {
        // create two files
        final DiskIO diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        diskIo.writeStringToFile(COPY_TEST_SOURCE_FILE, COPY_TEST_TEXT);

        assertEquals(COPY_TEST_TEXT, diskIo.getString(COPY_TEST_SOURCE_FILE));
        assertFalse(COPY_TEST_TARGET_FILE.exists());

        FileUtils.replaceFile(COPY_TEST_TARGET_FILE, COPY_TEST_SOURCE_FILE);

        assertTrue(COPY_TEST_TARGET_FILE.exists());
        assertEquals(COPY_TEST_TEXT, diskIo.getString(COPY_TEST_TARGET_FILE));
        assertFalse(COPY_TEST_SOURCE_FILE.exists());
    }


    /**
     * Tests if file replacement causes no changes in the file system if
     * the source file does not exist.
     */
    @Test
    public void testFileReplacementWithoutSource()
    {
        assertFalse(COPY_TEST_SOURCE_FILE.exists());
        assertFalse(COPY_TEST_TARGET_FILE.exists());

        FileUtils.replaceFile(COPY_TEST_TARGET_FILE, COPY_TEST_SOURCE_FILE);

        assertFalse(COPY_TEST_SOURCE_FILE.exists());
        assertFalse(COPY_TEST_TARGET_FILE.exists());
    }


    /**
     * Tests if file replacement does not overwrite an existing target file,
     * if it is opened, and does not delete the source file.
     */
    @Test
    public void testFileReplacementWithOpenStream()
    {
        // create source file
        final DiskIO diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        diskIo.writeStringToFile(COPY_TEST_SOURCE_FILE, COPY_TEST_TEXT);
        assertTrue(COPY_TEST_SOURCE_FILE.exists());

        // block target file
        FileUtils.createEmptyFile(COPY_TEST_TARGET_FILE);
        assertTrue(COPY_TEST_TARGET_FILE.exists());

        try
            (InputStreamReader reader = new InputStreamReader(new FileInputStream(COPY_TEST_TARGET_FILE))) {
            FileUtils.replaceFile(COPY_TEST_TARGET_FILE, COPY_TEST_SOURCE_FILE);

        } catch (IOException e) {
            assert(false);
        }

        assertEquals(COPY_TEST_TEXT, diskIo.getString(COPY_TEST_SOURCE_FILE));
        assertTrue(COPY_TEST_TARGET_FILE.exists());
        assertEquals(0L, COPY_TEST_TARGET_FILE.length());

        // check if backup file has been removed
        assertFalse(new File(COPY_TEST_TARGET_FILE.getPath() + CacheConstants.TEMP_FILE_EXTENSION).exists());
    }
}
