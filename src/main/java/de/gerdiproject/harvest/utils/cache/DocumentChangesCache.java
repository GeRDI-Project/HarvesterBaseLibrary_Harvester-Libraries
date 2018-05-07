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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.utils.cache.constants.CacheConstants;
import de.gerdiproject.json.GsonUtils;
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
     * @param harvesterName the name of the harvester for which the cache is created
     */
    public DocumentChangesCache(final String harvesterName)
    {
        super(String.format(
                  CacheConstants.STABLE_CHANGES_FOLDER_PATH,
                  MainContext.getModuleName(),
                  harvesterName),
              String.format(
                  CacheConstants.TEMP_CHANGES_FOLDER_PATH,
                  MainContext.getModuleName(),
                  harvesterName),
              DataCiteJson.class);
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


    @Override
    @Deprecated
    protected void migrateToNewSystem()
    {
        final String harvesterName = new File(stableFolderPath).getParentFile().getName();
        final File stableFile = new File(
            String.format(
                CacheConstants.UPDATE_CACHE_FILE_PATH,
                MainContext.getModuleName(),
                harvesterName));

        // skip if there is no old cache file
        if (stableFile.exists()) {
            final Gson gson = GsonUtils.getGson();

            try
                (JsonReader reader = new JsonReader(
                    new InputStreamReader(new FileInputStream(stableFile), MainContext.getCharset()))) {

                // iterate through cached documents
                reader.beginObject();

                while (reader.hasNext()) {
                    final String documentId = reader.nextName();
                    final File file = getFile(documentId, true);

                    if (reader.peek() == JsonToken.NULL) {
                        FileUtils.createEmptyFile(file);
                        reader.skipValue();
                    } else
                        diskIo.writeObjectToFile(file, gson.fromJson(reader, DataCiteJson.class));
                }

            } catch (IOException e) {
                return;
            }

            FileUtils.deleteFile(stableFile);
        }
    }
}
