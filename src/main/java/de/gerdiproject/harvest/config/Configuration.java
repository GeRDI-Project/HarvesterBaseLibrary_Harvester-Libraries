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
import de.gerdiproject.json.IJsonBuilder;
import de.gerdiproject.json.IJsonObject;
import de.gerdiproject.json.IJsonReader;
import de.gerdiproject.json.impl.JsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * This static class deals with saving and loading all application flags and
 * options to/from disk.
 *
 * @author row
 */
public class Configuration
{
    private final static String CONFIG_PATH = "config/%sConfig.json";
    private final static String SAVE_OK = "Saved configuration to '%s'.";
    private final static String SAVE_FAILED = "Could not save configuration: ";

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
    
    private final static String INFO = " Configuration:\n%s\n\n To change these settings, please use the corresponding PUT requests.";

    private final static IJsonBuilder JSON_BUILDER = new JsonBuilder();
    
    /**
     * The constructor is private, because this is a static class.
     */
    private Configuration()
    {
    }
    
    /**
     * Assembles a pretty String that summarizes all options and flags.
     * @return a pretty String that summarizes all options and flags
     */
    public static String getInfoString()
    {
        return MainContext.getModuleName() + String.format( INFO, toJson().toJsonString());
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
        final File file = new File( path );

        if (file.exists())
        {
            try
            {
                // parse the json object
                IJsonReader jsonReader = JSON_BUILDER.createReader( new FileReader( file ) );
                IJsonObject configJson = jsonReader.readObject();
                jsonReader.close();

                // retrieve config from json
                setConfigFromJson( configJson );

                return MainContext.getLogger().log( String.format( LOAD_OK, path ) );
            }
            catch (Exception ex)
            {
                return MainContext.getLogger().logError( String.format( LOAD_FAILED, path, ex.toString() ) );
            }
        }
        else
        {
            return MainContext.getLogger().logError( String.format( LOAD_FAILED, path, NO_EXISTS ) );
        }
    }


    /**
     * Sets configuration from reading a Json-object.
     *
     * @param config a Json-object containing the service configuration
     */
    private static void setConfigFromJson( IJsonObject config )
    {
        // set development tools configuration
        Object devToolsConfig = config.get( DEV_TITLE );
        if (devToolsConfig != null)
        {
            DevelopmentTools devTools = DevelopmentTools.instance();

            // set write-to-disk
            boolean writeToDisk = ((IJsonObject) devToolsConfig).getBoolean( DEV_WRITE_TO_DISK, devTools.isWritingHttpToDisk() );
            devTools.setWriteHttpToDisk( writeToDisk );

            // set read-from-disk
            boolean readFromDisk = ((IJsonObject) devToolsConfig).getBoolean( DEV_READ_FROM_DISK, devTools.isReadingHttpFromDisk() );
            devTools.setReadHttpFromDisk( readFromDisk );

            // set auto-save
            boolean autoSave = ((IJsonObject) devToolsConfig).getBoolean( DEV_AUTO_SAVE, devTools.isAutoSaving() );
            devTools.setAutoSave( autoSave );

            // set auto-submit
            boolean autoSubmit = ((IJsonObject) devToolsConfig).getBoolean( DEV_AUTO_SUBMIT, devTools.isAutoSubmitting() );
            devTools.setAutoSubmit( autoSubmit );
        }

        // set ElasticSearch configuration
        IJsonObject elasticSearchConfig = config.getJsonObject( ELASTIC_SEARCH_TITLE );
        if (elasticSearchConfig != null)
        {
            // set url
            if (!elasticSearchConfig.isNull( ELASTIC_SEARCH_URL )
                    && !elasticSearchConfig.isNull( ELASTIC_SEARCH_INDEX )
                    && !elasticSearchConfig.isNull( ELASTIC_SEARCH_TYPE ))
            {
                ElasticSearchSender.instance().setUrl(
                        elasticSearchConfig.getString( ELASTIC_SEARCH_URL ),
                        elasticSearchConfig.getString( ELASTIC_SEARCH_INDEX ),
                        elasticSearchConfig.getString( ELASTIC_SEARCH_TYPE )
                );
            }
        }

        // set harvester configuration
        IJsonObject harvesterConfig = config.getJsonObject( HARVESTER_TITLE );
        if (harvesterConfig != null)
        {
            AbstractHarvester harvester = MainContext.getHarvester();

            // set range from config
            IJsonObject rangeObject = harvesterConfig.getJsonObject( HARVESTER_RANGE );
            int rangeFrom = rangeObject.getInt( HARVESTER_FROM, harvester.getHarvestStartIndex() );
            int rangeTo = rangeObject.getInt( HARVESTER_TO, harvester.getHarvestEndIndex() );
            harvester.setRange( rangeFrom, rangeTo );

            // set parameters from config
            IJsonObject paramsObject = harvesterConfig.getJsonObject( HARVESTER_PARAMETERS );
            paramsObject.forEach( (Map.Entry<String, Object> e) -> harvester.setProperty( e.getKey(), (String) e.getValue() ) );
        }
    }


    /**
     * Returns the path to the configurationFile of this service.
     *
     * @return the path to the configurationFile of this service
     */
    private static String getConfigFilePath()
    {
        return String.format( CONFIG_PATH, MainContext.getModuleName() );
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

        // create directories
        final File file = new File( path );
        file.getParentFile().mkdirs();

        // write to disk
        try (FileWriter writer = new FileWriter( file ))
        {
            writer.write( toJson().toJsonString() );
            writer.close();

            return MainContext.getLogger().log( String.format( SAVE_OK, path ) );
        }
        catch (IOException e)
        {
            return MainContext.getLogger().logError( SAVE_FAILED + e );
        }
    }


    /**
     * Creates a Json-object that contains all flags and options that have been
     * set up in the service.
     *
     * @return a Json-object that contains the configuration of this service
     */
    private static IJsonObject toJson()
    {
        // get ElasticSearch configuration
        IJsonObject elasticSearchConfig = JSON_BUILDER.createObject();
        elasticSearchConfig.put( ELASTIC_SEARCH_URL, ElasticSearchSender.instance().getBaseUrl() );
        elasticSearchConfig.put( ELASTIC_SEARCH_INDEX, ElasticSearchSender.instance().getIndex() );
        elasticSearchConfig.put( ELASTIC_SEARCH_TYPE, ElasticSearchSender.instance().getType() );

        // get developer options
        IJsonObject devToolsConfig = JSON_BUILDER.createObject();
        devToolsConfig.put( DEV_WRITE_TO_DISK, DevelopmentTools.instance().isWritingHttpToDisk() );
        devToolsConfig.put( DEV_READ_FROM_DISK, DevelopmentTools.instance().isReadingHttpFromDisk() );
        devToolsConfig.put( DEV_AUTO_SAVE, DevelopmentTools.instance().isAutoSaving() );
        devToolsConfig.put( DEV_AUTO_SUBMIT, DevelopmentTools.instance().isAutoSubmitting() );

        AbstractHarvester harvester = MainContext.getHarvester();
        
        // get harvester range
        IJsonObject harvesterRange = JSON_BUILDER.createObject();
        harvesterRange.put( HARVESTER_FROM, harvester.getHarvestStartIndex() );
        harvesterRange.put( HARVESTER_TO, harvester.getHarvestEndIndex() );

        // get implementation specific harvester parameters
        IJsonObject harvesterParams = JSON_BUILDER.createObject();
        List<String> harvesterParamKeys = harvester.getValidProperties();
        harvesterParamKeys.forEach( (key) -> harvesterParams.put( key, harvester.getProperty( key ) ) );

        IJsonObject harvesterConfig = JSON_BUILDER.createObject();
        harvesterConfig.put( HARVESTER_RANGE, harvesterRange );
        harvesterConfig.put( HARVESTER_PARAMETERS, harvesterParams );
        

        IJsonObject globalConfig = JSON_BUILDER.createObject();
		globalConfig.put( HARVESTER_TITLE, harvesterConfig );
		globalConfig.put( ELASTIC_SEARCH_TITLE, elasticSearchConfig );
		globalConfig.put( DEV_TITLE, devToolsConfig );
		
		return globalConfig;
    }
}
