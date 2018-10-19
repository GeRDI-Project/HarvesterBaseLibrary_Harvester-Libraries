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

import java.util.List;

import de.gerdiproject.harvest.etls.AbstractETL;
import de.gerdiproject.harvest.etls.enums.ETLHealth;
import de.gerdiproject.harvest.etls.enums.ETLStatus;
import de.gerdiproject.harvest.etls.utils.ETLRegistry;
import de.gerdiproject.harvest.etls.utils.TimestampedEntry;

/**
 * This class represents a JSON object containing details
 * of an {@linkplain AbstractETL} or the {@linkplain ETLRegistry}.
 *
 * @author Robin Weiss
 */
public class ETLJson
{
    private final String name;
    private final List<TimestampedEntry<ETLStatus>> statusHistory;
    private final List<TimestampedEntry<ETLHealth>> healthHistory;
    private final int harvestedCount;
    private final int maxDocumentCount;
    private final String versionHash;


    /**
     * Constructor that requires all fields.
     *
     * @param name the name of the object
     * @param statusHistory the status history
     * @param healthHistory the health history
     * @param harvestedCount the number of harvested documents
     * @param maxDocumentCount the maximum number of harvestable documents
     * @param versionHash a hash representing a version of the harvested documents
     */
    public ETLJson(String name, List<TimestampedEntry<ETLStatus>> statusHistory,
                   List<TimestampedEntry<ETLHealth>> healthHistory, int harvestedCount,
                   int maxDocumentCount, String versionHash)
    {
        this.name = name;
        this.statusHistory = statusHistory;
        this.healthHistory = healthHistory;
        this.harvestedCount = harvestedCount;
        this.maxDocumentCount = maxDocumentCount;
        this.versionHash = versionHash;
    }


    public String getName()
    {
        return name;
    }


    public List<TimestampedEntry<ETLStatus>> getStatusHistory()
    {
        return statusHistory;
    }


    public List<TimestampedEntry<ETLHealth>> getHealthHistory()
    {
        return healthHistory;
    }


    public int getHarvestedCount()
    {
        return harvestedCount;
    }


    public int getMaxDocumentCount()
    {
        return maxDocumentCount;
    }


    public String getVersionHash()
    {
        return versionHash;
    }
}
