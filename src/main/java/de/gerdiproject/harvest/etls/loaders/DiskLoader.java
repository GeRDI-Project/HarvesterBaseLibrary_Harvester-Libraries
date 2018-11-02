/**
 * Copyright © 2017 Robin Weiss (http://www.gerdi-project.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.gerdiproject.harvest.etls.loaders;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Iterator;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;

import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.parameters.StringParameter;
import de.gerdiproject.harvest.etls.AbstractETL;
import de.gerdiproject.harvest.etls.loaders.constants.DiskLoaderConstants;
import de.gerdiproject.harvest.utils.file.FileUtils;
import de.gerdiproject.json.GsonUtils;
import de.gerdiproject.json.datacite.DataCiteJson;

/**
 * This class offers functions for saving a harvest result to disk.
 *
 * @author Robin Weiss
 */
public class DiskLoader extends AbstractIteratorLoader<DataCiteJson>
{
    private final Gson gson;
    private final StringParameter saveFolderParam;

    private JsonWriter writer;
    private File targetFile;

    /**
     * Constructor that sets the parser and save folder.
     */
    public DiskLoader()
    {
        super();
        this.saveFolderParam = Configuration.registerParameter(DiskLoaderConstants.FILE_PATH_PARAM);
        this.gson = GsonUtils.createGerdiDocumentGsonBuilder().create();
    }


    @Override
    public void unregisterParameters()
    {
        Configuration.unregisterParameter(saveFolderParam);
    }


    /**
     * Returns the file to which the documents are saved.
     *
     * @return the file to which the documents are saved, or null if init() was not called before
     */
    public File getTargetFile()
    {
        return targetFile;
    }


    @Override
    public void load(Iterator<DataCiteJson> documents) throws LoaderException
    {
        try {
            super.load(documents);
        } catch (Exception e) {
            throw new LoaderException(e.getMessage());
        }
    }


    @Override
    public void init(AbstractETL<?, ?> etl)
    {
        super.init(etl);

        final String sourceHash = etl.getHash();

        // create empty file
        final String fileName = etl.getName() + DiskLoaderConstants.JSON_EXTENSION;
        this.targetFile = new File(saveFolderParam.getStringValue(), fileName);
        FileUtils.createEmptyFile(targetFile);

        // abort if the file could not be created or cleaned up
        if (!targetFile.exists() || targetFile.length() != 0)
            throw new IllegalStateException(String.format(DiskLoaderConstants.SAVE_FAILED_CANNOT_CREATE, targetFile));

        // create JSON writer
        try {
            writer = new JsonWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(targetFile), etl.getCharset())));
            writer.beginObject();

            // write the current timestamp
            writer.name(DiskLoaderConstants.HARVEST_DATE_JSON);
            writer.value(System.currentTimeMillis());

            // write the hash
            if (sourceHash != null) {
                writer.name(DiskLoaderConstants.SOURCE_HASH_JSON);
                writer.value(sourceHash);
            }

            writer.name(DiskLoaderConstants.DOCUMENTS_JSON);
            writer.beginArray();

            System.out.println("OPEN: " + targetFile.getName());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }


    /**
     * Attempts to close the writer.
     *
     * @throws LoaderException thrown if there is an error closing the writer
     */
    @Override
    public void clear()
    {
        if (writer == null)
            return;

        try {
            writer.endArray();
            writer.endObject();
            writer.close();
            writer = null;
            System.out.println("CLOSE: " + targetFile.getName());
        } catch (IOException e) {
            throw new LoaderException(e.toString());
        }

        if (!hasLoadedDocuments)
            FileUtils.deleteFile(getTargetFile());
    }


    @Override
    public void loadElement(DataCiteJson document) throws LoaderException
    {
        if (document != null)
            gson.toJson(document, document.getClass(), writer);
    }
}
