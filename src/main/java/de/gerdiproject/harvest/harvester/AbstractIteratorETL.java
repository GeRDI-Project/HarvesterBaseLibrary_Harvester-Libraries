/**
 * Copyright © 2017 Robin Weiss (http://www.gerdi-project.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.gerdiproject.harvest.harvester;


import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.parameters.IntegerParameter;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.harvester.constants.HarvesterConstants;
import de.gerdiproject.harvest.harvester.enums.HarvesterStatus;
import de.gerdiproject.harvest.harvester.events.DocumentsHarvestedEvent;
import de.gerdiproject.harvest.harvester.extractors.AbstractIteratorExtractor;
import de.gerdiproject.harvest.harvester.loaders.AbstractIteratorLoader;
import de.gerdiproject.harvest.harvester.transformers.AbstractIteratorTransformer;


/**
 * This ETL harvests data via {@linkplain Iterator}s and creates a
 * document for each iterated element.
 *
 * @author Robin Weiss
 */
public abstract class AbstractIteratorETL<EXOUT, TRANSOUT> extends AbstractETL<Iterator<EXOUT>, Iterator<TRANSOUT>>
{
    protected volatile IntegerParameter startIndexParameter;
    protected volatile IntegerParameter endIndexParameter;
    private final AtomicInteger harvestedCount = new AtomicInteger(0);



    @Override
    protected void registerParameters()
    {
        super.registerParameters();

        this.startIndexParameter =
            Configuration.registerParameter(new IntegerParameter(
                                                HarvesterConstants.START_INDEX_PARAM.getKey(),
                                                harvesterCategory,
                                                HarvesterConstants.START_INDEX_PARAM.getValue()));

        this.endIndexParameter =
            Configuration.registerParameter(new IntegerParameter(
                                                HarvesterConstants.END_INDEX_PARAM.getKey(),
                                                harvesterCategory,
                                                HarvesterConstants.END_INDEX_PARAM.getValue()));
    }


    @Override
    public void prepareHarvest() throws ETLPreconditionException
    {
        super.prepareHarvest();

        if (getStartIndex() == getEndIndex()) {
            throw new ETLPreconditionException(
                String.format(HarvesterConstants.HARVESTER_SKIPPED_OUT_OF_RANGE, getName()));
        }

        if (!(extractor instanceof AbstractIteratorExtractor))
            throw new ETLPreconditionException(HarvesterConstants.INVALID_ITER_EXTRACTOR_ERROR);

        if (!(transformer instanceof AbstractIteratorTransformer))
            throw new ETLPreconditionException(HarvesterConstants.INVALID_ITER_TRANSFORMER_ERROR);

        if (!(loader instanceof AbstractIteratorLoader))
            throw new ETLPreconditionException(HarvesterConstants.INVALID_ITER_LOADER_ERROR);
    }


    @Override
    protected void harvestInternal() throws Exception // NOPMD - we want the inheriting class to be able to throw any exception
    {
        final AbstractIteratorExtractor<EXOUT> iterExtractor = (AbstractIteratorExtractor<EXOUT>) extractor;
        harvestedCount.set(0);

        // extract entries
        final Iterator<EXOUT> extracted = iterExtractor.extract();

        if (!extracted.hasNext())
            throw new IllegalStateException(String.format(HarvesterConstants.ERROR_NO_ENTRIES, getName()));

        // provide iterator over transformable elements
        final Iterator<TRANSOUT> transformed = transformer.transform(extracted);

        // load transformed elements
        final AbstractIteratorLoader<TRANSOUT> iterLoader = (AbstractIteratorLoader<TRANSOUT>) loader;

        while (transformed.hasNext() && status == HarvesterStatus.HARVESTING) {
            final TRANSOUT out = transformed.next();

            if (out != null)
                iterLoader.loadElement(out, !transformed.hasNext());

            harvestedCount.incrementAndGet();
            EventSystem.sendEvent(DocumentsHarvestedEvent.singleHarvestedDocument());
        }
    }


    @Override
    public int getHarvestedCount()
    {
        return harvestedCount.get();
    }


    /**
     * Returns start index 'a' of the harvesting range [a,b).
     *
     * @return the start index of the harvesting range
     */
    public int getStartIndex()
    {
        int index = startIndexParameter.getValue();

        if (index < 0)
            return 0;

        return index;
    }


    /**
     * Returns the end index 'b' of the harvesting range [a,b).
     *
     * @return the end index of the harvesting range
     */
    public int getEndIndex()
    {
        int index = endIndexParameter.getValue();

        if (index < 0)
            return 0;

        return index;
    }
}
