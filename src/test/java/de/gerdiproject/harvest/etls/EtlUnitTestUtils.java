/*
 *  Copyright © 2019 Robin Weiss (http://www.gerdi-project.de/)
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
package de.gerdiproject.harvest.etls;

import de.gerdiproject.harvest.etls.extractors.IExtractor;
import de.gerdiproject.harvest.etls.transformers.ITransformer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * This utility class is used to test {@linkplain AbstractETL} implementations
 * by exposing private methods, allowing to speed up unit testing by allowing
 * to test singled out components.
 *
 * @author Robin Weiss
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EtlUnitTestUtils
{

    /**
     * Retrieves the {@linkplain IExtractor} implementation
     * of a specified {@linkplain AbstractETL}.
     *
     * @param etl the {@linkplain AbstractETL} of which the
     * {@linkplain IExtractor} is to be retrieved
     *
     * @return the {@linkplain IExtractor} implementation
     * of a specified {@linkplain AbstractETL}
     */
    public static <T> IExtractor<T> getExtractor(final AbstractETL<T, ?> etl)
    {
        return etl.extractor;
    }


    /**
     * Retrieves the {@linkplain ITransformer} implementation
     * of a specified {@linkplain AbstractETL}.
     *
     * @param etl the {@linkplain AbstractETL} of which the
     * transformer is to be retrieved
     *
     * @return the {@linkplain ITransformer} implementation
     * of a specified {@linkplain AbstractETL}
     */
    public static <T, S> ITransformer<T, S> getTransformer(final AbstractETL<T, S> etl)
    {
        return etl.transformer;
    }
}
