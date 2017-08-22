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


import de.gerdiproject.harvest.development.DevelopmentTools;
import de.gerdiproject.harvest.utils.cleaner.GeoJsonCleaner;
import de.gerdiproject.json.IJsonArray;
import de.gerdiproject.json.IJsonBuilder;
import de.gerdiproject.json.IJsonObject;
import de.gerdiproject.json.impl.JsonBuilder;

import java.sql.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This factory creates JSON objects which can be submitted as a search index.
 *
 * @author Robin Weiss
 */
public class SearchIndexFactory
{
    public static final String WARNING_NO_LABEL = "Could not create document: Missing a label!";
    public static final String WARNING_NO_VIEW_URL = "Could not create document: Missing a view-URL!";

    public static final String LABEL_JSON = "label";
    public static final String LAST_UPDATED_JSON = "lastUpdated";
    public static final String VIEW_URL_JSON = "viewUrl";
    public static final String DOWNLOAD_URL_JSON = "downloadUrl";
    public static final String LOGO_URL_JSON = "logoUrl";
    public static final String DESCRIPTION_JSON = "description";
    public static final String TAGS_JSON = "tag";
    public static final String GEO_JSON = "geo";
    public static final String YEARS_JSON = "year";

    public static final String DATA_JSON = "data";
    public static final String HASH_JSON = "hash";

    public static final String DURATION_JSON = "durationInSeconds";
    public static final String IS_FROM_DISK_JSON = "wasHarvestedFromDisk";

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchIndexFactory.class);

    private final GeoJsonCleaner geoCleaner;
    private final IJsonBuilder jsonBuilder;


    /**
     * Initializes a geo object json cleaner.
     */
    public SearchIndexFactory()
    {
        geoCleaner = new GeoJsonCleaner();
        jsonBuilder = new JsonBuilder();
    }


    /**
     * Creates a JSON object that represents a searchable document within a
     * search index.
     *
     * @param label
     *            the title or display name of the document
     * @param lastUpdate
     *            the date when the document was last updated
     * @param viewUrl
     *            a URL that points to the official website that hosts the
     *            document
     * @param downloadUrls
     *            URLs that point to downloads of the (whole) document as files
     * @param logoUrl
     *            a URL that points to a logo of the document
     * @param descriptions
     *            continuous descriptive texts of the document
     * @param geoCoordinates
     *            a json array containing shape or point information that
     *            describes lat long coordinates
     * @param years
     *            a json array of years that are contained in the document
     * @param searchTags
     *            a json array of searchable terms or tags of the document
     * @return a JSON object that describes the searchable document
     */
    public IJsonObject createSearchableDocument(String label, Date lastUpdate, String viewUrl,
                                                IJsonArray downloadUrls, String logoUrl, IJsonArray descriptions, IJsonArray geoCoordinates,
                                                IJsonArray years,
                                                IJsonArray searchTags)
    {
        // label is required!
        if (label == null || label.isEmpty()) {
            LOGGER.warn(WARNING_NO_LABEL);
            return null;
        }

        // viewUrl is required for building a unique hash!
        if (viewUrl == null || viewUrl.isEmpty()) {
            LOGGER.warn(WARNING_NO_VIEW_URL);
            return null;
        }

        // init builder
        IJsonObject document = jsonBuilder.createObject();

        // add mandatory fields
        document.put(LABEL_JSON, HarvesterStringUtils.cleanString(label));
        document.put(VIEW_URL_JSON, HarvesterStringUtils.cleanString(viewUrl));

        // add optional fields
        if (downloadUrls != null && !downloadUrls.isEmpty())
            document.put(DOWNLOAD_URL_JSON, downloadUrls);

        if (logoUrl != null && !logoUrl.isEmpty())
            document.put(LOGO_URL_JSON, HarvesterStringUtils.cleanString(logoUrl));

        if (descriptions != null && !descriptions.isEmpty()) {
            // clean descriptions
            for (int i = 0, len = descriptions.size(); i < len; i++) {
                String cleanDesc = HarvesterStringUtils.cleanString(descriptions.getString(i));
                descriptions.put(i, cleanDesc);
            }

            document.put(DESCRIPTION_JSON, descriptions);
        }

        if (lastUpdate != null)
            document.put(LAST_UPDATED_JSON, lastUpdate.toString());


        if (geoCoordinates != null && !geoCoordinates.isEmpty()) {
            // correct possibly erroneous polygons
            for (int i = 0, len = geoCoordinates.size(); i < len; i++) {
                //IJsonObject cleanGeo =  geoCleaner.clean(geoCoordinates.getJsonObject(i));
                //geoCoordinates.put(i, cleanGeo);
            }

            document.put(GEO_JSON, geoCoordinates);
        }

        if (years != null && !years.isEmpty())
            document.put(YEARS_JSON, years);

        if (searchTags != null && !searchTags.isEmpty())
            document.put(TAGS_JSON, searchTags);

        // build and return json object
        return document;
    }


    /**
     * Creates a search index out of a list of searchable documents. The search
     * index also contains a date of the harvest.
     *
     * @param documents
     *            an array of searchable documents
     * @param harvestDate
     *            the date when the harvest started
     * @param hash
     *            a checksum of the entries before being harvested
     * @param duration
     *            the harvesting duration in seconds
     * @return a JSON object that contains an array of searchable documents and
     *         the latest update date
     */
    public IJsonObject createSearchIndex(IJsonArray documents, Date harvestDate, String hash, int duration)
    {
        IJsonObject searchIndex = jsonBuilder.createObject();
        searchIndex.put(HASH_JSON, hash);
        searchIndex.put(IS_FROM_DISK_JSON, DevelopmentTools.instance().isReadingHttpFromDisk());
        searchIndex.put(DURATION_JSON, duration);
        searchIndex.put(LAST_UPDATED_JSON, harvestDate.toString());
        searchIndex.put(DATA_JSON, documents);

        return searchIndex;
    }
}
