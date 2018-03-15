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


import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.harvester.constants.HarvesterConstants;
import de.gerdiproject.harvest.utils.cache.DocumentsCache;


/**
 * This harvester manages a set of sub-harvesters. When the harvest is started,
 * all sub-harvesters are started concurrently, but write to a shared
 * {@linkplain DocumentsCache}.
 *
 * @author Robin Weiss
 */
public abstract class AbstractCompositeHarvester extends AbstractHarvester
{
    // fields and members
    protected final Iterable<AbstractHarvester> subHarvesters;


    /**
     * Constructor that requires an Iterable of sub-harvesters and the harvester
     * name.
     *
     * @param harvesterName a unique name of the harvester
     * @param subHarvesters the harvesters that are executed concurrently when
     *            the composite harvester is started
     */
    public AbstractCompositeHarvester(String harvesterName, Iterable<AbstractHarvester> subHarvesters)
    {
        super(harvesterName);

        this.subHarvesters = subHarvesters;
    }


    /**
     * Constructor that requires an Iterable of sub-harvesters.
     *
     * @param subHarvesters the harvesters that are executed concurrently when
     *            the composite harvester is started
     */
    public AbstractCompositeHarvester(Iterable<AbstractHarvester> subHarvesters)
    {
        this(null, subHarvesters);
    }


    @Override
    protected void setStartIndex(int startIndex)
    {
        updateRangeIndex(startIndex, (AbstractHarvester h, Integer index) -> {
            h.setStartIndex(index);
        });
    }


    @Override
    protected void setEndIndex(int endIndex)
    {
        updateRangeIndex(endIndex, (AbstractHarvester h, Integer index) -> {
            h.setEndIndex(index);
        });
    }


    /**
     * Takes an index of all documents combined and adapts the harvesting ranges
     * of all sub-harvesters accordingly.
     *
     * @param index the new index, either start- or end index
     * @param indexSetter a function that sets the sub-harvesters index range
     */
    private void updateRangeIndex(int index, BiConsumer<AbstractHarvester, Integer> indexSetter)
    {
        int numberOfProcessedDocs = 0;

        for (AbstractHarvester subHarvester : subHarvesters) {
            int numberOfSubDocs = subHarvester.getMaxNumberOfDocuments();
            int previouslyProcessedDocs = numberOfProcessedDocs;
            numberOfProcessedDocs += numberOfSubDocs;
            int subValue;

            // index comes after this sub-harvester
            if (index >= numberOfProcessedDocs)
                subValue = Integer.MAX_VALUE;

            // index is within this sub-harvester
            else if (index >= previouslyProcessedDocs)
                subValue = index - previouslyProcessedDocs;

            // index comes before this sub-harvester
            else
                subValue = Integer.MIN_VALUE;

            indexSetter.accept(subHarvester, subValue);
        }
    }


    @Override
    protected boolean harvestInternal(int from, int to) throws Exception // NOPMD - we want the inheriting class to be able to throw any exception
    {
        List<CompletableFuture<?>> subProcesses = new LinkedList<>();

        // the range can be ignored at this point, because it was already set in
        // the subharvesters via the overriden setRange() method
        subHarvesters.forEach((AbstractHarvester subHarvester) -> {
            subHarvester.harvest();

            CompletableFuture<Boolean> subHarvestingProcess = subHarvester.currentHarvestingProcess;

            // add the process only if it was created successfully
            if (subHarvestingProcess != null)
                subProcesses.add(subHarvestingProcess);
        });

        // convert list to array
        CompletableFuture<?>[] futureArray = new CompletableFuture<?>[subProcesses.size()];

        for (int i = 0, len = futureArray.length; i < len; i++)
            futureArray[i] = subProcesses.get(i);

        // wait for all sub-harvesters to complete
        CompletableFuture.allOf(futureArray).get();

        return true;
    }


    @Override
    public void init()
    {
        subHarvesters.forEach((AbstractHarvester subHarvester) -> subHarvester.init());
        super.init();
    }


    @Override
    protected int initMaxNumberOfDocuments()
    {
        int total = 0;

        for (AbstractHarvester subHarvester : subHarvesters)
            total += subHarvester.getMaxNumberOfDocuments();

        return total;
    }


    @Override
    protected String initHash() throws NoSuchAlgorithmException, NullPointerException
    {
        // for now, concatenate all hashes
        final StringBuilder hashBuilder = new StringBuilder();

        subHarvesters.forEach((AbstractHarvester subHarvester) -> hashBuilder.append(subHarvester.getHash(false)));

        // generate hash of all concatenated hashes
        final MessageDigest md = MessageDigest.getInstance(HarvesterConstants.SHA_HASH_ALGORITHM);
        md.update(hashBuilder.toString().getBytes(MainContext.getCharset()));

        final byte[] digest = md.digest();

        final StringWriter buffer = new StringWriter(digest.length * 2);
        final PrintWriter pw = new PrintWriter(buffer);

        for (byte b : digest)
            pw.printf(HarvesterConstants.OCTAT_FORMAT, b);

        pw.close();

        return buffer.toString();
    }


    @Override
    protected void abortHarvest()
    {
        isAborting = true;
        if (currentHarvestingProcess != null)
            subHarvesters.forEach((AbstractHarvester sub) -> sub.abortHarvest());
    }
}
