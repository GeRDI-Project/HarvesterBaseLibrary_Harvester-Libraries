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
package de.gerdiproject.harvest.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.stream.JsonWriter;

import de.gerdiproject.harvest.IDocument;
import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.impl.DocumentHarvestedEvent;
import de.gerdiproject.harvest.event.impl.HarvestFinishedEvent;
import de.gerdiproject.harvest.event.impl.HarvestStartedEvent;
import de.gerdiproject.harvest.event.impl.StartSubmissionEvent;
import de.gerdiproject.harvest.harvester.AbstractHarvester;
import de.gerdiproject.harvest.utils.data.DiskIO;
import de.gerdiproject.json.GsonUtils;
import de.gerdiproject.json.SearchIndexJson;

public class SearchIndexWriter
{
    private static final String FILE_NAME = "harvestedIndices/%s_result_%d.json";
    private static final String FILE_NAME_PARTIAL = "harvestedIndices/%s_partialResult_%d-%d_%d.json";
    private static final String NO_HARVEST = "Cannot save: Nothing was harvested yet!";
    private static final String FILE_PATH = "cachedIndex/%s/%s.json";

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchIndexWriter.class);

    private long startTimeStamp;
    private long finishTimeStamp;
    private int documentCount;
    private JsonWriter writer;
    private File outputFile;


    private Consumer<HarvestStartedEvent> onHarvestStarted = (HarvestStartedEvent e) -> {
        startTimeStamp = new Date().getTime();
        clear();
        startCaching();
    };

    private Consumer<HarvestFinishedEvent> onHarvestFinished = (HarvestFinishedEvent e) -> {
        finishTimeStamp = new Date().getTime();
        finishCaching();
    };

    private Consumer<DocumentHarvestedEvent> onDocumentHarvested = (DocumentHarvestedEvent e) -> {
        addDocument(e.getDocument());
    };


    public SearchIndexWriter(String harvesterName)
    {
        documentCount = 0;
        outputFile = new File(String.format(FILE_PATH, MainContext.getModuleName(), harvesterName));

        EventSystem.addListener(DocumentHarvestedEvent.class, onDocumentHarvested);
        EventSystem.addListener(HarvestStartedEvent.class, onHarvestStarted);
        EventSystem.addListener(HarvestFinishedEvent.class, onHarvestFinished);
    }

    public void clear()
    {
        if (documentCount > 0)
            finishCaching();

        documentCount = 0;
        writer = null;
    }

    public boolean startCaching()
    {
        documentCount = 0;

        // create directories
        boolean isDirectoryCreated = outputFile.getParentFile().exists() || outputFile.getParentFile().mkdirs();

        if (isDirectoryCreated) {
            try {
                writer = new JsonWriter(
                    new OutputStreamWriter(
                        new FileOutputStream(outputFile),
                        MainContext.getCharset()));
                writer.beginArray();

                return true;
            } catch (IOException e1) {
                // TODO
            }
        }

        return false;
    }


    public void finishCaching()
    {
        try {
            if (writer != null) {
                writer.endArray();
                writer.close();
            }
        } catch (IOException e) {
            // TODO
        }
    }


    public void addDocument(IDocument doc)
    {
        boolean isInitialized = true;

        if (documentCount == 0)
            isInitialized = startCaching();

        if (isInitialized) {
            GsonUtils.getGson().toJson(doc, doc.getClass(), writer);
            documentCount++;
        }
    }

    public File getOutputFile()
    {
        return outputFile;
    }


    /**
     * Saves the harvested result to disk and logs if the operation was
     * successful or not.
     *
     * @return A status message describing whether the save succeeded or not
     */
    public String saveDocumentsToDisk()
    {
        Configuration config = MainContext.getConfiguration();

        SearchIndexJson result = harvester.createDetailedJson();

        if (result != null) {
            String fileName;

            if (documentCount < harvester.getMaxNumberOfDocuments()) {
                int from = config.getParameterValue(ConfigurationConstants.HARVEST_START_INDEX, Integer.class);
                int to = config.getParameterValue(ConfigurationConstants.HARVEST_END_INDEX, Integer.class);

                fileName = String.format(
                               FILE_NAME_PARTIAL,
                               MainContext.getModuleName(),
                               from,
                               to,
                               startTimeStamp);
            } else {
                fileName = String.format(
                               FILE_NAME,
                               MainContext.getModuleName(),
                               startTimeStamp);
            }

            DiskIO diskWriter = new DiskIO();
            return diskWriter.writeStringToFile(fileName, GsonUtils.getGson().toJson(result));

        } else {
            LOGGER.warn(NO_HARVEST);
            return NO_HARVEST;
        }
    }


    public String submitDocuments()
    {
        AbstractHarvester harvester = MainContext.getHarvester();

        // harvest the data, if it has not been done yet
        if (!harvester.isFinished() && !harvester.isHarvesting())
            harvester.harvest();

        // retrieve harvested search index
        // TODO List<IDocument> harvestedDocuments = harvester.getHarvestedDocuments();

        // send search index to Elastic Search
        // TODO String status = ElasticSearchSender.instance().sendToElasticSearch(harvestedDocuments);

        // TODO return status;
        return null;
    }
}
