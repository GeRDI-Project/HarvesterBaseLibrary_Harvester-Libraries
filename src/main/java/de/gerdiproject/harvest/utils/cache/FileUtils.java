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
package de.gerdiproject.harvest.utils.cache;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gerdiproject.harvest.utils.cache.constants.CacheConstants;

/**
 * A small class with helper functions that are used for file operations.
 *
 * @author Robin Weiss
 *
 */
public class FileUtils
{
    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtils.class);


    /**
     * Private constructor because this class only contains static functions.
     */
    private FileUtils()
    {
    }


    /**
     * Attempts to delete a file from disk if it exists and logs any errors.
     *
     * @param deletedFile the file that is to be deleted
     */
    public static void deleteFile(File deletedFile)
    {
        if (deletedFile.exists()) {
            boolean wasDeleted;
            Exception ioException = null;

            if (deletedFile.isDirectory()) {

                // delete contained files and directories recursively
                try
                    (DirectoryStream<Path> dirStream = Files.newDirectoryStream(deletedFile.toPath())) {
                    for (Path fileInDir : dirStream)
                        deleteFile(fileInDir.toFile());

                } catch (IOException e) {
                    wasDeleted = false;
                    ioException = e;
                }
            }

            // delete file or directory itself
            wasDeleted = deletedFile.delete();

            if (!wasDeleted) {
                if (ioException != null)
                    LOGGER.error(String.format(CacheConstants.DELETE_FILE_FAILED, deletedFile.getAbsolutePath()), ioException);
                else
                    LOGGER.error(String.format(CacheConstants.DELETE_FILE_FAILED, deletedFile.getAbsolutePath()));
            } else
                LOGGER.trace(String.format(CacheConstants.DELETE_FILE_SUCCESS, deletedFile.getAbsolutePath()));

        }
    }


    /**
     * Replaces one cache file with another and logs any errors.
     *
     * @param oldFile the file that is to be replaced
     * @param newFile the new file
     */
    public static void replaceFile(File oldFile, File newFile)
    {
        deleteFile(oldFile);

        if (!createDirectories(newFile) || !newFile.renameTo(oldFile))
            LOGGER.error(String.format(CacheConstants.REPLACE_FILE_FAILED, oldFile.getAbsolutePath(), newFile.getAbsolutePath()));
        else
            LOGGER.trace(String.format(CacheConstants.REPLACE_FILE_SUCCESS, oldFile.getAbsolutePath(), newFile.getAbsolutePath()));
    }


    /**
     * Copies one cache file to another and logs any errors.
     *
     * @param sourceFile the file that is to be copied
     * @param targetFile the destination file
     */
    public static void copyFile(File sourceFile, File targetFile)
    {
        if (createDirectories(targetFile)) {

            try {
                Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                LOGGER.error(
                    String.format(
                        CacheConstants.COPY_FILE_FAILED,
                        sourceFile.getAbsolutePath(),
                        targetFile.getAbsolutePath()));
                return;
            }

            LOGGER.trace(String.format(CacheConstants.COPY_FILE_SUCCESS, sourceFile.getAbsolutePath(), targetFile.getAbsolutePath()));
        }
    }


    /**
     * Attempts to create all directories of a specified file.
     *
     * @param file the file or directory for which all directories are supposed
     *            to be created
     *
     * @return true if the directories exist or were created or the file does
     *         not have directories
     */
    public static boolean createDirectories(File file)
    {
        final File directory = file.isDirectory() ? file : file.getParentFile();

        if (directory == null || directory.exists())
            return true;

        final boolean creationSuccessful = directory.mkdirs();

        if (creationSuccessful)
            LOGGER.trace(String.format(CacheConstants.CREATE_DIR_SUCCESS, directory.getAbsolutePath()));
        else
            LOGGER.error(String.format(CacheConstants.CREATE_DIR_FAILED, directory.getAbsolutePath()));

        return creationSuccessful;
    }


    /**
     * Creates a new cache file, replacing any file that already exists.
     *
     * @param file the file that is to be created
     */
    public static void createEmptyFile(File file)
    {
        deleteFile(file);

        // attempt to create parent folder
        boolean creationSuccessful = createDirectories(file);
        Exception ioException = null;

        try {
            creationSuccessful &= file.createNewFile();
        } catch (IOException e) {
            creationSuccessful = false;
            ioException = e;
        }

        if (!creationSuccessful) {
            if (ioException != null)
                LOGGER.error(String.format(CacheConstants.CREATE_FILE_FAILED, file.getAbsolutePath()), ioException);
            else
                LOGGER.error(String.format(CacheConstants.CREATE_FILE_FAILED, file.getAbsolutePath()));
        } else
            LOGGER.trace(String.format(CacheConstants.CREATE_FILE_SUCCESS, file.getAbsolutePath()));
    }


    /**
     * Merges one directory into another one.
     *
     * @param sourceDirectory the directory that is to be integrated
     * @param targetDirectory the directory into which the other folder is merged
     * @param replaceFiles if true, sourceDirectory files with the same name as in the
     * target directory, will be replaced
     */
    public static void integrateDirectory(File sourceDirectory, File targetDirectory, boolean replaceFiles)
    {
        // make sure the target directory exists
        if (!createDirectories(targetDirectory)) {
            LOGGER.error(String.format(
                             CacheConstants.DIR_MERGE_FAILED_NO_TARGET_DIR,
                             targetDirectory.getAbsolutePath()));
            return;
        }


        // make sure both files are directories
        if (!sourceDirectory.isDirectory() || !targetDirectory.isDirectory()) {
            LOGGER.error(String.format(
                             CacheConstants.DIR_MERGE_FAILED_NOT_DIRS,
                             sourceDirectory.getAbsolutePath(),
                             targetDirectory.getAbsolutePath()));
            return;
        }

        try
            (DirectoryStream<Path> sourceStream = Files.newDirectoryStream(sourceDirectory.toPath())) {
            for (Path sourceFilePath : sourceStream) {
                final File sourceFile = sourceFilePath.toFile();
                final File targetFile = new File(targetDirectory, sourceFile.getName());

                // recursively integrate subdirectories
                if (sourceFile.isDirectory())
                    integrateDirectory(sourceFile, targetFile, replaceFiles);

                else {
                    // delete existing file, if in replace-mode
                    if (replaceFiles)
                        deleteFile(targetFile);

                    // copy single file
                    if (!targetFile.exists())
                        copyFile(sourceFile, targetFile);
                }
            }
        } catch (IOException e) {
            LOGGER.error(String.format(
                             CacheConstants.DIR_MERGE_FAILED,
                             sourceDirectory.getAbsolutePath(),
                             targetDirectory.getAbsolutePath()),
                         e);
            return;
        }

        LOGGER.trace(String.format(
                         CacheConstants.DIR_MERGE_SUCCESS,
                         sourceDirectory.getAbsolutePath(),
                         targetDirectory.getAbsolutePath()));

        // delete source folder
        deleteFile(sourceDirectory);
    }
}
