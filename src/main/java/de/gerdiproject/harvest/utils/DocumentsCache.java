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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import de.gerdiproject.harvest.IDocument;
import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.impl.DocumentHarvestedEvent;
import de.gerdiproject.harvest.event.impl.HarvestFinishedEvent;
import de.gerdiproject.harvest.event.impl.HarvestStartedEvent;
import de.gerdiproject.harvest.event.impl.SaveFinishedEvent;
import de.gerdiproject.harvest.event.impl.SaveStartedEvent;
import de.gerdiproject.harvest.event.impl.StartSaveEvent;
import de.gerdiproject.harvest.event.impl.StartSubmissionEvent;
import de.gerdiproject.harvest.submission.AbstractSubmitter;
import de.gerdiproject.json.GsonUtils;
import de.gerdiproject.json.datacite.DataCiteJson;

public class DocumentsCache
{
    private static final String SAVE_FILE_NAME = "harvestedIndices/%s_result_%d.json";
    private static final String SAVE_FILE_NAME_PARTIAL = "harvestedIndices/%s_partialResult_%d-%d_%d.json";
    private static final String SAVE_FAILED_DIRECTORY = "Could not save documents: Unable to create directories!";
    private static final String SAVE_FAILED_ERROR = "Could not save documents: %s";
    private static final String NO_HARVEST = "Cannot save: Nothing was harvested yet!";
    private static final String CACHE_FILE_PATH = "cachedIndex/%s/cachedDocuments_%d.json";

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentsCache.class);
    public static final DocumentsCache instance = new DocumentsCache();

    private AbstractSubmitter submitter;
    private long startTimeStamp;
    private long finishTimeStamp;
    private int documentCount;
    private JsonWriter cacheWriter;
    private File cacheFile;


    private Consumer<HarvestStartedEvent> onHarvestStarted = (HarvestStartedEvent e) -> {
        startTimeStamp = new Date().getTime();
        clear();
        startCaching();
    };

    private Consumer<HarvestFinishedEvent> onHarvestFinished = (HarvestFinishedEvent e) -> {
        finishTimeStamp = new Date().getTime();
        finishCaching();
    };

    private Consumer<DocumentHarvestedEvent> onDocumentHarvested = (DocumentHarvestedEvent e) -> {
        addDocument(e.getDocument());
    };


    private Consumer<StartSubmissionEvent> onStartSubmitting = (StartSubmissionEvent e) -> {
        submitDocuments();
    };


    private Consumer<StartSaveEvent> onStartSaving = (StartSaveEvent e) -> {
        saveDocumentsToDisk();
    };


    private DocumentsCache()
    {
        this.documentCount = 0;
        this.cacheFile = new File(String.format(CACHE_FILE_PATH, MainContext.getModuleName(), new Date().getTime()));
    }


    public static void init(AbstractSubmitter submitter)
    {
        instance.submitter = submitter;

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
            } catch (IOException e1) {
                // TODO
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
            // TODO
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


    /**
     * Saves the harvested documents to disk and logs if the operation was
     * successful or not.
     *
     * @return A status message describing whether the save succeeded or not
     * @throws
     */
    private void saveDocumentsToDisk()
    {
        boolean isSuccessful = true;
        EventSystem.sendEvent(new SaveStartedEvent());

        final Gson gson = GsonUtils.getGson();
        final Configuration config = MainContext.getConfiguration();

        // get harvesting range
        int from = config.getParameterValue(ConfigurationConstants.HARVEST_START_INDEX, Integer.class);
        int to = config.getParameterValue(ConfigurationConstants.HARVEST_END_INDEX, Integer.class);

        // assemble file name
        String fileName;

        if (from > 0 || to != Integer.MAX_VALUE) {

            fileName = String.format(
                           SAVE_FILE_NAME_PARTIAL,
                           MainContext.getModuleName(),
                           from,
                           to,
                           startTimeStamp);
        } else {
            fileName = String.format(
                           SAVE_FILE_NAME,
                           MainContext.getModuleName(),
                           startTimeStamp);
        }

        // create file and directories
        File saveFile = new File(fileName);
        boolean isDirectoryCreated = saveFile.getParentFile().exists() || saveFile.getParentFile().mkdirs();

        if (!isDirectoryCreated) {
            LOGGER.error(SAVE_FAILED_DIRECTORY);
            isSuccessful = false;
        } else {
            try {
                // prepare json writer for the save file
                JsonWriter writer = new JsonWriter(
                    new OutputStreamWriter(
                        new FileOutputStream(saveFile),
                        MainContext.getCharset()));

                // prepare json reader for the cached document list
                JsonReader reader = new JsonReader(
                    new InputStreamReader(
                        new FileInputStream(cacheFile),
                        MainContext.getCharset()));


                writer.beginObject();
                writer.name("harvestDate");
                writer.value(startTimeStamp);

                writer.name("durationInSeconds");
                writer.value((finishTimeStamp - startTimeStamp) / 1000l);

                writer.name("wasHarvestedFromDisk");
                writer.value(config.getParameterValue(ConfigurationConstants.READ_HTTP_FROM_DISK, Boolean.class));

                writer.name("hash");
                writer.value(MainContext.getHarvester().getHash(false));

                writer.name("data");
                writer.beginArray();

                // iterate through cached array
                reader.beginArray();

                while (reader.hasNext()) {
                    // read a document from the array
                    gson.toJson(gson.fromJson(reader, DataCiteJson.class), writer);
                }

                // close reader
                reader.endArray();
                reader.close();

                // close writer
                writer.endArray();
                writer.endObject();
                writer.close();
            } catch (IOException e) {
                LOGGER.error(SAVE_FAILED_ERROR, e);
                isSuccessful = false;
            }
        }

        EventSystem.sendEvent(new SaveFinishedEvent(isSuccessful));
    }


    /**
     * Reads the cached documents and passes them onto an {@linkplain AbstractSubmitter}.
     */
    private void submitDocuments()
    {
        // start asynchronous submission
        CancelableFuture<Boolean> asyncSubmission = new CancelableFuture<>(submissionProcess);

        // exception handler
        asyncSubmission.exceptionally(throwable -> {
            submitter.endSubmission();
            return false;
        });
    }

    /**
     * Sends all harvested documents in fixed chunks to the {@linkplain AbstractSubmitter}.
     */
    private final Callable<Boolean> submissionProcess = () -> {

        boolean areAllSubmissionsSuccessful = true;

        // prepare variables
        final Gson gson = GsonUtils.getGson();
        final int maxDocs = MainContext.getConfiguration().getParameterValue(ConfigurationConstants.SUBMISSION_SIZE, Integer.class);
        List<IDocument> documentList = new LinkedList<IDocument>();

        // prepare json reader for the cached document list
        JsonReader reader = new JsonReader(
            new InputStreamReader(
                new FileInputStream(cacheFile),
                MainContext.getCharset()));

        // iterate through cached array
        reader.beginArray();

        while (reader.hasNext())
        {
            // read a document from the array
            documentList.add(gson.fromJson(reader, DataCiteJson.class));

            // send documents in chunks of a configurable size
            if (documentList.size() == maxDocs) {
                areAllSubmissionsSuccessful &= submitter.submit(documentList);
                documentList.clear();
            }
        }

        // close reader
        reader.endArray();
        reader.close();

        // send remainder of documents
        if (documentList.size() > 0)
        {
            submitter.submit(documentList);
            documentList.clear();
        }

        return areAllSubmissionsSuccessful;
    };
}
