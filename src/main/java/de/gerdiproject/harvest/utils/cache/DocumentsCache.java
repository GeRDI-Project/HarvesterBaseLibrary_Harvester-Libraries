/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.stream.JsonWriter;

import de.gerdiproject.harvest.IDocument;
import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.harvester.events.DocumentHarvestedEvent;
import de.gerdiproject.harvest.harvester.events.HarvestFinishedEvent;
import de.gerdiproject.harvest.harvester.events.HarvestStartedEvent;
import de.gerdiproject.harvest.save.HarvestSaver;
import de.gerdiproject.harvest.save.events.StartSaveEvent;
import de.gerdiproject.harvest.state.events.AbortingFinishedEvent;
import de.gerdiproject.harvest.submission.AbstractSubmitter;
import de.gerdiproject.harvest.submission.events.StartSubmissionEvent;
import de.gerdiproject.harvest.utils.cache.constants.CacheConstants;
import de.gerdiproject.json.GsonUtils;

/**
 * This singleton class is a wrapper for a JSON file writer that writes harvested documents as a JSON-array to a cache-file.
 * The cached documents can be saved to disk along with harvesting related metadata using the {@linkplain HarvestSaver}, or submitted to a Database
 * via an {@linkplain AbstractSubmitter}.
 *
 * @author Robin Weiss
 */
public class DocumentsCache
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentsCache.class);
    private static final DocumentsCache instance = new DocumentsCache();

    private AbstractSubmitter submitter;
    private HarvestSaver saver;
    private int documentCount;
    private JsonWriter cacheWriter;
    private File cacheFile;
    private String documentHash;


    /**
     * Event callback: When a harvest starts, the cache writer is opened.
     */
    private Consumer<HarvestStartedEvent> onHarvestStarted = (HarvestStartedEvent e) -> {
        startCaching();
    };


    /**
     * Event callback: When a harvest finishes, the cache writer is closed.
     */
    private Consumer<HarvestFinishedEvent> onHarvestFinished = (HarvestFinishedEvent e) -> {
        documentHash = e.getDocumentChecksum();
        finishCaching();
    };


    /**
     * Event callback: When a document is harvested, write it to the cache file.
     */
    private Consumer<DocumentHarvestedEvent> onDocumentHarvested = (DocumentHarvestedEvent e) -> {
        if (e.getDocument() != null)
            addDocument(e.getDocument());
    };


    /**
     * Event callback: When a submission starts, submit the cache file via the {@linkplain AbstractSubmitter}.
     */
    private Consumer<StartSubmissionEvent> onStartSubmitting = (StartSubmissionEvent e) -> {
        submitter.submit(cacheFile, documentCount);
    };


    /**
     * Event callback: When a save starts, save the cache file via the {@linkplain HarvestSaver}.
     */
    private Consumer<StartSaveEvent> onStartSaving = (StartSaveEvent e) -> {
        saver.save(cacheFile, documentHash, documentCount, e.isAutoTriggered());
    };


    /**
     * Event callback: When an abortion is finished, close the cache streaming, so all data gets submitted
     * if we want to submit an incomplete amount of documents.
     */
    private Consumer<AbortingFinishedEvent> onAbortingFinished = (AbortingFinishedEvent e) -> {
        finishCaching();
    };


    /**
     * Private constructor for the singleton instance.
     */
    private DocumentsCache()
    {
        this.documentCount = 0;
        this.cacheFile = new File(
            String.format(
                CacheConstants.CACHE_FILE_PATH,
                MainContext.getModuleName(),
                new Date().getTime()
            ));
    }


    /**
     * Removes all cache files that are no longer in use by this harvester.
     */
    private void clearOldCacheFiles()
    {
        String cacheDirPath = String.format(CacheConstants.CACHE_FOLDER_PATH, MainContext.getModuleName());
        File cacheDir = new File(cacheDirPath);

        if (cacheDir.exists() && cacheDir.isDirectory()) {
            File[] oldCacheFiles;

            // try to get all cache files in the folder
            try {
                oldCacheFiles = cacheDir.listFiles(new CacheFilenameFilter(cacheFile));
            } catch (SecurityException e) {
                oldCacheFiles = null;
            }

            // only continue if there are files
            if (oldCacheFiles != null) {
                for (File oldCache : oldCacheFiles) {
                    boolean deleteSuccess;

                    try {
                        deleteSuccess = oldCache.delete();
                    } catch (SecurityException e) {
                        deleteSuccess = false;
                    }

                    if (deleteSuccess)
                        LOGGER.info(String.format(CacheConstants.DELETE_FILE_SUCCESS, oldCache.getName()));
                    else
                        LOGGER.error(String.format(CacheConstants.DELETE_FILE_FAILED, oldCache.getName()));
                }
            }
        }
    }


    /**
     * Initializes the singleton instance.
     *
     * @param submitter the submitter that is used for sending away harvested documents
     */
    public static void init(AbstractSubmitter submitter)
    {
        instance.submitter = submitter;
        instance.saver = new HarvestSaver();

        EventSystem.addListener(DocumentHarvestedEvent.class, instance.onDocumentHarvested);
        EventSystem.addListener(HarvestStartedEvent.class, instance.onHarvestStarted);
        EventSystem.addListener(HarvestFinishedEvent.class, instance.onHarvestFinished);
        EventSystem.addListener(StartSubmissionEvent.class, instance.onStartSubmitting);
        EventSystem.addListener(StartSaveEvent.class, instance.onStartSaving);
    }


    /**
     * Finishes the potentially open cache writer, clears old files and resets the writer and
     * number of cached documents.
     */
    private void clear()
    {
        if (documentCount > 0)
            finishCaching();

        // should we delete old cache files?
        if (!MainContext.getConfiguration().getParameterValue(ConfigurationConstants.KEEP_CACHE, Boolean.class))
            clearOldCacheFiles();

        documentCount = 0;
        cacheWriter = null;
    }


    /**
     * Clears the old data, creates directories for the cache file and opens the cache writer.
     *
     * @return true if the writer could be opened successfully and the cache file was created
     */
    private boolean startCaching()
    {
        clear();

        // create directories
        boolean isDirectoryCreated = cacheFile.getParentFile().exists() || cacheFile.getParentFile().mkdirs();

        if (isDirectoryCreated) {
            try {
                cacheWriter = new JsonWriter(
                    new OutputStreamWriter(
                        new FileOutputStream(cacheFile),
                        MainContext.getCharset()));
                cacheWriter.beginArray();

                // listen to abort events
                EventSystem.addListener(AbortingFinishedEvent.class, onAbortingFinished);

                return true;
            } catch (IOException e) {
                LOGGER.error(CacheConstants.START_CACHE_ERROR, e);
            }
        }

        return false;
    }


    /**
     * Closes the cache writer.
     */
    private void finishCaching()
    {
        // stop listening to abort events
        EventSystem.removeListener(AbortingFinishedEvent.class, onAbortingFinished);

        try {
            if (cacheWriter != null) {
                cacheWriter.endArray();
                cacheWriter.close();
                cacheWriter = null;
            }
        } catch (IOException e) {
            LOGGER.error(CacheConstants.FINISH_CACHE_ERROR, e);
        }
    }


    /**
     * Writes a document to the cache file.
     *
     * @param doc the document that is to be written to the cache
     */
    private synchronized void addDocument(IDocument doc)
    {
        GsonUtils.getGson().toJson(doc, doc.getClass(), cacheWriter);
        documentCount++;
    }
}
