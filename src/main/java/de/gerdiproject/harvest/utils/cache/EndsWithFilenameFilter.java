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
 * A file name filter that checks if file names end with a specified suffix.
 *
 * @author Robin Weiss
 */
public class EndsWithFilenameFilter implements FilenameFilter
{
    private final String suffix;

    /**
     * Constructor that requires the filename suffix.
     *
     * @param extension the suffix of a matching file name
     */
    public EndsWithFilenameFilter(String extension)
    {
        this.suffix = extension;
    }

    @Override
    public boolean accept(File file, String fileName)
    {
        return fileName.endsWith(suffix);
    }
}
