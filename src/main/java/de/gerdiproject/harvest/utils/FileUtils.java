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
package de.gerdiproject.harvest.utils;

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
                    LOGGER.error(String.format(CacheConstants.DELETE_FILE_FAILED, deletedFile.getPath()), ioException);
                else
                    LOGGER.error(String.format(CacheConstants.DELETE_FILE_FAILED, deletedFile.getPath()));
            } else
                LOGGER.trace(String.format(CacheConstants.DELETE_FILE_SUCCESS, deletedFile.getPath()));

        }
    }


    /**
     * Replaces one file with another and logs any errors.
     *
     * @param targetFile the file that is to be replaced
     * @param newFile the new file that replaces the target file
     */
    public static void replaceFile(File targetFile, File newFile)
    {
        if (!newFile.exists()) {
            LOGGER.error(String.format(
                             CacheConstants.REPLACE_FILE_FAILED_NO_FILE,
                             targetFile.getPath(),
                             newFile.getPath()));
            return;
        }

        File backup = null;

        // back up target file, in case something goes wrong
        if (targetFile.exists()) {
            backup = new File(targetFile.getPath() + CacheConstants.TEMP_FILE_EXTENSION);

            if (!targetFile.renameTo(backup)) {
                LOGGER.error(String.format(
                                 CacheConstants.REPLACE_FILE_FAILED_CANNOT_BACKUP,
                                 targetFile.getPath(),
                                 newFile.getPath()));
                return;
            }
        }

        if (!createDirectories(targetFile.getParentFile())) {
            LOGGER.error(String.format(
                             CacheConstants.REPLACE_FILE_FAILED_NO_TARGET_DIR,
                             targetFile.getPath(),
                             newFile.getPath()));

            if (backup != null && !backup.renameTo(targetFile))
                LOGGER.error(String.format(CacheConstants.REPLACE_FILE_FAILED_CANNOT_RESTORE, targetFile.getPath()));

            return;
        }

        if (!newFile.renameTo(targetFile)) {
            LOGGER.error(String.format(CacheConstants.REPLACE_FILE_FAILED, targetFile.getPath(), newFile.getPath()));

            if (backup != null && !backup.renameTo(targetFile))
                LOGGER.error(String.format(CacheConstants.REPLACE_FILE_FAILED_CANNOT_RESTORE, targetFile.getPath()));

        } else {
            LOGGER.trace(String.format(CacheConstants.REPLACE_FILE_SUCCESS, targetFile.getPath(), newFile.getPath()));

            if (backup != null)
                deleteFile(backup);
        }
    }


    /**
     * Copies a single file to a target destination and logs any errors.
     *
     * @param sourceFile the file that is to be copied
     * @param targetFile the destination file
     */
    public static void copyFile(File sourceFile, File targetFile)
    {
        if (!sourceFile.exists())
            LOGGER.error(String.format(
                             CacheConstants.COPY_FILE_FAILED_NO_FILE,
                             sourceFile.getAbsolutePath(),
                             targetFile.getAbsolutePath()));

        else if (createDirectories(sourceFile.isDirectory() ? targetFile : targetFile.getParentFile())) {
            if (sourceFile.isDirectory()) {
                try
                    (DirectoryStream<Path> sourceStream = Files.newDirectoryStream(sourceFile.toPath())) {
                    for (Path sourceFilePath : sourceStream) {
                        final File sourceDirContent = sourceFilePath.toFile();
                        final File targetDirContent = new File(targetFile, sourceDirContent.getName());

                        if (sourceDirContent.isDirectory())
                            copyFile(sourceDirContent, targetDirContent);
                        else
                            Files.copy(sourceDirContent.toPath(), targetDirContent.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    LOGGER.error(String.format(
                                     CacheConstants.COPY_FILE_FAILED,
                                     sourceFile.getPath(),
                                     targetFile.getPath()),
                                 e);
                    return;
                }
            } else {
                try {
                    Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    LOGGER.error(
                        String.format(
                            CacheConstants.COPY_FILE_FAILED,
                            sourceFile.getAbsolutePath(),
                            targetFile.getAbsolutePath()),
                        e);
                    return;
                }
            }

            LOGGER.trace(String.format(CacheConstants.COPY_FILE_SUCCESS, sourceFile.getPath(), targetFile.getPath()));
        }
    }


    /**
     * Attempts to create all directories of a given directory path.
     *
     * @param directory the directory path to be created
     *
     * @return true if the directories already exist or were created
     */
    public static boolean createDirectories(File directory)
    {
        if (directory == null || directory.exists())
            return true;

        final boolean creationSuccessful = directory.mkdirs();

        if (creationSuccessful)
            LOGGER.trace(String.format(CacheConstants.CREATE_DIR_SUCCESS, directory.getPath()));
        else
            LOGGER.error(String.format(CacheConstants.CREATE_DIR_FAILED, directory.getPath()));

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
        boolean creationSuccessful = createDirectories(file.getParentFile());
        Exception ioException = null;

        try {
            creationSuccessful &= file.createNewFile();
        } catch (IOException e) {
            creationSuccessful = false;
            ioException = e;
        }

        if (!creationSuccessful) {
            if (ioException != null)
                LOGGER.error(String.format(CacheConstants.CREATE_FILE_FAILED, file.getPath()), ioException);
            else
                LOGGER.error(String.format(CacheConstants.CREATE_FILE_FAILED, file.getPath()));
        } else
            LOGGER.trace(String.format(CacheConstants.CREATE_FILE_SUCCESS, file.getPath()));
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
        // make sure the source directory exists
        if (!sourceDirectory.exists()) {
            LOGGER.error(String.format(
                             CacheConstants.DIR_MERGE_FAILED_NO_SOURCE_DIR,
                             sourceDirectory.getPath(),
                             targetDirectory.getPath()));
            return;
        }

        // make sure both files are directories
        if (!sourceDirectory.isDirectory() || targetDirectory.exists() && !targetDirectory.isDirectory()) {
            LOGGER.error(String.format(
                             CacheConstants.DIR_MERGE_FAILED_NOT_DIRS,
                             sourceDirectory.getPath(),
                             targetDirectory.getPath()));
            return;
        }

        // if the target directory does not exist, simply rename the source directory
        if (!targetDirectory.exists()) {
            if (!sourceDirectory.renameTo(targetDirectory))
                LOGGER.error(String.format(CacheConstants.DIR_MERGE_FAILED,
                                           sourceDirectory.getPath(),
                                           targetDirectory.getPath()));

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
                    // copy single file
                    if (replaceFiles || !targetFile.exists())
                        replaceFile(targetFile, sourceFile);
                }
            }
        } catch (IOException e) {
            LOGGER.error(String.format(
                             CacheConstants.DIR_MERGE_FAILED,
                             sourceDirectory.getPath(),
                             targetDirectory.getPath()),
                         e);
            return;
        }

        LOGGER.trace(String.format(
                         CacheConstants.DIR_MERGE_SUCCESS,
                         sourceDirectory.getPath(),
                         targetDirectory.getPath()));

        // delete source folder
        deleteFile(sourceDirectory);
    }
}
