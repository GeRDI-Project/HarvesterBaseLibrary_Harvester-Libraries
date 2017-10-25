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

import java.util.Date;
import java.util.function.Consumer;

import de.gerdiproject.harvest.event.AbstractSucceededOrFailedEvent;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.IEvent;
import de.gerdiproject.harvest.state.events.AbortingStartedEvent;
import de.gerdiproject.harvest.utils.time.constants.HarvestTimeKeeperConstants;

/**
 * This class listens to events that transport information about a process and
 * memorizes related timestamps as well as the process status itself.
 *
 * @author Robin Weiss
 */
public class ProcessTimeMeasure
{
    private long startTimestamp;
    private long endTimestamp;
    private ProcessStatus status;


    /**
     * This constructor requires start- and finish event classes in order to properly measure time.
     *
     * @param startEvent the class of an Event that marks the beginning of the time measurement
     * @param endEvent the class of an Event that marks the end of the time measurement
     * @param <R> the type of the start event
     * @param <T> the type of the end event
     */
    public <R extends IEvent, T extends AbstractSucceededOrFailedEvent>ProcessTimeMeasure(Class<R> startEvent, Class<T> endEvent)
    {
        this.status = ProcessStatus.NotStarted;

        // create process started event callback
        Consumer<R> onProcessStarted = (R event) -> start();

        // create process finished event callback
        Consumer<T> onProcessFinished = (T event) -> {
            if (event.isSuccessful())
                end(ProcessStatus.Finished);
            else
                end(ProcessStatus.Failed);
        };

        // create process aborted event callback
        Consumer<AbortingStartedEvent> onProcessAborted =
            (AbortingStartedEvent event) -> end(ProcessStatus.Aborted);

        // add event listener callbacks
        EventSystem.addListener(startEvent, onProcessStarted);
        EventSystem.addListener(endEvent, onProcessFinished);
        EventSystem.addListener(AbortingStartedEvent.class, onProcessAborted);
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
     * Finishes the process time measurement, updating the status to the reason why the process stopped.
     * @param reasonToEnd a new status that represents the reason for the process end
     */
    private void end(ProcessStatus reasonToEnd)
    {
        if (status == ProcessStatus.Started) {
            this.endTimestamp = System.currentTimeMillis();
            this.status = reasonToEnd;
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
}
