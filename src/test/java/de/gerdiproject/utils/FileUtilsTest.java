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
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
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
    private static final File TEST_FOLDER = new File("mocked");
    private static final File FILE_TEST_FOLDER = new File("mocked/fileUtilsTestDir");
    private static final File TEST_MULTI_DIRECTORY = new File(FILE_TEST_FOLDER, "moarTests/moar");

    private static final File TEST_FILE = new File(FILE_TEST_FOLDER, "fileUtilsTest.file");

    private static final File COPY_TEST_SOURCE_DIR = new File(FILE_TEST_FOLDER, "copyTestFrom/aaa/");
    private static final File COPY_TEST_TARGET_DIR = new File(FILE_TEST_FOLDER, "copyTestTo/bbb/");

    private static final File COPY_TEST_SOURCE_FILE = new File(COPY_TEST_SOURCE_DIR, "fileUtilsCopyTestSource.file");
    private static final File COPY_TEST_TARGET_FILE = new File(COPY_TEST_TARGET_DIR, "fileUtilsCopyTestTarget.file");


    private static final String COPY_TEST_TEXT = "Milch macht müde Männer munter.";
    private static final String COPY_TEST_OVERWRITE_TEXT = "Ohne Krimi geht die Mimi nie ins Bett.";

    private static final File MERGE_TEST_SOURCE_DIR = new File(FILE_TEST_FOLDER, "mergeTestFrom");
    private static final File MERGE_TEST_TARGET_DIR = new File(FILE_TEST_FOLDER, "mergeTestTo");

    private static final List<File> MERGE_TEST_SOURCE_FILES =
        Collections.unmodifiableList(
            Arrays.asList(
                new File(FILE_TEST_FOLDER, "mergeTestFrom/aaa/first.file"),
                new File(FILE_TEST_FOLDER, "mergeTestFrom/ccc/fifth.file"),
                new File(FILE_TEST_FOLDER, "mergeTestFrom/ccc/sixth.file")));

    private static final List<File> MERGE_TEST_MERGED_SOURCE_FILES =
        Collections.unmodifiableList(
            Arrays.asList(
                new File(FILE_TEST_FOLDER, "mergeTestTo/aaa/first.file"),
                new File(FILE_TEST_FOLDER, "mergeTestTo/ccc/fifth.file"),
                new File(FILE_TEST_FOLDER, "mergeTestTo/ccc/sixth.file")));

    private static final List<File> MERGE_TEST_TARGET_FILES =
        Collections.unmodifiableList(
            Arrays.asList(
                new File(FILE_TEST_FOLDER, "mergeTestTo/aaa/first.file"),
                new File(FILE_TEST_FOLDER, "mergeTestTo/aaa/second.file"),
                new File(FILE_TEST_FOLDER, "mergeTestTo/bbb/third.file"),
                new File(FILE_TEST_FOLDER, "mergeTestTo/bbb/fourth.file")));

    private static final List<File> MERGE_TEST_EXPECTED_TARGET_FILES =
        Collections.unmodifiableList(
            Arrays.asList(
                new File(FILE_TEST_FOLDER, "mergeTestTo/aaa/first.file"),
                new File(FILE_TEST_FOLDER, "mergeTestTo/aaa/second.file"),
                new File(FILE_TEST_FOLDER, "mergeTestTo/bbb/third.file"),
                new File(FILE_TEST_FOLDER, "mergeTestTo/bbb/fourth.file"),
                new File(FILE_TEST_FOLDER, "mergeTestTo/ccc/fifth.file"),
                new File(FILE_TEST_FOLDER, "mergeTestTo/ccc/sixth.file")));

    private static final String DUPLICATE_DIR_ERROR = "Creating directories that already exist, should not cause exceptions";
    private static final String DELETE_DIR_ERROR = "Deleting non-existing directories, should not cause exceptions";
    private static final String DELETE_FILE_ERROR = "Deleting non-existing files, should not cause exceptions";

    private final Level initialLogLevel = LoggerConstants.ROOT_LOGGER.getLevel();


    /**
     * Verifies that cache files are deleted.
     *
     * @throws IOException thrown when the temporary cache folder could not be deleted
     */
    @Before
    public void before() throws IOException
    {
        FileUtils.deleteFile(TEST_FOLDER);

        if (TEST_FOLDER.exists())
            throw new IOException();
    }


    /**
     * Cleans up the entire directory of test files after each test.
     */
    @After
    public void after()
    {
        FileUtils.deleteFile(TEST_FOLDER);
    }



    ///////////////////////
    // SINGLE FILE TESTS //
    ///////////////////////


    /**
     * Tests if creating files actually creates files...
     */
    @Test
    public void testFileCreation()
    {
        FileUtils.createEmptyFile(TEST_FILE);
        assert TEST_FILE.exists() && TEST_FILE.isFile();
    }


    /**
     * Tests if creating files overwrites existing files.
     */
    @Test
    public void testFileCreationExisting()
    {
        // write something to a file
        final DiskIO diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        diskIo.writeStringToFile(TEST_FILE, COPY_TEST_TEXT);

        // overwrite full file with an empty one
        FileUtils.createEmptyFile(TEST_FILE);

        assertEquals(TEST_FILE.length(), 0L);
    }


    /**
     * Tests if deleting files is reflected in the file system.
     */
    @Test
    public void testFileDeletion()
    {
        FileUtils.createEmptyFile(TEST_FILE);
        FileUtils.deleteFile(TEST_FILE);

        assert !TEST_FILE.exists();
    }


    /**
     * Tests if deleting non-existing files causes no exception.
     */
    @Test
    public void testFileDeletionNonExisting()
    {
        try {
            FileUtils.deleteFile(TEST_FILE);
        } catch (Exception e) {
            fail(DELETE_FILE_ERROR);
        }
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

        FileUtils.copyFile(COPY_TEST_SOURCE_FILE, COPY_TEST_TARGET_FILE);

        assertEquals(COPY_TEST_TEXT, diskIo.getString(COPY_TEST_TARGET_FILE));
    }


    /**
     * Tests if copying a non-existing file causes no changes in the
     * filesystem.
     */
    @Test
    public void testFileCopyingWithoutSource()
    {
        setLoggerEnabled(false);
        FileUtils.copyFile(COPY_TEST_SOURCE_FILE, COPY_TEST_TARGET_FILE);
        setLoggerEnabled(true);

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

        FileUtils.copyFile(COPY_TEST_SOURCE_FILE, COPY_TEST_TARGET_FILE);

        assertEquals(COPY_TEST_TEXT, diskIo.getString(COPY_TEST_TARGET_FILE));
    }


    /**
     * Tests if copying a file to a destination that already exists
     * will cause no changes in the filesystem, if the target file is currently
     * opened.
     *
     * throws IOException thrown when the copy target could not be read
     */
    @Test
    public void testFileCopyingWithOpenStream() throws IOException
    {
        // create source file
        final DiskIO diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        diskIo.writeStringToFile(COPY_TEST_SOURCE_FILE, COPY_TEST_TEXT);

        // open a reader to block the file from being copied
        FileUtils.createEmptyFile(COPY_TEST_TARGET_FILE);

        try
            (InputStreamReader reader = new InputStreamReader(new FileInputStream(COPY_TEST_TARGET_FILE), StandardCharsets.UTF_8)) {
            setLoggerEnabled(false);
            FileUtils.copyFile(COPY_TEST_SOURCE_FILE, COPY_TEST_TARGET_FILE);
            setLoggerEnabled(true);
        }

        assertEquals(0L, COPY_TEST_TARGET_FILE.length());
    }


    /**
     * Tests if file replacement removes the source file.
     */
    @Test
    public void testIfFileReplacementRemovesSourceFile()
    {
        // create two files
        final DiskIO diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        diskIo.writeStringToFile(COPY_TEST_SOURCE_FILE, COPY_TEST_TEXT);
        diskIo.writeStringToFile(COPY_TEST_TARGET_FILE, COPY_TEST_OVERWRITE_TEXT);

        FileUtils.replaceFile(COPY_TEST_TARGET_FILE, COPY_TEST_SOURCE_FILE);

        assert !COPY_TEST_SOURCE_FILE.exists();
    }


    /**
     * Tests if file replacement properly overwrites an existing target file.
     */
    @Test
    public void testFileReplacementReplacesTargetFile()
    {
        // create two files
        final DiskIO diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        diskIo.writeStringToFile(COPY_TEST_SOURCE_FILE, COPY_TEST_TEXT);
        diskIo.writeStringToFile(COPY_TEST_TARGET_FILE, COPY_TEST_OVERWRITE_TEXT);

        FileUtils.replaceFile(COPY_TEST_TARGET_FILE, COPY_TEST_SOURCE_FILE);

        assertEquals(COPY_TEST_TEXT, diskIo.getString(COPY_TEST_TARGET_FILE));
    }


    /**
     * Tests if file replacement does not leave behind temporary backup files.
     */
    @Test
    public void testFileReplacementRemovesBackupFile()
    {
        // create two files
        final DiskIO diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        diskIo.writeStringToFile(COPY_TEST_SOURCE_FILE, COPY_TEST_TEXT);
        diskIo.writeStringToFile(COPY_TEST_TARGET_FILE, COPY_TEST_OVERWRITE_TEXT);

        FileUtils.replaceFile(COPY_TEST_TARGET_FILE, COPY_TEST_SOURCE_FILE);

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

        FileUtils.replaceFile(COPY_TEST_TARGET_FILE, COPY_TEST_SOURCE_FILE);

        assertEquals(COPY_TEST_TEXT, diskIo.getString(COPY_TEST_TARGET_FILE));
    }


    /**
     * Tests if file replacement causes no file to be created if
     * the source file does not exist.
     */
    @Test
    public void testFileReplacementWithoutSource()
    {
        setLoggerEnabled(false);
        FileUtils.replaceFile(COPY_TEST_TARGET_FILE, COPY_TEST_SOURCE_FILE);
        setLoggerEnabled(true);

        assert !COPY_TEST_TARGET_FILE.exists();
    }


    /**
     * Tests if file replacement does not overwrite an existing target file,
     * if it is opened, and does not delete the source file.
     *
     * @throws IOException thrown when the target file could not be opened
     */
    @Test
    public void testFileReplacementWithOpenStream() throws IOException
    {
        // create source file
        final DiskIO diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        diskIo.writeStringToFile(COPY_TEST_SOURCE_FILE, COPY_TEST_TEXT);

        // block target file
        FileUtils.createEmptyFile(COPY_TEST_TARGET_FILE);

        try
            (InputStreamReader reader = new InputStreamReader(new FileInputStream(COPY_TEST_TARGET_FILE), StandardCharsets.UTF_8)) {
            setLoggerEnabled(false);
            FileUtils.replaceFile(COPY_TEST_TARGET_FILE, COPY_TEST_SOURCE_FILE);
            setLoggerEnabled(true);
        }

        assertEquals(0L, COPY_TEST_TARGET_FILE.length());
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
        assert TEST_MULTI_DIRECTORY.exists() && TEST_MULTI_DIRECTORY.isDirectory();
    }


    /**
     * Tests if creating the same directory structure twice
     * throws no exceptions.
     */
    @Test
    public void testDirectoryCreationExisting()
    {
        FileUtils.createDirectories(TEST_MULTI_DIRECTORY);

        try {
            FileUtils.createDirectories(TEST_MULTI_DIRECTORY);
        } catch (Exception e) {
            fail(DUPLICATE_DIR_ERROR);
        }
    }


    /**
     * Tests if a directory structure can be deleted
     * from the filesystem.
     */
    @Test
    public void testDirectoryDeletion()
    {
        FileUtils.createDirectories(TEST_MULTI_DIRECTORY);
        FileUtils.deleteFile(FILE_TEST_FOLDER);

        assert !FILE_TEST_FOLDER.exists();
    }


    /**
     * Tests if deleting a non-existing directory
     * throws no exceptions.
     */
    @Test
    public void testDirectoryDeletionNonExisting()
    {
        try {
            FileUtils.deleteFile(FILE_TEST_FOLDER);
        } catch (Exception e) {
            fail(DELETE_DIR_ERROR);
        }
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

        // copy the folder in which the source file resides
        FileUtils.copyFile(COPY_TEST_SOURCE_DIR, COPY_TEST_TARGET_DIR);

        final File targetFile = new File(COPY_TEST_TARGET_DIR, COPY_TEST_SOURCE_FILE.getName());
        assertEquals(COPY_TEST_TEXT, diskIo.getString(targetFile));
    }


    /**
     * Tests if copying a non-existing directory causes no changes in the
     * filesystem.
     */
    @Test
    public void testDirectoryCopyingWithoutSource()
    {
        setLoggerEnabled(false);
        FileUtils.copyFile(COPY_TEST_SOURCE_DIR, COPY_TEST_TARGET_DIR);
        setLoggerEnabled(true);

        assert !COPY_TEST_TARGET_DIR.exists();
    }


    /**
     * Tests if copying a directory to a destination that already exists
     * will properly overwrite the existing target directory.
     */
    @Test
    public void testDirectoryCopyingWithTarget()
    {
        final File targetFile = new File(COPY_TEST_TARGET_DIR, COPY_TEST_SOURCE_FILE.getName());

        // create two files
        final DiskIO diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        diskIo.writeStringToFile(COPY_TEST_SOURCE_FILE, COPY_TEST_TEXT);
        diskIo.writeStringToFile(targetFile, COPY_TEST_OVERWRITE_TEXT);

        FileUtils.copyFile(COPY_TEST_SOURCE_DIR, COPY_TEST_TARGET_DIR);

        assertEquals(COPY_TEST_TEXT, diskIo.getString(targetFile));
    }


    /**
     * Tests if directory replacement properly overwrites files in an existing target directory.
     */
    @Test
    public void testDirectoryReplacementFileOverwrite()
    {
        final File targetFile = new File(COPY_TEST_TARGET_FILE.getParentFile(), COPY_TEST_SOURCE_FILE.getName());

        // create two files
        final DiskIO diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        diskIo.writeStringToFile(COPY_TEST_SOURCE_FILE, COPY_TEST_TEXT);
        diskIo.writeStringToFile(targetFile, COPY_TEST_OVERWRITE_TEXT);

        FileUtils.replaceFile(targetFile.getParentFile(), COPY_TEST_SOURCE_FILE.getParentFile());

        assertEquals(COPY_TEST_TEXT, diskIo.getString(targetFile));
    }

    /**
     * Tests if directory replacement deletes the source directory.
     */
    @Test
    public void testDirectoryReplacementSourceDeletion()
    {
        final File targetFile = new File(COPY_TEST_TARGET_FILE.getParentFile(), COPY_TEST_SOURCE_FILE.getName());

        // create two files
        final DiskIO diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        diskIo.writeStringToFile(COPY_TEST_SOURCE_FILE, COPY_TEST_TEXT);
        diskIo.writeStringToFile(targetFile, COPY_TEST_OVERWRITE_TEXT);

        FileUtils.replaceFile(targetFile.getParentFile(), COPY_TEST_SOURCE_FILE.getParentFile());

        assert !COPY_TEST_SOURCE_FILE.getParentFile().exists();
    }

    /**
     * Tests if directory replacement does not leave behind temporary backup files.
     */
    @Test
    public void testDirectoryReplacementCleanUp()
    {
        final File targetFile = new File(COPY_TEST_TARGET_FILE.getParentFile(), COPY_TEST_SOURCE_FILE.getName());

        // create two files
        final DiskIO diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        diskIo.writeStringToFile(COPY_TEST_SOURCE_FILE, COPY_TEST_TEXT);
        diskIo.writeStringToFile(targetFile, COPY_TEST_OVERWRITE_TEXT);

        FileUtils.replaceFile(targetFile.getParentFile(), COPY_TEST_SOURCE_FILE.getParentFile());

        // check if backup file has been removed
        assert !new File(targetFile.getPath() + CacheConstants.TEMP_FILE_EXTENSION).exists();
    }


    /**
     * Tests if directory replacement properly creates a previously non-existing target directory.
     */
    @Test
    public void testDirectoryReplacementWithoutTarget()
    {
        final File targetFile = new File(COPY_TEST_TARGET_DIR, COPY_TEST_SOURCE_FILE.getName());

        // create two files
        final DiskIO diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        diskIo.writeStringToFile(COPY_TEST_SOURCE_FILE, COPY_TEST_TEXT);

        FileUtils.replaceFile(COPY_TEST_TARGET_DIR, COPY_TEST_SOURCE_DIR);

        assertEquals(COPY_TEST_TEXT, diskIo.getString(targetFile));
    }


    /**
     * Tests if directory replacement causes no changes in the file system if
     * the source directory does not exist.
     */
    @Test
    public void testDirectoryReplacementWithoutSource()
    {
        setLoggerEnabled(false);
        FileUtils.replaceFile(COPY_TEST_TARGET_DIR, COPY_TEST_SOURCE_DIR);
        setLoggerEnabled(true);

        assert !COPY_TEST_TARGET_DIR.exists();
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
     * Tests if a new directory is created when there is no target directory
     * when integrating. The replacement flag is checked.
     */
    @Test
    public void testDirectoryMergeWithoutTarget()
    {
        final DiskIO diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);

        for (File sourceFile : MERGE_TEST_SOURCE_FILES)
            diskIo.writeStringToFile(sourceFile, COPY_TEST_TEXT);

        FileUtils.integrateDirectory(MERGE_TEST_SOURCE_DIR, MERGE_TEST_TARGET_DIR, false);

        for (File targetFile : MERGE_TEST_MERGED_SOURCE_FILES)
            assertEquals(COPY_TEST_TEXT, diskIo.getString(targetFile));
    }


    /**
     * Tests directory integration when there is no target directory and checks
     * if all source files are properly removed.
     */
    @Test
    public void testDirectoryMergeWithoutTargetRemovingOldDirectory()
    {
        final DiskIO diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);

        for (File sourceFile : MERGE_TEST_SOURCE_FILES)
            diskIo.writeStringToFile(sourceFile, COPY_TEST_TEXT);

        FileUtils.integrateDirectory(MERGE_TEST_SOURCE_DIR, MERGE_TEST_TARGET_DIR, false);

        for (File sourceFile : MERGE_TEST_SOURCE_FILES)
            assert !sourceFile.exists();
    }


    /**
     * Tests if there are no changes in the filesystem if a non-existing
     * directory is attempted to be integrated into another one.
     */
    @Test
    public void testDirectoryMergeWithoutSource()
    {
        setLoggerEnabled(false);
        FileUtils.integrateDirectory(MERGE_TEST_SOURCE_DIR, MERGE_TEST_TARGET_DIR, false);
        setLoggerEnabled(true);

        assert !MERGE_TEST_TARGET_DIR.exists();
    }


    /**
     * Tests if all source files of a directory merge get moved over to
     * the target directory.
     */
    @Test
    public void testDirectoryMergeCompletion()
    {
        final DiskIO diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);

        for (File sourceFile : MERGE_TEST_SOURCE_FILES)
            diskIo.writeStringToFile(sourceFile, COPY_TEST_TEXT);

        for (File targetFile : MERGE_TEST_TARGET_FILES)
            diskIo.writeStringToFile(targetFile, COPY_TEST_OVERWRITE_TEXT);

        FileUtils.integrateDirectory(MERGE_TEST_SOURCE_DIR, MERGE_TEST_TARGET_DIR, false);

        assert !MERGE_TEST_SOURCE_DIR.exists();

        for (File targetFile : MERGE_TEST_EXPECTED_TARGET_FILES)
            assert targetFile.exists();
    }


    //////////////////////
    // Non-test Methods //
    //////////////////////

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

        FileUtils.integrateDirectory(MERGE_TEST_SOURCE_DIR, MERGE_TEST_TARGET_DIR, replaceFiles);

        final File possiblyReplacedFile = MERGE_TEST_TARGET_FILES.get(0);
        final String expectedFileContent = replaceFiles ? COPY_TEST_TEXT : COPY_TEST_OVERWRITE_TEXT;
        assertEquals(expectedFileContent, diskIo.getString(possiblyReplacedFile));
    }


    /**
     * Enables or disables the logger.
     *
     * @param state if true, the logger is enabled
     */
    protected void setLoggerEnabled(boolean state)
    {
        final Level newLevel = state ? initialLogLevel : Level.OFF;
        LoggerConstants.ROOT_LOGGER.setLevel(newLevel);
    }
}
