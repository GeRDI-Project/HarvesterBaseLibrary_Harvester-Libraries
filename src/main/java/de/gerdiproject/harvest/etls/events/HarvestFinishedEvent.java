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

import de.gerdiproject.harvest.event.AbstractSucceededOrFailedEvent;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

/**
 * This event signifies that a harvest has been completed.
 *
 * @author Robin Weiss
 */
@Value @EqualsAndHashCode(callSuper = true) @ToString(callSuper = true)
public class HarvestFinishedEvent extends AbstractSucceededOrFailedEvent
{
    /**
     * -- GETTER --
     * Returns a hash value over all harvested documents.
     * @return a hash value over all harvested documents
     */
    private final String documentChecksum;

    /**
     * -- GETTER --
     * Returns the unix timestamp at which the event was created.
     * @return the unix timestamp at which the event was created
     */
    private final long endTimestamp;


    /**
     * Constructor that sets up the payload and uses the current time as the end timestamp.
     *
     * @param isSuccessful true if the harvest finished successfully
     * @param documentChecksum a hash value over all harvested documents
     */
    public HarvestFinishedEvent(final boolean isSuccessful, final String documentChecksum)
    {
        super(isSuccessful);
        this.documentChecksum = documentChecksum;
        this.endTimestamp = Instant.now().toEpochMilli();
    }


    /**
     * Constructor that sets up the payload and allows the current time as the end timestamp.
     *
     * @param isSuccessful true if the harvest finished successfully
     * @param documentChecksum a hash value over all harvested documents
     * @param timestamp the time at which the harvest ended
     */
    public HarvestFinishedEvent(final boolean isSuccessful, final String documentChecksum, final long timestamp)
    {
        super(isSuccessful);
        this.documentChecksum = documentChecksum;
        this.endTimestamp = timestamp;
    }
}
