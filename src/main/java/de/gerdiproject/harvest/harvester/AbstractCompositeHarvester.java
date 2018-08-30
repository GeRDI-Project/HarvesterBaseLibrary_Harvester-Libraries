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
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import de.gerdiproject.harvest.utils.HashGenerator;
import de.gerdiproject.harvest.utils.cache.HarvesterCache;


/**
 * This harvester manages a set of sub-harvesters. When the harvest is started,
 * all sub-harvesters are started concurrently.
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
    public void addEventListeners()
    {
        super.addEventListeners();
        subHarvesters.forEach((AbstractHarvester subHarvester) -> subHarvester.addEventListeners());
    }


    @Override
    public void removeEventListeners()
    {
        subHarvesters.forEach((AbstractHarvester subHarvester) -> subHarvester.removeEventListeners());
        super.removeEventListeners();
    }


    @Override
    public void init(final boolean isMainHarvester, final String moduleName)
    {
        subHarvesters.forEach((AbstractHarvester subHarvester) -> subHarvester.init(false, moduleName));
        super.init(isMainHarvester, moduleName);
    }


    @Override
    public void update()
    {
        super.update();

        // recalculate sub-harvester range
        final int compositeStartIndex = getStartIndex();
        final int compositeEndIndex = getEndIndex();
        int numberOfProcessedDocs = 0;

        for (AbstractHarvester subHarvester : subHarvesters) {
            int numberOfSubHarvesterDocs = subHarvester.getMaxNumberOfDocuments();
            int previouslyProcessedDocs = numberOfProcessedDocs;
            numberOfProcessedDocs += numberOfSubHarvesterDocs;
            int subHarvesterStartIndex = 0;
            int subHarvesterEndIndex = 0;

            // harvesting range begins after this sub-harvester
            if (compositeStartIndex >= numberOfProcessedDocs)
                subHarvesterStartIndex = Integer.MAX_VALUE;

            // harvesting range begins in the middle of this sub-harvester
            else if (compositeStartIndex >= previouslyProcessedDocs)
                subHarvesterStartIndex = compositeStartIndex - previouslyProcessedDocs;

            // harvesting range ends after this sub-harvester
            if (compositeEndIndex >= numberOfProcessedDocs)
                subHarvesterEndIndex = Integer.MAX_VALUE;

            // harvesting range ends in the middle of this sub-harvester
            else if (compositeEndIndex >= previouslyProcessedDocs)
                subHarvesterEndIndex = compositeEndIndex - previouslyProcessedDocs;

            // update sub-harvester range
            subHarvester.startIndexParameter.setValue(String.valueOf(subHarvesterStartIndex), null);
            subHarvester.endIndexParameter.setValue(String.valueOf(subHarvesterEndIndex), null);

            // update sub-harvester
            subHarvester.update();
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
    protected int initMaxNumberOfDocuments()
    {
        int total = 0;

        for (AbstractHarvester subHarvester : subHarvesters)
            total += subHarvester.getMaxNumberOfDocuments();

        return total;
    }


    /**
     * The composite harvester does not harvest documents on its own. Therefore,
     * no cache is required.
     *
     * @param temporaryPath the path to a folder were documents are temporarily stored
     * @param stablePath the path to a folder were documents are permanently stored
     *         when the harvest was successful
     */
    @Override
    protected HarvesterCache initCache(final String temporaryPath, final String stablePath)
    {
        return null;
    }


    @Override
    protected String initHash() throws NoSuchAlgorithmException, NullPointerException
    {
        // for now, concatenate all hashes
        final StringBuffer hashBuilder = new StringBuffer();

        subHarvesters.forEach((AbstractHarvester subHarvester) -> hashBuilder.append(subHarvester.getHash(false)));

        final HashGenerator generator = new HashGenerator(getCharset());
        return generator.getShaHash(hashBuilder.toString());
    }


    @Override
    protected boolean isOutdated()
    {
        boolean hasOutdatedSubHarvesters = false;

        for (AbstractHarvester h : subHarvesters) {
            hasOutdatedSubHarvesters |= h.isOutdated();

            if (hasOutdatedSubHarvesters)
                break;
        }

        return hasOutdatedSubHarvesters;
    }


    @Override
    protected void applyCacheChanges()
    {
        subHarvesters.forEach((AbstractHarvester subHarvester) -> subHarvester.applyCacheChanges());
    }


    @Override
    protected void skipAllDocuments()
    {
        subHarvesters.forEach((AbstractHarvester subHarvester) -> subHarvester.skipAllDocuments());
    }


    @Override
    protected void abortHarvest()
    {
        isAborting = true;

        if (currentHarvestingProcess != null)
            subHarvesters.forEach((AbstractHarvester sub) -> sub.abortHarvest());
    }
}
