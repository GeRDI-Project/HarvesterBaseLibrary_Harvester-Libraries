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
package de.gerdiproject.harvest.etls.transformers;

import java.util.Iterator;

import de.gerdiproject.harvest.etls.AbstractETL;
import de.gerdiproject.harvest.etls.extractors.AbstractIteratorExtractor;
import de.gerdiproject.harvest.etls.extractors.ExtractorException;

/**
 * This transformer can transform multiple documents.
 *
 * @param <TRANSIN> the type of objects that are to be transformed
 * @param <TRANSOUT> the resulting type of the transformed objects
 *
 * @author Robin Weiss
 */
public abstract class AbstractIteratorTransformer <TRANSIN, TRANSOUT> implements ITransformer<Iterator<TRANSIN>, Iterator<TRANSOUT>>
{

    @Override
    public void init(AbstractETL<?, ?> etl)
    {
        // by default, nothing needs to be done
    }


    /**
     * Transforms a single element from the input iterator.
     *
     * @param source a single element from the input iterator
     *
     * @throws TransformerException thrown when an element cannot be transformed
     *
     * @return a transformed document
     */
    protected abstract TRANSOUT transformElement(TRANSIN source) throws TransformerException;


    @Override
    public Iterator<TRANSOUT> transform(Iterator<TRANSIN> elements) throws TransformerException
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
            final TRANSIN in;

            try {
                in = input.next();
            } catch (ExtractorException e) {
                throw e;
            } catch (Exception e) {
                throw new ExtractorException(e);
            }

            try {
                return in != null ? transformElement(in) : null;
            } catch (Exception e) {
                throw new TransformerException(e);
            }
        }
    }


    @Override
    public void clear()
    {
        // nothing to do here
    }
}
