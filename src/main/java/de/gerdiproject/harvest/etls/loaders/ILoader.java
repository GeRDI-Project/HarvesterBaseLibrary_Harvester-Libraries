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
package de.gerdiproject.harvest.etls.loaders;

import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.etls.AbstractETL;

/**
 * This class represents the Loader of an ETL process.
 *
 * @param <S> the type of the document that is to be loaded
 *
 * @author Robin Weiss
 */
public interface ILoader <S>
{
    /**
     * Initializes the loader for a new harvest.
     *
     * @param etl the {@linkplain AbstractETL} to which the loader belongs
     */
    void init(AbstractETL<?, ?> etl);


    /**
     * Loads the document to a targeted search index.
     *
     * @param document the document that is to be loaded
     * @throws LoaderException when the load did not work properly
     */
    void load(S document) throws LoaderException;


    /**
     * If the Loader created parameters in the {@linkplain Configuration},
     * this method should unregister all of them. It's called
     * when a different Loader is used.
     */
    void unregisterParameters();


    /**
     * Closes potentially open readers/writers and finishes the Load process
     * if it is still ongoing.
     */
    void clear();
}
