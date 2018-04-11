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
package de.gerdiproject.harvest.save;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;

import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.save.constants.SaveConstants;
import de.gerdiproject.harvest.save.events.DocumentSavedEvent;
import de.gerdiproject.harvest.save.events.SaveFinishedEvent;
import de.gerdiproject.harvest.save.events.SaveStartedEvent;
import de.gerdiproject.harvest.save.events.StartSaveEvent;
import de.gerdiproject.harvest.state.events.AbortingFinishedEvent;
import de.gerdiproject.harvest.state.events.AbortingStartedEvent;
import de.gerdiproject.harvest.state.events.StartAbortingEvent;
import de.gerdiproject.harvest.utils.CancelableFuture;
import de.gerdiproject.harvest.utils.cache.DocumentChangesCache;
import de.gerdiproject.harvest.utils.cache.constants.CacheConstants;
import de.gerdiproject.harvest.utils.cache.events.RegisterCacheEvent;
import de.gerdiproject.json.GsonUtils;
import de.gerdiproject.json.datacite.DataCiteJson;

/**
 * This static class offers some helper functions for saving a harvest result to
 * disk.
 *
 * @author Robin Weiss
 */
public class HarvestSaver
{
    private final static HarvestSaver instance = new HarvestSaver();

    private final List<DocumentChangesCache> cacheList = Collections.synchronizedList(new LinkedList<>());

    private CancelableFuture<Boolean> currentSavingProcess;
    private boolean isAborting;
    private File saveFile;

    private final static Logger LOGGER = LoggerFactory.getLogger(HarvestSaver.class);


    /**
     * Adds event listeners, granting .
     */
    public static void init()
    {
        EventSystem.addListener(RegisterCacheEvent.class, instance.onRegisterCache);
        EventSystem.addListener(StartSaveEvent.class, instance.onStartSave);
    }


    /**
     * Saves cached harvested documents to disk.
     *
     * @param isAutoTriggered true if the save was not explicitly triggered via
     *            a REST call
     */
    private void save(boolean isAutoTriggered)
    {
        // listen to abort requests
        isAborting = false;
        EventSystem.addListener(StartAbortingEvent.class, onStartAborting);

        // get timestamps
        long startTimestamp = MainContext.getTimeKeeper().getHarvestMeasure().getStartTimestamp();
        long finishTimestamp = MainContext.getTimeKeeper().getHarvestMeasure().getEndTimestamp();

        // start asynchronous save
        currentSavingProcess =
                new CancelableFuture<>(createSaveProcess(startTimestamp, finishTimestamp, isAutoTriggered));

        // exception handler
        currentSavingProcess.thenApply((isSuccessful) -> {
            onSaveFinishedSuccessfully(isSuccessful);
            return isSuccessful;
        }).exceptionally(throwable -> {
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

        // if the save was aborted while it finished, notify listeners to
        // prevent dead-locks
        if (isAborting) {
            isAborting = false;
            EventSystem.sendEvent(new AbortingFinishedEvent());
        }
    }


    /**
     * This function is executed if the saving process is interrupted due to an
     * exception.
     *
     * @param reason the exception that caused the saving to be interrupted
     */
    private void onSaveFailed(Throwable reason)
    {
        // clean up unfinished save file
        if (saveFile != null
                && saveFile.exists()
                && MainContext.getConfiguration().getParameterValue(
                        ConfigurationConstants.DELETE_UNFINISHED_SAVE,
                        Boolean.class)) {
            try {
                boolean wasDeleted = saveFile.delete();

                if (wasDeleted)
                    LOGGER.debug(String.format(SaveConstants.DELETED_SAVE_FILE, saveFile.getPath()));
                else
                    LOGGER.debug(String.format(SaveConstants.DELETED_SAVE_FILE_FAILED, saveFile.getPath()));

            } catch (SecurityException e) {
                LOGGER.error(String.format(SaveConstants.DELETED_SAVE_FILE_FAILED, saveFile.getPath()), e);
            }
        }

        currentSavingProcess = null;
        EventSystem.removeListener(StartAbortingEvent.class, onStartAborting);

        // if the save was aborted, notify listeners
        if (isAborting) {
            isAborting = false;
            EventSystem.sendEvent(new AbortingFinishedEvent());
        } else
            LOGGER.error(CacheConstants.SAVE_FAILED_ERROR, reason);

        EventSystem.sendEvent(new SaveFinishedEvent(false));
    }


    /**
     * Creates a saving-process that can be called asynchronously.
     *
     * @param cachedDocuments the cache of changed documents
     * @param startTimestamp the UNIX Timestamp of the beginning of the harvest
     * @param finishTimestamp the UNIX Timestamp of the end of the harvest
     * @param isAutoTriggered true if the save was not explicitly triggered via
     *            a REST call
     *
     * @return true, if the file was saved successfully
     */
    private Callable<Boolean> createSaveProcess(long startTimestamp, long finishTimestamp, boolean isAutoTriggered)
    {
        return () -> {
            EventSystem.sendEvent(new SaveStartedEvent(isAutoTriggered, getNumberOfChangedDocuments()));

            // create file
            final Configuration config = MainContext.getConfiguration();
            saveFile = createTargetFile(config, startTimestamp);

            // check if file was created
            boolean isSuccessful = saveFile != null;

            if (isSuccessful) {
                try {
                    // prepare json writer for the save file
                    JsonWriter writer = new JsonWriter(
                            new OutputStreamWriter(new FileOutputStream(saveFile), MainContext.getCharset()));

                    // transfer data to target file
                    writeDocuments(
                            writer,
                            startTimestamp,
                            finishTimestamp,
                            config.getParameterValue(ConfigurationConstants.READ_HTTP_FROM_DISK, Boolean.class));
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
        } else
            fileName = String.format(CacheConstants.SAVE_FILE_NAME, MainContext.getModuleName(), startTimestamp);

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
     * Writes cached documents from a reader directly to a writer, adding
     * additional harvesting related data
     *
     * @param writer a JSON writer to a file
     * @param startTimestamp the UNIX Timestamp of the beginning of the harvest
     * @param finishTimestamp the UNIX Timestamp of the end of the harvest
     * @param readFromDisk if true, the harvest was not retrieved from the web,
     *            but instead, from locally cached HTTP responses
     *
     * @throws IOException thrown by either the cacheReader or the writer
     */
    private void writeDocuments(JsonWriter writer, long startTimestamp, long finishTimestamp, boolean readFromDisk) throws IOException
    {
        // this event holds no unique data, we can resubmit it as often as we
        // want
        DocumentSavedEvent savedEvent = new DocumentSavedEvent();

        writer.beginObject();
        writer.name(SaveConstants.HARVEST_DATE_JSON);
        writer.value(startTimestamp);

        writer.name(SaveConstants.DURATION_JSON);
        writer.value((finishTimestamp - startTimestamp) / 1000l);

        writer.name(SaveConstants.IS_FROM_DISK_JSON);
        writer.value(readFromDisk);

        writer.name(SaveConstants.DATA_JSON);
        writer.beginArray();

        // iterate through cached array
        final Gson gson = GsonUtils.getGson();
        for (DocumentChangesCache cachedDocuments : cacheList) {
            cachedDocuments.forEach((String documentId, DataCiteJson document) -> {
                if (isAborting)
                    return false;
                else {
                    // write a document to the array
                    if (document != null)
                        gson.toJson(document, DataCiteJson.class, writer);

                    EventSystem.sendEvent(savedEvent);
                    return true;
                }
            });
        }

        // close writer
        writer.endArray();
        writer.endObject();
        writer.close();

        // cancel the asynchronous process
        if (isAborting)
            currentSavingProcess.cancel(false);
    }


    /**
     * Iterates through all registered caches and calculates the total number of
     * changed documents.
     *
     * @return the total number of submitted changes.
     */
    protected int getNumberOfChangedDocuments()
    {
        int docCount = 0;
        for (final DocumentChangesCache cache : cacheList)
            docCount += cache.size();

        return docCount;
    }


    //////////////////////////////
    // Event Callback Functions //
    //////////////////////////////


    /**
     * Event listener for registering a new documents cache.
     */
    private final Consumer<RegisterCacheEvent> onRegisterCache = (RegisterCacheEvent e) -> {
        cacheList.add(e.getCache().getChangesCache());
    };


    /**
     * Event callback: When a save starts, save the cache file via the
     * {@linkplain HarvestSaver}.
     */
    private final Consumer<StartSaveEvent> onStartSave = (StartSaveEvent e) -> {
        save(e.isAutoTriggered());
    };


    /**
     * Event listener for aborting the submitter.
     */
    private final Consumer<StartAbortingEvent> onStartAborting = (StartAbortingEvent e) -> {
        isAborting = true;
        EventSystem.removeListener(StartAbortingEvent.class, this.onStartAborting);
        EventSystem.sendEvent(new AbortingStartedEvent());
    };

}
