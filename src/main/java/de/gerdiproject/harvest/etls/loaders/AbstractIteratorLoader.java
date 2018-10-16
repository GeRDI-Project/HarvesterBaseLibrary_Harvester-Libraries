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

import java.util.Iterator;

import de.gerdiproject.harvest.etls.AbstractETL;
import de.gerdiproject.harvest.etls.AbstractIteratorETL;
import de.gerdiproject.harvest.etls.enums.ETLStatus;
import de.gerdiproject.harvest.etls.loaders.constants.LoaderConstants;

/**
 * This loader can load multiple documents.
 *
 * @param <LOUT> the type of the documents to be loaded
 *
 * @author Robin Weiss
 */
public abstract class AbstractIteratorLoader <LOUT> implements ILoader<Iterator<LOUT>>
{
    protected AbstractIteratorETL<?, ?> dedicatedEtl;
    protected boolean hasLoadedDocuments;


    @Override
    public void init(AbstractETL<?, ?> etl)
    {
        if (!(etl instanceof AbstractIteratorETL))
            throw new IllegalStateException(String.format(LoaderConstants.NO_ITER_ETL_ERROR, getClass().getSimpleName()));

        this.dedicatedEtl = (AbstractIteratorETL<?, ?>) etl;
        this.hasLoadedDocuments = false;
    }


    @Override
    public void load(Iterator<LOUT> documents, boolean isLastDocument) throws LoaderException
    {
        // only load documents while the harvester is running
        while (documents.hasNext() && dedicatedEtl.getStatus() == ETLStatus.HARVESTING)
            loadElementAndIncrement(documents.next(), isLastDocument && !documents.hasNext());

        // if nothing was loaded, throw an exception
        if (isLastDocument && !hasLoadedDocuments)
            throw new LoaderException(LoaderConstants.NO_DOCS_ERROR);
    }


    /**
     * Loads a single element of the {@linkplain Iterator} and increments
     * the number of harvested documents of the dedicated harvester, regardless
     * of whether the loading succeeded or not.
     *
     * @param document a document that is to be loaded
     * @param isLastDocument if true, this is the last document that is to be loaded
     *
     * @throws LoaderException when the load failed
     */
    protected void loadElementAndIncrement(LOUT document, boolean isLastDocument) throws LoaderException
    {
        // even if nothing was harvested, one source was processed, so we increment the counter
        if (document == null) {
            dedicatedEtl.incrementHarvestedDocuments();
            return;
        }

        try {
            loadElement(document, isLastDocument);
            hasLoadedDocuments = true;
        } finally {
            // even if the loading failed, we processed something, so we increment the counter
            dedicatedEtl.incrementHarvestedDocuments();
        }
    }


    /**
     * Loads a single element of the {@linkplain Iterator}.
     *
     * @param document a document that is to be loaded
     * @param isLastDocument if true, this is the last document that is to be loaded
     *
     * @throws LoaderException when the load failed
     */
    protected abstract void loadElement(LOUT document, boolean isLastDocument) throws LoaderException;
}
