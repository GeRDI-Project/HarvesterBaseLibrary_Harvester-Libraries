/**
 * Copyright © 2017 Robin Weiss (http://www.gerdi-project.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.gerdiproject.harvest.utils.cache;

import java.io.File;

import de.gerdiproject.harvest.IDocument;
import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.application.events.ContextDestroyedEvent;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.harvester.AbstractHarvester;
import de.gerdiproject.harvest.harvester.events.GetProviderNameEvent;
import de.gerdiproject.harvest.save.HarvestSaver;
import de.gerdiproject.harvest.submission.AbstractSubmitter;
import de.gerdiproject.harvest.utils.HashGenerator;
import de.gerdiproject.harvest.utils.cache.constants.CacheConstants;
import de.gerdiproject.json.datacite.DataCiteJson;

/**
 * This class serves as a cache for harvested documents. The cached documents
 * can be saved to disk along with harvesting related metadata using the
 * {@linkplain HarvestSaver}, or submitted to a Database via an
 * {@linkplain AbstractSubmitter}.
 *
 * @author Robin Weiss
 */
public class HarvesterCache
{
    private final DocumentVersionsCache versionsCache;
    private final DocumentChangesCache changesCache;
    private final HashGenerator hashGenerator;
    private final String harvesterId;
    private final File harvesterCacheFolder;


    /**
     * Constructs a cache for a harvester.
     *
     * @param harvester the harvester of which the documents are to be cached
     */
    public HarvesterCache(final AbstractHarvester harvester)
    {
        this.hashGenerator = new HashGenerator(MainContext.getCharset());

        // set harvesterID
        final String providerName = EventSystem.sendSynchronousEvent(new GetProviderNameEvent());
        this.harvesterId = providerName + harvester.getName();

        // set cache folder
        this.harvesterCacheFolder = new File(
            String.format(
                CacheConstants.TEMP_CHANGES_FOLDER_PATH,
                MainContext.getModuleName(),
                harvester
            )).getParentFile();

        // set versions and changes caches
        this.versionsCache = new DocumentVersionsCache(harvester);
        this.changesCache = new DocumentChangesCache(harvester);

        EventSystem.addListener(ContextDestroyedEvent.class, this::onContextDestroyed);

        HarvesterCacheManager.instance().registerCache(this);
    }


    /**
     * Returns the cache that contains updated and new documents.
     *
     * @return the cache that contains updated and new documents
     */
    public DocumentChangesCache getChangesCache()
    {
        return changesCache;
    }


    /**
     * Returns the cache that contains hash values of already harvested
     * documents.
     *
     * @return the cache that contains hash values of already harvested
     *         documents
     */
    public DocumentVersionsCache getVersionsCache()
    {
        return versionsCache;
    }


    /**
     * Skips all documents of the version cache, removing them from the list of
     * documents that are to be submitted.
     */
    public void skipAllDocuments()
    {
        versionsCache.forEach((String documentId, String documentHash) -> {
            changesCache.removeFile(documentId);
            return true;
        });
    }


    /**
     * Checks if a document has changed since the last harvest and either skips
     * it, or adds it to the changes cache.
     *
     * @param doc the document that is to be processed
     */
    public void cacheDocument(IDocument doc)
    {
        if (hasDocumentChanged(doc))
            addDocument(doc);
        else
            skipDocument(doc);
    };


    /**
     * Writes a document to the cache file.
     *
     * @param doc the document that is to be written to the cache
     */
    public void addDocument(IDocument doc)
    {
        final String documentId = getDocumentId(doc);

        if (doc instanceof DataCiteJson)
            changesCache.putFile(documentId, (DataCiteJson) doc);

        versionsCache.putFile(documentId, hashGenerator.getShaHash(doc.toJson()));
    }


    /**
     * Applies all cache changes that were caused by the latest harvest.
     *
     * @param isAborted if true, the harvest was aborted
     * @param isSuccessful if true, the harvest was completed
     */
    public void applyChanges(boolean isSuccessful, boolean isAborted)
    {
        changesCache.applyChanges();

        if (isSuccessful && !isAborted) {
            changesCache.forEach((String documentId, DataCiteJson document) -> {
                if (document == null)
                    versionsCache.removeFile(documentId);
                return true;
            });
        }

        versionsCache.applyChanges();
    }


    /**
     * Initializes all caches.
     *
     * @param hash the hash value that represents the entire source data of the
     *            harvester
     * @param harvestStartIndex the start index of the harvesting range
     * @param harvestEndIndex the exclusive end index of the harvesting range
     */
    public void init(String hash, int harvestStartIndex, int harvestEndIndex)
    {
        final String harvesterHash = hash == null
                                     ? null
                                     : hashGenerator.getShaHash(hash + harvestStartIndex + harvestEndIndex);
        versionsCache.init(harvesterHash);
        changesCache.init(versionsCache);
    }


    /**
     * Removes a document from the deletion cache, but does not add it to the
     * changes cache.
     *
     * @param doc the document that is to be skipped
     */
    private void skipDocument(IDocument doc)
    {
        final String documentId = getDocumentId(doc);
        changesCache.removeFile(documentId);
    }


    /**
     * Assembles a unique identifier of a document.
     *
     * @param doc the document of which an ID is to be created
     *
     * @return a unique identifier of a document
     */
    private String getDocumentId(IDocument doc)
    {
        return hashGenerator.getShaHash(harvesterId + doc.getSourceId());
    }


    /**
     * Checks whether the hash of a document differs from that of the versions
     * cache.
     *
     * @param doc the document that is to be checked
     *
     * @return true, if the hash value of the document has changed
     */
    private boolean hasDocumentChanged(IDocument doc)
    {
        final String documentId = getDocumentId(doc);
        final String oldHash = versionsCache.getFileContent(documentId);

        if (oldHash == null)
            return true;

        final String currentHash = hashGenerator.getShaHash(doc.toJson());
        return !oldHash.equals(currentHash);
    }


    //////////////////////////////
    // Event Callback Functions //
    //////////////////////////////

    /**
     * This callback function is called when the web service is terminated. It
     * cleans up temporary files.
     *
     * @param event the event that triggered the callback
     */
    private void onContextDestroyed(ContextDestroyedEvent event) // NOPMD - Event callbacks always require the event
    {
        FileUtils.deleteFile(harvesterCacheFolder);
    }
}
