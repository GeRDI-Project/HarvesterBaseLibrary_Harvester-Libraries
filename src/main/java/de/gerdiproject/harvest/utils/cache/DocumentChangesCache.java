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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import de.gerdiproject.harvest.IDocument;
import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.utils.cache.constants.CacheConstants;
import de.gerdiproject.harvest.utils.data.DiskIO;
import de.gerdiproject.json.GsonUtils;
import de.gerdiproject.json.datacite.DataCiteJson;

/**
 * This class manages a cache folder that represents a mapping of document identifiers to
 * document JSON. It is used for determining which files need to be added or
 * changed for the submission.
 *
 * @author Robin Weiss
 */
public class DocumentChangesCache
{
    private final DiskIO diskIo;
    private final String stableFolderPath;
    private final String wipFolderPath;

    private int size;


    /**
     * Constructor that requires the harvester name for the creation of a dedicated
     * folder.
     *
     * @param harvesterName the name of the harvester for which the cache is created
     */
    public DocumentChangesCache(final String harvesterName)
    {
        this.stableFolderPath =
            String.format(
                CacheConstants.STABLE_CHANGES_FOLDER_PATH,
                MainContext.getModuleName(),
                harvesterName);

        this.wipFolderPath =
            String.format(
                CacheConstants.TEMP_CHANGES_FOLDER_PATH,
                MainContext.getModuleName(),
                harvesterName);

        this.diskIo = new DiskIO(false);

        // if outdated caches exist, migrate them to folder structure
        migrateToNewSystem(harvesterName);
    }


    /**
     * Migrates the changes cache file from RestfulHarvester-Library
     * version 6.5.0 and below to the new folder structure, introduced in 6.5.1.
     *
     * @param filePrefix the filePrefix of the cache file
     */
    @SuppressWarnings("deprecation")
    public void migrateToNewSystem(final String filePrefix)
    {
        final File stableFile = new File(
            String.format(
                CacheConstants.UPDATE_CACHE_FILE_PATH,
                MainContext.getModuleName(),
                filePrefix));

        // skip if there is no old cache file
        if (stableFile.exists()) {
            final Gson gson = GsonUtils.getGson();

            try {
                // prepare json reader for the cached document list
                final JsonReader reader = new JsonReader(
                    new InputStreamReader(new FileInputStream(stableFile), MainContext.getCharset()));

                // iterate through cached documents
                reader.beginObject();

                while (reader.hasNext()) {
                    final String documentId = reader.nextName();
                    final File file = getDocumentFile(documentId, true);

                    if (reader.peek() == JsonToken.NULL) {
                        FileUtils.createEmptyFile(file);
                        reader.skipValue();
                    } else
                        diskIo.writeObjectToFile(file, gson.fromJson(reader, DataCiteJson.class));
                }

                // close reader
                reader.close();

                // delete old file
                FileUtils.deleteFile(stableFile);

            } catch (IOException e) { // NOPMD if something goes wrong, do not convert the cache
            }
        }
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
            FileUtils.createEmptyFile(getDocumentFile(documentId, false));
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


    /**
     * Replaces the stable folder with the work-in-progress changes.
     */
    public void applyChanges()
    {
        FileUtils.replaceFile(new File(stableFolderPath), new File(wipFolderPath));
    }


    /**
     * Iterates through the stable folder of cached documents and executes a
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
        boolean isSuccessful = true;

        final File stableDir = new File(stableFolderPath);

        if (stableDir.exists() && size > 0) {
            try
                (DirectoryStream<Path> prefixStream = Files.newDirectoryStream(stableDir.toPath())) {
                for (Path prefixFolderPath : prefixStream) {
                    final File prefixFolder = prefixFolderPath.toFile();

                    // skip files of the top folder
                    if (!prefixFolder.isDirectory())
                        continue;

                    // iterate through the sub folder
                    final String documentIdPrefix = prefixFolder.getName();

                    // iterate through the sub folder
                    try
                        (DirectoryStream<Path> suffixStream = Files.newDirectoryStream(prefixFolderPath)) {
                        for (Path suffixFile : suffixStream) {
                            // the file name without extension is the suffix of the document ID
                            String documentIdSuffix = suffixFile.getFileName().toString();
                            documentIdSuffix = documentIdSuffix.substring(0, documentIdSuffix.lastIndexOf('.'));

                            // assemble documentID
                            final String documentId = documentIdPrefix + documentIdSuffix;

                            final File documentFile = getDocumentFile(documentId, true);
                            final DataCiteJson document = documentFile.length() == 0
                                                          ? null
                                                          : diskIo.getObject(documentFile, DataCiteJson.class);

                            isSuccessful = documentFunction.apply(documentId, document);

                            if (!isSuccessful)
                                break;
                        }
                    }

                    if (!isSuccessful)
                        break;
                }
            } catch (IOException e) {
                isSuccessful = false;
            }
        }

        return isSuccessful;
    }


    /**
     * Removes a document from the cache.
     *
     * @param documentId the ID of the document that is to be removed
     */
    public void removeDocument(String documentId)
    {
        putDocument(documentId, null);
    }


    /**
     * Adds or replaces a document in the work-in-progress folder.
     * The key is the unique documentID and the value is the JSON representation of the document.
     *
     * @param documentId the unique document identifier
     * @param document the JSON representation of the document or null, if the
     *            document is only to be removed
     */
    public void putDocument(final String documentId, final IDocument document)
    {
        final File documentFile = getDocumentFile(documentId, false);

        // decrement size when we remove the previous document, if it existed
        if (documentFile.exists()) {
            FileUtils.deleteFile(documentFile);
            size--;
        }

        // write new file, increment size
        if (document != null) {
            diskIo.writeObjectToFile(documentFile, document);
            size++;
        }
    }


    /**
     * Retrieves the document object for a specified document ID from the cache
     * folder, or null if this document does not exist or is empty.
     *
     * @param documentId the identifier of the document which is to be retrieved
     * @return a JSON document or null, if no document exists
     */
    public DataCiteJson getDocument(String documentId)
    {
        final File retrievedDoc = getDocumentFile(documentId, true);

        if (retrievedDoc.exists() && retrievedDoc.length() > 0)
            return diskIo.getObject(retrievedDoc, DataCiteJson.class);
        else
            return null;
    }

    /**
     * Assembles the file path to a document file.
     *
     * @param documentId the documentID of which the document object is retrieved
     * @param isStable if true, the file points to the stable folder
     *
     * @return a path to the document file with the specified documentId
     */
    private File getDocumentFile(final String documentId, boolean isStable)
    {
        return new File(
                   String.format(
                       CacheConstants.DOCUMENT_HASH_FILE_PATH,
                       isStable ? stableFolderPath : wipFolderPath,
                       documentId.substring(0, 2),
                       documentId.substring(2)));
    }
}
