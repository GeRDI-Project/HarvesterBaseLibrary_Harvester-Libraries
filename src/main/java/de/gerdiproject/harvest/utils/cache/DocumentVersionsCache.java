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
import java.nio.file.Files;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.utils.cache.constants.CacheConstants;

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
    private File workInProgressFile;
    private String harvesterHash;


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
     */
    public void init()
    {
        if (workInProgressFile.exists())
            workInProgressFile.delete();

        // copy stable to WIP file, or create empty WIP file, if no stable exists
        try {
            if (stableFile.exists()) {
                Files.copy(stableFile.toPath(), workInProgressFile.toPath());
                this.harvesterHash = getHarvesterHashFromFile();
            } else {
                workInProgressFile.createNewFile();
                this.harvesterHash = null;
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
     */
    public void setHarvesterHash(String hash)
    {
        if (hash != null && hash.equals(harvesterHash))
            return;

        final File tempFile = new File(workInProgressFile.getAbsolutePath() + CacheConstants.TEMP_FILE_EXTENSION);
        try {
            // prepare json reader for the cached document list
            final JsonReader reader = new JsonReader(
                    new InputStreamReader(new FileInputStream(workInProgressFile), MainContext.getCharset()));

            final JsonWriter writer = new JsonWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(tempFile),
                            MainContext.getCharset()));


            // skip reading the old harvesterHash value
            reader.beginObject();
            reader.nextName();
            reader.skipValue();

            // write the new harvesterHash value
            writer.beginObject();
            writer.name(CacheConstants.HARVESTER_HASH_JSON);
            writer.value(hash);

            // write the beginning of the document-hashes-map
            writer.name(CacheConstants.DOCUMENT_HASHES_JSON);
            writer.beginObject();

            // skip reading the beginning of the document-hashes-map
            reader.nextName();
            reader.beginObject();

            // copy the document-hashes-map
            while (reader.hasNext())
                writer.name(reader.nextName()).value(reader.nextString());

            // write end of document-hashes-map, close writer
            writer.endObject();
            writer.endObject();
            writer.close();

            reader.close();
        } catch (IOException e) {
            return;
        }

        // replace current file with temporary file
        workInProgressFile.delete();
        tempFile.renameTo(workInProgressFile);
        harvesterHash = hash;
    }


    /**
     * Reads the stable cache file and returns the harvesterHash value.
     * 
     * @return the harvesterHash value, or null if it could not be retrieved
     */
    private String getHarvesterHashFromFile()
    {
        String hash = null;

        if (stableFile.exists()) {
            try {
                // prepare json reader for the cached document list
                final JsonReader reader = new JsonReader(
                        new InputStreamReader(new FileInputStream(stableFile), MainContext.getCharset()));

                // read only the first key-value pair
                reader.beginObject();
                reader.nextName();
                hash = reader.nextString();

                reader.close();
            } catch (IOException e) {

            }
        }
        return hash;
    }


    /**
     * Returns the hash value that represents the entire source data of the
     * harvester.
     * 
     * @return the hash value that represents the entire source data of the
     *         harvester
     */
    public String getHarvesterHash()
    {
        return harvesterHash;
    }


    /**
     * Applies all changes that had been made to the work-in-progress file to
     * the stable file.
     */
    public void applyChanges()
    {
        final String wipFileName = workInProgressFile.getAbsolutePath();

        // replace stable file with wip file
        if (stableFile.exists())
            stableFile.delete();

        workInProgressFile.renameTo(stableFile);

        // restore the path to the wip file
        workInProgressFile = new File(wipFileName);
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

                // skip the harvesterHash
                reader.beginObject();
                reader.nextName();
                reader.skipValue();
                reader.nextName();
                reader.beginObject();

                // iterate through all entries
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

            // copy the harvesterHash
            reader.beginObject();
            writer.beginObject();

            writer.name(reader.nextName());
            writer.value(reader.nextString());

            writer.name(reader.nextName());
            writer.beginObject();
            reader.beginObject();

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
            reader.endObject();
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
