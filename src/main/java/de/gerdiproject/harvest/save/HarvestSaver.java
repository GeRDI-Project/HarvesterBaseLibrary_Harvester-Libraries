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
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.stream.JsonWriter;

import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.IEventListener;
import de.gerdiproject.harvest.harvester.events.HarvestFinishedEvent;
import de.gerdiproject.harvest.harvester.events.HarvestStartedEvent;
import de.gerdiproject.harvest.save.constants.SaveConstants;
import de.gerdiproject.harvest.save.events.DocumentSavedEvent;
import de.gerdiproject.harvest.save.events.SaveFinishedEvent;
import de.gerdiproject.harvest.save.events.SaveStartedEvent;
import de.gerdiproject.harvest.save.events.StartSaveEvent;
import de.gerdiproject.harvest.state.events.AbortingFinishedEvent;
import de.gerdiproject.harvest.state.events.AbortingStartedEvent;
import de.gerdiproject.harvest.state.events.StartAbortingEvent;
import de.gerdiproject.harvest.utils.CancelableFuture;
import de.gerdiproject.harvest.utils.FileUtils;
import de.gerdiproject.harvest.utils.cache.HarvesterCache;
import de.gerdiproject.harvest.utils.cache.HarvesterCacheManager;
import de.gerdiproject.harvest.utils.time.ProcessTimeMeasure;
import de.gerdiproject.json.datacite.DataCiteJson;

/**
 * This class offers functions for saving a harvest result to disk.
 *
 * @author Robin Weiss
 */
public class HarvestSaver implements IEventListener
{
    private final static Logger LOGGER = LoggerFactory.getLogger(HarvestSaver.class);

    private CancelableFuture<Boolean> currentSavingProcess;
    private boolean isAborting;
    private File saveFile;

    private int harvestFrom;
    private int harvestTo;
    private long harvestStartTime;
    private long harvestEndTime;
    private String sourceHash;

    private final File saveFolder;
    private final String fileName;
    private final Charset charset;
    private final HarvesterCacheManager cacheManager;


    /**
     * Constructor that sets final fields and retrieves timestamps from a specified {@linkplain ProcessTimeMeasure}.
     *
     * @param saveFolder the folder in which the files are saved
     * @param fileName the name of the saved file which serves as a prefix to which
     * a timestamp will be appended
     * @param charset the charset of the file writer
     * @param harvestMeasure the harvest time measure of which timestamps will be retrieved
     * @param cacheManager the manager of harvester caches
     */
    public HarvestSaver(File saveFolder, String fileName, Charset charset, ProcessTimeMeasure harvestMeasure, HarvesterCacheManager cacheManager)
    {
        this.saveFolder = saveFolder;
        this.fileName = fileName;
        this.charset = charset;
        this.cacheManager = cacheManager;

        this.harvestStartTime = harvestMeasure.getStartTimestamp();
        this.harvestEndTime = harvestMeasure.getEndTimestamp();

        this.sourceHash = null;
        this.harvestFrom = -1;
        this.harvestTo = -1;
    }


    @Override
    public void addEventListeners()
    {
        EventSystem.addListener(StartSaveEvent.class, onStartSave);
        EventSystem.addListener(HarvestStartedEvent.class, onHarvestStarted);
        EventSystem.addListener(HarvestFinishedEvent.class, onHarvestFinished);
    }


    @Override
    public void removeEventListeners()
    {
        EventSystem.removeListener(StartSaveEvent.class, onStartSave);
        EventSystem.removeListener(HarvestStartedEvent.class, onHarvestStarted);
        EventSystem.removeListener(HarvestFinishedEvent.class, onHarvestFinished);
        EventSystem.removeListener(StartAbortingEvent.class, onStartAborting);
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

        // start asynchronous save
        currentSavingProcess =
            new CancelableFuture<>(createSaveProcess(isAutoTriggered));

        // exception handler
        currentSavingProcess.thenApply((isSuccessful) -> {
            if (isSuccessful)
                onSaveFinishedSuccessfully();
            else
                onSaveFailed(null);

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
    private void onSaveFinishedSuccessfully()
    {
        LOGGER.info(SaveConstants.SAVE_OK);

        currentSavingProcess = null;
        EventSystem.removeListener(StartAbortingEvent.class, onStartAborting);
        EventSystem.sendEvent(new SaveFinishedEvent(true));

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
        if (saveFile != null)
            FileUtils.deleteFile(saveFile);

        currentSavingProcess = null;
        EventSystem.removeListener(StartAbortingEvent.class, onStartAborting);

        // if the save was aborted, notify listeners
        if (isAborting) {
            isAborting = false;
            EventSystem.sendEvent(new AbortingFinishedEvent());

        } else if (reason != null)
            LOGGER.error(SaveConstants.SAVE_FAILED, reason);

        else
            LOGGER.error(SaveConstants.SAVE_FAILED);

        EventSystem.sendEvent(new SaveFinishedEvent(false));
    }


    /**
     * Creates a saving-process that can be called asynchronously.
     *
     * @param isAutoTriggered true if the save was not explicitly triggered via
     *            a REST call
     *
     * @return true, if the file was saved successfully
     */
    private Callable<Boolean> createSaveProcess(boolean isAutoTriggered)
    {
        return () -> {
            int documentCount = cacheManager.getNumberOfHarvestedDocuments();
            EventSystem.sendEvent(new SaveStartedEvent(isAutoTriggered, documentCount));

            if (documentCount == 0)
            {
                LOGGER.error(SaveConstants.SAVE_FAILED_EMPTY);
                return false;
            }

            saveFile = getTargetFile();
            FileUtils.createEmptyFile(saveFile);
            boolean isSuccessful = saveFile.exists();

            if (isSuccessful)
            {
                LOGGER.info(String.format(SaveConstants.SAVE_START, saveFile.getAbsolutePath()));

                try {
                    // prepare json writer for the save file
                    JsonWriter writer = new JsonWriter(
                        new OutputStreamWriter(new FileOutputStream(saveFile), charset));

                    // transfer data to target file
                    writeDocuments(writer);

                } catch (IOException e) {
                    LOGGER.error(SaveConstants.SAVE_INTERRUPTED, e);
                    isSuccessful = false;
                }
            }

            return isSuccessful;
        };
    }


    /**
     * Returns the target file for the documents that are to be saved.
     *
     * @return a target save file
     */
    public File getTargetFile()
    {
        return new File(saveFolder, String.format(
                            SaveConstants.SAVE_FILE_NAME,
                            fileName,
                            harvestStartTime));
    }


    /**
     * Writes cached documents from a reader directly to a writer, adding
     * additional harvesting related data
     *
     * @param writer a JSON writer to a file
     *
     * @throws IOException thrown by either the cacheReader or the writer
     */
    private void writeDocuments(JsonWriter writer) throws IOException
    {
        // this event holds no unique data, we can resubmit it as often as we
        // want
        DocumentSavedEvent savedEvent = new DocumentSavedEvent();

        writer.beginObject();
        writer.name(SaveConstants.HARVEST_DATE_JSON);
        writer.value(harvestStartTime);

        writer.name(SaveConstants.DURATION_JSON);
        writer.value((harvestEndTime - harvestStartTime) / 1000l);

        if (harvestFrom != -1) {
            writer.name(SaveConstants.HARVEST_FROM_JSON);
            writer.value(harvestFrom);
        }

        if (harvestTo != -1) {
            writer.name(SaveConstants.HARVEST_TO_JSON);
            writer.value(harvestTo);
        }

        if (sourceHash != null) {
            writer.name(SaveConstants.SOURCE_HASH_JSON);
            writer.value(sourceHash);
        }

        writer.name(SaveConstants.DOCUMENTS_JSON);
        writer.beginArray();

        // iterate through cached array
        final List<HarvesterCache> cacheList = cacheManager.getHarvesterCaches();
        boolean isSuccessful = false;

        for (HarvesterCache cache : cacheList) {
            isSuccessful =
            cache.getChangesCache().forEach((String documentId, DataCiteJson document) -> {
                if (isAborting)
                    return false;
                else
                {
                    // write a document to the array
                    if (document != null) {
                        try {
                            writer.jsonValue(document.toJson());
                        } catch (IOException e) {
                            return false;
                        }
                    }

                    EventSystem.sendEvent(savedEvent);
                    return true;
                }
            });

            if (!isSuccessful)
                break;
        }

        // close writer
        writer.endArray();
        writer.endObject();
        writer.close();

        // cancel the asynchronous process
        if (!isSuccessful)
            currentSavingProcess.cancel(false);
    }


    //////////////////////////////
    // Event Callback Functions //
    //////////////////////////////


    /**
     * Event callback: When a save starts, save the cache file via the
     * {@linkplain HarvestSaver}.
     */
    private final Consumer<StartSaveEvent> onStartSave = (StartSaveEvent e) -> {
        save(e.isAutoTriggered());
    };


    /**
     * Event callback that saves the parameters that were set at the beginning of a harvest.
     */
    private final Consumer<HarvestStartedEvent> onHarvestStarted = (HarvestStartedEvent event) -> {
        this.sourceHash = event.getHarvesterHash();
        this.harvestFrom = event.getStartIndex();
        this.harvestTo = event.getEndIndex();
        this.harvestStartTime = event.getStartTimestamp();
        this.harvestEndTime = -1;
    };


    /**
     * Event callback that saves a timestamp when the harvest finishes.
     */
    private final Consumer<HarvestFinishedEvent> onHarvestFinished = (HarvestFinishedEvent event) -> {
        this.harvestEndTime = Instant.now().toEpochMilli();
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
