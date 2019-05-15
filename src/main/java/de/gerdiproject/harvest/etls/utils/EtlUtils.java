/*
 *  Copyright © 2019 Robin Weiss (http://www.gerdi-project.de/)
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
package de.gerdiproject.harvest.etls.utils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import de.gerdiproject.harvest.etls.AbstractETL;
import de.gerdiproject.harvest.etls.constants.ETLConstants;
import de.gerdiproject.harvest.etls.enums.ETLHealth;
import de.gerdiproject.harvest.etls.enums.ETLState;
import de.gerdiproject.harvest.utils.HashGenerator;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * This class contains static helper functions for processing multiple
 * {@linkplain AbstractETL}s.
 *
 * @author Robin Weiss
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EtlUtils
{
    /**
     * Generates a combined hash over the concatenated hashes of a specified
     * {@linkplain Collection} of enabled {@linkplain AbstractETL}s.
     *
     * @param etls the {@linkplain AbstractETL}s of which the hashes are combined
     *
     * @return a combined hash over the concatenated hashes of specified {@linkplain AbstractETL}s
     */
    public static String getCombinedHashes(final Collection<AbstractETL<?, ?>> etls)
    {
        // concatenate all hashes
        final StringBuffer hashBuilder = new StringBuffer();

        for (final AbstractETL<?, ?> etl : etls) {
            // skip disabled ETLs
            if (!etl.isEnabled())
                continue;

            final String subHash = etl.getHash();

            // if a single hash value is unknown, we cannot generate a combined version hash
            if (subHash == null)
                return null;

            hashBuilder.append(subHash);
        }

        // rare case when no enabled ETLs exist
        if (hashBuilder.length() == 0)
            return null;

        final HashGenerator generator = new HashGenerator(StandardCharsets.UTF_8);
        return generator.getShaHash(hashBuilder.toString());
    }


    /**
     * Retrieves the combined health status of a specified
     * {@linkplain Collection} of {@linkplain AbstractETL}s.
     *
     * @param etls the {@linkplain AbstractETL}s of which the health statusses are combined
     *
     * @return a health status representing the combined health of all specified ETLs
     */
    public static ETLHealth getCombinedHealth(final Collection<AbstractETL<?, ?>> etls)
    {
        final List<ETLHealth> healthStatuses =
            processETLsAsList(etls, (final AbstractETL<?, ?> harvester) -> harvester.getHealth());

        ETLHealth overallHealth = ETLHealth.OK;

        // highest priority: the initialization of an ETL has failed
        if (healthStatuses.contains(ETLHealth.INITIALIZATION_FAILED))
            overallHealth = ETLHealth.INITIALIZATION_FAILED;

        // second highest priority: the overall harvesting process failed
        else if (healthStatuses.contains(ETLHealth.HARVEST_FAILED))
            overallHealth = ETLHealth.HARVEST_FAILED;

        else {
            // if two or more ETLs failed for different reasons, summarize health as HARVEST_FAILED
            if (healthStatuses.contains(ETLHealth.EXTRACTION_FAILED))
                overallHealth = ETLHealth.EXTRACTION_FAILED;

            if (healthStatuses.contains(ETLHealth.TRANSFORMATION_FAILED))
                overallHealth = overallHealth == ETLHealth.OK
                                ? ETLHealth.TRANSFORMATION_FAILED
                                : ETLHealth.HARVEST_FAILED;

            if (healthStatuses.contains(ETLHealth.LOADING_FAILED))
                overallHealth = overallHealth == ETLHealth.OK
                                ? ETLHealth.LOADING_FAILED
                                : ETLHealth.HARVEST_FAILED;
        }

        return overallHealth;
    }


    /**
     * Estimates the remaining harvesting duration in milliseconds.
     *
     * @param harvestStartTimestamp the unix timestamp that denotes when the harvest started
     * @param etlStatus the current status of the {@linkplain AbstractETL}s
     * @param harvestedDocuments the number of documents that were harvested
     * @param maxDocuments the total number of harvestable documents, or -1
     * if unknown
     *
     * @return the remaining harvesting duration in milliseconds,
     * or -1 if it cannot be estimated
     */
    public static long estimateRemainingHarvestTime(
        final long harvestStartTimestamp,
        final ETLState etlStatus,
        final int harvestedDocuments,
        final int maxDocuments)
    {
        // if there is no ongoing harvest, we cannot estimate the time
        if (etlStatus != ETLState.HARVESTING)
            return -1;

        // if we do not now how many documents there are, we cannot estimate the time
        if (maxDocuments == -1)
            return -1;

        // if nothing was harvested yet, we cannot estimate the time
        if (harvestedDocuments == 0)
            return -1;

        // calculate for how many milliseconds the harvest has been going on
        final long millisSinceHarvestStarted = System.currentTimeMillis() - harvestStartTimestamp;

        // calculate the average milliseconds for harvesting a single document
        final long averageMillisPerDocument = millisSinceHarvestStarted / harvestedDocuments;

        // estimate how many milliseconds it will take to harvest the remaining documents
        return averageMillisPerDocument * (maxDocuments - harvestedDocuments);
    }


    /**
     * Creates a duration string out of a specified number of seconds
     *
     * @param milliseconds the time span in milliseconds
     * @return a formatted duration string, or "unknown" if the duration is
     *         negative
     */
    public static String formatHarvestTime(final long milliseconds)
    {
        if (milliseconds < 0 || milliseconds == Long.MAX_VALUE)
            return ETLConstants.REMAINING_TIME_UNKNOWN;

        final long hours = milliseconds / 3600000;
        return String.format(ETLConstants.REMAINING_TIME, hours, milliseconds);
    }


    /**
     * Processes a {@linkplain Collection} of {@linkplain AbstractETL}s sequentially and stores the return
     * values of a specified {@linkplain Function} in a {@linkplain List}.
     *
     * @param etls the {@linkplain AbstractETL}s that are to be processed
     * @param function a {@linkplain Function} that is called for each ETL, using the ETL as a parameter
     * @param <T> the type of the {@linkplain Function} return value
     *
     * @return a {@linkplain List} containing the return values of all {@linkplain Function}s
     */
    public static <T> List<T> processETLsAsList(final Collection<AbstractETL<?, ?>> etls, final Function<AbstractETL<?, ?>, T> function)
    {
        final List<T> returnValues = new ArrayList<>(etls.size());

        for (final AbstractETL<?, ?> etl : etls)
            if (etl.isEnabled())
                returnValues.add(function.apply(etl));

        return returnValues;
    }


    /**
     * Sequentially processes a {@linkplain Collection} of {@linkplain AbstractETL}s and
     * executes a specified {@linkplain Consumer} on each one.
     *
     * @param etls the {@linkplain AbstractETL}s that are to be processed
     * @param consumer a {@linkplain Consumer} that is called for each ETL, using the ETL as a parameter
     */
    public static void processETLs(final Collection<AbstractETL<?, ?>> etls, final Consumer<AbstractETL<?, ?>> consumer)
    {
        for (final AbstractETL<?, ?> etl : etls)
            if (etl.isEnabled())
                consumer.accept(etl);
    }


    /**
     * Iterates a {@linkplain Collection} of {@linkplain AbstractETL}s sequentially
     * and sums up the return values of a specified {@linkplain Function}.
     *
     * @param etls the {@linkplain AbstractETL}s that are to be processed
     * @param intFunction a {@linkplain Function} that is called for each ETL, using the ETL as a parameter
     * and returning an integer value
     *
     * @return the sum of all return values of the {@linkplain Function}
     */
    public static int sumUpETLValues(final Collection<AbstractETL<?, ?>> etls, final Function<AbstractETL<?, ?>, Integer> intFunction)
    {
        final List<Integer> processedData = processETLsAsList(etls, intFunction);

        int total = 0;

        for (final int pd : processedData)
            total += pd;

        return total;
    }
}
