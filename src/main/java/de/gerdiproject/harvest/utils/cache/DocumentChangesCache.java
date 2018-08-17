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
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import de.gerdiproject.harvest.utils.FileUtils;
import de.gerdiproject.harvest.utils.cache.constants.CacheConstants;
import de.gerdiproject.json.datacite.DataCiteJson;

/**
 * This class manages a cache folder that represents a mapping of document identifiers to
 * document JSON. It is used for determining which files need to be added or
 * changed for the submission.
 *
 * @author Robin Weiss
 */
public class DocumentChangesCache extends AbstractCache<DataCiteJson>
{
    private int size;


    /**
     * Constructor that sets up folders for the cache.
     *
     * @param wipFolderPath the path of the cache folder with pending changes
     * @param stableFolderPath the path of the stable cache folder
     * @param charset the charset of the cached files
     */
    public DocumentChangesCache(final String wipFolderPath, final String stableFolderPath, final Charset charset)
    {
        super(stableFolderPath + CacheConstants.CHANGES_FOLDER_NAME,
              wipFolderPath + CacheConstants.CHANGES_FOLDER_NAME,
              DataCiteJson.class,
              charset);
    }


    /**
     * Initializes the cache by creating empty files for every document
     * that is expected to be harvested.
     *
     * @param documentIDs a List of documentIDs for which empty
     */
    public void init(List<String> documentIDs)
    {
        // create new file
        final AtomicInteger numberOfCopiedIds = new AtomicInteger(0);

        // copy documentIds and count them
        documentIDs.forEach((String documentId) -> {
            FileUtils.createEmptyFile(getFile(documentId, false));
            numberOfCopiedIds.incrementAndGet();
        });

        this.size = numberOfCopiedIds.get();
    }


    /**
     * Returns the number of cached documents.
     *
     * @return the number of cached documents
     */
    public int size()
    {
        return size;
    }


    @Override
    public void applyChanges()
    {
        FileUtils.replaceFile(new File(stableFolderPath), new File(wipFolderPath));
    }


    @Override
    public void putFile(String documentId, DataCiteJson document)
    {
        final File documentFile = getFile(documentId, false);

        if (documentFile.exists() && document == null)
            size--;
        else if (!documentFile.exists() && document != null)
            size++;

        super.putFile(documentId, document);

    }
}
