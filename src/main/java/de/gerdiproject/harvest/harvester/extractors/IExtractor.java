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

import de.gerdiproject.harvest.harvester.AbstractETL;

/**
 * This class represents the Extractor of an ETL process.
 *
 * @param <EXOUT> the type of objects that are extracted
 *
 * @author Robin Weiss
 */
public interface IExtractor <EXOUT>
{
    /**
     * Initializes the extractor for a new harvest.
     *
     * @param harvester the harvester to which the extractor belongs
     * @param <H> the type of the harvester to which the extractor belongs
     */
    <H extends AbstractETL<?, ?>> void init(H harvester);


    /**
     * Extracts elements from a source repository.
     *
     * @return an object that is to be transformed
     */
    EXOUT extract();


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
}
