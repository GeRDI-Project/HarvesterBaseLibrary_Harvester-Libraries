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


import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.List;

import de.gerdiproject.harvest.IDocument;
import de.gerdiproject.harvest.harvester.constants.HarvesterConstants;
import de.gerdiproject.harvest.utils.HashGenerator;


/**
 * This harvester iterates through a {@link Collection} and creates a fixed
 * number of documents out of each element of the collection.
 *
 * @author Robin Weiss
 */
public abstract class AbstractListHarvester<T> extends AbstractHarvester
{
    protected Collection<T> entries;
    protected final int numberOfDocumentsPerEntry;


    /**
     * Forwarding the superclass constructor.
     *
     * @param harvesterName a unique name of the harvester
     * @param numberOfDocumentsPerEntry the number of documents that are
     *            expected to be harvested from each entry
     */
    public AbstractListHarvester(String harvesterName, int numberOfDocumentsPerEntry)
    {
        super(harvesterName);
        this.numberOfDocumentsPerEntry = numberOfDocumentsPerEntry;
    }


    /**
     * Forwarding the superclass constructor.
     *
     * @param numberOfDocumentsPerEntry the number of documents that are
     *            expected to be harvested from each entry
     */
    public AbstractListHarvester(int numberOfDocumentsPerEntry)
    {
        this(null, numberOfDocumentsPerEntry);
    }


    /**
     * Retrieves a collection of entries that are to be searched.
     *
     * @return a collection of entries
     */
    protected abstract Collection<T> loadEntries();


    /**
     * Harvests a single entry, adding between zero and
     * 'numberOfDocumentsPerEntry' entries to the search index.
     *
     * @param entry the entry that is to be read
     *
     * @return a list of search documents, or null if no documents could be
     *         retrieved from the entry
     */
    protected abstract List<IDocument> harvestEntry(T entry);


    @Override
    protected boolean harvestInternal(int from, int to) throws Exception // NOPMD - we want the inheriting class to be able to throw any exception
    {
        if (from == to) {
            logger.warn(String.format(HarvesterConstants.LOG_OUT_OF_RANGE, name));
            return true;

        } else if (entries == null || entries.isEmpty()) {
            logger.error(String.format(HarvesterConstants.ERROR_NO_ENTRIES, name));
            return false;
        }

        // indices of entries that are to be harvested
        int firstEntryIndex = from / numberOfDocumentsPerEntry;
        int lastEntryIndex = (to - 1) / numberOfDocumentsPerEntry;

        // indices of documents that are harvested from one entry
        int startIndex = from % numberOfDocumentsPerEntry;
        int endIndex = to % numberOfDocumentsPerEntry;

        // the endIndex must be in [1, docsPerEntry]
        if (endIndex == 0)
            endIndex = numberOfDocumentsPerEntry;

        // harvest the first entry
        int i = 0;

        for (T e : entries) {
            // abort harvest, if it is flagged for cancellation
            if (isAborting) {
                currentHarvestingProcess.cancel(false);
                return false;
            }

            // skip entries that come before the firstEntryIndex
            if (i >= firstEntryIndex) {
                // get documents from entry
                final List<IDocument> docs = harvestEntry(e);

                int j = (i == firstEntryIndex) ? startIndex : 0;
                int jEnd = (i == lastEntryIndex) ? endIndex : numberOfDocumentsPerEntry;

                // add all harvested documents to the cache
                if (docs != null) {
                    while (j < jEnd && j < docs.size())
                        addDocument(docs.get(j++));
                }

                // if less docs were harvested than expected,
                // skip the correct amount of documents
                while (j++ < jEnd)
                    addDocument(null);

                // finish iteration after harvesting the last index
                if (i == lastEntryIndex)
                    break;
            }

            i++;
        }

        return true;
    }


    @Override
    public void init()
    {
        entries = loadEntries();
        super.init();
    }


    @Override
    protected int initMaxNumberOfDocuments()
    {
        return entries.size() * numberOfDocumentsPerEntry;
    }


    @Override
    protected String initHash() throws NoSuchAlgorithmException, NullPointerException
    {
        return HashGenerator.instance().getShaHash(entries.toString());
    }
}
