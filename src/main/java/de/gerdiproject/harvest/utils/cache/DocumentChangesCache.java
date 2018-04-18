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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import de.gerdiproject.harvest.IDocument;
import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.utils.cache.constants.CacheConstants;
import de.gerdiproject.json.GsonUtils;
import de.gerdiproject.json.datacite.DataCiteJson;

/**
 * This class wraps a cache file that stores a map of document identifiers to
 * document JSON. It is used for determining which files need to be added or
 * changed for the submission
 *
 * @author Robin Weiss
 */
public class DocumentChangesCache
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentChangesCache.class);

    private final File workInProgressFile;
    private final File stableFile;
    private final Gson gson;
    private int size;


    /**
     * Constructor that requires the file name prefix of the cache file that is
     * to be created.
     *
     * @param filePrefix the file name prefix of the changes cache file
     */
    public DocumentChangesCache(final String filePrefix)
    {
        this.gson = GsonUtils.getGson();
        this.stableFile = new File(
            String.format(
                CacheConstants.UPDATE_CACHE_FILE_PATH,
                MainContext.getModuleName(),
                filePrefix));

        this.workInProgressFile = new File(
            String.format(
                CacheConstants.UPDATE_CACHE_TEMP_FILE_PATH,
                MainContext.getModuleName(),
                filePrefix));
    }


    /**
     * Initializes the cache by creating an empty file and copying all
     * documentIDs from a versions cache as null entries.
     *
     * @param versionsCache a cache of previously harvested IDs
     */
    public void init(DocumentVersionsCache versionsCache)
    {
        // create new file
        try {
            // retrieve harvester cache metadata

            FileUtils.createEmptyFile(workInProgressFile);
            final JsonWriter writer = new JsonWriter(
                new OutputStreamWriter(
                    new FileOutputStream(workInProgressFile),
                    MainContext.getCharset()));

            final AtomicInteger numberOfCopiedIds = new AtomicInteger(0);

            // copy documentIds and count them
            boolean isSuccessful = false;
            writer.beginObject();
            isSuccessful = versionsCache.forEach((String documentId, String documentHash) -> {
                try
                {
                    writer.name(documentId);
                    writer.nullValue();
                } catch (IOException e)
                {
                    return false;
                }
                numberOfCopiedIds.incrementAndGet();
                return true;
            });

            // close writer
            writer.endObject();
            writer.close();

            if (isSuccessful)
                this.size = numberOfCopiedIds.get();
            else
                this.size = 0;

        } catch (IOException e) {
            LOGGER.error(String.format(CacheConstants.CACHE_CREATE_FAILED, workInProgressFile.getAbsolutePath()));
        }
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


    /**
     * Removes all work-in-progress timestamps and deletes the work-in-progress
     * cache file.
     */
    public void clearWorkInProgress()
    {
        FileUtils.deleteFile(workInProgressFile);
    }


    /**
     * (Over{@literal-})writes a stable cache file of document changes, with the
     * work in progress changes.
     *
     */
    public void applyChanges()
    {
        FileUtils.replaceFile(stableFile, workInProgressFile);
    }


    /**
     * Iterates through the stable file of cached documents and executes a
     * function on each document. If the function returns false, the whole
     * process is aborted.
     *
     * @param documentFunction a function that accepts a documentId and the
     *            corresponding document JSON object and returns true if it was
     *            successfully processed
     *
     * @return true if all documents were processed successfully
     */
    public boolean forEach(BiFunction<String, DataCiteJson, Boolean> documentFunction)
    {
        // prepare variables
        boolean isFunctionSuccessful = true;

        if (size > 0) {
            try {
                // prepare json reader for the cached document list
                final JsonReader reader = new JsonReader(
                    new InputStreamReader(new FileInputStream(stableFile), MainContext.getCharset()));

                // iterate through cached documents
                reader.beginObject();

                while (isFunctionSuccessful && reader.hasNext()) {
                    final String documentId = reader.nextName();
                    final DataCiteJson addedDoc;

                    if (reader.peek() == JsonToken.NULL) {
                        addedDoc = null;
                        reader.skipValue();
                    } else
                        addedDoc = gson.fromJson(reader, DataCiteJson.class);

                    isFunctionSuccessful = documentFunction.apply(documentId, addedDoc);
                }

                // close reader
                if (!isFunctionSuccessful)
                    reader.endObject();

                reader.close();
            } catch (IOException e) {
                isFunctionSuccessful = false;
            }
        }

        return isFunctionSuccessful;
    }


    /**
     * Removes a document entry from the cache.
     *
     * @param documentId the ID of the document that is to be removed
     */
    public void removeDocument(String documentId)
    {
        putDocument(documentId, null);
    }


    /**
     * Adds a key-value pair to the cache. The key is the unique documentId and
     * the value is the JSON representation of the document.
     *
     * @param documentId the unique document identifier
     * @param document the JSON representation of the document or null, if the
     *            document is to be removed
     */
    public void putDocument(final String documentId, final IDocument document)
    {
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
            while (reader.hasNext()) {
                final String readKey = reader.nextName();
                final JsonElement readValue = gson.fromJson(reader, JsonElement.class);

                if (readKey.equals(documentId)) {
                    size--;
                    break;
                } else {
                    writer.name(readKey);

                    if (readValue.isJsonNull())
                        writer.nullValue();
                    else
                        gson.toJson(readValue, JsonElement.class, writer);
                }
            }

            // add specified key-value pair
            if (document != null) {
                writer.name(documentId);
                gson.toJson(document, document.getClass(), writer);
                size++;
            }

            // if any old key-value pairs remain in the source file, copy them
            while (reader.hasNext()) {
                final String readKey = reader.nextName();
                final JsonElement readValue = gson.fromJson(reader, JsonElement.class);
                writer.name(readKey);

                if (readValue.isJsonNull())
                    writer.nullValue();
                else
                    gson.toJson(readValue, JsonElement.class, writer);
            }

            // close reader
            reader.close();

            // close writer
            writer.endObject();
            writer.close();

        } catch (IOException e) {
            LOGGER.error(String.format(CacheConstants.ENTRY_STREAM_WRITE_ERROR, tempFile.getAbsolutePath()));
            return;
        }

        // replace current file with temporary file
        FileUtils.replaceFile(workInProgressFile, tempFile);
    }


    /**
     * Retrieves the document JSON for a specified document ID from the cache
     * file, or null if this document is to be deleted.
     *
     * @param documentId the identifier of the document which is to be retrieved
     * @return a JSON document or null, if no entry exists for the document
     */
    public DataCiteJson getDocument(String documentId)
    {
        DataCiteJson document = null;

        if (stableFile.exists()) {
            try {
                // prepare json reader for the cached document list
                final JsonReader reader = new JsonReader(
                    new InputStreamReader(new FileInputStream(stableFile), MainContext.getCharset()));

                reader.beginObject();

                while (reader.hasNext()) {
                    final String id = reader.nextName();

                    if (id.equals(documentId)) {
                        if (reader.peek() == JsonToken.BEGIN_OBJECT)
                            document = gson.fromJson(reader, DataCiteJson.class);

                        break;
                    }

                    reader.skipValue();
                }

                reader.close();
            } catch (IOException e) { // NOPMD - nothing to do here, document is null by default
            }
        }

        return document;
    }
}
