/*
 *  Copyright © 2018 Robin Weiss (http://www.gerdi-project.de/)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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
package de.gerdiproject.harvest.harvester.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.ParameterizedType;
import java.nio.charset.Charset;
import java.util.Iterator;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

/**
 * This {@linkplain Iterator} iterates through a JSON array that was saved to a file on disk.
 *
 * @author Robin Weiss
 */
public class CacheIterator<OUT> implements Iterator<OUT>
{
    // this warning is suppressed, because the only generic Superclass MUST be T. The cast will always succeed.
    @SuppressWarnings("unchecked")
    private Class<OUT> outputClass =
        (Class<OUT>)((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];

    private final JsonReader reader;
    private final Gson gson;


    /**
     * Constructor that opens up a JSON reader on a file that should contain a JSON array.
     *
     * @param cacheFile a file containing a JSON array
     * @param charset the charset used for parsing the file
     * @param gson the {@linkplain Gson} object used for converting the JSON object to a Java Object
     *
     * @throws IOException thrown when there is a problem reading the file
     */
    public CacheIterator(File cacheFile, Charset charset, Gson gson) throws IOException
    {
        this.gson = gson;
        this.reader = new JsonReader(new InputStreamReader(new FileInputStream(cacheFile), charset));
        reader.beginArray();
    }


    @Override
    public boolean hasNext()
    {
        try {
            return reader.hasNext();
        } catch (IOException e) {
            return false;
        }
    }


    @Override
    public OUT next()
    {
        return gson.fromJson(reader, outputClass);
    }
}
