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
package de.gerdiproject.harvest.state;

import java.util.function.Consumer;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.state.constants.StateConstants;
import de.gerdiproject.harvest.state.events.AbortingStartedEvent;
import de.gerdiproject.harvest.state.events.StartAbortingEvent;
import de.gerdiproject.harvest.state.impl.AbortingState;

/**
 * This abstract class is a state representing a process that has a clearly
 * defined start and end time. This allows for an estimation of remaining time
 * and thus, for more information to the user.
 *
 * @author Robin Weiss
 */
public abstract class AbstractProgressingState implements IState
{
    protected static final Logger LOGGER = LoggerFactory.getLogger(StateMachine.class);

    private final boolean isMaxNumberKnown;
    protected final int maxProgress;

    protected int currentProgress;
    protected long startTimeStamp;


    /**
     * Event callback for the start of the aborting process.
     */
    private final Consumer<AbortingStartedEvent> onAbortingStarted =
    (AbortingStartedEvent e) -> {
        StateMachine.setState(new AbortingState(getName()));
    };


    /**
     * Constructor that requires the progress limit.
     *
     * @param maxProgress the progress limit
     */
    public AbstractProgressingState(int maxProgress)
    {
        this.maxProgress = maxProgress;

        isMaxNumberKnown = maxProgress > 0 && maxProgress != Integer.MAX_VALUE;
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
        String status;

        if (isMaxNumberKnown)
            status = String.format(
                         StateConstants.PROGESS_TEXT_DETAILED,
                         currentProgress,
                         maxProgress,
                         getProgressInPercent(),
                         getDurationText(estimateRemainingSeconds()));
        else
            status = String.format(
                         StateConstants.PROGESS_TEXT_NO_MAX_VALUE,
                         currentProgress);

        return status;
    }


    /**
     * Returns a minimalistic progress representation of two values separated by
     * a slash. If the maximum progress number is unknown, only the number of
     * the current progress is returned.
     *
     * @return current- and max value separated by a slash, or only the current
     *         value, if the max value is unknown
     */
    @Override
    public Response getProgress()
    {
        final String entity;

        if (isMaxNumberKnown)
            entity = String.format(
                         StateConstants.PROGESS_TEXT_SIMPLE,
                         currentProgress,
                         maxProgress);
        else
            entity = String.valueOf(currentProgress);

        return Response
               .status(Status.OK)
               .entity(entity)
               .type(MediaType.TEXT_PLAIN)
               .build();
    }


    @Override
    public Response abort()
    {
        EventSystem.sendEvent(new StartAbortingEvent());

        return Response
               .status(Status.OK)
               .entity(String.format(StateConstants.ABORT_STATUS, getName()))
               .type(MediaType.TEXT_PLAIN)
               .build();
    }


    /**
     * Estimates the remaining seconds of the state by regarding the already
     * passed time in relation to the progress.
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

        return -1;
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
     * @param durationInSeconds the duration in seconds (duh!)
     * @return a formatted duration string, or "unknown" if the duration is
     *         negative
     */
    private String getDurationText(long durationInSeconds)
    {
        String durationText;

        if (durationInSeconds < 0 || durationInSeconds == Long.MAX_VALUE)
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
        if (isMaxNumberKnown && newProgressInPercent > oldProgressInPercent) {
            LOGGER.info(
                String.format(
                    StateConstants.PROGESS_TEXT,
                    getName(),
                    newProgressInPercent,
                    currentProgress,
                    maxProgress));
        }
    }

    /**
     * Creates a response, replying that the service is not available at the moment.
     * If available, a Retry-Again header is set with the remaining seconds of the process.
     *
     * @param prefix a prefix for the error response, explaining what has failed
     * @param suffix a suffix for the error response, explaining why it has failed
     *
     * @return a response, replying that the service is not available at the moment
     */
    protected Response createBusyResponse(final String prefix, final String suffix)
    {
        final ResponseBuilder rb = Response
                                   .status(Status.SERVICE_UNAVAILABLE)
                                   .entity(prefix + suffix)
                                   .type(MediaType.TEXT_PLAIN);

        long remainingSeconds = estimateRemainingSeconds();

        if (remainingSeconds != -1)
            rb.header(StateConstants.RETRY_AFTER_HEADER, remainingSeconds);

        return rb.build();
    }
}
