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
package de.gerdiproject.harvest.harvester.loaders;

import de.gerdiproject.harvest.harvester.AbstractETL;

/**
 * This class represents the Loader of an ETL process.
 *
 * @author Robin Weiss
 */
public interface ILoader <OUT>
{
    /**
     * Initializes the loader for a new harvest.
     *
     * @param harvester the harvester to which the loader belongs
     */
    <H extends AbstractETL<?, ?>> void init(H harvester);


    /**
     * Resets the loader, making it ready to start
     * another loading process.
     */
    void reset();


    /**
     * Submits the document to a targeted search index.
     *
     * @param document the document that is to be submitted
     * @param isLastDocument if true, this is the last document to be submitted
     *
     * @throws LoaderException when the load did not work properly
     */
    void load(OUT document, boolean isLastDocument) throws LoaderException;
}
