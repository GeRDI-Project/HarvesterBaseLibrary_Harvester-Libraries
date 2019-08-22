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
package de.gerdiproject.harvest.etls.json;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.gerdiproject.harvest.etls.AbstractETL;
import de.gerdiproject.harvest.etls.events.GetRepositoryNameEvent;
import de.gerdiproject.harvest.etls.utils.ETLManager;
import de.gerdiproject.harvest.event.EventSystem;
import lombok.Data;

/**
 * This class serves as the JSON representation of the {@linkplain ETLManager}.
 * It is used for persisting certain values through sessions.
 *
 * @author Robin Weiss
 */
@Data
public class ETLInfosJson
{
    /**
     * -- GETTER --
     * Returns an info summary of all registered ETLs combined.
     * @return an info summary of all registered ETLs combined
     */
    private final ETLJson overallInfo;

    /**
     * -- GETTER --
     * Returns a map of ETL names to {@linkplain ETLJson}s of
     * all registered {@linkplain AbstractETL}s.
     * @return a map of ETL names to {@linkplain ETLJson}s
     */
    private final Map<String, ETLJson> etlInfos;

    /**
     * -- GETTER --
     * Returns the name of the harvested repository, or null if it is unknown.
     * @return the name of the harvested repository, or null if it is unknown
     */
    private final String repositoryName;


    /**
     * Constructor that requires all fields.
     * @param overallInfo the {@linkplain ETLManager} details summarized
     * @param registeredEtls all {@linkplain AbstractETL}s registered at the registry
     */
    public ETLInfosJson(final ETLJson overallInfo, final List<AbstractETL<?, ?>> registeredEtls)
    {
        this.overallInfo = overallInfo;
        this.etlInfos = new HashMap<>();

        for (final AbstractETL<?, ?> etl : registeredEtls)
            etlInfos.put(etl.getName(), etl.getAsJson());

        this.repositoryName = EventSystem.sendSynchronousEvent(new GetRepositoryNameEvent());
    }
}
