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

import com.google.gson.stream.JsonReader;

import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.utils.cache.constants.CacheConstants;

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
public class DocumentVersionsCache extends AbstractCache<String>
{
    /**
     * Constructor that requires the harvester name for the creation of a folder.
     *
     * @param harvesterName the name of the harvester for which the cache is created
     */
    public DocumentVersionsCache(final String harvesterName)
    {
        super(
            String.format(
                CacheConstants.STABLE_VERSIONS_FOLDER_PATH,
                MainContext.getModuleName(),
                harvesterName),
            String.format(
                CacheConstants.TEMP_VERSIONS_FOLDER_PATH,
                MainContext.getModuleName(),
                harvesterName),
            String.class);
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


    @Override
    public void applyChanges()
    {
        FileUtils.integrateDirectory(new File(wipFolderPath), new File(stableFolderPath), true);
    }


    @Override
    @Deprecated
    protected void migrateToNewSystem()
    {
        final String harvesterName = new File(stableFolderPath).getParentFile().getName();
        final File stableFile = new File(
            String.format(
                CacheConstants.VERSIONS_CACHE_FILE_PATH,
                MainContext.getModuleName(),
                harvesterName));

        if (stableFile.exists()) {
            try
                (JsonReader reader = new JsonReader(
                    new InputStreamReader(
                        new FileInputStream(stableFile),
                        MainContext.getCharset()))) {

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
                    diskIo.writeObjectToFile(getFile(documentId, true), documentHash);
                }

            } catch (IOException e) {
                return;
            }

            // delete old file
            FileUtils.deleteFile(stableFile);
        }
    }
}
