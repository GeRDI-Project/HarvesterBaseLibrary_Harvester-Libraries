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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.utils.cache.constants.CacheConstants;
import de.gerdiproject.json.datacite.DataCiteJson;

/**
 * This class wraps a cache file that stores a map of document identifiers to
 * document hash values. It encompasses one file that is treated as a stable
 * version of the cache and one file that is copied from the stable version
 * during initialization and will receive all changes. The stable file can be
 * updated with said changes via a method call.
 *
 * @author Robin Weiss
 */
public class DocumentVersionsCache
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentVersionsCache.class);

    private final File stableFile;
    private final File workInProgressFile;
    private String stableHarvesterHash;
    private String workInProgressHarvesterHash;


    /**
     * Constructor that requires the file name prefix of the cache files that
     * are to be created.
     *
     * @param filePrefix the file name prefix of the cache files that are to be
     *            created
     */
    public DocumentVersionsCache(final String filePrefix)
    {
        this.stableFile = new File(
            String.format(
                CacheConstants.VERSIONS_CACHE_FILE_PATH,
                MainContext.getModuleName(),
                filePrefix));

        this.workInProgressFile = new File(
            String.format(
                CacheConstants.VERSIONS_CACHE_TEMP_FILE_PATH,
                MainContext.getModuleName(),
                filePrefix));
    }


    /**
     * Initializes the cache by copying the existing cache file to the WIP file.
     *
     * @param hash the hash value that represents a version of the source data
     */
    public void init(String hash)
    {
        this.stableHarvesterHash = null;
        this.workInProgressHarvesterHash = hash;

        try {
            // create a new WIP file
            FileUtils.createEmptyFile(workInProgressFile);

            final JsonWriter writer = new JsonWriter(
                new OutputStreamWriter(
                    new FileOutputStream(workInProgressFile),
                    MainContext.getCharset()));
            // if no stable cache exists, write an empty object to the WIP file
            writer.beginObject();

            // write empty harvester hash
            writer.name(CacheConstants.HARVESTER_SOURCE_HASH_JSON);
            writer.value(workInProgressHarvesterHash);

            // write empty document map
            writer.name(CacheConstants.DOCUMENTS_JSON);
            writer.beginObject();

            // copy documents from stable file
            if (stableFile.exists()) {
                try {
                    final JsonReader reader = new JsonReader(
                        new InputStreamReader(
                            new FileInputStream(stableFile),
                            MainContext.getCharset()));
                    reader.beginObject();

                    // retrieve source hash
                    reader.nextName();

                    if (reader.peek() != JsonToken.NULL)
                        this.stableHarvesterHash = reader.nextString();
                    else
                        reader.skipValue();

                    // retrieve document hashes
                    reader.nextName();
                    reader.beginObject();

                    while (reader.hasNext())
                        writer.name(reader.nextName()).value(reader.nextString());

                    reader.close();
                } catch (IOException e) { // NOPMD - Nothing to do here. The stable hash is already null
                }
            }

            writer.endObject();

            writer.endObject();
            writer.close();
        } catch (IOException e) {
            LOGGER.error(String.format(CacheConstants.CACHE_INIT_FAILED, this.getClass().getSimpleName()), e);
        }

    }


    /**
     * Checks if the harvested documents are outdated.
     *
     * @return true if the harvester hash changed since the last harvest
     */
    public boolean isOutdated()
    {
        return stableHarvesterHash == null || !stableHarvesterHash.equals(workInProgressHarvesterHash);
    }


    /**
     * Deletes the work-in-progress cache file.
     */
    public void clearWorkInProgress()
    {
        FileUtils.deleteFile(new File(workInProgressFile.getAbsolutePath() + CacheConstants.TEMP_FILE_EXTENSION));
        FileUtils.deleteFile(workInProgressFile);
        this.workInProgressHarvesterHash = stableHarvesterHash;
    }


    /**
     * Applies all changes that had been made to the work-in-progress file to
     * the stable file.
     */
    public void applyChanges()
    {
        FileUtils.replaceFile(stableFile, workInProgressFile);
        this.stableHarvesterHash = workInProgressHarvesterHash;
    }


    /**
     * Deletes all entries that have null values in a specified changes cache.
     *
     * @param changesCache the cache that is being iterated to look for null
     *            entries
     */
    public void removeDeletedEntries(final DocumentChangesCache changesCache)
    {
        changesCache.forEach((String documentId, DataCiteJson document) -> {
            if (document == null)
                putDocumentHash(documentId, null);
            return true;
        });
    }


    /**
     * Iterates through all cached entries of the stable file and executes a
     * specified function for each entry. If the function returns false, the
     * process is aborted.
     *
     * @param entryFunction a function that accepts a documentId and
     *            documentHash and returns true if the forEach should continue
     *
     * @return true if all entries were processed
     */
    public boolean forEach(BiFunction<String, String, Boolean> entryFunction)
    {
        boolean isSuccessful = true;

        if (stableFile.exists()) {
            try {
                // prepare json reader for the cached document list
                final JsonReader reader = new JsonReader(
                    new InputStreamReader(
                        new FileInputStream(stableFile),
                        MainContext.getCharset()));

                // skip harvester hash
                reader.beginObject();
                reader.nextName();
                reader.skipValue();
                reader.nextName();

                // iterate through all entries
                reader.beginObject();

                while (isSuccessful && reader.hasNext()) {
                    final String documentId = reader.nextName();
                    final String documentHash = reader.nextString();
                    isSuccessful = entryFunction.apply(documentId, documentHash);
                }

                // close reader
                reader.close();
            } catch (IOException e) {
                isSuccessful = false;
            }
        } else
            isSuccessful = false;

        return isSuccessful;
    }


    /**
     * Adds or replaces a documentId to documentHash entry of the WIP file. If
     * the documentHash is null, the entry with the corresponding id is deleted.
     *
     * @param documentId the identifier of the document that is to be
     *            changed/added
     * @param documentHash the value that is to be assigned to the documentId,
     *            or null if the documentId is to be removed
     */
    public void putDocumentHash(final String documentId, final String documentHash)
    {
        boolean hasChanges = false;
        final File tempFile = new File(workInProgressFile.getAbsolutePath() + CacheConstants.TEMP_FILE_EXTENSION);

        try {
            // prepare json reader for the cached document list
            final JsonReader reader = new JsonReader(
                new InputStreamReader(new FileInputStream(workInProgressFile), MainContext.getCharset()));

            final JsonWriter writer = new JsonWriter(
                new OutputStreamWriter(
                    new FileOutputStream(tempFile),
                    MainContext.getCharset()));

            // copy harvester hash
            reader.beginObject();
            writer.beginObject();
            writer.name(reader.nextName());

            if (workInProgressHarvesterHash == null)
                writer.nullValue();

            else
                writer.value(workInProgressHarvesterHash);

            reader.skipValue();

            // copy key value pairs until the specified key is found
            writer.name(reader.nextName());
            reader.beginObject();
            writer.beginObject();
            boolean doesKeyExist = false;

            while (reader.hasNext()) {
                final String readKey = reader.nextName();
                final String readValue = reader.nextString();

                if (readKey.equals(documentId)) {
                    hasChanges = !readValue.equals(documentHash);
                    doesKeyExist = true;
                    break;
                } else
                    writer.name(readKey).value(readValue);
            }

            // add specified key-value pair
            if (documentHash != null) {
                writer.name(documentId).value(documentHash);
                hasChanges |= !doesKeyExist;
            }

            // if any old key-value pairs remain in the source file, copy them
            while (reader.hasNext())
                writer.name(reader.nextName()).value(reader.nextString());

            // close reader
            reader.close();

            // close writer
            writer.endObject();
            writer.endObject();
            writer.close();

        } catch (IOException e) {
            LOGGER.error(String.format(CacheConstants.ENTRY_STREAM_WRITE_ERROR, tempFile.getAbsolutePath()), e);
            return;
        }

        // replace current file with temporary file
        if (hasChanges)
            FileUtils.replaceFile(workInProgressFile, tempFile);
        else if (tempFile.exists())
            FileUtils.deleteFile(tempFile);
    }


    /**
     * Retrieves the document hash for a specified document ID from the stable
     * cache file.
     *
     * @param documentId the identifier of the document of which the hash is
     *            retrieved
     * @return a hash value or null, if no entry exists for the document
     */
    public String getDocumentHash(String documentId)
    {
        String[] documentHash = {
            null
        };
        forEach((String stableDocId, String stableDocHash) -> {
            if (stableDocId.equals(documentId))
            {
                documentHash[0]
                = stableDocHash;

                // abort for each prematurely
                return false;
            } else
                return true;
        });

        return documentHash[0];
    }
}
