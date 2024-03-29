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

import de.gerdiproject.harvest.etls.AbstractETL;

/**
 * This class represents the Transformer of an ETL process.
 *
 * @param <T> the type of objects that are to be transformed
 * @param <S> the resulting type of the transformed objects
 *
 * @author Robin Weiss
 */
public interface ITransformer <T, S>
{
    /**
     * Initializes the transformer for a new harvest.
     *
     * @param etl the {@linkplain AbstractETL} to which the transformer belongs
     */
    void init(AbstractETL<?, ?> etl);


    /**
     * Transforms an extracted element to a document that can be loaded.
     *
     * @param source an extracted element
     *
     * @throws TransformerException thrown when an element cannot be transformed
     *
     * @return a document that can be loaded
     */
    S transform(T source) throws TransformerException;


    /**
     * Closes potentially open readers/writers and finishes the Transformation process
     * if it is still ongoing.
     */
    void clear();
}
