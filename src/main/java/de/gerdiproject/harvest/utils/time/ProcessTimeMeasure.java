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

import java.util.Date;
import java.util.function.Consumer;

import de.gerdiproject.harvest.event.AbstractSucceededOrFailedEvent;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.IEvent;
import de.gerdiproject.harvest.event.IEventListener;
import de.gerdiproject.harvest.state.events.AbortingStartedEvent;
import de.gerdiproject.harvest.utils.Procedure;
import de.gerdiproject.harvest.utils.time.constants.HarvestTimeKeeperConstants;
import de.gerdiproject.harvest.utils.time.events.ProcessTimeMeasureFinishedEvent;

/**
 * This class listens to events that transport information about a process and
 * memorizes related timestamps as well as the process status itself.
 *
 * @author Robin Weiss
 */
public class ProcessTimeMeasure implements IEventListener
{
    private final transient ProcessTimeMeasureFinishedEvent finishedEvent;
    private final transient Procedure eventListenerAdder;
    private final transient Procedure eventListenerRemover;

    private long startTimestamp;
    private long endTimestamp;
    private ProcessStatus status;


    /**
     * Constructor that sets up the timestamps to invalid values.
     */
    public ProcessTimeMeasure()
    {
        this.finishedEvent = new ProcessTimeMeasureFinishedEvent(this);
        this.status = ProcessStatus.NotStarted;
        this.startTimestamp = -1;
        this.endTimestamp = -1;
        eventListenerAdder = null;
        eventListenerRemover = null;
    }


    /**
     * Constructor that sets up the events for measuring the process time,
     * but does not automatically add event listeners.
     *
     * @param startEvent the class of an Event that marks the beginning of the
     *            time measurement
     * @param endEvent the class of an Event that marks the end of the time
     *            measurement
     * @param <R> the type of the start event
     * @param <T> the type of the end event
     */
    public <R extends IEvent, T extends AbstractSucceededOrFailedEvent> ProcessTimeMeasure(Class<R> startEvent, Class<T> endEvent)
    {
        this.finishedEvent = new ProcessTimeMeasureFinishedEvent(this);
        this.status = ProcessStatus.NotStarted;
        this.startTimestamp = -1;
        this.endTimestamp = -1;

        // define function for starting process measurement
        final Consumer<R> onProcessStarted = (R event) -> start();

        // define function for finishing process measurement
        final Consumer<T> onProcessFinished =
        (T event) -> {
            if (event.isSuccessful())
                end(ProcessStatus.Finished);
            else
                end(ProcessStatus.Failed);
        };

        // define function for adding event listeners
        this.eventListenerAdder = () -> {
            EventSystem.addListener(startEvent, onProcessStarted);
            EventSystem.addListener(endEvent, onProcessFinished);
            EventSystem.addListener(AbortingStartedEvent.class, onProcessAborted);
        };

        // define function for removing event listeners
        this.eventListenerRemover = () -> {
            EventSystem.removeListener(startEvent, onProcessStarted);
            EventSystem.removeListener(endEvent, onProcessFinished);
            EventSystem.removeListener(AbortingStartedEvent.class, onProcessAborted);
        };
    }


    @Override
    public void addEventListeners()
    {
        if (eventListenerAdder != null)
            eventListenerAdder.run();

    }


    @Override
    public void removeEventListeners()
    {
        if (eventListenerRemover != null)
            eventListenerRemover.run();

    }


    /**
     * Starts the process time measurement.
     */
    private void start()
    {
        this.startTimestamp = System.currentTimeMillis();
        this.endTimestamp = -1;
        this.status = ProcessStatus.Started;
    }


    /**
     * Finishes the process time measurement, updating the status to the reason
     * why the process stopped.
     *
     * @param reasonToEnd a new status that represents the reason for the
     *            process end
     */
    private void end(ProcessStatus reasonToEnd)
    {
        if (status == ProcessStatus.Started) {
            this.endTimestamp = System.currentTimeMillis();
            this.status = reasonToEnd;
            EventSystem.sendEvent(finishedEvent);
        }
    }


    /**
     * Sets all fields by copying them from another
     * {@linkplain ProcessTimeMeasure}, if the process is currently not started.
     *
     * @param other another time measure
     */
    public void set(ProcessTimeMeasure other)
    {
        if (this.status != ProcessStatus.Started) {
            this.status = other.status;
            this.startTimestamp = other.startTimestamp;
            this.endTimestamp = other.endTimestamp;
        }
    }


    /**
     * Sets all fields according to specified values.
     *
     * @param startTimestamp the new timestamp at which the measure started
     * @param endTimestamp the new timestamp at which the measure ended
     * @param status the new status of the measure
     */
    public void set(long startTimestamp, long endTimestamp, ProcessStatus status)
    {
        if (this.status != ProcessStatus.Started) {
            this.status = status;
            this.startTimestamp = startTimestamp;
            this.endTimestamp = endTimestamp;
        }
    }


    /**
     * Returns the timestamp at which the process started.
     *
     * @return the timestamp at which the process started
     */
    public long getStartTimestamp()
    {
        return startTimestamp;
    }


    /**
     * Returns the timestamp at which the process stopped.
     *
     * @return the timestamp at which the process stopped
     */
    public long getEndTimestamp()
    {
        return endTimestamp;
    }


    /**
     * Returns the status of the measured process.
     *
     * @return the status of the measured process
     */
    public ProcessStatus getStatus()
    {
        return status;
    }


    @Override
    public String toString()
    {
        if (endTimestamp != -1)
            return String.format(
                       HarvestTimeKeeperConstants.STATUS_FORMAT,
                       status.name(),
                       new Date(endTimestamp).toString());

        if (startTimestamp != -1)
            return String.format(
                       HarvestTimeKeeperConstants.STATUS_FORMAT,
                       status.name(),
                       new Date(startTimestamp).toString());

        return status.name();
    }


    /**
     * An enumeration that represents the status of the process.
     *
     * @author Robin Weiss
     */
    public enum ProcessStatus {
        NotStarted, Started, Finished, Aborted, Failed
    }


    //////////////////////////////
    // Event Callback Functions //
    //////////////////////////////

    /**
     * Event callback for aborting the process
     */
    private final transient Consumer<AbortingStartedEvent> onProcessAborted = (AbortingStartedEvent event) -> {
        end(ProcessStatus.Aborted);
    };
}
