/**
 * Copyright © 2017 Robin Weiss (http://www.gerdi-project.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.gerdiproject.harvest.utils.cache;

import java.io.File;
import java.io.FilenameFilter;

/**
 * A file name filter that checks if file names do not match a given timestamp.
 *
 * @author Robin Weiss
 */
public class CacheFilenameFilter implements FilenameFilter
{
    private final long currentCacheTimestamp;


    /**
     * Constructor that requires the cache file that is currently in use.
     * 
     * @param currentCacheTimestamp the timestamp at which the current harvest
     *            started
     */
    public CacheFilenameFilter(long currentCacheTimestamp)
    {
        this.currentCacheTimestamp = currentCacheTimestamp;
    }


    @Override
    public boolean accept(File file, String fileName)
    {
        return !fileName.equals(String.valueOf(currentCacheTimestamp));
    }
}
