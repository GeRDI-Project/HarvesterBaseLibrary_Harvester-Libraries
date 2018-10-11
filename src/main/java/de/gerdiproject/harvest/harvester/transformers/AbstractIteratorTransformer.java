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
package de.gerdiproject.harvest.harvester.transformers;

import java.util.Iterator;

import de.gerdiproject.harvest.harvester.AbstractETL;
import de.gerdiproject.harvest.harvester.extractors.AbstractIteratorExtractor;

/**
 * This transformer can transform multiple documents.
 *
 * @author Robin Weiss
 */
public abstract class AbstractIteratorTransformer <TRANSIN, TRANSOUT> implements ITransformer<Iterator<TRANSIN>, Iterator<TRANSOUT>>
{

    @Override
    public <H extends AbstractETL<?, ?>> void init(H harvester)
    {
        // by default, nothing needs to be done
    }


    /**
     * Transforms a single element from the input iterator.
     *
     * @param source a single element from the input iterator
     *
     * @return a transformed document
     */
    protected abstract TRANSOUT transformElement(TRANSIN source);


    /**
     * Loads all documents of a specified {@linkplain Iterable}.
     *
     * @param documents an {@linkplain Iterable} of documents that are to be loaded
     */
    @Override
    public Iterator<TRANSOUT> transform(Iterator<TRANSIN> elements)
    {
        return new PassThroughIterator(elements);
    }


    /**
     * This class is an {@linkplain Iterator} that wraps around the {@linkplain Iterator} provided by
     * an {@linkplain AbstractIteratorExtractor} and transforms each element before returning it.
     *
     * @author Robin Weiss
     */
    private class PassThroughIterator implements Iterator<TRANSOUT>
    {
        private final Iterator<TRANSIN> input;

        public PassThroughIterator(Iterator<TRANSIN> input)
        {
            this.input = input;
        }

        @Override
        public boolean hasNext()
        {
            return input.hasNext();
        }

        @Override
        public TRANSOUT next()
        {
            final TRANSIN in = input.next();
            return in != null ? transformElement(in) : null;
        }

    }
}
