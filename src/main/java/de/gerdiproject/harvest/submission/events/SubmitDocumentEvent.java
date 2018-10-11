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


import de.gerdiproject.harvest.IDocument;
import de.gerdiproject.harvest.event.ISynchronousEvent;

/**
 * This event aims to submit a single document.
 *
 * @author Robin Weiss
 */
public class SubmitDocumentEvent implements ISynchronousEvent<Boolean>
{
    private final String documentId;
    private final IDocument document;

    /**
     * Simple Constructor.
     *
     * @param documentId a unique identifier of the document that is to be submitted
     * @param document the document that is to be submitted
     */
    public SubmitDocumentEvent(String documentId, IDocument document)
    {
        this.documentId = documentId;
        this.document = document;
    }


    /**
     * Returns the document that is to be submitted.
     *
     * @return the document that is to be submitted
     */
    public IDocument getDocument()
    {
        return document;
    }


    /**
     * Returns a unique identifier of the document that is to be submitted.
     *
     * @return a unique identifier of the document that is to be submitted
     */
    public String getDocumentId()
    {
        return documentId;
    }
}
