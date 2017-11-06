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
package de.gerdiproject.harvest.save.events;


import de.gerdiproject.harvest.event.IEvent;

/**
 * This event signifies that the process of saving all documents to disk was started.
 *
 * @author Robin Weiss
 */
public class SaveStartedEvent implements IEvent
{
    private final int numberOfDocs;
    private final boolean isAutoTriggered;


    /**
     * Simple Constructor.
     *
     * @param isAutoTriggered true if the event was not explicitly triggered via a REST call
     * @param numberOfDocs the number of documents that are to be saved
     */
    public SaveStartedEvent(boolean isAutoTriggered, int numberOfDocs)
    {
        this.isAutoTriggered = isAutoTriggered;
        this.numberOfDocs = numberOfDocs;
    }


    /**
     * Returns the number of documents that are to be saved.
     *
     * @return the number of documents that are to be saved
     */
    public int getNumberOfDocuments()
    {
        return numberOfDocs;
    }


    /**
     * Returns true if the event was not explicitly triggered via a REST call.
     * @return true if the event was not explicitly triggered via a REST call
     */
    public boolean isAutoTriggered()
    {
        return isAutoTriggered;
    }
}