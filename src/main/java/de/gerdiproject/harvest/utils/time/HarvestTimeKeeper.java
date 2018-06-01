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

import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.IEventListener;
import de.gerdiproject.harvest.harvester.events.HarvestFinishedEvent;
import de.gerdiproject.harvest.harvester.events.HarvestStartedEvent;
import de.gerdiproject.harvest.save.events.SaveFinishedEvent;
import de.gerdiproject.harvest.save.events.SaveStartedEvent;
import de.gerdiproject.harvest.submission.events.SubmissionFinishedEvent;
import de.gerdiproject.harvest.submission.events.SubmissionStartedEvent;
import de.gerdiproject.harvest.utils.data.DiskIO;
import de.gerdiproject.harvest.utils.time.ProcessTimeMeasure.ProcessStatus;
import de.gerdiproject.harvest.utils.time.events.ProcessTimeMeasureFinishedEvent;

/**
 * This class keeps timestamps of important events, such as harvests,
 * submissions, and saves.
 *
 * @author Robin Weiss
 */
public class HarvestTimeKeeper implements IEventListener
{
    private final ProcessTimeMeasure harvestMeasure;
    private final ProcessTimeMeasure submissionMeasure;
    private final ProcessTimeMeasure saveMeasure;

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
        this.saveMeasure = new ProcessTimeMeasure(SaveStartedEvent.class, SaveFinishedEvent.class);
        this.submissionMeasure = new ProcessTimeMeasure(SubmissionStartedEvent.class, SubmissionFinishedEvent.class);

        this.cacheFilePath = cacheFilePath;
    }


    @Override
    public void addEventListeners()
    {
        harvestMeasure.addEventListeners();
        saveMeasure.addEventListeners();
        submissionMeasure.addEventListeners();

        EventSystem.addListener(HarvestStartedEvent.class, onHarvestStarted);
        EventSystem.addListener(ProcessTimeMeasureFinishedEvent.class, onProcessTimeMeasureFinished);
    }


    @Override
    public void removeEventListeners()
    {
        harvestMeasure.removeEventListeners();
        saveMeasure.removeEventListeners();
        submissionMeasure.removeEventListeners();

        EventSystem.removeListener(HarvestStartedEvent.class, onHarvestStarted);
        EventSystem.removeListener(ProcessTimeMeasureFinishedEvent.class, onProcessTimeMeasureFinished);
    }


    /**
     * Attempts to load values from a cached file.
     */
    public void loadFromDisk()
    {
        final HarvestTimeKeeper parsedKeeper = diskIo.getObject(cacheFilePath, HarvestTimeKeeper.class);

        if (parsedKeeper != null) {

            // copy status if it is not started
            if (parsedKeeper.harvestMeasure.getStatus() != ProcessStatus.Started)
                harvestMeasure.set(parsedKeeper.harvestMeasure);

            if (parsedKeeper.saveMeasure.getStatus() != ProcessStatus.Started)
                saveMeasure.set(parsedKeeper.saveMeasure);

            if (parsedKeeper.submissionMeasure.getStatus() != ProcessStatus.Started)
                submissionMeasure.set(parsedKeeper.submissionMeasure);
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
     * Returns the process time measure of the submission process.
     *
     * @return the process time measure of the submission process
     */
    public ProcessTimeMeasure getSubmissionMeasure()
    {
        return submissionMeasure;
    }


    /**
     * Returns the process time measure of the saving process.
     *
     * @return the process time measure of the saving process
     */
    public ProcessTimeMeasure getSaveMeasure()
    {
        return saveMeasure;
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


    /**
     * Returns true if there are unsubmitted documents.
     *
     * @return true ifthere are unsubmitted documents
     */
    public boolean hasUnsubmittedChanges()
    {
        return harvestMeasure.getStatus() != ProcessStatus.NotStarted
               && submissionMeasure.getStatus() != ProcessStatus.Finished;
    }


    /**
     * (Over{@literal-})writes the time keeper cache file.
     */
    private void saveToDisk()
    {
        diskIo.writeObjectToFile(cacheFilePath, this);
    }

    private void resetSaveAndSubmissionMeasures()
    {
        saveMeasure.set(-1, -1, ProcessStatus.NotStarted);
        submissionMeasure.set(-1, -1, ProcessStatus.NotStarted);
    }


    //////////////////////////////
    // Event Callback Functions //
    //////////////////////////////

    /**
     * Callback function for the successful beginning of a harvest. Resets the
     * timestamps of the saving and submission process.
     *
     * @param event the event that triggered the callback
     */
    private final transient Consumer<HarvestStartedEvent> onHarvestStarted = (HarvestStartedEvent event)  -> {
        resetSaveAndSubmissionMeasures();
    };


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
