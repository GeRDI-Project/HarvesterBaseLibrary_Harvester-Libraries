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
import java.util.function.Function;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.utils.cache.constants.CacheConstants;

/**
 * This class wraps a cache file that holds a JSON array of document identifiers
 * of documents that no longer exist, and should therefore be deleted. Upon
 * initialization, all document identifiers of a specified
 * {@linkplain DocumentVersionsCache} are copied over to this the deletion
 * cache. For every document id that was harvested, the corresponding element in
 * the deletion cache is removed. When the harvest finishes, only those document
 * identifiers remain, that no longer exist.
 * 
 * @author Robin Weiss
 */
public class DocumentDeletionsCache
{
    private final File cacheFile;
    private int size;


    /**
     * Constructor that requires the file name prefix of the deletion cache
     * file.
     * 
     * @param filePrefix a prefix of the file name of the deletion cache file
     */
    public DocumentDeletionsCache(final String filePrefix)
    {
        this.size = 0;
        this.cacheFile = new File(
                String.format(
                        CacheConstants.DELETION_CACHE_FILE_PATH,
                        MainContext.getModuleName(),
                        filePrefix));
    }


    /**
     * Initializes the deletion cache file and size by attempting to copy the
     * documentIDs of a {@linkplain DocumentVersionsCache}.
     * 
     * @param versionsCache the cache from which the documendIDs are retrieved
     */
    public void init(final DocumentVersionsCache versionsCache)
    {
        if (cacheFile.exists())
            cacheFile.delete();

        size = createDeletionCacheFile(versionsCache);
    }


    /**
     * Copies all documentIDs of a specified {@linkplain DocumentVersionsCache}
     * to a JSON array in the deletionCacheFile.
     * 
     * @param versionsCache the cache of which the documentIDs are retrieved
     * 
     * @return the number of copied documentIDs
     */
    private int createDeletionCacheFile(final DocumentVersionsCache versionsCache)
    {
        final AtomicInteger numberOfCopiedIds = new AtomicInteger(0);
        boolean isSuccessful = false;
        try {
            final JsonWriter writer = new JsonWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(cacheFile),
                            MainContext.getCharset()));

            // copy documentIds and count them
            writer.beginArray();
            isSuccessful = versionsCache.forEach((String documentId, String documentHash) -> {
                try {
                    writer.value(documentId);
                } catch (IOException e) {
                    return false;
                }
                numberOfCopiedIds.incrementAndGet();
                return true;
            });

            // close writer
            writer.endArray();
            writer.close();

        } catch (IOException e) {
        }

        if (!isSuccessful)
            numberOfCopiedIds.set(0);
        return numberOfCopiedIds.get();
    }


    /**
     * Returns the number of document IDs within the cached JSON array.
     * 
     * @return the number of document IDs within the cached JSON array
     */
    public int getSize()
    {
        return size;
    }


    public void removeDocumentId(final String removedId)
    {
        if (size == 0)
            return;

        boolean hasChanges = false;
        final File tempFile = new File(cacheFile.getAbsolutePath() + CacheConstants.TEMP_FILE_EXTENSION);
        try {
            // prepare json reader for the cached document list
            final JsonReader reader = new JsonReader(
                    new InputStreamReader(new FileInputStream(cacheFile), MainContext.getCharset()));

            final JsonWriter writer = new JsonWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(tempFile),
                            MainContext.getCharset()));

            reader.beginArray();
            writer.beginArray();

            // copy every element until the removed string is reached
            while (reader.hasNext()) {
                final String stringElement = reader.nextString();

                if (stringElement.equals(removedId)) {
                    hasChanges = true;
                    break;
                } else
                    writer.value(stringElement);
            }

            // copy the rest of the file
            while (reader.hasNext())
                writer.value(reader.nextString());

            // close reader
            reader.endArray();
            reader.close();

            // close writer
            writer.endArray();
            writer.close();

        } catch (IOException e) {
            return;
        }

        if (hasChanges) {
            // replace current file with temporary file
            cacheFile.delete();
            tempFile.renameTo(cacheFile);
            size--;
        } else
            tempFile.delete();
    }


    /**
     * Iterates through the file of cached documents that are to be deleted and
     * executes a function on each document. If the function returns false, the
     * whole process is aborted.
     * 
     * @param documentFunction a function that accepts a documentID and returns
     *            true if the document was successfully processed
     * 
     * @return true if all documents were processed successfully or no documents
     *         exist
     */
    public boolean forEach(Function<String, Boolean> documentFunction)
    {
        if (size == 0)
            return true;

        // prepare variables
        boolean isFunctionSuccessful = true;

        try {
            // prepare json reader for the cached document list
            final JsonReader reader = new JsonReader(
                    new InputStreamReader(new FileInputStream(cacheFile), MainContext.getCharset()));

            // iterate through cached array
            reader.beginArray();

            while (isFunctionSuccessful && reader.hasNext())
                isFunctionSuccessful = documentFunction.apply(reader.nextString());

            reader.close();
        } catch (IOException e) {
            isFunctionSuccessful = false;
        }

        return isFunctionSuccessful;
    }
}
