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
package de.gerdiproject.harvest.harvester;

import java.util.Iterator;

/**
 * This loader can load multiple documents.
 *
 * @author Robin Weiss
 */
public abstract class AbstractIteratorLoader <OUT> implements ILoader<Iterator<OUT>>
{
    /**
     * Loads all documents of a specified {@linkplain Iterable}.
     *
     * @param documents an {@linkplain Iterable} of documents that are to be loaded
     */
    @Override
    public void load(Iterator<OUT> documents)
    {
        while (documents.hasNext())
            loadElement(documents.next());
    }


    /**
     * Loads a single element of the {@linkplain Iterable}.
     *
     * @param document a document that is to be loaded
     */
    public abstract void loadElement(OUT document);
}
