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
package de.gerdiproject.harvest.harvester.impl;

import de.gerdiproject.harvest.ICleanable;
import de.gerdiproject.harvest.config.parameters.BooleanParameter;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.harvester.ILoader;
import de.gerdiproject.harvest.harvester.events.DocumentsHarvestedEvent;
import de.gerdiproject.harvest.utils.cache.HarvesterCache;
import de.gerdiproject.json.datacite.DataCiteJson;

/**
 * @author Robin Weiss
 *
 */
public class SubmitterLoader <OUT extends DataCiteJson> implements ILoader<OUT>
{
    final HarvesterCache documentsCache;
    final BooleanParameter forceHarvestParameter;


    public SubmitterLoader(HarvesterCache documentsCache, BooleanParameter forceHarvestParameter)
    {
        super();
        this.documentsCache = documentsCache;
        this.forceHarvestParameter = forceHarvestParameter;
    }


    @Override
    public void load(OUT document)
    {
        if (document != null) {
            if (document instanceof ICleanable)
                ((ICleanable) document).clean();

            documentsCache.cacheDocument(document, forceHarvestParameter.getValue());
        }

        EventSystem.sendEvent(DocumentsHarvestedEvent.singleHarvestedDocument());
    }


    @Override
    public void init()
    {
        // TODO Auto-generated method stub

    }
}
