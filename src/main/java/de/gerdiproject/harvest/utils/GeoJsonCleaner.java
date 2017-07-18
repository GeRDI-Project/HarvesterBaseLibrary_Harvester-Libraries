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


import java.io.StringReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esri.core.geometry.ogc.OGCGeometry;

import de.gerdiproject.json.IJsonBuilder;
import de.gerdiproject.json.IJsonObject;
import de.gerdiproject.json.IJsonReader;
import de.gerdiproject.json.impl.JsonBuilder;


/**
 * Before being submitted to ElasticSearch, polygons must abide the ISO
 * 19107:2003 standard. It's possible that polygons are valid GeoJson objects,
 * but cannot be processed by ElasticSearch. For these cases, polygons must be
 * cleaned first.
 *
 * @author Robin Weiss
 *
 */
public class GeoJsonCleaner
{
    public static final String COORDINATES_JSON = "coordinates";
    public static final String TYPE_JSON = "type";
    public static final String MULTI_POLYGON_TYPE = "MultiPolygon";
    public static final String POLYGON_TYPE = "Polygon";
    private static final Logger LOGGER = LoggerFactory.getLogger(GeoJsonCleaner.class);

    private static final Map<String, IJsonObject> CACHED_GEO_MAP = new ConcurrentHashMap<>();

    private final IJsonBuilder jsonBuilder;
    private MessageDigest hashGenerator;


    public GeoJsonCleaner()
    {
        jsonBuilder = new JsonBuilder();

        try {
            hashGenerator = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Did not find algorithm", e);
        }
    }


    private String getGeoHash(IJsonObject geoJson)
    {
        hashGenerator.update(geoJson.toJsonString().getBytes());
        return new String(hashGenerator.digest());
    }


    /**
     * Clears all stored geo objects from the cache.
     */
    public static void clearCache()
    {
        CACHED_GEO_MAP.clear();
    }


    /**
     * Attempts to detect and remove errors in a geoJson object, such as
     * self-intersecting polygons.
     *
     * @param geoJson
     *            the original, possibly erroneous geo json object
     *
     * @return a geo json object that is accepted by Elasticsearch
     */
    public IJsonObject cleanGeoData(IJsonObject geoJson)
    {
        IJsonObject cleanedGeo = geoJson;

        // check if this object has been processed in the past
        String geoJsonHash = getGeoHash(geoJson);
        IJsonObject cachedGeoObject = CACHED_GEO_MAP.getOrDefault(geoJsonHash, null);

        if (cachedGeoObject != null)
            cleanedGeo = cachedGeoObject;

        else {
            String type = geoJson.getString(TYPE_JSON, null);

            switch (type) {
                case POLYGON_TYPE:
                case MULTI_POLYGON_TYPE:

                    try {
                        OGCGeometry polygon = OGCGeometry.fromGeoJson(geoJson.toString());
                        if (!polygon.isSimple()) {
                            IJsonReader reader = jsonBuilder
                                                 .createReader(new StringReader(polygon.makeSimple().asGeoJson()));
                            cleanedGeo = reader.readObject();

                            reader.close();
                        }
                    } catch (Exception e) {
                        cleanedGeo = geoJson;
                    }
            }
        }

        // cache the result
        CACHED_GEO_MAP.put(geoJsonHash, cleanedGeo);

        return cleanedGeo;

    }
}