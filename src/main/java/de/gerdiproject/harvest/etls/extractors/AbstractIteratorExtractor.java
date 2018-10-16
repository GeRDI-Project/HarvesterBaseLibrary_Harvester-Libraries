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
package de.gerdiproject.harvest.etls.extractors;

import java.util.Iterator;

import de.gerdiproject.harvest.etls.AbstractETL;
import de.gerdiproject.harvest.etls.AbstractIteratorETL;

/**
 * This {@linkplain IExtractor} can extract an {@linkplain Iterator} in order
 * to be able to iterate lists or similar constructs.
 *
 * @param <EXOUT> the type of objects that are extracted
 *
 * @author Robin Weiss
 */
public abstract class AbstractIteratorExtractor <EXOUT> implements IExtractor<Iterator<EXOUT>>
{
    private int startIndex;
    private int endIndex;


    @Override
    public void init(AbstractETL<?, ?> etl)
    {
        if (etl instanceof AbstractIteratorETL) {
            final AbstractIteratorETL<?, ?> iterHarvester = (AbstractIteratorETL<?, ?>) etl;

            this.startIndex = iterHarvester.getStartIndex();
            this.endIndex = iterHarvester.getEndIndex();
        }
    }


    /**
     * This method extracts everything there is to extract.
     *
     * @return an {@linkplain Iterator} over the complete data
     *
     * @throws ExtractorException thrown when the extraction fails
     */
    protected abstract Iterator<EXOUT> extractAll()  throws ExtractorException;


    @Override
    public Iterator<EXOUT> extract() throws ExtractorException
    {
        return new RangeRestrictedIterator(extractAll());
    }


    @Override
    public int size()
    {
        return endIndex - startIndex;
    }


    /**
     * This class is an {@linkplain Iterator} wrapper that incorporates
     * the range that was set in the {@linkplain AbstractIteratorExtractor}.
     *
     * @author Robin Weiss
     */
    private class RangeRestrictedIterator implements Iterator<EXOUT>
    {
        final Iterator<EXOUT> completeIterator;
        int index;


        public RangeRestrictedIterator(Iterator<EXOUT> completeIterator)
        {
            this.completeIterator = completeIterator;
            index = 0;

            // skip the first x entries
            while (index < startIndex && completeIterator.hasNext()) {
                next();
                index++;
            }
        }


        @Override
        public boolean hasNext()
        {
            return index < endIndex && completeIterator.hasNext();
        }


        @Override
        public EXOUT next()
        {
            return completeIterator.next();
        }
    }
}
