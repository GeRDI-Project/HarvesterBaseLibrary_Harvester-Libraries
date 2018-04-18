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

import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.application.events.ContextDestroyedEvent;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.harvester.events.HarvestFinishedEvent;
import de.gerdiproject.harvest.harvester.events.HarvestStartedEvent;
import de.gerdiproject.harvest.save.events.SaveFinishedEvent;
import de.gerdiproject.harvest.save.events.SaveStartedEvent;
import de.gerdiproject.harvest.submission.events.SubmissionFinishedEvent;
import de.gerdiproject.harvest.submission.events.SubmissionStartedEvent;
import de.gerdiproject.harvest.utils.cache.constants.CacheConstants;
import de.gerdiproject.harvest.utils.data.DiskIO;
import de.gerdiproject.harvest.utils.time.ProcessTimeMeasure.ProcessStatus;

/**
 * This class keeps timestamps of important events, such as harvests,
 * submissions, and saves.
 *
 * @author Robin Weiss
 */
public class HarvestTimeKeeper
{
    private final ProcessTimeMeasure harvestMeasure;
    private final ProcessTimeMeasure submissionMeasure;
    private final ProcessTimeMeasure saveMeasure;


    /**
     * Constructor that creates time measures for harvesting, submitting, and
     * saving.
     */
    public HarvestTimeKeeper()
    {
        this.harvestMeasure = new ProcessTimeMeasure();
        this.saveMeasure = new ProcessTimeMeasure();
        this.submissionMeasure = new ProcessTimeMeasure();
    }


    /**
     * Attempts to load old timestamps from a cache file from disk and
     * initializes event listeners of the time measures and the time keeper
     * itself. This separation from the constructor is required in order to not
     * create Zombie-listeners when parsing the {@linkplain HarvestTimeKeeper}
     * from disk.
     */
    public void init()
    {
        loadFromDisk();

        harvestMeasure.init(HarvestStartedEvent.class, HarvestFinishedEvent.class);
        saveMeasure.init(SaveStartedEvent.class, SaveFinishedEvent.class);
        submissionMeasure.init(SubmissionStartedEvent.class, SubmissionFinishedEvent.class);

        EventSystem.addListener(HarvestStartedEvent.class, this::onHarvestStarted);
        EventSystem.addListener(ContextDestroyedEvent.class, this::onContextDestroyed);
    }


    /**
     * Attempts to load values from a cached file.
     */
    private void loadFromDisk()
    {
        final String stableFilePath = String.format(
                                          CacheConstants.HARVEST_TIME_KEEPER_CACHE_FILE_PATH,
                                          MainContext.getModuleName());

        final DiskIO diskReader = new DiskIO();
        final HarvestTimeKeeper parsedKeeper = diskReader.getObject(stableFilePath, HarvestTimeKeeper.class);

        if (parsedKeeper != null) {
            harvestMeasure.set(parsedKeeper.harvestMeasure);
            saveMeasure.set(parsedKeeper.saveMeasure);
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
        final String stableFilePath = String.format(
                                          CacheConstants.HARVEST_TIME_KEEPER_CACHE_FILE_PATH,
                                          MainContext.getModuleName());

        final DiskIO diskWriter = new DiskIO();
        diskWriter.writeObjectToFile(stableFilePath, this);
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
    private void onHarvestStarted(HarvestStartedEvent event) // NOPMD - Event callbacks always require the event
    {
        saveMeasure.set(-1, -1, ProcessStatus.NotStarted);
        submissionMeasure.set(-1, -1, ProcessStatus.NotStarted);
    }


    /**
     * This callback function is called when the status of a time measure
     * process is being changed. It updates time keeper cache file when a
     * process is finished.
     *
     * @param event the event that triggered the listener
     */
    private void onContextDestroyed(ContextDestroyedEvent event) // NOPMD - Event callbacks always require the event
    {
        saveToDisk();
    }
}
