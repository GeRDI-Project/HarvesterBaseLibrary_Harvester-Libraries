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
package de.gerdiproject.harvest.harvester.events;

import java.time.Instant;

import de.gerdiproject.harvest.event.IEvent;

/**
 * This event signifies that a harvest has been started.
 *
 * @author Robin Weiss
 */
public class HarvestStartedEvent implements IEvent
{
    private final long startTimestamp;
    private final int startIndex;
    private final int endIndex;
    private final String harvesterHash;


    /**
     * Constructor that sets up the payload, and uses the current time as the start timestamp.
     *
     * @param startIndex the index of the first document to be harvested
     * @param endIndex the index of the last document that is to be harvested + 1
     * @param harvesterHash a hash value representing the current state of the source data
     */
    public HarvestStartedEvent(int startIndex, int endIndex, String harvesterHash)
    {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.harvesterHash = harvesterHash;
        this.startTimestamp = Instant.now().toEpochMilli();
    }


    /**
     * Constructor that sets up the payload, and allows to define the start timestamp.
     *
     * @param startIndex the index of the first document to be harvested
     * @param endIndex the index of the last document that is to be harvested + 1
     * @param harvesterHash a hash value representing the current state of the source data
     * @param timestamp the time at wich the harvest started
     */
    public HarvestStartedEvent(int startIndex, int endIndex, String harvesterHash, long timestamp)
    {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.harvesterHash = harvesterHash;
        this.startTimestamp = timestamp;
    }


    /**
     * Returns the index of the first document to be harvested.
     *
     * @return the index of the first document to be harvested
     */
    public int getStartIndex()
    {
        return startIndex;
    }


    /**
     * Returns the index of the last document that is to be harvested + 1.
     *
     * @return the index of the last document that is to be harvested + 1
     */
    public int getEndIndex()
    {
        return endIndex;
    }


    /**
     * Returns the unix timestamp at which the event was created.
     *
     * @return the unix timestamp at which the event was created
     */
    public long getStartTimestamp()
    {
        return startTimestamp;
    }


    /**
     * Returns the version hash of the harvested documents.
     *
     * @return the version hash of the harvested documents
     */
    public String getHarvesterHash()
    {
        return harvesterHash;
    }
}
