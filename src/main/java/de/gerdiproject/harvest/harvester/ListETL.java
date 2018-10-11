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
package de.gerdiproject.harvest.harvester;


import java.util.Iterator;
import java.util.List;

import de.gerdiproject.harvest.harvester.constants.HarvesterConstants;
import de.gerdiproject.harvest.harvester.enums.HarvesterStatus;


/**
 * This ETL iterates through a {@link List} and creates a
 * documents for each element of the collection.
 *
 * @author Robin Weiss
 */
public class ListETL<EXOUT, TRANSOUT, E extends IIteratorExtractor<EXOUT>, T extends AbstractIteratorTransformer<EXOUT, TRANSOUT>, L extends AbstractIteratorLoader<TRANSOUT>> extends AbstractETL<Iterator<EXOUT>, Iterator<TRANSOUT>, E, T, L>
{
    private volatile int harvestedCount;

    /**
     * Forwarding the superclass constructor.
     *
     * @param harvesterName a unique name of the harvester
     * @param extractor retrieves an object from the harvested repository
     * @param transformer transforms the extracted object to a document that can be put to the search index
     * @param loader submits the transformed object to a search index
     */
    public ListETL(String harvesterName, E extractor, T transformer, L loader)
    {
        super(harvesterName, extractor, transformer, loader);
    }


    /**
     * Forwarding the superclass constructor.
     * @param extractor retrieves an object from the harvested repository
     * @param transformer transforms the extracted object to a document that can be put to the search index
     * @param loader submits the transformed object to a search index
     */
    public ListETL(E extractor, T transformer, L loader)
    {
        this(null, extractor, transformer, loader);
    }


    @Override
    protected void harvestInternal() throws Exception // NOPMD - we want the inheriting class to be able to throw any exception
    {
        extractor.setRange(getStartIndex(), getEndIndex());
        harvestedCount = 0;

        // extract entries
        final Iterator<EXOUT> extracted = extractor.extract();

        if (!extracted.hasNext())
            throw new IllegalStateException(String.format(HarvesterConstants.ERROR_NO_ENTRIES, name));

        // provide iterator over transformable elements
        final Iterator<TRANSOUT> transformed = transformer.transform(extracted);

        // load transformed elements
        while (transformed.hasNext() && status == HarvesterStatus.HARVESTING) {
            final TRANSOUT out = transformed.next();

            if (out != null)
                loader.loadElement(transformed.next());

            harvestedCount++;
        }
    }


    @Override
    public int getHarvestedCount()
    {
        return harvestedCount;
    }
}
