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

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.state.constants.StateConstants;
import de.gerdiproject.harvest.state.events.AbortingStartedEvent;
import de.gerdiproject.harvest.state.events.ChangeStateEvent;
import de.gerdiproject.harvest.state.events.StartAbortingEvent;
import de.gerdiproject.harvest.state.impl.AbortingState;

/**
 * This abstract class is a state that has a clearly defined start and end time.
 * This allows for an estimation of remaining time and thus, for more information to the user.
 *
 * @author Robin Weiss
 */
public abstract class AbstractProgressingState implements IState
{
    protected static final Logger LOGGER = LoggerFactory.getLogger(StateMachine.class);

    protected int currentProgress;
    protected final int maxProgress;
    protected long startTimeStamp;


    /**
     * Event callback for the start of the aborting process.
     */
    private final Consumer<AbortingStartedEvent> onAbortingStarted =
    (AbortingStartedEvent e) -> {
        AbortingState nextState = new AbortingState(getName());
        EventSystem.sendEvent(new ChangeStateEvent(nextState));
    };


    /**
     * Constructor that requires the progress limit.
     * @param maxProgress the progress limit
     */
    public AbstractProgressingState(int maxProgress)
    {
        this.maxProgress = maxProgress;
    }


    @Override
    public void onStateEnter()
    {
        startTimeStamp = System.currentTimeMillis();
        EventSystem.addListener(AbortingStartedEvent.class, onAbortingStarted);
    }


    @Override
    public void onStateLeave()
    {
        EventSystem.removeListener(AbortingStartedEvent.class, onAbortingStarted);
    }


    @Override
    public String getStatusString()
    {
        return String.format(
                   StateConstants.PROGESS_TEXT_DETAILED,
                   currentProgress,
                   maxProgress,
                   getProgressInPercent(),
                   getDurationText(estimateRemainingSeconds()));
    }


    @Override
    public String abort()
    {
        EventSystem.sendEvent(new StartAbortingEvent());
        return String.format(StateConstants.ABORT_STATUS, getName());
    }


    /**
     * Estimates the remaining seconds of the state by regarding the
     * already passed time in relation to the progress.
     *
     * @return the remaining seconds or -1, if the time cannot be estimated
     */
    private long estimateRemainingSeconds()
    {
        // only estimate if some progress was made
        if (currentProgress > 0) {

            // calculate how many milliseconds the harvest has been going on
            long milliSecondsUntilNow = System.currentTimeMillis() - startTimeStamp;

            // estimate how many milliseconds the state will take
            long milliSecondsTotal = (milliSecondsUntilNow * maxProgress) / currentProgress;

            return (milliSecondsTotal - milliSecondsUntilNow) / 1000l;
        }

        return StateConstants.UNKNOWN_NUMBER;
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

        if (durationInSeconds == StateConstants.UNKNOWN_NUMBER)
            durationText = StateConstants.TIME_UNKNOWN;

        else if (durationInSeconds <= 60)
            durationText = String.format(StateConstants.SECONDS, durationInSeconds);

        else if (durationInSeconds <= 3600) {
            long minutes = durationInSeconds / 60;
            long seconds = durationInSeconds - minutes * 60;
            durationText = String.format(StateConstants.MINUTES_SECONDS, minutes, seconds);

        } else if (durationInSeconds <= 86400) {
            long hours = durationInSeconds / 3600;
            long minutes = durationInSeconds / 60 - hours * 60;
            durationText = String.format(StateConstants.HOURS_MINUTES, hours, minutes);

        } else {
            long days = durationInSeconds / 86400;
            long hours = durationInSeconds / 3600 - days * 24;
            durationText = String.format(StateConstants.DAYS_HOURS, days, hours);
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
                             StateConstants.PROGESS_TEXT,
                             getName(),
                             newProgressInPercent,
                             currentProgress,
                             maxProgress)
                        );
        }
    }
}
