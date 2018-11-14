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

import de.gerdiproject.harvest.etls.AbstractETL;

/**
 * This class represents the Extractor of an ETL process.
 *
 * @param <T> the type of objects that are extracted
 *
 * @author Robin Weiss
 */
public interface IExtractor <T>
{
    /**
     * Initializes the extractor for a new harvest.
     *
     * @param etl the {@linkplain AbstractETL} to which the extractor belongs
     */
    void init(AbstractETL<?, ?> etl);


    /**
     * Extracts elements from a source repository.
     *
     * @throws ExtractorException thrown when the extraction fails
     *
     * @return an object that is to be transformed
     */
    T extract() throws ExtractorException;


    /**
     * Returns a string that can be hashed to generate a unique
     * identifier for the current version of the extractable elements.
     *
     * @return a unique version of the extractable elements
     */
    String getUniqueVersionString();


    /**
     * Returns the number of extractable documents, or -1 if they cannot
     * be estimated.
     *
     * @return the number of extractable documents, or -1
     */
    int size();


    /**
     * Closes potentially open readers/writers and finishes the Extraction process
     * if it is still ongoing.
     */
    void clear();
}
