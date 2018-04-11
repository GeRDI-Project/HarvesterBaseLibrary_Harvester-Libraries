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
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.harvester.events.GetProviderNameEvent;
import de.gerdiproject.harvest.save.HarvestSaver;
import de.gerdiproject.harvest.submission.AbstractSubmitter;
import de.gerdiproject.harvest.utils.HashGenerator;
import de.gerdiproject.harvest.utils.cache.constants.CacheConstants;
import de.gerdiproject.harvest.utils.cache.events.RegisterCacheEvent;

/**
 * This singleton class is a wrapper for a JSON file writer that writes
 * harvested documents as a JSON-array to a cache-file. The cached documents can
 * be saved to disk along with harvesting related metadata using the
 * {@linkplain HarvestSaver}, or submitted to a Database via an
 * {@linkplain AbstractSubmitter}.
 *
 * @author Robin Weiss
 */
public class DocumentsCache
{
    private final DocumentVersionsCache versionsCache;
    private final DocumentChangesCache changesCache;
    private final String harvesterId;


    /**
     * Constructs a cache for a harvester.
     * 
     * @param harvesterName the unique name of the harvester
     */
    public DocumentsCache(final String harvesterName)
    {
        final String providerName = EventSystem.sendSynchronousEvent(new GetProviderNameEvent());
        this.harvesterId = providerName + harvesterName;

        // create cache directory
        final File cacheDirectory =
                new File(String.format(CacheConstants.CACHE_FOLDER_PATH, MainContext.getModuleName()));
        final boolean isDirectoryCreated = cacheDirectory.exists() || cacheDirectory.mkdirs();

        this.versionsCache = new DocumentVersionsCache(harvesterName);
        this.changesCache = new DocumentChangesCache(harvesterName);

        EventSystem.sendEvent(new RegisterCacheEvent(this));
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
            changesCache.removeDocument(documentId);
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
        changesCache.putDocument(documentId, doc);
        versionsCache.putDocumentHash(documentId, HashGenerator.instance().getShaHash(doc));
    }


    /**
     * Initializes all caches.
     */
    public void init()
    {
        versionsCache.init();
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
        changesCache.removeDocument(documentId);
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
        return HashGenerator.instance().getShaHash(harvesterId + doc.getSourceId());
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
        final String currentHash = HashGenerator.instance().getShaHash(doc);
        final String oldHash = versionsCache.getDocumentHash(documentId);

        return oldHash == null || !oldHash.equals(currentHash);
    }
}
