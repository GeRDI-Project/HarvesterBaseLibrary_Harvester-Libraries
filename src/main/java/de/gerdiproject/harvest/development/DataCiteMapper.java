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
    private static final String CONVERSION_START = "+++ Starting Conversion to DataCite +++";
    private static final String CONVERSION_START_FAILED = "Could not start DataCite conversion. Finish harvesting first!";

    private static final String STATUS_CONVERTING = "Converted %d / %d (%.2f%%)";

    private static final String CONVERSION_PROGRESS_NAME = "DataCite Conversion";
    private static final String IDENTIFIER_JSON = "identifier";
    private static final String PUBLISHER_JSON = "publisher";
    private static final String AFFILIATION_JSON = "affiliation";
    private static final String NAME_JSON = "name";
    private static final String CREATORS_JSON = "creators";
    private static final String TITLE_JSON = "title";
    private static final String TITLES_JSON = "titles";
    private static final String DESCRIPTION_JSON = "description";
    private static final String DESCRIPTIONS_JSON = "descriptions";
    private static final String PUBLICATION_YEAR_JSON = "publicationYear";
    private static final String RESOURCE_TYPE_JSON = "resourceType";
    private static final String GENERAL_JSON = "general";
    private static final String SUBJECT_JSON = "subject";
    private static final String SUBJECTS_JSON = "subjects";
    private static final String FROM_JSON = "from";
    private static final String TO_JSON = "to";
    private static final String DATE_JSON = "date";
    private static final String DATES_JSON = "dates";
    private static final String EXACT_JSON = "exact";
    private static final String TYPE_JSON = "type";
    private static final String PLACE_JSON = "place";
    private static final String POINT_JSON = "point";
    private static final String POLYGON_JSON = "polygon";
    private static final String GEO_LOCATIONS_JSON = "geoLocations";
    private static final String URL_JSON = "url";
    private static final String WEBLINKS_JSON = "weblinks";
    private static final String FILES_JSON = "files";
    private static final String SOURCES_JSON = "sources";
    private static final String URI_JSON = "URI";
    private static final String PROVIDER_JSON = "provider";
    private static final String PROVIDER_URI_JSON = "providerURI";
    private static final String LABEL_JSON = "label";

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
    public static synchronized DataCiteMapper instance()
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
        if (isConverting() || !harvester.isFinished() || originalDocs == null || originalDocs.isEmpty()) {
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

            LOGGER.info(HarvesterStringUtils.formatProgress(CONVERSION_PROGRESS_NAME, ++convertedCount, docCount));
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
        result.put(IDENTIFIER_JSON, encoder.encodeToString(viewUrl.getBytes(MainContext.getCharset())));

        // publisher
        String publisher = MainContext.getModuleName()
                           .substring(0, MainContext.getModuleName().indexOf("arvester") - 1);
        result.put(PUBLISHER_JSON, publisher);

        // creators
        {

            IJsonObject creator = jsonBuilder.createObject();
            creator.put(AFFILIATION_JSON, publisher);
            creator.put(NAME_JSON, "Peter Pansen");
            result.put(CREATORS_JSON, jsonBuilder.createArrayFromObjects(creator));
        }

        // titles
        {
            IJsonObject title = jsonBuilder.createObject();
            title.put(TITLE_JSON, label);
            result.put(TITLES_JSON, jsonBuilder.createArrayFromObjects(title));
        }

        // descriptions
        if (descriptions != null) {
            IJsonArray descriptionInfos = jsonBuilder.createArray();
            descriptions.forEach((Object o) -> {
                IJsonObject description = jsonBuilder.createObject();
                description.put(DESCRIPTION_JSON, (String) o);
                descriptionInfos.add(description);
            });
            result.put(DESCRIPTIONS_JSON, descriptionInfos);

        } else {
            IJsonObject description = jsonBuilder.createObject();
            description.put(DESCRIPTION_JSON, "I wanted a real description, but all I got was this stupid text...");
            result.put(DESCRIPTIONS_JSON, jsonBuilder.createArrayFromObjects(description));
        }

        // publication year
        result.put(PUBLICATION_YEAR_JSON, 2017);

        // resource type
        {
            IJsonObject resourceType = jsonBuilder.createObject();
            resourceType.put(RESOURCE_TYPE_JSON, publisher + " Data");
            resourceType.put(GENERAL_JSON, "Dataset");
            result.put(RESOURCE_TYPE_JSON, resourceType);
        }

        // subjects
        if (tags != null) {
            IJsonArray subjects = jsonBuilder.createArray();
            tags.forEach((Object tag) -> {
                IJsonObject subject = jsonBuilder.createObject();
                subject.put(SUBJECT_JSON, (String) tag);
                subjects.add(subject);
            });
            result.put(SUBJECTS_JSON, subjects);
        }

        // dates
        if (years != null || lastUpdated != null) {
            IJsonArray dates = jsonBuilder.createArray();

            if (years != null) {
                int minYear = Integer.MAX_VALUE;
                int maxYear = Integer.MIN_VALUE;

                for (Object year : years) {
                    int y = (Integer) year;
                    minYear = y < minYear ? y : minYear;
                    maxYear = y > maxYear ? y : maxYear;
                }

                IJsonObject date = jsonBuilder.createObject();
                date.put(FROM_JSON, minYear + "-01-01");
                date.put(TO_JSON, maxYear + "-12-31");
                IJsonObject dateInfo = jsonBuilder.createObject();
                dateInfo.put(TYPE_JSON, "Collected");
                dateInfo.put(DATE_JSON, date);
                dates.add(dateInfo);
            }

            if (lastUpdated != null) {
                IJsonObject date = jsonBuilder.createObject();
                date.put(EXACT_JSON, lastUpdated);
                IJsonObject dateInfo = jsonBuilder.createObject();
                dateInfo.put(TYPE_JSON, "Updated");
                dateInfo.put(DATE_JSON, date);
                dates.add(dateInfo);
            }

            result.put(DATES_JSON, dates);
        }

        // geo locations
        if (geo != null) {
            IJsonArray geoLocations = jsonBuilder.createArray();
            geo.forEach((Object geoShape) -> {
                IJsonObject geoEntry = jsonBuilder.createObject();
                geoEntry.put(PLACE_JSON, "This region encompasses: " + label);

                if (((IJsonObject) geoShape).getString("type").equals("Point"))
                    geoEntry.put(POINT_JSON, geoShape);
                else
                    geoEntry.put(POLYGON_JSON, geoShape);

                geoLocations.add(geoEntry);
            });
            result.put(GEO_LOCATIONS_JSON, geoLocations);
        }

        // web links
        {
            IJsonObject webLink = jsonBuilder.createObject();
            webLink.put(NAME_JSON, "View: " + label);
            webLink.put(TYPE_JSON, "ViewUrl");
            webLink.put(URL_JSON, viewUrl);
            result.put(WEBLINKS_JSON, jsonBuilder.createArrayFromObjects(webLink));
        }

        // sources
        {
            String providerUrl = viewUrl.startsWith("http")
                                 ? viewUrl.substring(0, viewUrl.indexOf('/', 9))
                                 : viewUrl.substring(0, viewUrl.indexOf('/'));

            IJsonObject dataSource = jsonBuilder.createObject();
            dataSource.put(PROVIDER_URI_JSON, providerUrl);
            dataSource.put(PROVIDER_JSON, publisher);
            dataSource.put(URI_JSON, viewUrl);
            result.put(SOURCES_JSON, jsonBuilder.createArrayFromObjects(dataSource));
        }

        // files
        if (downloadUrls != null) {
            IJsonArray files = jsonBuilder.createArray();
            int i = 0;

            for (Object dlUrl : downloadUrls) {
                IJsonObject file = jsonBuilder.createObject();
                file.put(IDENTIFIER_JSON, "file " + i++);
                file.put(LABEL_JSON, publisher + " Dataset #" + i);
                file.put(URL_JSON, (String) dlUrl);

                files.add(file);
            }

            result.put(FILES_JSON, files);
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
