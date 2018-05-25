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
import java.util.concurrent.atomic.AtomicInteger;

import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.harvester.AbstractHarvester;
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
     * Constructor that requires the harvester name for the creation of a dedicated
     * folder.
     *
     * @param harvester the harvester for which the cache is created
     */
    public DocumentChangesCache(final AbstractHarvester harvester)
    {
        super(String.format(
                  CacheConstants.STABLE_CHANGES_FOLDER_PATH,
                  MainContext.getModuleName(),
                  harvester.getName()),
              String.format(
                  CacheConstants.TEMP_CHANGES_FOLDER_PATH,
                  MainContext.getModuleName(),
                  harvester.getName()),
              DataCiteJson.class,
              harvester.getCharset());
    }


    /**
     * Initializes the cache by copying all documentIDs from a
     * versions cache folder as empty files.
     *
     * @param versionsCache a cache of previously harvested IDs
     */
    public void init(DocumentVersionsCache versionsCache)
    {
        // create new file
        final AtomicInteger numberOfCopiedIds = new AtomicInteger(0);

        // copy documentIds and count them
        boolean isSuccessful = false;
        isSuccessful = versionsCache.forEach((String documentId, String documentHash) -> {
            FileUtils.createEmptyFile(getFile(documentId, false));
            numberOfCopiedIds.incrementAndGet();
            return true;
        });

        if (isSuccessful)
            this.size = numberOfCopiedIds.get();
        else
            this.size = 0;
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
