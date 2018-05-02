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
import java.nio.file.Files;
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
        if (deletedFile.exists() && !deletedFile.delete())
            LOGGER.error(String.format(CacheConstants.DELETE_FILE_FAILED, deletedFile.getAbsolutePath()));
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
            LOGGER.error(String.format(CacheConstants.CACHE_CREATE_FAILED, oldFile.getAbsolutePath()));
    }


    /**
     * Copies one cache file to another and logs any errors.
     *
     * @param sourceFile the file that is to be copied
     * @param targetFile the destination file
     */
    public static void copyFile(File sourceFile, File targetFile)
    {
        createDirectories(targetFile);

        try {
            Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.error(
                String.format(
                    CacheConstants.COPY_FILE_FAILED,
                    sourceFile.getAbsolutePath(),
                    targetFile.getAbsolutePath()));
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

        return directory == null || directory.exists() || file.getParentFile().mkdirs();
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

        try {
            creationSuccessful &= file.createNewFile();
        } catch (IOException e) {
            creationSuccessful = false;
        }

        if (!creationSuccessful)
            LOGGER.error(String.format(CacheConstants.CACHE_CREATE_FAILED, file.getAbsolutePath()));
    }
}
