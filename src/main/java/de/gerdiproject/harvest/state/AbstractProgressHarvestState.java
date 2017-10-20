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
package de.gerdiproject.harvest.state;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractProgressHarvestState implements IState
{
    private static final int UNKNOWN_NUMBER = -1;
    private static final String TIME_UNKNOWN = "unknown";
    private static final String DAYS_HOURS = "%dd %dh";
    private static final String HOURS_MINUTES = "%dh %dm";
    private static final String MINUTES_SECONDS = "%dm %ds";
    private static final String SECONDS = "%ds";

    private static final String PROGESS_TEXT = "%s: %3d%% (%d / %d)";
    private static final String PROGESS_TEXT_DETAILED = "%s %d / %d (%.2f%%)  Remaining Time: %s";
    protected static final Logger LOGGER = LoggerFactory.getLogger(StateMachine.class);

    protected int currentProgress;
    protected int maxProgress;
    protected long startTimeStamp;


    @Override
    public void onStateEnter()
    {
        startTimeStamp = new Date().getTime();
    }


    @Override
    public String getProgressString()
    {
        return String.format(
                   PROGESS_TEXT_DETAILED,
                   getName(),
                   currentProgress,
                   maxProgress,
                   getProgressInPercent(),
                   getDurationText(estimateRemainingSeconds()));
    }


    /**
     * Estimates the remaining seconds of the state by regarding the
     * already passed time in accordance with the progress.
     *
     * @return the remaining seconds or -1, if the time cannot be estimated
     */
    private long estimateRemainingSeconds()
    {
        // only estimate if some progress was made
        if (currentProgress > 0) {

            // calculate how many milliseconds the harvest has been going on
            long milliSecondsUntilNow = new Date().getTime() - startTimeStamp;

            // estimate how many milliseconds the state will take
            long milliSecondsTotal = milliSecondsUntilNow * (maxProgress / currentProgress);

            return (milliSecondsTotal - milliSecondsUntilNow) / 1000l;
        }

        return UNKNOWN_NUMBER;
    }


    /**
     * Calculates the progress of the state in percent.
     *
     * @return the progress in percent
     */
    private float getProgressInPercent()
    {
        return Math.min(100f, 100f * currentProgress / maxProgress);
    }


    /**
     * Creates a duration string out of a specified number of seconds
     *
     * @param durationInSeconds
     *            the duration in seconds (duh!)
     * @return a formatted duration string, or "unknown" if the duration is
     *         negative
     */
    private String getDurationText(long durationInSeconds)
    {
        String durationText;

        if (durationInSeconds == UNKNOWN_NUMBER)
            durationText = TIME_UNKNOWN;

        else if (durationInSeconds <= 60)
            durationText = String.format(SECONDS, durationInSeconds);

        else if (durationInSeconds <= 3600) {
            long minutes = durationInSeconds / 60;
            long seconds = durationInSeconds - minutes * 60;
            durationText = String.format(MINUTES_SECONDS, minutes, seconds);

        } else if (durationInSeconds <= 86400) {
            long hours = durationInSeconds / 3600;
            long minutes = durationInSeconds / 60 - hours * 60;
            durationText = String.format(HOURS_MINUTES, hours, minutes);

        } else {
            long days = durationInSeconds / 86400;
            long hours = durationInSeconds / 3600 - days * 24;
            durationText = String.format(DAYS_HOURS, days, hours);
        }

        return durationText;
    }


    /**
     * Adds progress to the state and logs a percentual increase if applicable.
     *
     * @param progress a number that represents progress of this state
     */
    public void addProgress(int progress)
    {
        int oldProgressInPercent = (int) getProgressInPercent();
        currentProgress += progress;
        int newProgressInPercent = (int) getProgressInPercent();

        // log updated progress in percent
        if (newProgressInPercent != oldProgressInPercent) {
            LOGGER.debug(String.format(
                             PROGESS_TEXT,
                             getName(),
                             newProgressInPercent,
                             currentProgress,
                             maxProgress)
                        );
        }
    }
}
