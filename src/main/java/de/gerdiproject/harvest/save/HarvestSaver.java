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
package de.gerdiproject.harvest.save;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.save.events.DocumentSavedEvent;
import de.gerdiproject.harvest.save.events.SaveFinishedEvent;
import de.gerdiproject.harvest.save.events.SaveStartedEvent;
import de.gerdiproject.harvest.state.events.AbortingFinishedEvent;
import de.gerdiproject.harvest.state.events.AbortingStartedEvent;
import de.gerdiproject.harvest.state.events.StartAbortingEvent;
import de.gerdiproject.harvest.utils.CancelableFuture;
import de.gerdiproject.harvest.utils.cache.constants.CacheConstants;
import de.gerdiproject.json.GsonUtils;
import de.gerdiproject.json.datacite.DataCiteJson;

/**
 * This static class offers some helper functions for saving a harvest result to disk.
 *
 * @author Robin Weiss
 */
public class HarvestSaver
{
    private final static Logger LOGGER = LoggerFactory.getLogger(HarvestSaver.class);

    /**
     * Event listener for aborting the submitter.
     */
    private final Consumer<StartAbortingEvent> onStartAborting = (StartAbortingEvent e) -> {
        isAborting = true;
        EventSystem.removeListener(StartAbortingEvent.class, this.onStartAborting);
        EventSystem.sendEvent(new AbortingStartedEvent());
    };

    private CancelableFuture<Boolean> currentSavingProcess;
    private boolean isAborting;


    /**
     * Saves cached harvested documents to disk.
     *
     * @param cachedDocuments the file in which the cached documents are stored as a JSON array
     * @param sourceHash a String used for version checks of the source data that was harvested
     * @param numberOfDocs the amount of documents that are to be saved
     */
    public void save(File cachedDocuments, String sourceHash, int numberOfDocs)
    {
        EventSystem.sendEvent(new SaveStartedEvent(numberOfDocs));

        // listen to abort requests
        isAborting = false;
        EventSystem.addListener(StartAbortingEvent.class, onStartAborting);

        // get timestamps
        long startTimestamp = MainContext.getTimeKeeper().getHarvestMeasure().getStartTimestamp();
        long finishTimestamp = MainContext.getTimeKeeper().getHarvestMeasure().getEndTimestamp();

        // start asynchronous save
        currentSavingProcess = new CancelableFuture<>(
            createSaveProcess(cachedDocuments, startTimestamp, finishTimestamp, sourceHash));

        // exception handler
        currentSavingProcess.thenApply((isSuccessful) -> {
            onSaveFinishedSuccessfully(isSuccessful);
            return isSuccessful;
        })
        .exceptionally(throwable -> {
            onSaveFailed(throwable);
            return false;
        });
    }


    /**
     * This function is executed after the saving process.
     *
     * @param isSuccessful if true, the save was successful
     */
    private void onSaveFinishedSuccessfully(boolean isSuccessful)
    {
        currentSavingProcess = null;
        EventSystem.removeListener(StartAbortingEvent.class, onStartAborting);
        EventSystem.sendEvent(new SaveFinishedEvent(isSuccessful));
    }


    /**
     * This function is executed if the saving process is interrupted due to an exception.
     *
     * @param reason the exception that caused the saving to be interrupted
     */
    private void onSaveFailed(Throwable reason)
    {
        currentSavingProcess = null;
        EventSystem.removeListener(StartAbortingEvent.class, onStartAborting);

        if (reason instanceof CancellationException || reason.getCause() instanceof CancellationException)
            EventSystem.sendEvent(new AbortingFinishedEvent());
        else
            LOGGER.error(CacheConstants.SAVE_FAILED_ERROR, reason);

        EventSystem.sendEvent(new SaveFinishedEvent(false));
    }


    /**
     * Creates a saving-process that can be called asynchronously.
     *
     * @param cachedDocuments the file in which the cached documents are stored as a JSON array
     * @param startTimestamp the UNIX Timestamp of the beginning of the harvest
     * @param finishTimestamp the UNIX Timestamp of the end of the harvest
     * @param sourceHash a String used for version checks of the source data that was harvested
     *
     * @return true, if the file was saved successfully
     */
    private Callable<Boolean> createSaveProcess(File cachedDocuments, long startTimestamp, long finishTimestamp, String sourceHash)
    {
        return () -> {
            // create file
            final Configuration config = MainContext.getConfiguration();
            File saveFile = createTargetFile(config, startTimestamp);
            boolean isSuccessful = saveFile != null;

            if (isSuccessful)
            {
                try {
                    // prepare json reader for the cached document list
                    JsonReader reader = new JsonReader(
                        new InputStreamReader(
                            new FileInputStream(cachedDocuments),
                            MainContext.getCharset()));

                    // prepare json writer for the save file
                    JsonWriter writer = new JsonWriter(
                        new OutputStreamWriter(
                            new FileOutputStream(saveFile),
                            MainContext.getCharset()));

                    // transfer data to target file
                    writeDocuments(
                        reader,
                        writer,
                        startTimestamp,
                        finishTimestamp,
                        sourceHash,
                        config.getParameterValue(ConfigurationConstants.READ_HTTP_FROM_DISK, Boolean.class)
                    );
                } catch (IOException e) {
                    LOGGER.error(CacheConstants.SAVE_FAILED_ERROR, e);
                    isSuccessful = false;
                }
            }

            return isSuccessful;
        };
    }


    /**
     * Creates a target file for the harvest that is to be saved.
     *
     * @param config the global harvester configuration
     * @param startTimestamp the UNIX Timestamp of the beginning of the harvest
     *
     * @return a file, if the path could be resolved or created, or null if not
     */
    private File createTargetFile(Configuration config, long startTimestamp)
    {
        // get harvesting range
        int from = config.getParameterValue(ConfigurationConstants.HARVEST_START_INDEX, Integer.class);
        int to = config.getParameterValue(ConfigurationConstants.HARVEST_END_INDEX, Integer.class);

        // assemble file name
        String fileName;

        if (from > 0 || to != Integer.MAX_VALUE) {

            fileName = String.format(
                           CacheConstants.SAVE_FILE_NAME_PARTIAL,
                           MainContext.getModuleName(),
                           from,
                           to,
                           startTimestamp);
        } else {
            fileName = String.format(
                           CacheConstants.SAVE_FILE_NAME,
                           MainContext.getModuleName(),
                           startTimestamp);
        }

        // create file and directories
        File saveFile = new File(fileName);
        boolean isDirectoryCreated = saveFile.getParentFile().exists() || saveFile.getParentFile().mkdirs();

        if (isDirectoryCreated)
            return saveFile;
        else {
            LOGGER.error(CacheConstants.SAVE_FAILED_DIRECTORY);
            return null;
        }
    }


    /**
     * Writes cached documents from a reader directly to a writer, adding additional harvesting related data
     *
     * @param cacheReader a JSON reader of a file that contains cached documents
     * @param writer a JSON writer to a file
     * @param startTimestamp the UNIX Timestamp of the beginning of the harvest
     * @param finishTimestamp the UNIX Timestamp of the end of the harvest
     * @param sourceHash a String used for version checks of the source data that was harvested
     * @param readFromDisk if true, the harvest was not retrieved from the web, but instead, from locally cached HTTP responses
     *
     * @throws IOException thrown by either the cacheReader or the writer
     */
    private void writeDocuments(JsonReader cacheReader, JsonWriter writer, long startTimestamp, long finishTimestamp, String sourceHash, boolean readFromDisk) throws IOException
    {
        // this event holds no unique data, we can resubmit it as often as we want
        DocumentSavedEvent savedEvent = new DocumentSavedEvent();

        writer.beginObject();
        writer.name("harvestDate");
        writer.value(startTimestamp);

        writer.name("durationInSeconds");
        writer.value((finishTimestamp - startTimestamp) / 1000l);

        writer.name("wasHarvestedFromDisk");
        writer.value(readFromDisk);

        writer.name("hash");
        writer.value(sourceHash);

        writer.name("data");
        writer.beginArray();

        // iterate through cached array
        final Gson gson = GsonUtils.getGson();
        cacheReader.beginArray();

        while (cacheReader.hasNext()) {
            if (isAborting)
                break;

            // read a document from the array
            gson.toJson(gson.fromJson(cacheReader, DataCiteJson.class), DataCiteJson.class, writer);
            EventSystem.sendEvent(savedEvent);
        }

        // close reader
        cacheReader.endArray();
        cacheReader.close();

        // close writer
        writer.endArray();
        writer.endObject();
        writer.close();


        // cancel the asynchronous process
        if (isAborting)
            currentSavingProcess.cancel(false);
    }
}
