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

import com.google.gson.stream.JsonWriter;

import de.gerdiproject.harvest.IDocument;
import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.json.GsonUtils;
import de.gerdiproject.json.SearchIndexJson;

public class SearchIndexWriter
{
    private static final String FILE_PATH = "cachedIndex/%s/%s.json";
    private int documentCount;
    private JsonWriter writer;
    private File outputFile;


    public SearchIndexWriter(String harvesterName)
    {
        documentCount = 0;
        outputFile = new File(String.format(FILE_PATH, MainContext.getModuleName(), harvesterName));
    }

    public void clear()
    {
        if (documentCount > 0)
            submit();

        documentCount = 0;
        writer = null;
    }

    public boolean start()
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


    public void submit()
    {
        try {
            writer.endArray();
            writer.close();
        } catch (IOException e) {
            // TODO
        }
    }


    public void addDocument(IDocument doc)
    {
        boolean isInitialized = true;

        if (documentCount == 0)
            isInitialized = start();

        if (isInitialized) {
            GsonUtils.getGson().toJson(doc, doc.getClass(), writer);
            documentCount++;
        }
    }

	public File getOutputFile()
	{
		return outputFile;
	}
}
