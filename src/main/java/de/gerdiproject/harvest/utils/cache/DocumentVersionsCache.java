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
 * This class manages a cache folder that represents a mapping of document identifiers to
 * document hash values. Inside the top folder, there is one folder for every combination
 * of the first two characters of the document identifiers. Inside each folder, there is
 * one file for the rest of every document identifier.
 *
 * Additionally, there is a work-in-progress folder that is created at the beginning of a
 * harvest and is merged to the stable folder if the harvest completes successfully.
 *
 * @author Robin Weiss
 */
public class DocumentVersionsCache
{
    private final DiskIO diskIo;
    private final String stableFolderPath;
    private final String wipFolderPath;


    /**
     * Constructor that requires the harvester name for the creation of a folder.
     *
     * @param harvesterName the name of the harvester for which the cache is created
     */
    public DocumentVersionsCache(final String harvesterName)
    {
        this.stableFolderPath =
            String.format(
                CacheConstants.STABLE_VERSIONS_FOLDER_PATH,
                MainContext.getModuleName(),
                harvesterName);

        this.wipFolderPath =
            String.format(
                CacheConstants.TEMP_VERSIONS_FOLDER_PATH,
                MainContext.getModuleName(),
                harvesterName);

        this.diskIo = new DiskIO(false);

        // if outdated caches exist, migrate them to folder structure
        migrateToNewSystem(harvesterName);
    }


    /**
     * Migrates the versions cache file from RestfulHarvester-Library
     * version 6.5.0 and below to the new folder structure, introduced in 6.5.1.
     *
     * @param filePrefix the filePrefix of the cache file
     */
    @SuppressWarnings("deprecation")
    private void migrateToNewSystem(final String filePrefix)
    {
        final File stableFile = new File(
            String.format(
                CacheConstants.VERSIONS_CACHE_FILE_PATH,
                MainContext.getModuleName(),
                filePrefix));

        if (stableFile.exists()) {
            try {
                // prepare json reader for the cached document list
                final JsonReader reader = new JsonReader(
                    new InputStreamReader(
                        new FileInputStream(stableFile),
                        MainContext.getCharset()));

                // retrieve harvester hash
                reader.beginObject();
                reader.nextName();
                final String sourceHash = reader.nextString();

                final File stableHarvesterHash =
                    new File(String.format(
                                 CacheConstants.SOURCE_HASH_FILE_PATH,
                                 stableFolderPath));
                diskIo.writeStringToFile(stableHarvesterHash, sourceHash);
                reader.nextName();

                // iterate through all entries
                reader.beginObject();

                while (reader.hasNext()) {
                    final String documentId = reader.nextName();
                    final String documentHash = reader.nextString();
                    diskIo.writeStringToFile(getVersionFile(documentId, true), documentHash);
                }

                // delete old file
                FileUtils.deleteFile(stableFile);

                // close reader
                reader.close();
            } catch (IOException e) { // NOPMD if we could not migrate, we don't want to
            }
        }
    }


    /**
     * Initializes the cache by creating a work-in-progress source hash file.
     *
     * @param hash the hash value that represents a version of the source data
     */
    public void init(String hash)
    {
        // write new source hash to wip-file
        final File wipSourceHash = new File(
            String.format(
                CacheConstants.SOURCE_HASH_FILE_PATH,
                wipFolderPath
            ));

        FileUtils.deleteFile(wipSourceHash);

        if (hash != null)
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
        final String stableHarvesterHash =
            diskIo.getString(
                String.format(
                    CacheConstants.SOURCE_HASH_FILE_PATH,
                    stableFolderPath));

        if (stableHarvesterHash == null)
            return true;

        // read work-in-progress source hash
        final String wipHarvesterHash =
            diskIo.getString(
                String.format(
                    CacheConstants.SOURCE_HASH_FILE_PATH,
                    wipFolderPath));

        return !stableHarvesterHash.equals(wipHarvesterHash);
    }


    /**
     * Applies all changes that had been made to the work-in-progress file to
     * the stable file.
     */
    public void applyChanges()
    {
        FileUtils.integrateDirectory(new File(stableFolderPath), new File(wipFolderPath), true);
    }


    /**
     * Deletes all document hashes that have null values in a specified changes cache.
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
                for (Path prefixFolderPath : prefixStream) {
                    final File prefixFolder = prefixFolderPath.toFile();

                    // skip files of the top folder
                    if (!prefixFolder.isDirectory())
                        continue;

                    // the folder name makes the first two characters of the documentId
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

                            // read file content as hash
                            final String documentHash = diskIo.getString(suffixFile.toString());

                            // apply the iterator function
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
                       CacheConstants.DOCUMENT_HASH_FILE_PATH,
                       isStable ? stableFolderPath : wipFolderPath,
                       documentId.substring(0, 2),
                       documentId.substring(2)));
    }
}
