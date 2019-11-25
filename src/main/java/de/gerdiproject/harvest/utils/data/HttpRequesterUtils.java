/**
 * Copyright © 2019 Robin Weiss (http://www.gerdi-project.de)
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
package de.gerdiproject.harvest.utils.data;


import java.io.File;

import de.gerdiproject.harvest.utils.data.constants.DataOperationConstants;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;


/**
 * This class offers helper functions for the {@linkplain HttpRequester}.
 *
 * @author Robin Weiss
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HttpRequesterUtils
{
    /**
     * Converts a web URL to a path on disk from which a file can be read
     *
     * @param url the URL that is to be converted to a file path
     * @param parentFilePath the parent folder structure with which the converted path will start
     * @return a file path on disk
     */
    public static File urlToFilePath(final String url, final File parentFilePath)
    {
        String path = url;

        // remove the scheme
        int schemeEnd = path.indexOf("://");
        schemeEnd = (schemeEnd == -1) ? 0 : schemeEnd + 3;
        path = path.substring(schemeEnd);

        // remove double slashes
        path = path.replace("//", "/");

        // filter out :?*
        path = path.replace(":", "%colon%");
        path = path.replace("?", "/%query%/");
        path = path.replace("*", "%star%");
        path = path.replace("&", "/&");

        // remove slash at the end
        if (path.charAt(path.length() - 1) == '/')
            path = path.substring(0, path.length() - 1);

        // add file extension
        path += DataOperationConstants.RESPONSE_FILE_ENDING; // NOPMD StringBuffer does not pay off here

        // assemble the complete file name
        return new File(parentFilePath, path);
    }
}
