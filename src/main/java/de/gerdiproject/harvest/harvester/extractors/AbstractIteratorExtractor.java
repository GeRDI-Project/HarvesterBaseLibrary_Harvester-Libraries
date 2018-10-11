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
package de.gerdiproject.harvest.harvester.extractors;

import java.util.Iterator;

import de.gerdiproject.harvest.harvester.AbstractETL;
import de.gerdiproject.harvest.harvester.AbstractIteratorETL;

/**
 * This {@linkplain IExtractor} can extract an {@linkplain Iterator} in order
 * to be able to iterate lists or similar constructs.
 *
 * @author Robin Weiss
 */
public abstract class AbstractIteratorExtractor <IN> implements IExtractor<Iterator<IN>>
{
    private int startIndex;
    private int endIndex;


    @Override
    public <H extends AbstractETL<?, ?>> void init(H harvester)
    {
        if (harvester instanceof AbstractIteratorETL) {
            final AbstractIteratorETL<?, ?> iterHarvester = (AbstractIteratorETL<?, ?>) harvester;

            this.startIndex = iterHarvester.getStartIndex();
            this.endIndex = iterHarvester.getEndIndex();
        }
    }


    protected abstract Iterator<IN> extractAll();


    @Override
    public Iterator<IN> extract()
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
    private class RangeRestrictedIterator implements Iterator<IN>
    {
        final Iterator<IN> completeIterator;
        int index;

        public RangeRestrictedIterator(Iterator<IN> completeIterator)
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
        public IN next()
        {
            return completeIterator.next();
        }
    }
}
