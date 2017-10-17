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
package de.gerdiproject.harvest.utils.data;

import org.jsoup.nodes.Document;

import com.google.gson.JsonElement;

/**
 * This interface describes a means for retrieving data from an arbitrary source.
 *
 * @author Robin Weiss
 */
public interface IDataRetriever
{
    /**
     * Tries to parse the content from a specified path as a string.
     *
     * @param path
     *            the path to a HTML file
     * @return a text, or null if the file could not be parsed
     */
    String getString(String path);

    /**
     * Tries to parse the content from a specified path as a
     * JSON object or array.
     *
     * @param path the path to a JSON file
     * @return a JSON object or array, or null if the file could not be parsed
     */
    JsonElement getJson(String path);

    /**
     * Tries to parse the content from a specified path as a
     * JSON object.
     *
     * @param path the path to a JSON file
     * @param targetClass the class of the object that is read from disc
     * @param <T> the type of the object that is to be read
     *
     * @return an object, or null if the file could not be parsed
     */
    <T> T getObject(String path, Class<T> targetClass);

    /**
     * Tries to parse the content from a specified path as a
     * HTML document.
     *
     * @param path the path to a HTML file
     * @return a HTML document, or null if the file could not be parsed
     */
    Document getHtml(String path);
}
