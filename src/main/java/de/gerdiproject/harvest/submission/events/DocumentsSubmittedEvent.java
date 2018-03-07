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
package de.gerdiproject.harvest.submission.events;

import de.gerdiproject.harvest.event.AbstractSucceededOrFailedEvent;

/**
 * This event indicates that a subset of documents have been submitted.
 *
 * @author Robin Weiss
 */
public class DocumentsSubmittedEvent extends AbstractSucceededOrFailedEvent
{

    private final int numberOfSubmittedDocs;

    /**
     * Simple Constructor.
     *
     * @param isSuccessful if true, the submission was successful
     * @param numberOfSubmittedDocs the number of documents that have been sent
     */
    public DocumentsSubmittedEvent(boolean isSuccessful, int numberOfSubmittedDocs)
    {
        super(isSuccessful);
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
