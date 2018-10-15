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
import java.nio.charset.Charset;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;

import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.parameters.StringParameter;
import de.gerdiproject.harvest.etls.AbstractETL;
import de.gerdiproject.harvest.etls.loaders.constants.SaveConstants;
import de.gerdiproject.harvest.utils.FileUtils;
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


    /**
     * Constructor that sets the parser and save folder.
     */
    public DiskLoader()
    {
        super();
        this.saveFolderParam = Configuration.registerParameter(SaveConstants.FILE_PATH_PARAM);
        this.gson = GsonUtils.createGerdiDocumentGsonBuilder().create();
    }


    @Override
    public void unregisterParameters()
    {
        Configuration.unregisterParameter(saveFolderParam);
    }


    @Override
    public <H extends AbstractETL<?, ?>> void init(H harvester)
    {
        final String sourceHash = harvester.getHash();
        final Charset charset = harvester.getCharset();

        // create empty file
        final String fileName = harvester.getName() + SaveConstants.JSON_EXTENSION;
        final File result = new File(saveFolderParam.getStringValue(), fileName);
        FileUtils.createEmptyFile(result);

        // abort if the file could not be created or cleaned up
        if (!result.exists() || !result.isFile() || result.length() != 0)
            throw new IllegalStateException(String.format(SaveConstants.SAVE_FAILED_CANNOT_CREATE, result));

        // create JSON writer
        try {
            writer = new JsonWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(result), charset)));
            writer.beginObject();

            // write the current timestamp
            writer.name(SaveConstants.HARVEST_DATE_JSON);
            writer.value(System.currentTimeMillis());

            // write the hash
            if (sourceHash != null) {
                writer.name(SaveConstants.SOURCE_HASH_JSON);
                writer.value(sourceHash);
            }

            writer.name(SaveConstants.DOCUMENTS_JSON);
            writer.beginArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

    }


    @Override
    public void loadElement(DataCiteJson document, boolean isLastDocument) throws LoaderException
    {
        if (document != null)
            gson.toJson(document, document.getClass(), writer);

        if (isLastDocument) {
            try {
                writer.endArray();
                writer.endObject();
                writer.close();
            } catch (IOException e) {
                throw new LoaderException(e.toString());
            }

            writer = null;
        }
    }
}
