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
package de.gerdiproject.harvest.etls.utils;

import java.time.Instant;

/**
 * This class serves as an entry in the {@linkplain TimestampedList}.
 *
 * @param <T> the type of the value that is stored
 *
 * @author Robin Weiss
 */
public class TimestampedEntry<T>
{
    private final T value;
    private final long timestamp;


    /**
     * Constructor that sets all fields.
     *
     * @param value the value that is stored
     * @param timestamp the time at which the value was stored
     */
    public TimestampedEntry(final T value, final long timestamp)
    {
        this.value = value;
        this.timestamp = timestamp;
    }


    /**
     * Constructor that sets the timestamp to the current time.
     *
     * @param value the value that is stored
     */
    public TimestampedEntry(final T value)
    {
        this(value, System.currentTimeMillis());
    }


    /**
     * Retrieves the stored value.
     *
     * @return the stored value
     */
    public T getValue()
    {
        return value;
    }


    /**
     * Retrieves the time at which the value was stored.
     *
     * @return the time at which the value was stored
     */
    public long getTimestamp()
    {
        return timestamp;
    }


    @Override
    public String toString()
    {
        return value.toString() + " since " + Instant.ofEpochMilli(timestamp);
    }
}
