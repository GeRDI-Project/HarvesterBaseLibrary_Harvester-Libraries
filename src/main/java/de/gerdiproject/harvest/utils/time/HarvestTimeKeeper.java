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
package de.gerdiproject.harvest.utils.time;

import de.gerdiproject.harvest.harvester.events.HarvestFinishedEvent;
import de.gerdiproject.harvest.harvester.events.HarvestStartedEvent;
import de.gerdiproject.harvest.save.events.SaveFinishedEvent;
import de.gerdiproject.harvest.save.events.SaveStartedEvent;
import de.gerdiproject.harvest.submission.events.SubmissionFinishedEvent;
import de.gerdiproject.harvest.submission.events.SubmissionStartedEvent;

/**
 * This class keeps timestamps of important events, such as harvests, submissions, and saves.
 *
 * @author Robin Weiss
 */
public class HarvestTimeKeeper
{
    private final ProcessTimeMeasure harvestMeasure;
    private final ProcessTimeMeasure submissionMeasure;
    private final ProcessTimeMeasure saveMeasure;


    /**
     * Constructor that creates time measures for harvesting, submitting, and saving.
     */
    public HarvestTimeKeeper()
    {
        harvestMeasure = new ProcessTimeMeasure(HarvestStartedEvent.class, HarvestFinishedEvent.class);
        submissionMeasure = new ProcessTimeMeasure(SubmissionStartedEvent.class, SubmissionFinishedEvent.class);
        saveMeasure = new ProcessTimeMeasure(SaveStartedEvent.class, SaveFinishedEvent.class);
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
}
