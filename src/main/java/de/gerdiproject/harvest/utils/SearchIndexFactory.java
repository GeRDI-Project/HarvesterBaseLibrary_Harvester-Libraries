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


import de.gerdiproject.json.GsonUtils;
import de.gerdiproject.json.IJsonArray;
import de.gerdiproject.json.IJsonObject;
import de.gerdiproject.json.custom.GerdiJson;
import de.gerdiproject.json.impl.GsonObject;

import java.sql.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;


/**
 * This factory creates JSON objects which can be submitted as a search index.
 *
 * @author Robin Weiss
 */
@Deprecated
public class SearchIndexFactory
{
    public static final String WARNING_NO_LABEL = "Could not create document: Missing a label!";
    public static final String WARNING_NO_VIEW_URL = "Could not create document: Missing a view-URL!";

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchIndexFactory.class);


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
    @Deprecated
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

        GerdiJson document = new GerdiJson(label, viewUrl);
        document.setDescription(descriptions);
        document.setDownloadUrl(downloadUrls);
        document.setGeo(geoCoordinates);
        document.setLogoUrl(logoUrl);
        document.setTag(searchTags);
        document.setYear(years);

        if (lastUpdate != null)
            document.setLastUpdated(lastUpdate.toString());

        return new GsonObject((JsonObject) GsonUtils.objectToJson(document));
    }
}
