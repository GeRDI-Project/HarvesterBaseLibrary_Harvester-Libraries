/*
 *  Copyright © 2018 Robin Weiss (http://www.gerdi-project.de/)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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
package de.gerdiproject.harvest.utils.time.events;

import de.gerdiproject.harvest.event.IEvent;
import de.gerdiproject.harvest.utils.time.ProcessTimeMeasure;

/**
 * This event is sent when a {@linkplain ProcessTimeMeasure} finished measuring
 * a process.
 *
 * @author Robin Weiss
 *
 */
public class ProcessTimeMeasureFinishedEvent implements IEvent
{
    private final ProcessTimeMeasure finishedMeasure;


    /**
     * Simple Constructor that sets up the payload.
     *
     * @param finishedMeasure the time measure that finished
     */
    public ProcessTimeMeasureFinishedEvent(ProcessTimeMeasure finishedMeasure)
    {
        super();
        this.finishedMeasure = finishedMeasure;
    }


    /**
     * Returns the time measure that finished.
     *
     * @return the time measure that finished
     */
    public ProcessTimeMeasure getFinishedMeasure()
    {
        return finishedMeasure;
    }
}
