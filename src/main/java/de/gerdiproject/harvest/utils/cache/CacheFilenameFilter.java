/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
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
import java.io.FilenameFilter;

import de.gerdiproject.harvest.utils.cache.constants.CacheConstants;

/**
 * A file name filter that checks if the file names are document cache files, but not
 * the cache file that is currently in use.
 *
 * @author Robin Weiss
 */
public class CacheFilenameFilter implements FilenameFilter
{
    private final String currentCacheName;


    /**
     * Constructor that requires the cache file that is currently in use.
     * @param currentCacheFile the cache file that is currently in use
     */
    public CacheFilenameFilter(File currentCacheFile)
    {
        currentCacheName = currentCacheFile.getName();
    }

    @Override
    public boolean accept(File file, String fileName)
    {
        return !fileName.equals(currentCacheName) && fileName.matches(CacheConstants.CACHE_FILE_REGEX);
    }
}
