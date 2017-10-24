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
package de.gerdiproject.harvest.utils;

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
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.harvester.events.DocumentHarvestedEvent;
import de.gerdiproject.harvest.harvester.events.HarvestFinishedEvent;
import de.gerdiproject.harvest.harvester.events.HarvestStartedEvent;
import de.gerdiproject.harvest.save.HarvestSaver;
import de.gerdiproject.harvest.save.events.StartSaveEvent;
import de.gerdiproject.harvest.submission.AbstractSubmitter;
import de.gerdiproject.harvest.submission.events.StartSubmissionEvent;
import de.gerdiproject.harvest.utils.constants.DocumentsCacheConstants;
import de.gerdiproject.json.GsonUtils;

public class DocumentsCache
{
    //private static final String NO_HARVEST = "Cannot save: Nothing was harvested yet!";

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentsCache.class);
    public static final DocumentsCache instance = new DocumentsCache();

    private AbstractSubmitter submitter;
    private HarvestSaver saver;

    private long startTimeStamp;
    private long finishTimeStamp;
    private int documentCount;
    private JsonWriter cacheWriter;
    private File cacheFile;
    private String documentHash;


    private Consumer<HarvestStartedEvent> onHarvestStarted = (HarvestStartedEvent e) -> {
        startTimeStamp = new Date().getTime();
        clear();
        startCaching();
    };

    private Consumer<HarvestFinishedEvent> onHarvestFinished = (HarvestFinishedEvent e) -> {
        finishTimeStamp = new Date().getTime();
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
        saver.save(cacheFile, startTimeStamp, finishTimeStamp, documentHash, documentCount);
    };


    private DocumentsCache()
    {
        this.documentCount = 0;
        this.cacheFile = new File(
            String.format(
                DocumentsCacheConstants.CACHE_FILE_PATH,
                MainContext.getModuleName(),
                new Date().getTime()
            ));
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

        documentCount = 0;
        cacheWriter = null;
    }

    private boolean startCaching()
    {
        documentCount = 0;

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
                LOGGER.error(DocumentsCacheConstants.START_CACHE_ERROR, e);
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
            }
        } catch (IOException e) {
            LOGGER.error(DocumentsCacheConstants.FINISH_CACHE_ERROR, e);
        }
    }


    private void addDocument(IDocument doc)
    {
        boolean isInitialized = true;

        if (documentCount == 0)
            isInitialized = startCaching();

        if (isInitialized) {
            GsonUtils.getGson().toJson(doc, doc.getClass(), cacheWriter);
            documentCount++;
        }
    }
}
