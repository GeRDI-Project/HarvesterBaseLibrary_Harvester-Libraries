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
package de.gerdiproject.harvest.harvester.events;

import de.gerdiproject.harvest.event.AbstractSucceededOrFailedEvent;

/**
 * This event signifies that a harvest has been completed.
 *
 * @author Robin Weiss
 */
public class HarvestFinishedEvent extends AbstractSucceededOrFailedEvent
{
    private final String documentChecksum;


    /**
     * Simple Constructor.
     *
     * @param isSuccessful true if the harvest finished successfully
     * @param documentChecksum a hash value over all harvested documents
     */
    public HarvestFinishedEvent(boolean isSuccessful, String documentChecksum)
    {
        super(isSuccessful);
        this.documentChecksum = documentChecksum;
    }


    /**
     * Returns a hash value over all harvested documents.
     * @return a hash value over all harvested documents
     */
    public String getDocumentChecksum()
    {
        return documentChecksum;
    }
}
