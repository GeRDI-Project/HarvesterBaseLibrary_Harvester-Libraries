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
package de.gerdiproject.harvest.utils.time;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import com.google.gson.GsonBuilder;

import de.gerdiproject.harvest.etls.events.HarvestFinishedEvent;
import de.gerdiproject.harvest.etls.events.HarvestStartedEvent;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.IEventListener;
import de.gerdiproject.harvest.utils.cache.ICachedObject;
import de.gerdiproject.harvest.utils.data.DiskIO;
import de.gerdiproject.harvest.utils.time.ProcessTimeMeasure.ProcessStatus;
import de.gerdiproject.harvest.utils.time.events.ProcessTimeMeasureFinishedEvent;

/**
 * This class keeps timestamps of important events, such as harvests,
 * submissions, and saves.
 *
 * @author Robin Weiss
 */
public class HarvestTimeKeeper implements IEventListener, ICachedObject
{
    private final ProcessTimeMeasure harvestMeasure;

    private final transient DiskIO diskIo;
    private final transient String cacheFilePath;


    /**
     * Constructor that creates time measures for harvesting, submitting, and
     * saving.
     *
     * @param cacheFilePath the path to the cache file in which
     *         the JSON representation of this class is cached
     */
    public HarvestTimeKeeper(String cacheFilePath)
    {
        this.diskIo = new DiskIO(new GsonBuilder().create(), StandardCharsets.UTF_8);
        this.harvestMeasure = new ProcessTimeMeasure(HarvestStartedEvent.class, HarvestFinishedEvent.class);

        this.cacheFilePath = cacheFilePath;
    }


    @Override
    public void addEventListeners()
    {
        harvestMeasure.addEventListeners();

        EventSystem.addListener(ProcessTimeMeasureFinishedEvent.class, onProcessTimeMeasureFinished);
    }


    @Override
    public void removeEventListeners()
    {
        harvestMeasure.removeEventListeners();

        EventSystem.removeListener(ProcessTimeMeasureFinishedEvent.class, onProcessTimeMeasureFinished);
    }


    @Override
    public void saveToDisk()
    {
        diskIo.writeObjectToFile(cacheFilePath, this);
    }


    @Override
    public void loadFromDisk()
    {
        final HarvestTimeKeeper parsedKeeper = diskIo.getObject(cacheFilePath, HarvestTimeKeeper.class);

        if (parsedKeeper != null && parsedKeeper.harvestMeasure.getStatus() != ProcessStatus.Started) {
            // copy status if it is not started
            harvestMeasure.set(parsedKeeper.harvestMeasure);
        }
    }


    /**
     * Returns the process time measure of the harvesting process.
     *
     * @return the process time measure of the harvesting process
     */
    public ProcessTimeMeasure getHarvestMeasure()
    {
        return harvestMeasure;
    }


    /**
     * Returns true if the harvest was finished prematurely due to an exception
     * or a user triggered abort.
     *
     * @return true if the harvest was aborted or it failed
     */
    public boolean isHarvestIncomplete()
    {
        return harvestMeasure.getStatus() == ProcessStatus.Failed
               || harvestMeasure.getStatus() == ProcessStatus.Aborted;
    }


    //////////////////////////////
    // Event Callback Functions //
    //////////////////////////////


    /**
     * This callback function is called when the measurement of a process comes
     * to an end. This function saves this Time Keeper to a cache file on disk.
     *
     * @param event the event that triggered the listener
     */
    private final transient Consumer<ProcessTimeMeasureFinishedEvent>  onProcessTimeMeasureFinished = (ProcessTimeMeasureFinishedEvent event) -> {
        saveToDisk();
    };
}
