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
 * @param <T> the type of objects that are extracted
 *
 * @author Robin Weiss
 */
public abstract class AbstractIteratorExtractor <T> implements IExtractor<Iterator<T>>
{
    protected int startIndex;
    protected int endIndex;


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
    protected abstract Iterator<T> extractAll()  throws ExtractorException;


    @Override
    public Iterator<T> extract() throws ExtractorException
    {
        if (endIndex == Integer.MAX_VALUE && startIndex == 0)
            return extractAll();
        else
            return new RangeRestrictedIterator(extractAll());
    }


    @Override
    public int size()
    {
        // Return -1, it is not possible to count the maximum number of
        // documents before harvesting.
        if (endIndex == Integer.MAX_VALUE)
            return -1;

        return endIndex - startIndex;
    }


    /**
     * This class is an {@linkplain Iterator} wrapper that incorporates
     * the range that was set in the {@linkplain AbstractIteratorExtractor}.
     *
     * @author Robin Weiss
     */
    private class RangeRestrictedIterator implements Iterator<T>
    {
        final Iterator<T> completeIterator;
        int index;

        public RangeRestrictedIterator(Iterator<T> completeIterator)
        {
            this.completeIterator = completeIterator;
            index = 0;

            // skip the first x entries
            while (index < startIndex && completeIterator.hasNext())
                next();
        }


        @Override
        public boolean hasNext()
        {
            return (index < endIndex || endIndex == Integer.MAX_VALUE) && completeIterator.hasNext();
        }


        @Override
        public T next()
        {
            index++;

            try {
                return completeIterator.next();
            } catch (Exception e) {
                throw new ExtractorException(e);
            }
        }
    }


    @Override
    public void clear()
    {
        // nothing to clean up
    }
}
