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


import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.harvester.AbstractHarvester;
import de.gerdiproject.harvest.utils.CancelableFuture;
import de.gerdiproject.harvest.utils.HarvesterStringUtils;
import de.gerdiproject.harvest.utils.SearchIndexFactory;
import de.gerdiproject.json.IJsonArray;
import de.gerdiproject.json.IJsonBuilder;
import de.gerdiproject.json.IJsonObject;
import de.gerdiproject.json.impl.JsonBuilder;


/**
 * This class provides methods for converting harvested documents to the
 * DataCite schema.
 *
 * @author Robin Weiss
 *
 */
public class DataCiteMapper
{
    private static final String CONVERSION_FAILED = "!!! CONVERTING TO DATACITE FAILED !!! ";
    private static final String CONVERSION_SUCCESS = "SUCCESSFULLY CONVERTED DOCUMENTS TO DATACITE";
    private final static String CONVERSION_START = "+++ Starting Conversion to DataCite +++";
    private final static String CONVERSION_START_FAILED = "Could not start DataCite conversion. Finish harvesting first!";

    private final static String STATUS_CONVERTING = "Converted %d / %d (%.2f%%)";

    private static final Logger LOGGER = LoggerFactory.getLogger(DataCiteMapper.class);

    private static DataCiteMapper instance;

    private final Base64.Encoder encoder;
    private final IJsonArray convertedDocuments;
    private final IJsonBuilder jsonBuilder;
    private CancelableFuture<Boolean> conversionProcess;


    /**
     * Private constructor used by the Singleton instance
     */
    private DataCiteMapper()
    {
        jsonBuilder = new JsonBuilder();
        convertedDocuments = jsonBuilder.createArray();
        encoder = Base64.getEncoder();
    }


    /**
     * Returns the Singleton instance of this class.
     *
     * @return a Singleton instance of this class
     */
    public static DataCiteMapper instance()
    {
        if (instance == null)
            instance = new DataCiteMapper();

        return instance;
    }


    public String startConversion()
    {
        AbstractHarvester harvester = MainContext.getHarvester();
        IJsonArray originalDocs = harvester.getHarvestedDocuments();

        // check if the conversion is allowed to start
        if (isConverting() || !harvester.isHarvestFinished() || originalDocs == null || originalDocs.isEmpty()) {
            LOGGER.info(CONVERSION_START_FAILED);
            return CONVERSION_START_FAILED;
        }

        // start asynchronous conversion process
        conversionProcess = new CancelableFuture<>(
            () -> convertDocumentsToDataCite(originalDocs));

        // add success and exception listeners
        conversionProcess.thenApply((isSuccessful) -> {
            endConversion();
            return isSuccessful;
        })
        .exceptionally(throwable -> {
            failConversion(throwable.getCause());
            return false;
        });

        LOGGER.info(CONVERSION_START);
        return CONVERSION_START;
    }


    private void endConversion()
    {
        conversionProcess = null;
        LOGGER.info(CONVERSION_SUCCESS);
    }


    private void failConversion(Throwable reason)
    {
        // log stack trace
        if (reason != null) {
            final StringBuilder errorBuilder = new StringBuilder(reason.toString());
            StackTraceElement[] stackTrace = reason.getStackTrace();

            for (StackTraceElement ele : stackTrace)
                errorBuilder.append('\n').append(ele);

            LOGGER.error(errorBuilder.toString());
        }

        conversionProcess = null;

        LOGGER.error(CONVERSION_FAILED);
    }


    private boolean convertDocumentsToDataCite(IJsonArray documents)
    {
        IJsonBuilder builder = new JsonBuilder();

        synchronized (convertedDocuments) {
            convertedDocuments.clear();
        }

        final int docCount = documents.size();
        int convertedCount = 0;

        for (Object o : documents) {
            IJsonObject convertedDoc = convertSingleDocument(o, builder);

            synchronized (convertedDocuments) {
                convertedDocuments.add(convertedDoc);
            }

            LOGGER.info(HarvesterStringUtils.formatProgress("DataCite Conversion", ++convertedCount, docCount));
        }

        return true;
    }


    private IJsonObject convertSingleDocument(Object doc, IJsonBuilder builder)
    {
        // original values
        IJsonObject source = (IJsonObject) doc;
        String label = source.getString(SearchIndexFactory.LABEL_JSON);
        String lastUpdated = source.getString(SearchIndexFactory.LAST_UPDATED_JSON);
        String viewUrl = source.getString(SearchIndexFactory.VIEW_URL_JSON);
        IJsonArray downloadUrls = source.getJsonArray(SearchIndexFactory.DOWNLOAD_URL_JSON);
        IJsonArray descriptions = source.getJsonArray(SearchIndexFactory.DESCRIPTION_JSON);
        IJsonArray tags = source.getJsonArray(SearchIndexFactory.TAGS_JSON);
        IJsonArray geo = source.getJsonArray(SearchIndexFactory.GEO_JSON);
        IJsonArray years = source.getJsonArray(SearchIndexFactory.YEARS_JSON);

        IJsonObject result = builder.createObject();

        // identifier
        result.put("identifier", encoder.encodeToString(viewUrl.getBytes()));

        // publisher
        String publisher = MainContext.getModuleName()
                           .substring(0, MainContext.getModuleName().indexOf("arvester") - 1);
        result.put("publisher", publisher);

        // creators
        {

            IJsonObject creator = jsonBuilder.createObject();
            creator.put("affiliation", publisher);
            creator.put("name", "Peter Pansen");
            result.put("creators", jsonBuilder.createArrayFromObjects(creator));
        }

        // titles
        {
            IJsonObject title = jsonBuilder.createObject();
            title.put("title", label);
            result.put("titles", jsonBuilder.createArrayFromObjects(title));
        }

        // descriptions
        if (descriptions != null) {
            IJsonArray descriptionInfos = jsonBuilder.createArray();
            descriptions.forEach((Object o) -> {
                IJsonObject description = jsonBuilder.createObject();
                description.put("description", (String) o);
                descriptionInfos.add(description);
            });
            result.put("descriptions", descriptionInfos);

        } else {
            IJsonObject description = jsonBuilder.createObject();
            description.put("description", "I wanted a real description, but all I got was this stupid text...");
            result.put("descriptions", jsonBuilder.createArrayFromObjects(description));
        }

        // publication year
        result.put("publicationYear", 2017);

        // resource type
        {
            IJsonObject resourceType = jsonBuilder.createObject();
            resourceType.put("resourceType", publisher + " Data");
            resourceType.put("general", "Dataset");
            result.put("resourceType", resourceType);
        }

        // subjects
        if (tags != null) {
            IJsonArray subjects = jsonBuilder.createArray();
            tags.forEach((Object tag) -> {
                IJsonObject subject = jsonBuilder.createObject();
                subject.put("subject", (String) tag);
                subjects.add(subject);
            });
            result.put("subjects", subjects);
        }

        // dates
        if (years != null || lastUpdated != null) {
            IJsonArray dates = jsonBuilder.createArray();

            if (years != null) {
                int minYear = Integer.MAX_VALUE;
                int maxYear = Integer.MIN_VALUE;

                for (Object year : years) {
                    int y = (Integer) year;
                    minYear = (y < minYear) ? y : minYear;
                    maxYear = (y > maxYear) ? y : maxYear;
                }

                IJsonObject date = jsonBuilder.createObject();
                date.put("from", minYear + "-01-01");
                date.put("to", maxYear + "-12-31");
                IJsonObject dateInfo = jsonBuilder.createObject();
                dateInfo.put("type", "Collected");
                dateInfo.put("date", date);
                dates.add(dateInfo);
            }

            if (lastUpdated != null) {
                IJsonObject date = jsonBuilder.createObject();
                date.put("exact", lastUpdated);
                IJsonObject dateInfo = jsonBuilder.createObject();
                dateInfo.put("type", "Updated");
                dateInfo.put("date", date);
                dates.add(dateInfo);
            }

            result.put("dates", dates);
        }

        // geo locations
        if (geo != null) {
            IJsonArray geoLocations = jsonBuilder.createArray();
            geo.forEach((Object geoShape) -> {
                IJsonObject geoEntry = jsonBuilder.createObject();
                geoEntry.put("place", "This region encompasses: " + label);

                if (((IJsonObject) geoShape).getString("type").equals("Point"))
                    geoEntry.put("point", geoShape);
                else
                    geoEntry.put("polygon", geoShape);

                geoLocations.add(geoEntry);
            });
            result.put("geoLocations", geoLocations);
        }

        // web links
        {
            IJsonObject webLink = jsonBuilder.createObject();
            webLink.put("name", "View: " + label);
            webLink.put("type", "ViewUrl");
            webLink.put("url", viewUrl);
            result.put("weblinks", jsonBuilder.createArrayFromObjects(webLink));
        }

        // sources
        {
            String providerUrl = (viewUrl.startsWith("http"))
                                 ? viewUrl.substring(0, viewUrl.indexOf('/', 9))
                                 : viewUrl.substring(0, viewUrl.indexOf('/'));

            IJsonObject dataSource = jsonBuilder.createObject();
            dataSource.put("providerURI", providerUrl);
            dataSource.put("provider", publisher);
            dataSource.put("URI", viewUrl);
            result.put("sources", jsonBuilder.createArrayFromObjects(dataSource));
        }

        // files
        if (downloadUrls != null) {
            IJsonArray files = jsonBuilder.createArray();
            int i = 0;

            for (Object dlUrl : downloadUrls) {
                IJsonObject file = jsonBuilder.createObject();
                file.put("identifier", "file " + i++);
                file.put("label", publisher + " Dataset #" + i);
                file.put("url", (String) dlUrl);

                files.add(file);
            }

            result.put("files", files);
        }

        return result;
    }


    public String getProgressString()
    {
        int conversionCount;

        synchronized (convertedDocuments) {
            conversionCount = convertedDocuments.size();
        }

        int documentCount = MainContext.getHarvester().getHarvestedDocuments().size();

        double percentage = 100.0 * conversionCount / documentCount;

        return String.format(STATUS_CONVERTING, conversionCount, documentCount, percentage);
    }


    public boolean isFinished()
    {
        synchronized (convertedDocuments) {
            return convertedDocuments != null && !convertedDocuments.isEmpty() && conversionProcess == null;
        }
    }


    public boolean isConverting()
    {
        return conversionProcess != null;
    }


    public IJsonArray getConvertedDocuments()
    {
        return convertedDocuments;
    }
}
