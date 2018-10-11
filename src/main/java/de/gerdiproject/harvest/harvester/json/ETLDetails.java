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
package de.gerdiproject.harvest.harvester.json;

import de.gerdiproject.harvest.application.enums.HealthStatus;
import de.gerdiproject.harvest.harvester.AbstractETL;
import de.gerdiproject.harvest.harvester.ETLRegistry;
import de.gerdiproject.harvest.harvester.enums.HarvesterStatus;

/**
 * @author Robin Weiss
 *
 */
public class ETLDetails
{
    private final String name;
    private final HarvesterStatus status;
    private final HealthStatus health;
    private final int harvestedCount;
    private final int maxDocumentCount;


    public ETLDetails(AbstractETL<?, ?, ?, ?, ?> etl)
    {
        name = etl.getName();
        status = etl.getStatus();
        health = etl.getHealth();
        harvestedCount = etl.getHarvestedCount();
        maxDocumentCount = etl.getMaxNumberOfDocuments();
    }


    public ETLDetails(ETLRegistry registry)
    {
        name = registry.getClass().getSimpleName();
        status = registry.getStatus();
        health = registry.getHealth();
        harvestedCount = registry.getHarvestedCount();
        maxDocumentCount = registry.getMaxNumberOfDocuments();
    }


    public String getName()
    {
        return name;
    }


    public HarvesterStatus getStatus()
    {
        return status;
    }


    public HealthStatus getHealth()
    {
        return health;
    }


    public int getHarvestedCount()
    {
        return harvestedCount;
    }


    public int getMaxDocumentCount()
    {
        return maxDocumentCount;
    }
}
