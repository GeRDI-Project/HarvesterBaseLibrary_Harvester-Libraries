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

import de.gerdiproject.harvest.IDocument;
import de.gerdiproject.harvest.event.IEvent;

/**
 * This event signifies that a document was harvested and successfully cached.
 *
 * @author Robin Weiss
 */
public class DocumentHarvestedEvent implements IEvent
{
    private final IDocument document;

    /**
     * Simple constructor providing the harvested document.
     *
     * @param doc the document that was harvested, or null if nothing could be harvested
     */
    public DocumentHarvestedEvent(IDocument doc)
    {
        document = doc;
    }


    /**
     * Returns the harvested document.
     * @return the document that was harvested or null if nothing could be harvested
     */
    public IDocument getDocument()
    {
        return document;
    }
}
