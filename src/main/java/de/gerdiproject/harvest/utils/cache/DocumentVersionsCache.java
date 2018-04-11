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
import com.google.gson.stream.JsonWriter;

import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.utils.cache.constants.CacheConstants;
import de.gerdiproject.json.GsonUtils;

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

    private final HarvesterCacheMetadata stableHarvesterMetadata;
    private final HarvesterCacheMetadata workInProgressHarvesterMetadata;
    private final File stableFile;
    private final File workInProgressFile;


    /**
     * Constructor that requires the file name prefix of the cache files that
     * are to be created.
     * 
     * @param filePrefix the file name prefix of the cache files that are to be
     *            created
     */
    public DocumentVersionsCache(final String filePrefix)
    {
        this.stableHarvesterMetadata = new HarvesterCacheMetadata();
        this.stableFile = new File(
                String.format(
                        CacheConstants.VERSIONS_CACHE_FILE_PATH,
                        MainContext.getModuleName(),
                        filePrefix));

        this.workInProgressHarvesterMetadata = new HarvesterCacheMetadata();
        this.workInProgressFile = new File(
                String.format(
                        CacheConstants.VERSIONS_CACHE_TEMP_FILE_PATH,
                        MainContext.getModuleName(),
                        filePrefix));
    }


    /**
     * Initializes the cache by copying the existing cache file to the WIP file.
     */
    public void init()
    {
        // delete old WIP file
        if (workInProgressFile.exists())
            workInProgressFile.delete();

        // copy stable document hashes to WIP file, or create empty WIP file, if no stable exists
        try {
            // create empty WIP file
            workInProgressFile.createNewFile();
            final JsonWriter writer = new JsonWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(workInProgressFile),
                            MainContext.getCharset()));

            if (stableFile.exists()) {
                // prepare json reader for the cached document list
                final JsonReader reader = new JsonReader(
                        new InputStreamReader(new FileInputStream(stableFile), MainContext.getCharset()));

                // retrieve harvester cache metadata
                reader.beginObject();
                reader.nextName();
                HarvesterCacheMetadata readStableMetadata =
                        GsonUtils.getGson().fromJson(reader, HarvesterCacheMetadata.class);
                this.stableHarvesterMetadata.set(readStableMetadata);

                // copy the document-hashes-map
                reader.nextName();
                reader.beginObject();
                writer.beginObject();
                while (reader.hasNext())
                    writer.name(reader.nextName()).value(reader.nextString());

                // write end of document-hashes-map, close writer
                writer.endObject();
                writer.close();

                // close reader
                reader.close();
            } else {
                writer.beginObject();
                writer.endObject();
                writer.close();
            }
        } catch (IOException e) {
            LOGGER.error(String.format(CacheConstants.CACHE_CREATE_FAILED, workInProgressFile.getAbsolutePath()), e);
        }
    }


    /**
     * Sets the hash value that represents the entire source data of the
     * harvester.
     * 
     * @param hash the hash value that represents the entire source data of the
     *            harvester
     * @param harvestStartIndex the start index of the harvesting range
     * @param harvestEndIndex the exclusive end index of the harvesting range
     */
    public void setHarvesterMetadata(String hash, int harvestStartIndex, int harvestEndIndex)
    {
        workInProgressHarvesterMetadata.setRangeFrom(harvestStartIndex);
        workInProgressHarvesterMetadata.setRangeTo(harvestEndIndex);
        workInProgressHarvesterMetadata.setSourceHash(hash);
    }


    /**
     * Reads the stable cache file and returns the harvesterHash value.
     * 
     * @return the harvesterHash value, or null if it could not be retrieved
     *
     *         private HarvesterCacheMetadata getHarvesterMetadataFromFile() {
     *         HarvesterCacheMetadata cacheMetadata = null;
     * 
     *         if (stableFile.exists()) { try { // prepare json reader for the
     *         cached document list final JsonReader reader = new JsonReader(
     *         new InputStreamReader(new FileInputStream(stableFile),
     *         MainContext.getCharset()));
     * 
     *         // read only the first key-value pair of the harvester values
     *         reader.beginObject(); reader.nextName(); cacheMetadata =
     *         GsonUtils.getGson().fromJson(reader,
     *         HarvesterCacheMetadata.class); reader.close(); } catch
     *         (IOException e) {
     * 
     *         } } return cacheMetadata; }
     */


    /**
     * Checks if the WIP-harvester-metadata is newer than the stable one.
     * 
     * @return true, if the current harvester metadata differs from the stable
     *         metadata
     */
    public boolean isCacheOutdated()
    {
        return stableHarvesterMetadata.isUpdateNeeded(workInProgressHarvesterMetadata);
    }


    /**
     * Applies all changes that had been made to the work-in-progress file to
     * the stable file.
     */
    public void applyChanges()
    {
        final File tempFile = new File(stableFile.getAbsolutePath() + CacheConstants.TEMP_FILE_EXTENSION);
        try {
            // prepare json reader for the cached document list
            final JsonReader reader = new JsonReader(
                    new InputStreamReader(new FileInputStream(workInProgressFile), MainContext.getCharset()));

            final JsonWriter writer = new JsonWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(tempFile),
                            MainContext.getCharset()));

            // copy the harvester cache metadata
            writer.beginObject();
            writer.name(CacheConstants.HARVESTER_VALUES_JSON);
            GsonUtils.getGson().toJson(
                    workInProgressHarvesterMetadata,
                    workInProgressHarvesterMetadata.getClass(),
                    writer);

            // start the map of document hashes
            writer.name(CacheConstants.DOCUMENT_HASHES_JSON);
            writer.beginObject();
            reader.beginObject();

            // copy key value pairs until the specified key is found
            while (reader.hasNext()) {
                writer.name(reader.nextName());
                writer.value(reader.nextString());
            }

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

        // replace stable file with wip file
        boolean isStableFileDeleted = !stableFile.exists();
        if (!isStableFileDeleted)
            isStableFileDeleted = stableFile.delete();

        // only replace stable file if it no longer exists
        if (isStableFileDeleted) {
            tempFile.renameTo(stableFile);
            workInProgressFile.delete();
            stableHarvesterMetadata.set(workInProgressHarvesterMetadata);
        } else
            LOGGER.error(String.format(CacheConstants.DELETE_FILE_FAILED, stableFile.getAbsolutePath()));
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

                // skip the harvester cache metadata if it is the stable file
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
     * Removes an entry with a specified documentId from the WIP file.
     * 
     * @param documentId the identifier of the document that is to be removed
     */
    public void removeDocumentHash(final String documentId)
    {
        putDocumentHash(documentId, null);
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

            reader.beginObject();
            writer.beginObject();

            // copy key value pairs until the specified key is found
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
            writer.close();

        } catch (IOException e) {
            return;
        }

        // replace current file with temporary file
        if (hasChanges) {
            workInProgressFile.delete();
            tempFile.renameTo(workInProgressFile);
        }
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
        String documentHash = null;

        if (stableFile.exists()) {
            try {
                // prepare json reader for the cached document list
                final JsonReader reader = new JsonReader(
                        new InputStreamReader(new FileInputStream(stableFile), MainContext.getCharset()));

                // skip the harvesterHash
                reader.beginObject();
                reader.nextName();
                reader.skipValue();
                reader.nextName();
                reader.beginObject();

                // search the ID in the document-hashes-map
                while (reader.hasNext()) {
                    final String id = reader.nextName();
                    final String hash = reader.nextString();

                    if (id.equals(documentId)) {
                        documentHash = hash;
                        break;
                    }
                }
                reader.close();
            } catch (IOException e) { // NOPMD - nothing to do here, documentHash is null by default

            }
        }
        return documentHash;
    }
}
