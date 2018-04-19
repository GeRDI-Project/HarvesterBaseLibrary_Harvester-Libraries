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

import de.gerdiproject.harvest.event.IEvent;

/**
 * This event signifies that documents were harvested.
 *
 * @author Robin Weiss
 */
public class DocumentsHarvestedEvent implements IEvent
{
    private static final DocumentsHarvestedEvent singleDocHarvested = new DocumentsHarvestedEvent(1);
    private final int numberOfDocuments;


    /**
     * Returns an event that signifies that a single document was harvested.
     *
     * @return
     */
    public static DocumentsHarvestedEvent singleHarvestedDocument()
    {
        return singleDocHarvested;
    }


    /**
     * Simple constructor..
     *
     * @param numberOfDocuments the number of harvested documents
     */
    public DocumentsHarvestedEvent(int numberOfDocuments)
    {
        this.numberOfDocuments = numberOfDocuments;
    }


    /**
     * Returns the number of harvested documents.
     *
     * @return the number of harvested documents
     */
    public int getDocumentCount()
    {
        return numberOfDocuments;
    }
}
