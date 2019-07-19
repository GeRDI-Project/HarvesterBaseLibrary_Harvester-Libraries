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
package de.gerdiproject.harvest.etls.events;

import java.time.Instant;

import de.gerdiproject.harvest.event.IEvent;

/**
 * This event signifies that a harvest has been started.
 *
 * @author Robin Weiss
 */
public class HarvestStartedEvent implements IEvent
{
    private final int maxHarvestableDocuments;
    private final long startTimestamp;
    private final String harvesterHash;


    /**
     * Constructor that sets up the payload, and uses the current time as the start timestamp.
     *
     * @param harvesterHash a hash value representing the current state of the source data
     * @param maxHarvestableDocuments the maximum number of expected documents
     */
    public HarvestStartedEvent(final String harvesterHash, final int maxHarvestableDocuments)
    {
        this(harvesterHash, maxHarvestableDocuments, Instant.now().toEpochMilli());
    }


    /**
     * Constructor that sets up the payload, and allows to define the start timestamp.
     *
     * @param harvesterHash a hash value representing the current state of the source data
     * @param maxHarvestableDocuments the maximum number of expected documents
     * @param timestamp the time at wich the harvest started
     */
    public HarvestStartedEvent(final String harvesterHash, final int maxHarvestableDocuments, final long timestamp)
    {
        this.harvesterHash = harvesterHash;
        this.maxHarvestableDocuments = maxHarvestableDocuments;
        this.startTimestamp = timestamp;
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


    /**
     * Returns the maximum number of harvestable documents.
     *
     * @return the maximum number of harvestable documents
     */
    public int getMaxHarvestableDocuments()
    {
        return maxHarvestableDocuments;
    }
}
