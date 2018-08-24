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
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.stream.JsonWriter;

import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.IEventListener;
import de.gerdiproject.harvest.harvester.events.HarvestFinishedEvent;
import de.gerdiproject.harvest.harvester.events.HarvestStartedEvent;
import de.gerdiproject.harvest.save.constants.SaveConstants;
import de.gerdiproject.harvest.save.events.SaveHarvestEvent;
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


    /**
     * Event callback and main method for saving harvested documents to disk.
     *
     * @param event the event that triggered the callback
     *
     * @return a File that contains all documents and harvest metadata
     * @throws IOException thrown when there was an error saving the file
     */
    private File saveHarvest(SaveHarvestEvent event) // NOPMD event listeners must have the event as paramete
    {
        int documentCount = cacheManager.getNumberOfHarvestedDocuments();

        // abort if there is nothing to save
        if (documentCount == 0) {
            LOGGER.error(SaveConstants.SAVE_FAILED_EMPTY);
            throw new IllegalStateException(SaveConstants.SAVE_FAILED_EMPTY);
        }

        // create empty file
        File result = new File(saveFolder, String.format(SaveConstants.SAVE_FILE_NAME, fileName));
        FileUtils.createEmptyFile(result);

        // abort if the file could not be created or cleaned up
        if (!result.exists() || result.length() != 0)
            throw new UncheckedIOException(new IOException(String.format(SaveConstants.SAVE_FAILED_CANNOT_CREATE, result)));

        LOGGER.info(String.format(SaveConstants.SAVE_START, result.getAbsolutePath()));

        try
            (JsonWriter writer = new JsonWriter(
                new OutputStreamWriter(new FileOutputStream(result), charset))) {

            // transfer data to target file
            writeDocuments(writer);

        } catch (IOException e) {
            LOGGER.error(String.format(SaveConstants.SAVE_FAILED_EXCEPTION, e.getClass().getSimpleName()), e);
            throw new UncheckedIOException(String.format(SaveConstants.SAVE_FAILED_EXCEPTION, e.getMessage()), e);
        }

        LOGGER.info(SaveConstants.SAVE_OK);

        return result;
    }


    @Override
    public void addEventListeners()
    {
        EventSystem.addListener(HarvestStartedEvent.class, onHarvestStarted);
        EventSystem.addListener(HarvestFinishedEvent.class, onHarvestFinished);
        EventSystem.addSynchronousListener(SaveHarvestEvent.class, this::saveHarvest);
    }


    @Override
    public void removeEventListeners()
    {
        EventSystem.removeListener(HarvestStartedEvent.class, onHarvestStarted);
        EventSystem.removeListener(HarvestFinishedEvent.class, onHarvestFinished);
        EventSystem.removeSynchronousListener(SaveHarvestEvent.class);
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
                // write a document to the array
                if (document != null)
                {
                    try {
                        writer.jsonValue(document.toJson());
                    } catch (IOException e) {
                        return false;
                    }
                }
                return true;
            });

            if (!isSuccessful)
                break;
        }

        // close writer
        writer.endArray();
        writer.endObject();
    }


    //////////////////////////////
    // Event Callback Functions //
    //////////////////////////////

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
        this.harvestEndTime = event.getEndTimestamp();
    };
}
