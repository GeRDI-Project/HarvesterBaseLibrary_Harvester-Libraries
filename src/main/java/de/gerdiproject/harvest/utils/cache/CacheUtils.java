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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gerdiproject.harvest.utils.cache.constants.CacheConstants;

/**
 * A small class with helper functions that are used by cache related classes.
 * 
 * @author Robin Weiss
 *
 */
class CacheUtils
{
    private static final Logger LOGGER = LoggerFactory.getLogger(CacheUtils.class);


    /**
     * Private constructor because this class only contains static functions.
     */
    private CacheUtils()
    {
    }


    /**
     * Replaces one cache file with another and logs any errors.
     * 
     * @param oldFile the file that is to be replaced
     * @param newFile the new file
     */
    public static void replaceFile(File oldFile, File newFile)
    {
        if (oldFile.exists() && !oldFile.delete())
            LOGGER.error(String.format(CacheConstants.DELETE_FILE_FAILED, oldFile.getAbsolutePath()));

        if (!newFile.renameTo(oldFile))
            LOGGER.error(String.format(CacheConstants.CACHE_CREATE_FAILED, oldFile.getAbsolutePath()));
    }


    /**
     * Creates a new cache file, replacing any file that already exists.
     * 
     * @param file the file that is to be created
     */
    public static void createEmptyFile(File file)
    {
        if (file.exists() && !file.delete())
            LOGGER.error(String.format(CacheConstants.DELETE_FILE_FAILED, file.getAbsolutePath()));

        boolean creationSuccessful;
        try {
            creationSuccessful = file.createNewFile();
        } catch (IOException e) {
            creationSuccessful = false;
        }
        if (!creationSuccessful)
            LOGGER.error(String.format(CacheConstants.CACHE_CREATE_FAILED, file.getAbsolutePath()));
    }
}
