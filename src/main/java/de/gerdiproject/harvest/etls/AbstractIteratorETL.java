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
package de.gerdiproject.harvest.etls;


import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.parameters.AbstractParameter;
import de.gerdiproject.harvest.config.parameters.IntegerParameter;
import de.gerdiproject.harvest.config.parameters.constants.ParameterMappingFunctions;
import de.gerdiproject.harvest.etls.constants.ETLConstants;
import de.gerdiproject.harvest.etls.enums.ETLHealth;
import de.gerdiproject.harvest.etls.enums.ETLState;
import de.gerdiproject.harvest.etls.extractors.AbstractIteratorExtractor;
import de.gerdiproject.harvest.etls.json.ETLJson;
import de.gerdiproject.harvest.etls.loaders.AbstractIteratorLoader;
import de.gerdiproject.harvest.etls.transformers.AbstractIteratorTransformer;


/**
 * This ETL harvests data via {@linkplain Iterator}s and creates a
 * document for each iterated element.
 *
 * @param <T> the type of the extracted source data
 * @param <S> the type of the transformed documents
 *
 * @author Robin Weiss
 */
public abstract class AbstractIteratorETL<T, S> extends AbstractETL<Iterator<T>, Iterator<S>>
{
    protected volatile IntegerParameter startIndexParameter;
    protected volatile IntegerParameter endIndexParameter;
    protected final AtomicInteger harvestedCount = new AtomicInteger(0);


    /**
     * Forwarding super class constructor.
     */
    public AbstractIteratorETL()
    {
        super();
    }


    /**
     * Forwarding super class constructor.
     *
     * @param name the name of this ETL
     */
    public AbstractIteratorETL(final String name)
    {
        super(name);
    }


    @Override
    protected void registerParameters()
    {
        super.registerParameters();

        this.startIndexParameter =
            Configuration.registerParameter(new IntegerParameter(
                                                ETLConstants.START_INDEX_PARAM_KEY,
                                                getName(),
                                                ETLConstants.START_INDEX_PARAM_DEFAULT_VALUE,
                                                ParameterMappingFunctions.createMapperForETL(ParameterMappingFunctions::mapToUnsignedInteger, this)));

        this.endIndexParameter =
            Configuration.registerParameter(new IntegerParameter(
                                                ETLConstants.END_INDEX_PARAM_KEY,
                                                getName(),
                                                ETLConstants.END_INDEX_PARAM_DEFAULT_VALUE,
                                                ParameterMappingFunctions.createMapperForETL(ParameterMappingFunctions::mapToUnsignedInteger, this)));
    }


    @Override
    public void loadFromJson(final ETLJson json)
    {
        super.loadFromJson(json);
        this.harvestedCount.set(json.getHarvestedCount());
    }


    @Override
    public void prepareHarvest() throws ETLPreconditionException
    {
        super.prepareHarvest();

        if (getStartIndex() == getEndIndex()) {
            setStatus(ETLState.DONE);
            throw new ETLPreconditionException(
                String.format(ETLConstants.ETL_SKIPPED_OUT_OF_RANGE, getName()));
        }

        try {
            if (!(extractor instanceof AbstractIteratorExtractor))
                throw new ETLPreconditionException(ETLConstants.INVALID_ITER_EXTRACTOR_ERROR);

            if (!(transformer instanceof AbstractIteratorTransformer))
                throw new ETLPreconditionException(ETLConstants.INVALID_ITER_TRANSFORMER_ERROR);

            if (!(loader instanceof AbstractIteratorLoader))
                throw new ETLPreconditionException(ETLConstants.INVALID_ITER_LOADER_ERROR);
        } catch (final ETLPreconditionException e) {
            setStatus(ETLState.DONE);
            setHealth(ETLHealth.HARVEST_FAILED);
            throw e;
        }

        harvestedCount.set(0);
    }


    @Override
    public int getHarvestedCount()
    {
        return harvestedCount.get();
    }


    /**
     * Returns the total number of documents that are harvested,
     * considering the range parameters.
     *
     * @return the total number of documents that can possibly be harvested with the set range parameters
     */
    @Override
    public int getMaxNumberOfDocuments()
    {
        final int unrestrictedMaxDocs = super.getMaxNumberOfDocuments();

        if (unrestrictedMaxDocs == -1) {
            return getEndIndex() != Integer.MAX_VALUE
                   ? getEndIndex() - getStartIndex()
                   : -1;
        } else
            return Math.min(unrestrictedMaxDocs, getEndIndex()) - getStartIndex();
    }


    /**
     * Returns start index 'a' of the harvesting range [a,b).
     *
     * @return the start index of the harvesting range
     */
    public int getStartIndex()
    {
        final int index = startIndexParameter.getValue();

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
        final int index = endIndexParameter.getValue();

        if (index < 0)
            return 0;

        return index;
    }


    /**
     * This function increments the document counter. It is called when
     * the assigned {@linkplain AbstractIteratorLoader} loads a document.
     */
    public void incrementHarvestedDocuments()
    {
        harvestedCount.incrementAndGet();
    }


    //////////////////////////////
    // Event Callback Functions //
    //////////////////////////////

    @Override
    protected void onParameterChanged(final AbstractParameter<?> param)
    {
        super.onParameterChanged(param);

        final String paramKey = param.getCompositeKey();

        // if the range changed, re-init the extractor to recalculate the max
        // number of harvestable documents
        if (this.extractor != null &&
            (paramKey.equals(startIndexParameter.getCompositeKey())
             || paramKey.equals(endIndexParameter.getCompositeKey())))
            this.extractor.init(this);
    }
}
