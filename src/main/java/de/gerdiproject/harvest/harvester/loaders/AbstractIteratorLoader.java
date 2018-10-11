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
package de.gerdiproject.harvest.harvester.loaders;

import java.util.Iterator;

/**
 * This loader can load multiple documents.
 *
 * @param <LOUT> the type of the documents to be loaded
 *
 * @author Robin Weiss
 */
public abstract class AbstractIteratorLoader <LOUT> implements ILoader<Iterator<LOUT>>
{
    @Override
    public void load(Iterator<LOUT> documents, boolean isLastDocument)
    {
        while (documents.hasNext())
            loadElement(documents.next(), isLastDocument && !documents.hasNext());
    }


    /**
     * Loads a single element of the {@linkplain Iterator}.
     *
     * @param document a document that is to be loaded
     * @param isLastDocument if true, this is the last document that is to be loaded
     */
    public abstract void loadElement(LOUT document, boolean isLastDocument);
}
