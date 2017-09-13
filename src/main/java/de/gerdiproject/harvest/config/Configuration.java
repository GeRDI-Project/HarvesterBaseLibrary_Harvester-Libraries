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
package de.gerdiproject.harvest.config;


import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.development.DevelopmentTools;
import de.gerdiproject.harvest.elasticsearch.ElasticSearchSender;
import de.gerdiproject.harvest.harvester.AbstractHarvester;
import de.gerdiproject.harvest.utils.data.DiskIO;
import de.gerdiproject.json.GsonUtils;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


/**
 * This static class deals with saving and loading all application flags and
 * options to/from disk.
 *
 * @author Robin Weiss
 */
public class Configuration
{
    private final static String CONFIG_PATH = "config/%sConfig.json";

    private final static String LOAD_OK = "Loaded configuration from '%s'.";
    private final static String LOAD_FAILED = "Could not load configuration from '%s': %s";
    private final static String NO_EXISTS = "No configuration exists!";

    private final static String ELASTIC_SEARCH_TITLE = "elasticSearch";
    private final static String ELASTIC_SEARCH_URL = "url";
    private final static String ELASTIC_SEARCH_INDEX = "index";
    private final static String ELASTIC_SEARCH_TYPE = "type";

    private final static String HARVESTER_TITLE = "harvester";
    private final static String HARVESTER_RANGE = "range";
    private final static String HARVESTER_FROM = "from";
    private final static String HARVESTER_TO = "to";
    private final static String HARVESTER_PARAMETERS = "parameters";

    private final static String DEV_TITLE = "devOptions";
    private final static String DEV_WRITE_TO_DISK = "writeHttpToDisk";
    private final static String DEV_READ_FROM_DISK = "readHttpFromDisk";
    private final static String DEV_AUTO_SAVE = "autoSave";
    private final static String DEV_AUTO_SUBMIT = "autoSubmit";

    private final static String INFO = " Configuration:%n%s%n%n To change these settings, please use the corresponding PUT requests.";

    private final static DiskIO DISK_IO = new DiskIO();

    private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);


    /**
     * The constructor is private, because this is a static class.
     */
    private Configuration()
    {
    }


    /**
     * Assembles a pretty String that summarizes all options and flags.
     *
     * @return a pretty String that summarizes all options and flags
     */
    public static String getInfoString()
    {
        return MainContext.getModuleName()
               + String.format(INFO, GsonUtils.getPrettyGson().toJson(toJson()));
    }


    /**
     * Attempts to load a configuration file and sets all options according to
     * the file.
     *
     * @return a string describing the status of the operation
     */
    public static String loadFromDisk()
    {
        String path = getConfigFilePath();
        JsonElement configElement = DISK_IO.getJson(path);

        if (configElement != null) {
            JsonObject configJson = configElement.getAsJsonObject();
            setConfigFromJson(configJson);

            String okMsg = String.format(LOAD_OK, path);
            LOGGER.info(okMsg);
            return okMsg;
        } else {
            String errMsg = String.format(LOAD_FAILED, path, NO_EXISTS);
            LOGGER.error(errMsg);
            return errMsg;
        }
    }


    /**
     * Sets configuration from reading a Json-object.
     *
     * @param config
     *            a Json-object containing the service configuration
     */
    private static void setConfigFromJson(JsonObject config)
    {
        // set development tools configuration

        if (config.has(DEV_TITLE)) {
            JsonObject devToolsConfig = config.get(DEV_TITLE).getAsJsonObject();
            DevelopmentTools devTools = DevelopmentTools.instance();

            // set write-to-disk
            boolean writeToDisk = devToolsConfig.get(DEV_WRITE_TO_DISK).getAsBoolean();
            devTools.setWriteHttpToDisk(writeToDisk);

            // set read-from-disk
            boolean readFromDisk = devToolsConfig.get(DEV_READ_FROM_DISK).getAsBoolean();
            devTools.setReadHttpFromDisk(readFromDisk);

            // set auto-save
            boolean autoSave = devToolsConfig.get(DEV_AUTO_SAVE).getAsBoolean();
            devTools.setAutoSave(autoSave);

            // set auto-submit
            boolean autoSubmit = devToolsConfig.get(DEV_AUTO_SUBMIT).getAsBoolean();
            devTools.setAutoSubmit(autoSubmit);
        }

        // set ElasticSearch configuration
        if (config.has(ELASTIC_SEARCH_TITLE)) {
            JsonObject elasticSearchConfig = config.get(ELASTIC_SEARCH_TITLE).getAsJsonObject();

            if (elasticSearchConfig.has(ELASTIC_SEARCH_URL)
                && elasticSearchConfig.has(ELASTIC_SEARCH_INDEX)
                && elasticSearchConfig.has(ELASTIC_SEARCH_TYPE)) {
                ElasticSearchSender.instance().setUrl(
                    elasticSearchConfig.get(ELASTIC_SEARCH_URL).getAsString(),
                    elasticSearchConfig.get(ELASTIC_SEARCH_INDEX).getAsString(),
                    elasticSearchConfig.get(ELASTIC_SEARCH_TYPE).getAsString());
            }
        }




        // set harvester configuration

        if (config.has(HARVESTER_TITLE)) {
            JsonObject harvesterConfig = config.get(HARVESTER_TITLE).getAsJsonObject();
            AbstractHarvester harvester = MainContext.getHarvester();

            // set range from config
            JsonObject rangeObject = harvesterConfig.get(HARVESTER_RANGE).getAsJsonObject();
            int rangeFrom = rangeObject.get(HARVESTER_FROM).getAsInt();
            int rangeTo = rangeObject.get(HARVESTER_TO).getAsInt();
            harvester.setRange(rangeFrom, rangeTo);

            // set parameters from config
            JsonObject paramsObject = harvesterConfig.get(HARVESTER_PARAMETERS).getAsJsonObject();
            paramsObject.entrySet().forEach(
                (Map.Entry<String, JsonElement> e) -> harvester.setProperty(e.getKey(), e.getValue().getAsString()));
        }
    }


    /**
     * Returns the path to the configurationFile of this service.
     *
     * @return the path to the configurationFile of this service
     */
    private static String getConfigFilePath()
    {
        return String.format(CONFIG_PATH, MainContext.getModuleName());
    }


    /**
     * Saves the configuration as a Json file.
     *
     * @return a string describing the status of the operation
     */
    public static String saveToDisk()
    {
        // assemble path
        String path = getConfigFilePath();

        // write to disk
        return  DISK_IO.writeJsonToFile(path, toJson());
    }


    /**
     * Creates a Json-object that contains all flags and options that have been
     * set up in the service.
     *
     * @return a Json-object that contains the configuration of this service
     */
    private static JsonObject toJson()
    {
        // get ElasticSearch configuration
        JsonObject elasticSearchConfig = new JsonObject();
        elasticSearchConfig.addProperty(ELASTIC_SEARCH_URL, ElasticSearchSender.instance().getBaseUrl());
        elasticSearchConfig.addProperty(ELASTIC_SEARCH_INDEX, ElasticSearchSender.instance().getIndex());
        elasticSearchConfig.addProperty(ELASTIC_SEARCH_TYPE, ElasticSearchSender.instance().getType());

        // get developer options
        JsonObject devToolsConfig = new JsonObject();
        devToolsConfig.addProperty(DEV_WRITE_TO_DISK, DevelopmentTools.instance().isWritingHttpToDisk());
        devToolsConfig.addProperty(DEV_READ_FROM_DISK, DevelopmentTools.instance().isReadingHttpFromDisk());
        devToolsConfig.addProperty(DEV_AUTO_SAVE, DevelopmentTools.instance().isAutoSaving());
        devToolsConfig.addProperty(DEV_AUTO_SUBMIT, DevelopmentTools.instance().isAutoSubmitting());

        AbstractHarvester harvester = MainContext.getHarvester();

        // get harvester range
        JsonObject harvesterRange = new JsonObject();
        harvesterRange.addProperty(HARVESTER_FROM, harvester.getStartIndex());
        harvesterRange.addProperty(HARVESTER_TO, harvester.getEndIndex());

        // get implementation specific harvester parameters
        JsonObject harvesterParams = new JsonObject();
        List<String> harvesterParamKeys = harvester.getValidProperties();
        harvesterParamKeys.forEach((key) -> harvesterParams.addProperty(key, harvester.getProperty(key)));

        JsonObject harvesterConfig = new JsonObject();
        harvesterConfig.add(HARVESTER_RANGE, harvesterRange);
        harvesterConfig.add(HARVESTER_PARAMETERS, harvesterParams);

        JsonObject globalConfig = new JsonObject();
        globalConfig.add(HARVESTER_TITLE, harvesterConfig);
        globalConfig.add(ELASTIC_SEARCH_TITLE, elasticSearchConfig);
        globalConfig.add(DEV_TITLE, devToolsConfig);

        return globalConfig;
    }
}
