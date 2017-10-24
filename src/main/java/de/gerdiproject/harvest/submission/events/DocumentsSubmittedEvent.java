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
package de.gerdiproject.harvest.submission.events;

import de.gerdiproject.harvest.event.IEvent;

/**
 * This event indicates that somedocuments have been submitted successfully.
 *
 * @author Robin Weiss
 */
public class DocumentsSubmittedEvent implements IEvent
{

    private final int numberOfSubmittedDocs;

    /**
     * Simple Constructor.
     *
     * @param numberOfSubmittedDocs the number of documents that have been sent
     */
    public DocumentsSubmittedEvent(int numberOfSubmittedDocs)
    {
        this.numberOfSubmittedDocs = numberOfSubmittedDocs;
    }

    /**
     * Returns the number of documents that have been sent.
     *
     * @return the number of documents that have been sent
     */
    public int getNumberOfSubmittedDocuments()
    {
        return numberOfSubmittedDocs;
    }
}
