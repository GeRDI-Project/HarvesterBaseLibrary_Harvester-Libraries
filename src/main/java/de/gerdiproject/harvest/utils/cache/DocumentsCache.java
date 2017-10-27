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
import de.gerdiproject.harvest.submission.AbstractSubmitter;
import de.gerdiproject.harvest.submission.events.StartSubmissionEvent;
import de.gerdiproject.harvest.utils.cache.constants.CacheConstants;
import de.gerdiproject.json.GsonUtils;

public class DocumentsCache
{
    //private static final String NO_HARVEST = "Cannot save: Nothing was harvested yet!";

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentsCache.class);
    public static final DocumentsCache instance = new DocumentsCache();

    private AbstractSubmitter submitter;
    private HarvestSaver saver;

    private int documentCount;
    private JsonWriter cacheWriter;
    private File cacheFile;
    private String documentHash;


    private Consumer<HarvestStartedEvent> onHarvestStarted = (HarvestStartedEvent e) -> {
        startCaching();
    };

    private Consumer<HarvestFinishedEvent> onHarvestFinished = (HarvestFinishedEvent e) -> {
        documentHash = e.getDocumentChecksum();
        finishCaching();
    };

    private Consumer<DocumentHarvestedEvent> onDocumentHarvested = (DocumentHarvestedEvent e) -> {
        addDocument(e.getDocument());
    };


    private Consumer<StartSubmissionEvent> onStartSubmitting = (StartSubmissionEvent e) -> {
        submitter.submit(cacheFile, documentCount);
    };


    private Consumer<StartSaveEvent> onStartSaving = (StartSaveEvent e) -> {
        saver.save(cacheFile, documentHash, documentCount);
    };


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
     * Removes all cache files for this harvester, that are no longer in use.
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

                return true;
            } catch (IOException e) {
                LOGGER.error(CacheConstants.START_CACHE_ERROR, e);
            }
        }

        return false;
    }


    private void finishCaching()
    {
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


    private synchronized void addDocument(IDocument doc)
    {
        GsonUtils.getGson().toJson(doc, doc.getClass(), cacheWriter);
        documentCount++;
    }
}
