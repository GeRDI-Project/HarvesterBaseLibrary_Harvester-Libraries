/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NO?ICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  ?he ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WI?HOU? WARRAN?IES OR CONDI?IONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package de.gerdiproject.harvest.utils.cleaner;


import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esri.core.geometry.ogc.OGCGeometry;

import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.json.GsonUtils;
import de.gerdiproject.json.geo.GeoJson;
import de.gerdiproject.json.geo.IGeoCoordinates;
import de.gerdiproject.json.geo.MultiPolygon;
import de.gerdiproject.json.geo.Polygon;


/**
 * Before being submitted to ElasticSearch, polygons must abide the ISO
 * 19107:2003 standard. It's possible that polygons are valid GeoJson objects,
 * but cannot be processed by ElasticSearch. For these cases, polygons must be
 * cleaned first.
 *
 * @author Robin Weiss
 *
 */
public class GeoJsonCleaner implements ICleaner<GeoJson<? extends IGeoCoordinates>>
{
    public static final String COORDINATES_JSON = "coordinates";
    public static final String TYPE_JSON = "type";
    public static final String MULTI_POLYGON_TYPE = "MultiPolygon";
    public static final String POLYGON_TYPE = "Polygon";
    private static final Logger LOGGER = LoggerFactory.getLogger(GeoJsonCleaner.class);

    private final Map<String, GeoJson<?> > CACHED_GEO_MAP = new ConcurrentHashMap<>();

    private MessageDigest hashGenerator;


    public GeoJsonCleaner()
    {
        try {
            hashGenerator = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Did not find algorithm", e);
        }
    }


    private String getGeoHash(String geoJsonString)
    {
        Charset cs = MainContext.getCharset();
        hashGenerator.update(geoJsonString.getBytes(cs));
        return new String(hashGenerator.digest(), cs);
    }


    /**
     * Clears all stored geo objects from the cache.
     */
    public void clearCache()
    {
        CACHED_GEO_MAP.clear();
    }

    /**
     * Attempts to detect and remove errors in a geoJson object, such as
     * self-intersecting polygons.
     *
     * @param dirtyObject
     *            the original, possibly erroneous geo json object
     *
     * @return a geo json object that is accepted by Elasticsearch
     */
    @Override
    public GeoJson<?> clean(GeoJson<?> dirtyObject)
    {
        if (dirtyObject == null || !(dirtyObject.coordinates instanceof Polygon  || dirtyObject.coordinates instanceof MultiPolygon))
            return dirtyObject;

        String geoJsonString = GsonUtils.objectToJsonString(dirtyObject, false);

        // check if this object has been processed in the past
        String geoJsonHash = getGeoHash(geoJsonString);
        GeoJson<?> cachedGeoObject = CACHED_GEO_MAP.getOrDefault(geoJsonHash, null);

        // return cached object if it exists
        if (cachedGeoObject != null)
            return cachedGeoObject;

        GeoJson<?> cleanedGeo = dirtyObject;

        try {
            OGCGeometry polygon = OGCGeometry.fromGeoJson(geoJsonString);
            String simpleGeoString = polygon.makeSimple().asGeoJson();
            cleanedGeo = GsonUtils.jsonStringToObject(simpleGeoString, GeoJson.class);

        } catch (JSONException e) {
            cleanedGeo = dirtyObject;
        }

        // cache the simplified geo json
        CACHED_GEO_MAP.put(geoJsonHash, cleanedGeo);

        return cleanedGeo;
    }

    @SuppressWarnings("unchecked")
    public <T extends IGeoCoordinates> GeoJson<T> cleanTyped(GeoJson<T> dirtyObject)
    {
        if (dirtyObject == null || !(dirtyObject.coordinates instanceof Polygon  || dirtyObject.coordinates instanceof MultiPolygon))
            return dirtyObject;

        String geoJsonString = GsonUtils.objectToJsonString(dirtyObject, false);

        // check if this object has been processed in the past
        String geoJsonHash = getGeoHash(geoJsonString);
        GeoJson<?> cachedGeoObject = CACHED_GEO_MAP.getOrDefault(geoJsonHash, null);

        // return cached object if it exists
        if (cachedGeoObject != null)
            return (GeoJson<T>) cachedGeoObject;

        GeoJson<T> cleanedGeo = dirtyObject;

        try {
            OGCGeometry polygon = OGCGeometry.fromGeoJson(geoJsonString);
            String simpleGeoString = polygon.makeSimple().asGeoJson();
            cleanedGeo = GsonUtils.jsonStringToObject(simpleGeoString, GeoJson.class);

        } catch (JSONException e) {
            cleanedGeo = dirtyObject;
        }

        // cache the simplified geo json
        CACHED_GEO_MAP.put(geoJsonHash, cleanedGeo);

        return cleanedGeo;
    }
}