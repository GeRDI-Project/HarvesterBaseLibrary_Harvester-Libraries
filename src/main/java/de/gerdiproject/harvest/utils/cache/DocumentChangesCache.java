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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
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
    private final File cacheFile;
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
        this.cacheFile = new File(
                String.format(
                        CacheConstants.ADDITION_CACHE_FILE_PATH,
                        MainContext.getModuleName(),
                        filePrefix));
    }


    /**
     * Initializes the cache by creating an empty file and setting the size to
     * zero.
     */
    public void init()
    {
        // remove old file
        if (cacheFile.exists())
            cacheFile.delete();

        // create new file
        try {
            cacheFile.createNewFile();
        } catch (IOException e) {

        }

        this.size = 0;
    }


    /**
     * Returns the number of cached documents.
     * 
     * @return the number of cached documents
     */
    public int getSize()
    {
        return size;
    }


    /**
     * Iterates through the file of cached documents and executes a function on
     * each document. If the function returns false, the whole process is
     * aborted.
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

        try {
            // prepare json reader for the cached document list
            final JsonReader reader = new JsonReader(
                    new InputStreamReader(new FileInputStream(cacheFile), MainContext.getCharset()));

            // iterate through cached array
            reader.beginArray();

            while (isFunctionSuccessful && reader.hasNext()) {
                final String documentId = reader.nextName();
                final DataCiteJson addedDoc = gson.fromJson(reader, DataCiteJson.class);
                isFunctionSuccessful = documentFunction.apply(documentId, addedDoc);
            }

            // close reader
            if (!isFunctionSuccessful)
                reader.endArray();

            reader.close();
        } catch (IOException e) {
            isFunctionSuccessful = false;
        }

        return isFunctionSuccessful;
    }


    public void putDocument(final String documentId, final IDocument document)
    {
        final File tempFile = new File(cacheFile.getAbsolutePath() + CacheConstants.TEMP_FILE_EXTENSION);
        try {
            // prepare json reader for the cached document list
            final JsonReader reader = new JsonReader(
                    new InputStreamReader(new FileInputStream(cacheFile), MainContext.getCharset()));

            final JsonWriter writer = new JsonWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(tempFile),
                            MainContext.getCharset()));

            reader.beginObject();
            writer.beginObject();

            // copy key value pairs until the specified key is found
            while (reader.hasNext()) {
                final String readKey = reader.nextName();
                final JsonObject readValue = gson.fromJson(reader, JsonObject.class);

                if (readKey.equals(documentId)) {
                    size--;
                    break;
                } else {
                    writer.name(readKey);
                    gson.toJson(readValue, JsonObject.class, writer);
                }
            }

            // add specified key-value pair
            writer.name(documentId);
            gson.toJson(document, document.getClass(), writer);
            size++;

            // if any old key-value pairs remain in the source file, copy them
            while (reader.hasNext()) {
                final String readKey = reader.nextName();
                final JsonObject readValue = gson.fromJson(reader, JsonObject.class);
                writer.name(readKey);
                gson.toJson(readValue, JsonObject.class, writer);
            }

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
        cacheFile.delete();
        tempFile.renameTo(cacheFile);
    }


    /**
     * Retrieves the document JSON for a specified document ID from the cache
     * file.
     * 
     * @param documentId the identifier of the document which is to be retrieved
     * @return a JSON document or null, if no entry exists for the document
     */
    public DataCiteJson getDocument(String documentId)
    {
        DataCiteJson document = null;

        if (cacheFile.exists()) {
            try {
                // prepare json reader for the cached document list
                final JsonReader reader = new JsonReader(
                        new InputStreamReader(new FileInputStream(cacheFile), MainContext.getCharset()));

                reader.beginObject();

                while (reader.hasNext()) {
                    final String id = reader.nextName();
                    final DataCiteJson readValue = gson.fromJson(reader, DataCiteJson.class);

                    if (id.equals(documentId)) {
                        document = readValue;
                        break;
                    }
                }
                reader.close();
            } catch (IOException e) { // NOPMD - nothing to do here, documentHash is null by default

            }
        }
        return document;
    }
}
