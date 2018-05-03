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
import java.util.function.BiFunction;

import com.google.gson.stream.JsonReader;

import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.utils.cache.constants.CacheConstants;
import de.gerdiproject.harvest.utils.data.DiskIO;
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
    private final DiskIO diskIo;
    private final String stableFolderPath;
    private final String wipFolderPath;


    /**
     * Assembles the file path to a version file.
     *
     * @param documentId the document of which the version file path is retrieved
     * @param isStable if true, the file points to the stable folder
     *
     * @return a path to the version file of the document with the specified documentId
     */
    private File getVersionFile(final String documentId, boolean isStable)
    {
        return new File(
                   String.format(
                       CacheConstants.VERSION_FILE_PATH,
                       isStable ? stableFolderPath : wipFolderPath,
                       documentId.substring(0, 2),
                       documentId.substring(2)));
    }


    /**
     * Constructor that requires the file name prefix of the cache files that
     * are to be created.
     *
     * @param filePrefix the file name prefix of the cache files that are to be
     *            created
     */
    public DocumentVersionsCache(final String filePrefix)
    {
        this.stableFolderPath =
            String.format(
                CacheConstants.STABLE_FOLDER_PATH,
                MainContext.getModuleName(),
                filePrefix);

        this.wipFolderPath =
            String.format(
                CacheConstants.TEMP_FOLDER_PATH,
                MainContext.getModuleName(),
                filePrefix);

        this.diskIo = new DiskIO();

        // if outdated caches exist, migrate them to folder structure
        migrateToNewSystem(filePrefix);
    }


    /**
     * Migrates the versions cache file from RestfulHarvester-Library
     * version 6.5.0 and below to the new folder structure, introduced in 6.5.1.
     *
     * @param filePrefix the filePrefix of the cache file
     */
    public void migrateToNewSystem(final String filePrefix)
    {
        final File stableFile = new File(
            String.format(
                CacheConstants.OLD_VERSIONS_CACHE_FILE_PATH,
                MainContext.getModuleName(),
                filePrefix));

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

                while (reader.hasNext()) {
                    final String documentId = reader.nextName();
                    final String documentHash = reader.nextString();
                    diskIo.writeStringToFile(getVersionFile(documentId, true), documentHash);
                }

                // TODO delete stable file

                // close reader
                reader.close();
            } catch (IOException e) { // NOPMD if we could not migrate, we don't want to
            }
        }
    }


    /**
     * Initializes the cache by copying the existing cache file to the WIP file.
     *
     * @param hash the hash value that represents a version of the source data
     */
    public void init(String hash)
    {
        // read old source hash from stable file
        final File stableHarvesterHash =
            new File(String.format(
                         CacheConstants.SOURCE_HASH_FILE_PATH,
                         stableFolderPath));

        // copy documents from stable file
        if (stableHarvesterHash.exists())
            FileUtils.copyFile(new File(stableFolderPath), new File(wipFolderPath));

        // write new source hash to wip-file
        final File wipSourceHash = new File(
            String.format(
                CacheConstants.SOURCE_HASH_FILE_PATH,
                wipFolderPath
            ));
        FileUtils.deleteFile(wipSourceHash);
        diskIo.writeStringToFile(wipSourceHash, hash);
    }


    /**
     * Checks if the harvested documents are outdated.
     *
     * @return true if the harvester hash changed since the last harvest
     */
    public boolean isOutdated()
    {
        // read stable source hash
        final String stableHarvesterHash = diskIo.getString(
                                               String.format(
                                                   CacheConstants.SOURCE_HASH_FILE_PATH,
                                                   stableFolderPath));

        if (stableHarvesterHash == null)
            return true;

        // read work-in-progress source hash
        final String wipHarvesterHash = diskIo.getString(
                                            String.format(
                                                CacheConstants.SOURCE_HASH_FILE_PATH,
                                                wipFolderPath));

        return !stableHarvesterHash.equals(wipHarvesterHash);
    }


    /**
     * Deletes the work-in-progress cache folder.
     */
    public void clearWorkInProgress()
    {
        FileUtils.deleteFile(new File(wipFolderPath));
    }


    /**
     * Applies all changes that had been made to the work-in-progress file to
     * the stable file.
     */
    public void applyChanges()
    {
        FileUtils.replaceFile(new File(stableFolderPath), new File(wipFolderPath));
    }


    /**
     * Deletes all document versions that have null values in a specified changes cache.
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
     * Iterates through all files of the stable folder and executes a
     * specified function on each retrieved documentId-documentHash pair.
     * If the function returns false, the process is aborted.
     *
     * @param entryFunction a function that accepts a documentId and
     *            documentHash and returns true if the iteration should continue
     *
     * @return true if all entries were processed
     */
    public boolean forEach(BiFunction<String, String, Boolean> entryFunction)
    {
        boolean isSuccessful = false;

        final File stableDir = new File(stableFolderPath);

        if (stableDir.exists()) {
            try
                (DirectoryStream<Path> prefixStream = Files.newDirectoryStream(stableDir.toPath())) {
                for (Path prefixFolder : prefixStream) {

                    // skip files of the top folder
                    if (!prefixFolder.toFile().isDirectory())
                        continue;

                    // iterate through the sub folder
                    final String documentIdPrefix = prefixFolder.toAbsolutePath().getFileName().toString();

                    try
                        (DirectoryStream<Path> suffixStream = Files.newDirectoryStream(prefixFolder)) {
                        for (Path suffixFile : suffixStream) {
                            final String documentId = documentIdPrefix + suffixFile.getFileName().toString();
                            final String documentHash = diskIo.getString(suffixFile.toString());

                            isSuccessful = entryFunction.apply(documentId, documentHash);

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
     * Adds or removes a version file in the work-in-progress folder. If
     * the documentHash is null, the file with the corresponding id is deleted.
     *
     * @param documentId the identifier of the document that is to be
     *            changed/added
     * @param documentHash the value that is to be assigned to the documentId,
     *            or null if the documentId is to be removed
     */
    public void putDocumentHash(final String documentId, final String documentHash)
    {
        final File documentFile = getVersionFile(documentId, false);
        FileUtils.deleteFile(documentFile);

        if (documentHash != null)
            diskIo.writeStringToFile(documentFile, documentHash);
    }


    /**
     * Retrieves the document hash for a specified document ID from the stable
     * cache folder.
     *
     * @param documentId the identifier of the document of which the hash is
     *            retrieved
     * @return a hash value or null, if no entry exists for the document
     */
    public String getDocumentHash(String documentId)
    {
        final File documentFile = getVersionFile(documentId, true);
        return diskIo.getString(documentFile);
    }


    /*

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


    public boolean isOutdated()
    {
        return stableHarvesterHash == null || !stableHarvesterHash.equals(workInProgressHarvesterHash);
    }


    public void clearWorkInProgress()
    {
        FileUtils.deleteFile(new File(workInProgressFile.getAbsolutePath() + CacheConstants.TEMP_FILE_EXTENSION));
        FileUtils.deleteFile(workInProgressFile);
        this.workInProgressHarvesterHash = stableHarvesterHash;
    }


    public void applyChanges()
    {
        FileUtils.replaceFile(stableFile, workInProgressFile);
        this.stableHarvesterHash = workInProgressHarvesterHash;
    }


    public void removeDeletedEntries(final DocumentChangesCache changesCache)
    {
        changesCache.forEach((String documentId, DataCiteJson document) -> {
            if (document == null)
                putDocumentHash(documentId, null);
            return true;
        });
    }


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
    */
}
