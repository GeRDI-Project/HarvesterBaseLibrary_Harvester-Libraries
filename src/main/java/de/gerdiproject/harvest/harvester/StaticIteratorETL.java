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

import de.gerdiproject.harvest.harvester.extractors.IExtractor;
import de.gerdiproject.harvest.harvester.extractors.AbstractIteratorExtractor;
import de.gerdiproject.harvest.harvester.transformers.AbstractIteratorTransformer;
import de.gerdiproject.harvest.harvester.transformers.ITransformer;


/**
 * This ETL harvests data via {@linkplain Iterator}s and creates a
 * document for each iterated element. The Extractor and Transformer are
 * static and will not be created anew in this implementation.
 *
 * @author Robin Weiss
 */
public class StaticIteratorETL<EXOUT, TRANSOUT> extends AbstractIteratorETL<EXOUT, TRANSOUT>
{
    /**
     * Constructor that initializes the extractor and transformer.
     *
     * @param extractor retrieves an object from the harvested repository
     * @param transformer transforms the extracted object to a document that can be put to the search index
     */
    public StaticIteratorETL(AbstractIteratorExtractor<EXOUT> extractor, AbstractIteratorTransformer<EXOUT, TRANSOUT> transformer)
    {
        super();
        this.extractor = extractor;
        this.transformer = transformer;
    }


    @Override
    protected IExtractor<Iterator<EXOUT>> createExtractor()
    {
        return extractor;
    }


    @Override
    protected ITransformer<Iterator<EXOUT>, Iterator<TRANSOUT>> createTransformer()
    {
        return transformer;
    }


}
