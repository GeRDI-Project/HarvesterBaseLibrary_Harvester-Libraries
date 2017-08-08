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
package de.gerdiproject.harvest.development;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.harvester.AbstractHarvester;
import de.gerdiproject.harvest.utils.FileUtils;
import de.gerdiproject.json.IJsonObject;


/**
 * A class that stores development options and offers methods for writing json
 * reponses to disk.
 *
 * @author Robin Weiss
 */
public class DevelopmentTools
{
    private static final String FILE_NAME = "harvestedIndices/%s_result_%d.json";
    private static final String FILE_NAME_PARTIAL = "harvestedIndices/%s_partialResult_%d-%d_%d.json";
    private static final String NO_HARVEST = "Cannot save: Nothing was harvested yet!";

    private static final Logger LOGGER = LoggerFactory.getLogger(DevelopmentTools.class);

    private static DevelopmentTools instance;

    private boolean saveHttpRequestsToDisk;
    private boolean readHttpRequestsFromDisk;

    private boolean autoSaveHarvestResult;
    private boolean autoSubmitHarvestResult;


    /**
     * Returns the Singleton instance of this class.
     *
     * @return a Singleton instance of this class
     */
    public static synchronized DevelopmentTools instance()
    {
        if (instance == null)
            instance = new DevelopmentTools();

        return instance;
    }


    /**
     * Private constructor, because this class describes a Singleton.
     */
    private DevelopmentTools()
    {
    }


    /**
     * Saves the harvested result to disk and logs if the operation was
     * successful or not.
     *
     * @return A status message describing whether the save succeeded or not
     */
    public final String saveHarvestResultToDisk()
    {
        AbstractHarvester harvester = MainContext.getHarvester();
        IJsonObject result = harvester.createDetailedJson();

        if (result != null) {
            String fileName;
            int harvestCount = harvester.getNumberOfHarvestedDocuments();
            long harvestStartTimestamp = harvester.getHarvestStartDate().getTime();

            if (harvestCount < harvester.getMaxNumberOfDocuments()) {
                int from = harvester.getStartIndex();
                int to = from + harvestCount;

                fileName = String.format(
                               FILE_NAME_PARTIAL,
                               MainContext.getModuleName(),
                               from,
                               to,
                               harvestStartTimestamp);
            } else {
                fileName = String.format(
                               FILE_NAME,
                               MainContext.getModuleName(),
                               harvestStartTimestamp);
            }

            return FileUtils.writeToDisk(fileName, result.toJsonString());

        } else {
            LOGGER.warn(NO_HARVEST);
            return NO_HARVEST;
        }
    }



    /**
     * Changes the automatic-saving-to-disk developer option.
     *
     * @param state
     *            if true, results are automatically written to disk after a
     *            successful harvest
     */
    public void setAutoSave(boolean state)
    {
        autoSaveHarvestResult = state;
    }


    /**
     * Changes the automatic-submitting-to-elasticSearch developer option.
     *
     * @param state
     *            if true, results are automatically submitted to elastic search
     *            after a successful harvest
     */
    public void setAutoSubmit(boolean state)
    {
        autoSubmitHarvestResult = state;
    }


    /**
     * Changes the read-cached-http-responses developer option.
     *
     * @param state
     *            if true, instead of sending HTTP requests, the local file
     *            system is browsed for cached responses, that have been saved
     *            via the 'writeToDisk' flag
     */
    public void setReadHttpFromDisk(boolean state)
    {
        readHttpRequestsFromDisk = state;
    }


    /**
     * Changes the cache-http-responses developer option.
     *
     * @param state
     *            if true, all HTTP responses are written to the local file
     *            system. Failed responses result in an empty object
     */
    public void setWriteHttpToDisk(boolean state)
    {
        saveHttpRequestsToDisk = state;
    }


    /**
     * Checks the state of the automatic-saving-to-disk developer option.
     *
     * @return true, if the option is enabled
     */
    public boolean isAutoSaving()
    {
        return autoSaveHarvestResult;
    }


    /**
     * Checks the state of the automatic-submitting-to-elasticSearch developer
     * option.
     *
     * @return true, if the option is enabled
     */
    public boolean isAutoSubmitting()
    {
        return autoSubmitHarvestResult;
    }


    /**
     * Checks the state of the read-cached-http-responses developer option.
     *
     * @return true, if the option is enabled
     */
    public boolean isReadingHttpFromDisk()
    {
        return readHttpRequestsFromDisk;
    }


    /**
     * Checks the state of the cache-http-responses developer option.
     *
     * @return true, if the option is enabled
     */
    public boolean isWritingHttpToDisk()
    {
        return saveHttpRequestsToDisk;
    }
}
