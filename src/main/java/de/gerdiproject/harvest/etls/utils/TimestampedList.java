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

import java.util.Collection;
import java.util.LinkedList;

/**
 * This class stores a list of values and timestamps
 * that indicate when the values were added.
 * The oldest values are removed when the set capacity
 * reaches its limit.
 *
 * @param <T> the type of the stored values
 *
 * @author Robin Weiss
 */
public class TimestampedList <T> extends LinkedList<TimestampedEntry<T>>
{
    private static final long serialVersionUID = 6315549009940523304L;
    private final transient int capacity;

    /**
     * Constructor that allows to set the max amount of
     * items in the list.
     *
     * @param capacity the max amount of items in the list
     */
    public TimestampedList(int capacity)
    {
        super();
        this.capacity = capacity;
    }


    /**
     * Constructor that allows to set the max amount of
     * items in the list and the initial value.
     *
     * @param initialValue the first item on the list
     * @param capacity the max amount of items in the list
     */
    public TimestampedList(T initialValue, int capacity)
    {
        this(capacity);
        addValue(initialValue);
    }


    /**
     * Adds a new value and the current timestamp to the list.
     *
     * @param value the new value
     */
    public void addValue(T value)
    {
        if (size() == capacity)
            removeFirst();

        add(new TimestampedEntry<>(value));
    }


    /**
     * Adds elements of another {@linkplain TimestampedList}
     * to the bottom of this list.
     *
     * @param other the {@linkplain TimestampedList} that is appended
     */
    public void addAllSorted(Collection<TimestampedEntry<T>> other)
    {
        if (other != null && !other.isEmpty()) {
            addAll(other);
            sort(new TimestampedEntryComparator());
        }
    }


    /**
     * Returns the latest value of the list.
     *
     * @return the latest value of the list
     */
    public T getLatestValue()
    {
        return getLast().getValue();
    }


    /**
     * Returns the latest value of the list.
     *
     * @return the latest value of the list
     */
    public long getLatestTimestamp()
    {
        return getLast().getTimestamp();
    }


    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();

        for (TimestampedEntry<T> entry : this) {
            if (sb.length() != 0)
                sb.append('\n');

            sb.append(entry.toString());
        }

        return sb.toString();
    }
}
