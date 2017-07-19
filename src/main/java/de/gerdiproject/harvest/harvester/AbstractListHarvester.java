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


import de.gerdiproject.json.IJsonObject;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.List;


/**
 * This harvester iterates through a {@link Collection} and creates a fixed
 * number of documents out of each element of the collection.
 *
 * @author Robin Weiss
 */
public abstract class AbstractListHarvester<T> extends AbstractHarvester
{
    protected final static String ERROR_NO_ENTRIES = ": Could not harvest. The entries are empty or could not be retrieved";
    protected final static String LOG_OUT_OF_RANGE = ": Harvesting indices out of range. Harvesting will be skipped";

    protected Collection<T> entries;
    protected final int numberOfDocumentsPerEntry;
    protected boolean isAborting;


    /**
     * Forwarding the superclass constructor.
     *
     * @param harvesterName
     *            a unique name of the harvester
     * @param numberOfDocumentsPerEntry
     *            the number of documents that are expected to be harvested from
     *            each entry
     */
    public AbstractListHarvester(String harvesterName, int numberOfDocumentsPerEntry)
    {
        super(harvesterName);
        this.numberOfDocumentsPerEntry = numberOfDocumentsPerEntry;
    }


    /**
     * Forwarding the superclass constructor.
     *
     * @param numberOfDocumentsPerEntry
     *            the number of documents that are expected to be harvested from
     *            each entry
     */
    public AbstractListHarvester(int numberOfDocumentsPerEntry)
    {
        this(null, numberOfDocumentsPerEntry);
    }


    /**
     * Retrieves a JSON-array of entries that are to be searched.
     *
     * @return A JsonArray of entries
     */
    abstract protected Collection<T> getEntries();


    /**
     * Harvests a single entry, adding exactly one document to the search index.
     *
     * @param entry
     *            the entry that is to be read
     *
     * @return a list of search documents, created via the SearchIndexFactory
     * @see de.gerdiproject.harvest.utils.SearchIndexFactory
     */
    abstract protected List<IJsonObject> harvestEntry(T entry);


    @Override
    protected boolean harvestInternal(int from, int to) throws Exception
    {
        if (from == to) {
            logger.warn(name + LOG_OUT_OF_RANGE);
            return true;

        } else if (entries == null || entries.isEmpty()) {
            logger.error(name + ERROR_NO_ENTRIES);
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
                final List<IJsonObject> docs = harvestEntry(e);

                int jStart = (i == firstEntryIndex) ? startIndex : 0;
                int jEnd = (i == lastEntryIndex) ? endIndex : numberOfDocumentsPerEntry;

                // add all harvested documents to the index
                for (int j = jStart; j < jEnd; j++)
                    addDocumentToIndex(docs.get(j));

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
        entries = getEntries();
        super.init();
    }


    @Override
    protected int calculateTotalNumberOfDocumentsInternal()
    {
        return entries.size() * numberOfDocumentsPerEntry;
    }


    @Override
    protected String calculateHashInternal()
    {
        try {
            final MessageDigest md = MessageDigest.getInstance(SHA_HASH_ALGORITHM);
            md.update(entries.toString().getBytes());

            final byte[] digest = md.digest();

            final StringWriter buffer = new StringWriter(digest.length * 2);
            final PrintWriter pw = new PrintWriter(buffer);

            for (byte b : digest)
                pw.printf(OCTAT_FORMAT, b);

            return buffer.toString();

        } catch (NoSuchAlgorithmException | NullPointerException e) {
            logger.error(HASH_CREATE_FAILED);
            return null;
        }
    }


    @Override
    public void abortHarvest()
    {
        if (currentHarvestingProcess != null)
            isAborting = true;
    }


    @Override
    protected void onHarvestAborted()
    {
        isAborting = false;
        super.onHarvestAborted();
    }
}