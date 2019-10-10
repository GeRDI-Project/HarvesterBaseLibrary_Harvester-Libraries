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
package de.gerdiproject.harvest.utils;  // NOPMD JUnit 4 requires many static imports

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.google.gson.Gson;

import de.gerdiproject.harvest.AbstractUnitTest;
import de.gerdiproject.harvest.utils.data.DiskIO;
import de.gerdiproject.harvest.utils.file.FileUtils;
import de.gerdiproject.harvest.utils.file.constants.FileConstants;

/**
 * This class provides test cases for the {@linkplain FileUtils}.
 *
 * @author Robin Weiss
 */
public class FileUtilsTest extends AbstractUnitTest
{
    private static final String DUPLICATE_DIR_ERROR = "Creating directories that already exist, should not cause exceptions";
    private static final String DELETE_DIR_ERROR = "Deleting non-existing directories, should not cause exceptions";
    private static final String DELETE_FILE_ERROR = "Deleting non-existing files, should not cause exceptions";

    private final File fileTestFolder = getTemporaryTestDirectory();
    private final File multiTestDir = new File(fileTestFolder, "moarTests/moar");

    private final File testFile = new File(fileTestFolder, "fileUtilsTest.file");

    private final File copyTestSourceDir = new File(fileTestFolder, "copyTestFrom/aaa/");
    private final File copyTestTargetDir = new File(fileTestFolder, "copyTestTo/bbb/");

    private final File copyTestSourceFile = new File(copyTestSourceDir, "fileUtilsCopyTestSource.file");
    private final File copyTestTargetFile = new File(copyTestTargetDir, "fileUtilsCopyTestTarget.file");

    private final String copyTestText = "Milch macht müde Männer munter.";
    private final String copyTestOverwriteText = "Ohne Krimi geht die Mimi nie ins Bett.";

    private final File mergeTestSourceDir = new File(fileTestFolder, "mergeTestFrom");
    private final File mergeTestTargetDir = new File(fileTestFolder, "mergeTestTo");

    private final List<File> mergeTestSourceFiles =
        Collections.unmodifiableList(
            Arrays.asList(
                new File(fileTestFolder, "mergeTestFrom/aaa/first.file"),
                new File(fileTestFolder, "mergeTestFrom/ccc/fifth.file"),
                new File(fileTestFolder, "mergeTestFrom/ccc/sixth.file")));

    private final List<File> mergeTestMergedSourceFiles =
        Collections.unmodifiableList(
            Arrays.asList(
                new File(fileTestFolder, "mergeTestTo/aaa/first.file"),
                new File(fileTestFolder, "mergeTestTo/ccc/fifth.file"),
                new File(fileTestFolder, "mergeTestTo/ccc/sixth.file")));

    private final List<File> mergeTestTargetFiles =
        Collections.unmodifiableList(
            Arrays.asList(
                new File(fileTestFolder, "mergeTestTo/aaa/first.file"),
                new File(fileTestFolder, "mergeTestTo/aaa/second.file"),
                new File(fileTestFolder, "mergeTestTo/bbb/third.file"),
                new File(fileTestFolder, "mergeTestTo/bbb/fourth.file")));

    private final List<File> mergeTestExpectedTargetFiles =
        Collections.unmodifiableList(
            Arrays.asList(
                new File(fileTestFolder, "mergeTestTo/aaa/first.file"),
                new File(fileTestFolder, "mergeTestTo/aaa/second.file"),
                new File(fileTestFolder, "mergeTestTo/bbb/third.file"),
                new File(fileTestFolder, "mergeTestTo/bbb/fourth.file"),
                new File(fileTestFolder, "mergeTestTo/ccc/fifth.file"),
                new File(fileTestFolder, "mergeTestTo/ccc/sixth.file")));


    ///////////////////////
    // SINGLE FILE TESTS //
    ///////////////////////


    /**
     * Tests if creating files actually creates files...
     */
    @Test
    public void testFileCreation()
    {
        FileUtils.createEmptyFile(testFile);
        assertTrue("The method createEmptyFile() should create an empty file, duh!",
                   testFile.exists() && testFile.isFile());
    }


    /**
     * Tests if creating files overwrites existing files.
     */
    @Test
    public void testFileCreationExisting()
    {
        // write something to a file
        final DiskIO diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        diskIo.writeStringToFile(testFile, copyTestText);

        // overwrite full file with an empty one
        FileUtils.createEmptyFile(testFile);

        assertEquals("The method createEmptyFile() should overwrite existing files!",
                     0L,
                     testFile.length());
    }


    /**
     * Tests if deleting files is reflected in the file system.
     */
    @Test
    public void testFileDeletion()
    {
        FileUtils.createEmptyFile(testFile);
        FileUtils.deleteFile(testFile);

        assertFalse("The method deleteFile() should remove files from the file system!",
                    testFile.exists());
    }


    /**
     * Tests if deleting non-existing files causes no exception.
     */
    @Test
    public void testFileDeletionNonExisting()
    {
        try {
            FileUtils.deleteFile(testFile);
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
        diskIo.writeStringToFile(copyTestSourceFile, copyTestText);

        FileUtils.copyFile(copyTestSourceFile, copyTestTargetFile);

        assertEquals("The method copyFile() should create new files if the target destination does not exist!",
                     copyTestText,
                     diskIo.getString(copyTestTargetFile));
    }


    /**
     * Tests if copying a non-existing file causes no changes in the
     * filesystem.
     */
    @Test
    public void testFileCopyingWithoutSource()
    {
        FileUtils.copyFile(copyTestSourceFile, copyTestTargetFile);

        assertFalse("The method copyFile() should not create files if the source file does not exist!",
                    copyTestTargetFile.exists());
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
        diskIo.writeStringToFile(copyTestSourceFile, copyTestText);
        diskIo.writeStringToFile(copyTestTargetFile, copyTestOverwriteText);

        FileUtils.copyFile(copyTestSourceFile, copyTestTargetFile);

        assertEquals(
            "The method copyFile() should overwrite existing target files!",
            copyTestText,
            diskIo.getString(copyTestTargetFile));
    }


    /**
     * Tests if file replacement removes the source file.
     */
    @Test
    public void testIfFileReplacementRemovesSourceFile()
    {
        // create two files
        final DiskIO diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        diskIo.writeStringToFile(copyTestSourceFile, copyTestText);
        diskIo.writeStringToFile(copyTestTargetFile, copyTestOverwriteText);

        FileUtils.replaceFile(copyTestTargetFile, copyTestSourceFile);

        assertFalse("The method replaceFile() should remove the source file!",
                    copyTestSourceFile.exists());
    }


    /**
     * Tests if file replacement properly overwrites an existing target file.
     */
    @Test
    public void testFileReplacementReplacesTargetFile()
    {
        // create two files
        final DiskIO diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        diskIo.writeStringToFile(copyTestSourceFile, copyTestText);
        diskIo.writeStringToFile(copyTestTargetFile, copyTestOverwriteText);

        FileUtils.replaceFile(copyTestTargetFile, copyTestSourceFile);

        assertEquals("The method replaceFile() should overwrite existing target files!",
                     copyTestText,
                     diskIo.getString(copyTestTargetFile));
    }


    /**
     * Tests if file replacement does not leave behind temporary backup files.
     */
    @Test
    public void testFileReplacementRemovesBackupFile()
    {
        // create two files
        final DiskIO diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        diskIo.writeStringToFile(copyTestSourceFile, copyTestText);
        diskIo.writeStringToFile(copyTestTargetFile, copyTestOverwriteText);

        FileUtils.replaceFile(copyTestTargetFile, copyTestSourceFile);

        // check if backup file has been removed
        assertFalse("The method replaceFile() should remove the backup files created during execution of the method!",
                    new File(copyTestTargetFile.getPath() + FileConstants.TEMP_FILE_EXTENSION).exists());
    }


    /**
     * Tests if file replacement properly creates a previously non-existing target file.
     */
    @Test
    public void testFileReplacementWithoutTarget()
    {
        // create two files
        final DiskIO diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        diskIo.writeStringToFile(copyTestSourceFile, copyTestText);

        FileUtils.replaceFile(copyTestTargetFile, copyTestSourceFile);

        assertEquals("The method replaceFile() should create target files if they are missing!",
                     copyTestText,
                     diskIo.getString(copyTestTargetFile));
    }


    /**
     * Tests if file replacement causes no file to be created if
     * the source file does not exist.
     */
    @Test
    public void testFileReplacementWithoutSource()
    {
        FileUtils.replaceFile(copyTestTargetFile, copyTestSourceFile);

        assertFalse("The method replaceFile() should not alter the file system if the source file is missing!",
                    copyTestTargetFile.exists());
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
        FileUtils.createDirectories(multiTestDir);
        assertTrue("The method createDirectories() should create directories!",
                   multiTestDir.exists() && multiTestDir.isDirectory());
    }


    /**
     * Tests if creating the same directory structure twice
     * throws no exceptions.
     */
    @Test
    public void testDirectoryCreationExisting()
    {
        FileUtils.createDirectories(multiTestDir);

        try {
            FileUtils.createDirectories(multiTestDir);
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
        FileUtils.createDirectories(multiTestDir);
        final File tempDir = getTemporaryTestDirectory();
        FileUtils.deleteFile(tempDir);

        assertFalse("The method deleteFile() should delete directories!",
                    tempDir.exists());
    }


    /**
     * Tests if deleting a non-existing directory
     * throws no exceptions.
     */
    @Test
    public void testDirectoryDeletionNonExisting()
    {
        try {
            FileUtils.deleteFile(getTemporaryTestDirectory());
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
        diskIo.writeStringToFile(copyTestSourceFile, copyTestText);

        // copy the folder in which the source file resides
        FileUtils.copyFile(copyTestSourceDir, copyTestTargetDir);

        final File targetFile = new File(copyTestTargetDir, copyTestSourceFile.getName());
        assertEquals("The method copyFile() should create target directories if they are missing!",
                     copyTestText,
                     diskIo.getString(targetFile));
    }


    /**
     * Tests if copying a non-existing directory causes no changes in the
     * filesystem.
     */
    @Test
    public void testDirectoryCopyingWithoutSource()
    {
        FileUtils.copyFile(copyTestSourceDir, copyTestTargetDir);

        assertFalse("The method copyFile() should not create directories if there are no source directories!",
                    copyTestTargetDir.exists());
    }


    /**
     * Tests if copying a directory to a destination that already exists
     * will properly overwrite the existing target directory.
     */
    @Test
    public void testDirectoryCopyingWithTarget()
    {
        final File targetFile = new File(copyTestTargetDir, copyTestSourceFile.getName());

        // create two files
        final DiskIO diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        diskIo.writeStringToFile(copyTestSourceFile, copyTestText);
        diskIo.writeStringToFile(targetFile, copyTestOverwriteText);

        FileUtils.copyFile(copyTestSourceDir, copyTestTargetDir);

        assertEquals("The method copyFile(), if applied to directories, should overwrite existing directories!",
                     copyTestText, diskIo.getString(targetFile));
    }


    /**
     * Tests if directory replacement properly overwrites files in an existing target directory.
     */
    @Test
    public void testDirectoryReplacementFileOverwrite()
    {
        final File targetFile = new File(copyTestTargetFile.getParentFile(), copyTestSourceFile.getName());

        // create two files
        final DiskIO diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        diskIo.writeStringToFile(copyTestSourceFile, copyTestText);
        diskIo.writeStringToFile(targetFile, copyTestOverwriteText);

        FileUtils.replaceFile(targetFile.getParentFile(), copyTestSourceFile.getParentFile());

        assertEquals("The method replaceFile() should overwrite existing files in a targeted directory!",
                     copyTestText, diskIo.getString(targetFile));
    }

    /**
     * Tests if directory replacement deletes the source directory.
     */
    @Test
    public void testDirectoryReplacementSourceDeletion()
    {
        final File targetFile = new File(copyTestTargetFile.getParentFile(), copyTestSourceFile.getName());

        // create two files
        final DiskIO diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        diskIo.writeStringToFile(copyTestSourceFile, copyTestText);
        diskIo.writeStringToFile(targetFile, copyTestOverwriteText);

        FileUtils.replaceFile(targetFile.getParentFile(), copyTestSourceFile.getParentFile());

        assertFalse("The method replaceFile(), when applied to a directory, should delete the source directory!",
                    copyTestSourceFile.getParentFile().exists());
    }

    /**
     * Tests if directory replacement does not leave behind temporary backup files.
     */
    @Test
    public void testDirectoryReplacementCleanUp()
    {
        final File targetFile = new File(copyTestTargetFile.getParentFile(), copyTestSourceFile.getName());

        // create two files
        final DiskIO diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        diskIo.writeStringToFile(copyTestSourceFile, copyTestText);
        diskIo.writeStringToFile(targetFile, copyTestOverwriteText);

        FileUtils.replaceFile(targetFile.getParentFile(), copyTestSourceFile.getParentFile());

        // check if backup file has been removed
        assertFalse("The method replaceFile() should clean up all backup files created in the process!",
                    new File(targetFile.getPath() + FileConstants.TEMP_FILE_EXTENSION).exists());
    }


    /**
     * Tests if directory replacement properly creates a previously non-existing target directory.
     */
    @Test
    public void testDirectoryReplacementWithoutTarget()
    {
        final File targetFile = new File(copyTestTargetDir, copyTestSourceFile.getName());

        // create two files
        final DiskIO diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);
        diskIo.writeStringToFile(copyTestSourceFile, copyTestText);

        FileUtils.replaceFile(copyTestTargetDir, copyTestSourceDir);

        assertEquals("The method replaceFile() should create a target directory if it is missing!",
                     copyTestText,
                     diskIo.getString(targetFile));
    }


    /**
     * Tests if directory replacement causes no changes in the file system if
     * the source directory does not exist.
     */
    @Test
    public void testDirectoryReplacementWithoutSource()
    {
        FileUtils.replaceFile(copyTestTargetDir, copyTestSourceDir);

        assertFalse("The method replaceFile() should not create directories if the source directory is missing!",
                    copyTestTargetDir.exists());
    }


    /**
     * Tests if a directory can be merged into another existing one,
     * while replacing existing target files in the process.
     */
    @Test
    public void testReplacingDirectoryMergeWithTarget()
    {
        assertDirectoryIntegration(true);
    }


    /**
     * Tests if a directory can be merged into another existing one,
     * while preserving existing target files in the process.
     */
    @Test
    public void testNonReplacingDirectoryMergeWithTarget()
    {
        assertDirectoryIntegration(false);
    }


    /**
     * Tests if a new directory is created when there is no target directory
     * when integrating. The replacement flag is checked.
     */
    @Test
    public void testDirectoryMergeWithoutTarget()
    {
        final DiskIO diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);

        for (File sourceFile : mergeTestSourceFiles)
            diskIo.writeStringToFile(sourceFile, copyTestText);

        FileUtils.integrateDirectory(mergeTestSourceDir, mergeTestTargetDir, false);

        for (File targetFile : mergeTestMergedSourceFiles)
            assertEquals(
                "The method integrateDirectory() should create target directories if they are missing!",
                copyTestText,
                diskIo.getString(targetFile));
    }


    /**
     * Tests directory integration when there is no target directory and checks
     * if all source files are properly removed.
     */
    @Test
    public void testDirectoryMergeWithoutTargetRemovingOldDirectory()
    {
        final DiskIO diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);

        for (File sourceFile : mergeTestSourceFiles)
            diskIo.writeStringToFile(sourceFile, copyTestText);

        FileUtils.integrateDirectory(mergeTestSourceDir, mergeTestTargetDir, false);

        for (File sourceFile : mergeTestSourceFiles)
            assertFalse("The method integrateDirectory() should remove all source files!",
                        sourceFile.exists());
    }


    /**
     * Tests if there are no changes in the filesystem if a non-existing
     * directory is attempted to be integrated into another one.
     */
    @Test
    public void testDirectoryMergeWithoutSource()
    {
        FileUtils.integrateDirectory(mergeTestSourceDir, mergeTestTargetDir, false);

        assertFalse("The method integrateDirectory() should not create directories if the source does not exist!",
                    mergeTestTargetDir.exists());
    }


    /**
     * Tests if all source files of a directory merge get moved over to
     * the target directory.
     */
    @Test
    public void testDirectoryMergeCompletion()
    {
        final DiskIO diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);

        for (File sourceFile : mergeTestSourceFiles)
            diskIo.writeStringToFile(sourceFile, copyTestText);

        for (File targetFile : mergeTestTargetFiles)
            diskIo.writeStringToFile(targetFile, copyTestOverwriteText);

        FileUtils.integrateDirectory(mergeTestSourceDir, mergeTestTargetDir, false);

        for (File targetFile : mergeTestExpectedTargetFiles)
            assertTrue("The method integrateDirectory() should merge all directories and files to a target directory!",
                       targetFile.exists());
    }


    //////////////////////
    // Non-test Methods //
    //////////////////////

    /**
     * Tests if a directory can be integrated into another existing one.
     *
     * @param replaceFiles if true, existing files may be overridden
     */
    private void assertDirectoryIntegration(boolean replaceFiles)
    {
        final DiskIO diskIo = new DiskIO(new Gson(), StandardCharsets.UTF_8);

        for (File sourceFile : mergeTestSourceFiles)
            diskIo.writeStringToFile(sourceFile, copyTestText);

        for (File targetFile : mergeTestTargetFiles)
            diskIo.writeStringToFile(targetFile, copyTestOverwriteText);

        FileUtils.integrateDirectory(mergeTestSourceDir, mergeTestTargetDir, replaceFiles);

        final File possiblyReplacedFile = mergeTestTargetFiles.get(0);
        final String expectedFileContent = replaceFiles ? copyTestText : copyTestOverwriteText;
        assertEquals("The method integrateDirectory() should merge all files to the target directory!",
                     expectedFileContent,
                     diskIo.getString(possiblyReplacedFile));
    }
}
