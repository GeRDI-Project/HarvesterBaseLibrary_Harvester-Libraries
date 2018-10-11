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
package de.gerdiproject.harvest.harvester.utils;


import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.ws.rs.core.MultivaluedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gerdiproject.harvest.application.enums.HealthStatus;
import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.parameters.BooleanParameter;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.IEventListener;
import de.gerdiproject.harvest.harvester.AbstractETL;
import de.gerdiproject.harvest.harvester.ETLPreconditionException;
import de.gerdiproject.harvest.harvester.constants.HarvesterConstants;
import de.gerdiproject.harvest.harvester.enums.HarvesterStatus;
import de.gerdiproject.harvest.harvester.events.GetETLRegistryEvent;
import de.gerdiproject.harvest.harvester.events.HarvestFinishedEvent;
import de.gerdiproject.harvest.harvester.events.HarvestStartedEvent;
import de.gerdiproject.harvest.harvester.json.ETLDetails;
import de.gerdiproject.harvest.rest.AbstractRestObject;
import de.gerdiproject.harvest.state.StateMachine;
import de.gerdiproject.harvest.utils.HashGenerator;


/**
 *
 * @author Robin Weiss
 */
public class ETLRegistry extends AbstractRestObject<ETLRegistry, ETLDetails>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ETLRegistry.class);

    // fields and members
    private final List<AbstractETL<?, ?>> harvesters;
    private final BooleanParameter concurrentParam;


    /**
     * Constructor
     */
    public ETLRegistry(String moduleName)
    {
        super(moduleName, GetETLRegistryEvent.class);

        this.harvesters = new LinkedList<>();
        this.concurrentParam = Configuration.registerParameter(HarvesterConstants.CONCURRENT_PARAM);
    }


    @Override
    protected String getPrettyPlainText()
    {
        return StateMachine.getCurrentState().getStatusString();
    }


    @Override
    public ETLDetails getAsJson(MultivaluedMap<String, String> query)
    {
        return new ETLDetails(this);
    }


    public void register(AbstractETL<?, ?> harvester)
    {
        if (harvesters.contains(harvester))
            LOGGER.info(String.format(HarvesterConstants.DUPLICATE_ETL_REGISTERED_ERROR, harvester.getClass().getSimpleName()));
        else
            harvesters.add(harvester);
    }


    public boolean hasOutdatedHarvesters()
    {
        final List<Boolean> outDatedValues = processHarvesters((AbstractETL<?, ?> harvester) -> {
            harvester.update();
            return harvester.isOutdated();
        });
        return outDatedValues.contains(Boolean.TRUE);
    }


    @Override
    public void addEventListeners()
    {
        super.addEventListeners();

        processHarvesters((AbstractETL<?, ?> harvester) -> {
            if (harvester instanceof IEventListener)
                ((IEventListener)harvester).addEventListeners();
        });
    }


    @Override
    public void removeEventListeners()
    {
        super.removeEventListeners();

        processHarvesters((AbstractETL<?, ?> harvester) -> {
            if (harvester instanceof IEventListener)
                ((IEventListener)harvester).removeEventListeners();
        });
    }


    public void harvest() throws ETLPreconditionException
    {
        // do it asynchronously, so we can immediately return
        CompletableFuture.runAsync(()-> {

            processHarvesters((AbstractETL<?, ?> harvester) -> {
                harvester.prepareHarvest();
            });

            if (getStatus() == HarvesterStatus.HARVESTING)
                EventSystem.sendEvent(new HarvestStartedEvent(getHash(), getMaxNumberOfDocuments()));

            processHarvesters((AbstractETL<?, ?> harvester) -> {
                if (harvester.getStatus() == HarvesterStatus.HARVESTING)
                    harvester.harvest();

            });
        })
        .thenAccept((Void v) -> {
            EventSystem.sendEvent(new HarvestFinishedEvent(true, getHash()));
        })
        .exceptionally((Throwable cause) -> {

            EventSystem.sendEvent(new HarvestFinishedEvent(false, getHash()));
            return null;
        });
    }


    public int getMaxNumberOfDocuments()
    {
        final List<Integer> sizes = processHarvesters((AbstractETL<?, ?> harvester) ->
                                                      harvester.getMaxNumberOfDocuments());
        int total = 0;

        for (int size : sizes) {
            // of one harvester does not know its size, the total cannot be estimated
            if (size == -1)
                return -1;

            total += size;
        }

        return total;
    }


    public void abortHarvest()
    {
        processHarvesters((AbstractETL<?, ?> harvester) -> harvester.abortHarvest());
        // TODO EventSystem.sendEvent(new AbortingFinishedEvent());
    }


    public String getHash()
    {
        // for now, concatenate all hashes
        final StringBuffer hashBuilder = new StringBuffer();

        harvesters.forEach((AbstractETL<?, ?> subHarvester) -> hashBuilder.append(subHarvester.getHash()));

        final HashGenerator generator = new HashGenerator(StandardCharsets.UTF_8);
        return generator.getShaHash(hashBuilder.toString());
    }


    /**
     * Retrieves the number of documents that have been loaded.
     *
     * @return the number of documents that have been loaded
     */
    public int getHarvestedCount()
    {
        return sumUpHarvesterValues(
                   (AbstractETL<?, ?> harvester) -> harvester.getHarvestedCount()
               );
    }


    public HealthStatus getHealth()
    {
        final List<HealthStatus> healthStatuses = processHarvesters((AbstractETL<?, ?> harvester) ->
                                                                    harvester.getHealth());

        if (healthStatuses.contains(HealthStatus.FUBAR))
            return HealthStatus.FUBAR;

        if (healthStatuses.contains(HealthStatus.HARVEST_FAILED))
            return HealthStatus.HARVEST_FAILED;

        if (healthStatuses.contains(HealthStatus.SUBMISSION_FAILED))
            return HealthStatus.SUBMISSION_FAILED;

        return HealthStatus.OK;
    }


    public HarvesterStatus getStatus()
    {
        final List<HarvesterStatus> statuses = processHarvesters((AbstractETL<?, ?> harvester) -> harvester.getStatus());

        if (statuses.contains(HarvesterStatus.ABORTING))
            return HarvesterStatus.ABORTING;

        if (statuses.contains(HarvesterStatus.HARVESTING))
            return HarvesterStatus.HARVESTING;

        if (statuses.contains(HarvesterStatus.QUEUED))
            return HarvesterStatus.QUEUED;

        if (statuses.contains(HarvesterStatus.BUSY))
            return HarvesterStatus.BUSY;

        return HarvesterStatus.IDLE;
    }


    private void processHarvesters(Consumer<AbstractETL<?, ?>> consumer)
    {
        if (concurrentParam.getValue()) {
            final int len = harvesters.size();
            CompletableFuture<?>[] subProcesses = new CompletableFuture[len];

            for (int i = 0; i < len; i++) {
                final int j = i;
                subProcesses[i] = CompletableFuture.runAsync(() -> consumer.accept(harvesters.get(j)));
            }

            // wait for all sub-processes to complete
            try {
                CompletableFuture.allOf(subProcesses).get();
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.error(HarvesterConstants.ETL_PROCESSING_ERROR, e);
            }
        } else {
            for (AbstractETL<?, ?> harvester : harvesters)
                consumer.accept(harvester);
        }
    }


    private int sumUpHarvesterValues(Function<AbstractETL<?, ?>, Integer> function)
    {
        final List<Integer> processedData = processHarvesters(function);

        int total = 0;

        for (int pd : processedData)
            total += pd;

        return total;
    }


    private <T> List<T> processHarvesters(Function<AbstractETL<?, ?>, T> function)
    {
        final int len = harvesters.size();
        final List<T> returnValues = new ArrayList<>(len);

        if (concurrentParam.getValue()) {
            CompletableFuture<?>[] subProcesses = new CompletableFuture[len];

            for (int i = 0; i < len; i++) {
                final int j = i;
                subProcesses[i] = CompletableFuture.runAsync(() -> returnValues.set(j, function.apply(harvesters.get(j))));
            }

            // wait for all sub-processes to complete
            try {
                CompletableFuture.allOf(subProcesses).get();
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.error(HarvesterConstants.ETL_PROCESSING_ERROR, e);
            }
        } else {
            for (int i = 0; i < len; i++)
                returnValues.set(i, function.apply(harvesters.get(i)));
        }

        return returnValues;
    }


    /**
     * Removes a sub-harvester from the list of ongoing harvests and
     * checks if it was successful. It the last running harvester is removed,
     * another {@linkplain HarvestFinishedEvent} ist dispatched.
     *
    private void finishHarvester(HarvestFinishedEvent event)
    {
        if(harvestingSubHarvesters.remove(event.getHarvester()))
        {
            isFailing |= !event.isSuccessful();

            if(harvestingSubHarvesters.isEmpty())
            {
                EventSystem.sendEvent(new HarvestFinishedEvent(!isFailing, getHash(), this));
            }
        }

    }


    //////////////////////////////
    // Event Callback Functions //
    //////////////////////////////


    private final Consumer<HarvestFinishedEvent> onHarvesterFinished = (HarvestFinishedEvent event) -> {
            finishHarvester(event);
    };


    private final Consumer<AbortingFinishedEvent> onAbortingFinished = (AbortingFinishedEvent event) -> {
            finishHarvester(event);
    };
    */

}
