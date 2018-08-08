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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Test;

import com.google.gson.Gson;

import ch.qos.logback.classic.Level;
import de.gerdiproject.harvest.utils.FileUtils;
import de.gerdiproject.harvest.utils.cache.constants.CacheConstants;
import de.gerdiproject.harvest.utils.data.DiskIO;
import de.gerdiproject.harvest.utils.logger.constants.LoggerConstants;

/**
 * This class provides test cases for the {@linkplain FileUtils}.
 *
 * @author Robin Weiss
 */
public class FileUtilsTest
{
    private static final File TEST_FILE = new File("mocked/fileUtilsTestDir/fileUtilsTest.file");
    private static final File COPY_TEST_SOURCE_FILE = new File("mocked/fileUtilsTestDir/copyTestFrom/aaa/fileUtilsCopyTestSource.file");
    private static final File COPY_TEST_TARGET_FILE = new File("mocked/fileUtilsTestDir/copyTestTo/bbb/fileUtilsCopyTestTarget.file");

    private static final String COPY_TEST_TEXT = "Milch macht müde Männer munter.";
    private static final String COPY_TEST_OVERWRITE_TEXT = "Ohne Krimi geht die Mimi nie ins Bett.";

    private static final File MERGE_TEST_SOURCE_DIR = new File("mocked/fileUtilsTestDir/mergeTestFrom");
    private static final File MERGE_TEST_TARGET_DIR = new File("mocked/fileUtilsTestDir/mergeTestTo");

    private static final List<File> MERGE_TEST_SOURCE_FILES = Collections.unmodifiableList(Arrays.asList(
                                                                  new File("mocked/fileUtilsTestDir/mergeTestFrom/aaa/first.file"),
                                                                  new File("mocked/fileUtilsTestDir/mergeTestFrom/ccc/fifth.file"),
                                                                  new File("mocked/fileUtilsTestDir/mergeTestFrom/ccc/sixth.file")));

    private static final List<File> MERGE_TEST_MERGED_SOURCE_FILES = Collections.unmodifiableList(Arrays.asList(
                                                                         new File("mocked/fileUtilsTestDir/mergeTestTo/aaa/first.file"),
                                                                         new File("mocked/fileUtilsTestDir/mergeTestTo/ccc/fifth.file"),
                                                                         new File("mocked/fileUtilsTestDir/mergeTestTo/ccc/sixth.file")));

    private static final List<File> MERGE_TEST_TARGET_FILES = Collections.unmodifiableList(Arrays.asList(
                                                                  new File("mocked/fileUtilsTestDir/mergeTestTo/aaa/first.file"),
                                                                  new File("mocked/fileUtilsTestDir/mergeTestTo/aaa/second.file"),
                                                                  new File("mocked/fileUtilsTestDir/mergeTestTo/bbb/third.file"),
                                                                  new File("mocked/fileUtilsTestDir/mergeTestTo/bbb/fourth.file")));

    private static final List<File> MERGE_TEST_EXPECTED_TARGET_FILES = Collections.unmodifiableList(Arrays.asList(
                                                                           new File("mocked/fileUtilsTestDir/mergeTestTo/aaa/first.file"),
                                                                           new File("mocked/fileUtilsTestDir/mergeTestTo/aaa/second.file"),
                                                                           new File("mocked/fileUtilsTestDir/mergeTestTo/bbb/third.file"),
                                                                           new File("mocked/fileUtilsTestDir/mergeTestTo/bbb/fourth.file"),
                                                                           new File("mocked/fileUtilsTestDir/mergeTestTo/ccc/fifth.file"),
                                                                           new File("mocked/fileUtilsTestDir/mergeTestTo/ccc/sixth.file")));

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
     * Disables logging.
     */
    private static void disableLogging()
    {
        LoggerConstants.ROOT_LOGGER.setLevel(Level.OFF);
    }


    /**
     * Enables debug logging.
     */
    private static void enableLogging()
    {
        LoggerConstants.ROOT_LOGGER.setLevel(Level.DEBUG);
    }




    ///////////////////////
    // SINGLE FILE TESTS //
    ///////////////////////


    /**
     * Tests if creating files is reflected in the file system.
     */
    @Test
    public void testFileCreation()
    {
        assert !TEST_FILE.exists();
        FileUtils.createEmptyFile(TEST_FILE);

        assert TEST_FILE.exists();
        assert TEST_FILE.isFile();
        assertEquals(TEST_FILE.length(), 0L);
    }


    /**
     * Tests if creating files overwrites existing files.
     */
    @Test
    public void testFileCreationExisting()
    {
        final DiskIO diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        diskIo.writeStringToFile(TEST_FILE, COPY_TEST_TEXT);

        assert TEST_FILE.exists();
        FileUtils.createEmptyFile(TEST_FILE);

        assert TEST_FILE.exists();
        assert TEST_FILE.isFile();
        assertEquals(TEST_FILE.length(), 0L);
    }


    /**
     * Tests if deleting files is reflected in the file system.
     */
    @Test
    public void testFileDeletion()
    {
        FileUtils.createEmptyFile(TEST_FILE);
        assert TEST_FILE.exists();
        FileUtils.deleteFile(TEST_FILE);
        assert !TEST_FILE.exists();
    }


    /**
     * Tests if deleting non-existing files causes no exception.
     */
    @Test
    public void testFileDeletionNonExisting()
    {
        assert !TEST_FILE.exists();

        try {
            FileUtils.deleteFile(TEST_FILE);
        } catch (Exception e) {
            assert false;
        }

        assert !TEST_FILE.exists();
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

        assert COPY_TEST_SOURCE_FILE.exists();
        assert !COPY_TEST_TARGET_FILE.exists();

        FileUtils.copyFile(COPY_TEST_SOURCE_FILE, COPY_TEST_TARGET_FILE);

        assert COPY_TEST_SOURCE_FILE.exists();
        assert COPY_TEST_TARGET_FILE.exists();
        assertEquals(COPY_TEST_TEXT, diskIo.getString(COPY_TEST_TARGET_FILE));
    }


    /**
     * Tests if copying a non-existing file causes no changes in the
     * filesystem.
     */
    @Test
    public void testFileCopyingWithoutSource()
    {
        assert !COPY_TEST_SOURCE_FILE.exists();
        assert !COPY_TEST_TARGET_FILE.exists();

        disableLogging();
        FileUtils.copyFile(COPY_TEST_SOURCE_FILE, COPY_TEST_TARGET_FILE);
        enableLogging();

        assert !COPY_TEST_SOURCE_FILE.exists();
        assert !COPY_TEST_TARGET_FILE.exists();
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
        assert COPY_TEST_SOURCE_FILE.exists();

        // block target file
        FileUtils.createEmptyFile(COPY_TEST_TARGET_FILE);
        assert COPY_TEST_TARGET_FILE.exists();

        try
            (InputStreamReader reader = new InputStreamReader(new FileInputStream(COPY_TEST_TARGET_FILE))) {
            disableLogging();
            FileUtils.copyFile(COPY_TEST_SOURCE_FILE, COPY_TEST_TARGET_FILE);

        } catch (IOException e) {
            assert false;
        }

        enableLogging();

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
        assert !COPY_TEST_SOURCE_FILE.exists();

        // check if backup file has been removed
        assert !new File(COPY_TEST_TARGET_FILE.getPath() + CacheConstants.TEMP_FILE_EXTENSION).exists();
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
        assert !COPY_TEST_TARGET_FILE.exists();

        FileUtils.replaceFile(COPY_TEST_TARGET_FILE, COPY_TEST_SOURCE_FILE);

        assert COPY_TEST_TARGET_FILE.exists();
        assertEquals(COPY_TEST_TEXT, diskIo.getString(COPY_TEST_TARGET_FILE));
        assert !COPY_TEST_SOURCE_FILE.exists();
    }


    /**
     * Tests if file replacement causes no changes in the file system if
     * the source file does not exist.
     */
    @Test
    public void testFileReplacementWithoutSource()
    {
        assert !COPY_TEST_SOURCE_FILE.exists();
        assert !COPY_TEST_TARGET_FILE.exists();

        disableLogging();
        FileUtils.replaceFile(COPY_TEST_TARGET_FILE, COPY_TEST_SOURCE_FILE);
        enableLogging();

        assert !COPY_TEST_SOURCE_FILE.exists();
        assert !COPY_TEST_TARGET_FILE.exists();
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
        assert COPY_TEST_SOURCE_FILE.exists();

        // block target file
        FileUtils.createEmptyFile(COPY_TEST_TARGET_FILE);
        assert COPY_TEST_TARGET_FILE.exists();

        try
            (InputStreamReader reader = new InputStreamReader(new FileInputStream(COPY_TEST_TARGET_FILE))) {
            disableLogging();
            FileUtils.replaceFile(COPY_TEST_TARGET_FILE, COPY_TEST_SOURCE_FILE);

        } catch (IOException e) {
            assert false;
        }

        enableLogging();

        assertEquals(COPY_TEST_TEXT, diskIo.getString(COPY_TEST_SOURCE_FILE));
        assert COPY_TEST_TARGET_FILE.exists();
        assertEquals(0L, COPY_TEST_TARGET_FILE.length());

        // check if backup file has been removed
        assert !new File(COPY_TEST_TARGET_FILE.getPath() + CacheConstants.TEMP_FILE_EXTENSION).exists();
    }



    /////////////////////
    // DIRECTORY TESTS //
    /////////////////////


    /**
     * Tests if multiple directories can be created in the
     * file system.
     */
    @Test
    public void testDirectoryCreation()
    {
        FileUtils.createDirectories(TEST_MULTI_DIRECTORY);
        assert TEST_MULTI_DIRECTORY.exists();
        assert TEST_MULTI_DIRECTORY.isDirectory();
    }


    /**
     * Tests if creating the same directory structure twice
     * throws no exceptions.
     */
    @Test
    public void testDirectoryCreationExisting()
    {
        assert !TEST_MULTI_DIRECTORY.exists();
        FileUtils.createDirectories(TEST_MULTI_DIRECTORY);

        try {
            FileUtils.createDirectories(TEST_MULTI_DIRECTORY);
        } catch (Exception e) {
            assert false;
        }

        assert TEST_MULTI_DIRECTORY.exists();
    }


    /**
     * Tests if a directory structure can be deleted
     * from the filesystem.
     */
    @Test
    public void testDirectoryDeletion()
    {
        FileUtils.createDirectories(TEST_MULTI_DIRECTORY);
        assert TEST_MULTI_DIRECTORY.exists();

        FileUtils.deleteFile(TEST_DIRECTORY);

        assert !TEST_MULTI_DIRECTORY.exists();
        assert !TEST_DIRECTORY.exists();
    }


    /**
     * Tests if deleting a non-existing directory
     * throws no exceptions.
     */
    @Test
    public void testDirectoryDeletionNonExisting()
    {
        assert !TEST_DIRECTORY.exists();

        try {
            FileUtils.deleteFile(TEST_DIRECTORY);
        } catch (Exception e) {
            assert false;
        }

        assert !TEST_DIRECTORY.exists();
    }


    /**
     * Tests if copying a directory to a non-existing destination
     * causes the corresponding changes in the filesystem.
     */
    @Test
    public void testDirectoryCopyingWithoutTarget()
    {
        // create source file
        final DiskIO diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        diskIo.writeStringToFile(COPY_TEST_SOURCE_FILE, COPY_TEST_TEXT);

        assert COPY_TEST_SOURCE_FILE.getParentFile().exists();
        assert !COPY_TEST_TARGET_FILE.getParentFile().exists();

        FileUtils.copyFile(COPY_TEST_SOURCE_FILE.getParentFile(), COPY_TEST_TARGET_FILE.getParentFile());

        assert COPY_TEST_SOURCE_FILE.exists();
        final File targetFile = new File(COPY_TEST_TARGET_FILE.getParentFile(), COPY_TEST_SOURCE_FILE.getName());
        assert targetFile.exists();
        assertEquals(COPY_TEST_TEXT, diskIo.getString(targetFile));
    }


    /**
     * Tests if copying a non-existing directory causes no changes in the
     * filesystem.
     */
    @Test
    public void testDirectoryCopyingWithoutSource()
    {
        assert !COPY_TEST_SOURCE_FILE.getParentFile().exists();
        assert !COPY_TEST_TARGET_FILE.getParentFile().exists();

        disableLogging();
        FileUtils.copyFile(COPY_TEST_SOURCE_FILE.getParentFile(), COPY_TEST_TARGET_FILE.getParentFile());
        enableLogging();

        assert !COPY_TEST_SOURCE_FILE.getParentFile().exists();
        assert !COPY_TEST_TARGET_FILE.getParentFile().exists();
    }


    /**
     * Tests if copying a directory to a destination that already exists
     * will properly overwrite the existing target directory.
     */
    @Test
    public void testDirectoryCopyingWithTarget()
    {
        final File targetFile = new File(COPY_TEST_TARGET_FILE.getParentFile(), COPY_TEST_SOURCE_FILE.getName());

        // create two files
        final DiskIO diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        diskIo.writeStringToFile(COPY_TEST_SOURCE_FILE, COPY_TEST_TEXT);
        diskIo.writeStringToFile(targetFile, COPY_TEST_OVERWRITE_TEXT);

        assertEquals(COPY_TEST_TEXT, diskIo.getString(COPY_TEST_SOURCE_FILE));
        assertEquals(COPY_TEST_OVERWRITE_TEXT, diskIo.getString(targetFile));

        FileUtils.copyFile(COPY_TEST_SOURCE_FILE.getParentFile(), targetFile.getParentFile());

        assertEquals(COPY_TEST_TEXT, diskIo.getString(COPY_TEST_SOURCE_FILE));
        assertEquals(COPY_TEST_TEXT, diskIo.getString(targetFile));
    }


    /**
     * Tests if directory replacement properly overwrites an existing target directory,
     * while deleting the source directory without leaving behind temporary backup files.
     */
    @Test
    public void testDirectoryReplacementWithTarget()
    {

        final File targetFile = new File(COPY_TEST_TARGET_FILE.getParentFile(), COPY_TEST_SOURCE_FILE.getName());

        // create two files
        final DiskIO diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        diskIo.writeStringToFile(COPY_TEST_SOURCE_FILE, COPY_TEST_TEXT);
        diskIo.writeStringToFile(targetFile, COPY_TEST_OVERWRITE_TEXT);

        assertEquals(COPY_TEST_TEXT, diskIo.getString(COPY_TEST_SOURCE_FILE));
        assertEquals(COPY_TEST_OVERWRITE_TEXT, diskIo.getString(targetFile));

        FileUtils.replaceFile(targetFile.getParentFile(), COPY_TEST_SOURCE_FILE.getParentFile());

        assertEquals(COPY_TEST_TEXT, diskIo.getString(targetFile));
        assert !COPY_TEST_SOURCE_FILE.getParentFile().exists();

        // check if backup file has been removed
        assert !new File(targetFile.getPath() + CacheConstants.TEMP_FILE_EXTENSION).exists();
    }


    /**
     * Tests if directory replacement properly creates a previously non-existing target directory.
     */
    @Test
    public void testDirectoryReplacementWithoutTarget()
    {
        final File targetFile = new File(COPY_TEST_TARGET_FILE.getParentFile(), COPY_TEST_SOURCE_FILE.getName());

        // create two files
        final DiskIO diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        diskIo.writeStringToFile(COPY_TEST_SOURCE_FILE, COPY_TEST_TEXT);

        assertEquals(COPY_TEST_TEXT, diskIo.getString(COPY_TEST_SOURCE_FILE));
        assert !targetFile.getParentFile().exists();

        FileUtils.replaceFile(targetFile.getParentFile(), COPY_TEST_SOURCE_FILE.getParentFile());

        assert targetFile.getParentFile().exists();
        assertEquals(COPY_TEST_TEXT, diskIo.getString(targetFile));
        assert !COPY_TEST_SOURCE_FILE.getParentFile().exists();
    }


    /**
     * Tests if directory replacement causes no changes in the file system if
     * the source directory does not exist.
     */
    @Test
    public void testDirectoryReplacementWithoutSource()
    {
        assert !COPY_TEST_SOURCE_FILE.getParentFile().exists();
        assert !COPY_TEST_TARGET_FILE.getParentFile().exists();

        disableLogging();
        FileUtils.replaceFile(COPY_TEST_TARGET_FILE.getParentFile(), COPY_TEST_SOURCE_FILE.getParentFile());
        enableLogging();

        assert !COPY_TEST_SOURCE_FILE.getParentFile().exists();
        assert !COPY_TEST_TARGET_FILE.getParentFile().exists();
    }


    /**
     * Tests if a directory can be merged into another existing one,
     * while replacing existing target files in the process.
     */
    @Test
    public void testReplacingDirectoryMergeWithTarget()
    {
        testDirectoryMergeWithTarget(true);
    }


    /**
     * Tests if a directory can be merged into another existing one,
     * while preserving existing target files in the process.
     */
    @Test
    public void testNonReplacingDirectoryMergeWithTarget()
    {
        testDirectoryMergeWithTarget(false);
    }


    /**
     * Tests if a directory can be integrated into another existing one.
     *
     * @param replaceFiles if true, existing files may be overridden
     */
    private void testDirectoryMergeWithTarget(boolean replaceFiles)
    {
        final DiskIO diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);

        for (File sourceFile : MERGE_TEST_SOURCE_FILES)
            diskIo.writeStringToFile(sourceFile, COPY_TEST_TEXT);

        for (File targetFile : MERGE_TEST_TARGET_FILES)
            diskIo.writeStringToFile(targetFile, COPY_TEST_OVERWRITE_TEXT);

        assert MERGE_TEST_SOURCE_DIR.exists();
        assert MERGE_TEST_TARGET_DIR.exists();

        FileUtils.integrateDirectory(MERGE_TEST_SOURCE_DIR, MERGE_TEST_TARGET_DIR, replaceFiles);

        assert !MERGE_TEST_SOURCE_DIR.exists();
        assert MERGE_TEST_TARGET_DIR.exists();

        for (File targetFile : MERGE_TEST_EXPECTED_TARGET_FILES)
            assert targetFile.exists();

        final File possiblyReplacedFile = MERGE_TEST_TARGET_FILES.get(0);

        if (replaceFiles)
            assertEquals(COPY_TEST_TEXT, diskIo.getString(possiblyReplacedFile));
        else
            assertEquals(COPY_TEST_OVERWRITE_TEXT, diskIo.getString(possiblyReplacedFile));
    }


    /**
     * Tests if a new directory is created when there is no target directory
     * when integrating. The replacement flag is checked.
     */
    @Test
    public void testDirectoryMergeWithoutTarget()
    {
        final DiskIO diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);

        for (File sourceFile : MERGE_TEST_SOURCE_FILES)
            diskIo.writeStringToFile(sourceFile, COPY_TEST_TEXT);

        assert MERGE_TEST_SOURCE_DIR.exists();
        assert !MERGE_TEST_TARGET_DIR.exists();

        FileUtils.integrateDirectory(MERGE_TEST_SOURCE_DIR, MERGE_TEST_TARGET_DIR, false);

        assert !MERGE_TEST_SOURCE_DIR.exists();
        assert MERGE_TEST_TARGET_DIR.exists();

        for (File targetFile : MERGE_TEST_MERGED_SOURCE_FILES)
            assertEquals(COPY_TEST_TEXT, diskIo.getString(targetFile));
    }


    /**
     * Tests if there are no changes in the filesystem if a non-existing
     * directory is attempted to be integrated into another one.
     */
    @Test
    public void testDirectoryIntegrationWithoutSource()
    {
        assert !MERGE_TEST_SOURCE_DIR.exists();
        assert !MERGE_TEST_TARGET_DIR.exists();

        disableLogging();
        FileUtils.integrateDirectory(MERGE_TEST_SOURCE_DIR, MERGE_TEST_TARGET_DIR, false);
        enableLogging();

        assert !MERGE_TEST_SOURCE_DIR.exists();
        assert !MERGE_TEST_TARGET_DIR.exists();
    }
}
