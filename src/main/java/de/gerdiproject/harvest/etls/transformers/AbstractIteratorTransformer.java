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

import de.gerdiproject.harvest.etls.extractors.AbstractIteratorExtractor;
import de.gerdiproject.harvest.etls.extractors.ExtractorException;

/**
 * This transformer can transform multiple documents.
 *
 * @param <T> the type of objects that are to be transformed
 * @param <S> the resulting type of the transformed objects
 *
 * @author Robin Weiss
 */
public abstract class AbstractIteratorTransformer <T, S> implements ITransformer<Iterator<T>, Iterator<S>>
{
    /**
     * Transforms a single element.
     *
     * @param source a single element
     *
     * @throws TransformerException thrown when an element cannot be transformed
     *
     * @return a transformed document
     */
    protected abstract S transformElement(T source) throws TransformerException;


    @Override
    public Iterator<S> transform(final Iterator<T> elements) throws TransformerException
    {
        return new PassThroughIterator(elements);
    }


    /**
     * This class is an {@linkplain Iterator} that wraps around the {@linkplain Iterator} provided by
     * an {@linkplain AbstractIteratorExtractor} and transforms each element before returning it.
     *
     * @author Robin Weiss
     */
    private class PassThroughIterator implements Iterator<S>
    {
        private final Iterator<T> input;

        /**
         * Constructor.
         *
         * @param input the {@linkplain Iterator} returned by an {@linkplain AbstractIteratorExtractor}
         */
        public PassThroughIterator(final Iterator<T> input)
        {
            this.input = input;
        }

        @Override
        public boolean hasNext()
        {
            return input.hasNext();
        }

        @Override
        public S next()
        {
            final T in;

            try {
                in = input.next();
            } catch (final ExtractorException e) { // NOPMD handle extractor exceptions differently
                throw e;
            } catch (final RuntimeException e) { // NOPMD wrap any other exception in an extractor exception
                throw new ExtractorException(e);
            }

            try {
                return in == null ? null : transformElement(in);
            } catch (final TransformerException e) { // NOPMD handle transformer exceptions differently
                throw e;
            } catch (final RuntimeException e) { // NOPMD wrap any other exception in a transformer exception
                throw new TransformerException(e);
            }
        }
    }
}
