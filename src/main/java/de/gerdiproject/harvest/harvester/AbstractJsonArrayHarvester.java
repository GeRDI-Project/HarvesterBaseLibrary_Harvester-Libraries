/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
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
package de.gerdiproject.harvest.harvester;


import java.util.Collection;
import java.util.List;

import de.gerdiproject.json.IJsonArray;
import de.gerdiproject.json.IJsonObject;


/**
 * This abstract harvester class iterates a single {@link IJsonArray} and
 * harvests documents from it.
 *
 * @author Robin Weiss
 *
 */
public abstract class AbstractJsonArrayHarvester extends AbstractListHarvester<Object>
{
    /**
     * Forwarding the superclass constructor.
     *
     * @param harvesterName
     *            a unique name of the harvester
     * @param numberOfDocumentsPerEntry
     *            the number of documents that are expected to be harvested from
     *            each entry
     */
    public AbstractJsonArrayHarvester(String harvesterName, int numberOfDocumentsPerEntry)
    {
        super(harvesterName, numberOfDocumentsPerEntry);
    }


    /**
     * Forwarding the superclass constructor.
     *
     * @param numberOfDocumentsPerEntry
     *            the number of documents that are expected to be harvested from
     *            each entry
     */
    public AbstractJsonArrayHarvester(int numberOfDocumentsPerEntry)
    {
        super(null, numberOfDocumentsPerEntry);
    }


    /**
     * Reads a single element from the JsonArray and creates at least one
     * document.
     *
     * @param entry
     *            an element from the JsonArray
     * @return a list of at least one document
     */
    protected abstract List<IJsonObject> harvestJsonArrayEntry(IJsonObject entry);


    /**
     * Retrieves the JsonArray that is to be harvested.
     *
     * @return the JsonArray that is to be harvested
     */
    protected abstract IJsonArray getJsonArray();


    @Override
    final protected Collection<Object> getEntries()
    {
        return getJsonArray();
    }


    @Override
    final protected List<IJsonObject> harvestEntry(Object entry)
    {
        return harvestJsonArrayEntry((IJsonObject) entry);
    }
}
