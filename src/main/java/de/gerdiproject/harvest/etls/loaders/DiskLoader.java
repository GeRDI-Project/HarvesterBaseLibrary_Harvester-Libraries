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

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.stream.JsonWriter;

import de.gerdiproject.harvest.application.events.GetCacheFolderEvent;
import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.parameters.StringParameter;
import de.gerdiproject.harvest.etls.AbstractETL;
import de.gerdiproject.harvest.etls.extractors.ExtractorException;
import de.gerdiproject.harvest.etls.loaders.constants.DiskLoaderConstants;
import de.gerdiproject.harvest.etls.transformers.TransformerException;
import de.gerdiproject.harvest.event.EventSystem;
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

    private File targetFile;
    private JsonWriter writer;

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
     * The file location is determined by the deployment specific
     * cache folder root and the subfolder determined by a {@linkplain StringParameter}.
     *
     * @param fileName the name of the file without extension
     *
     * @return the {@linkplain File} to which the documents are saved, or null if init() was not called before
     */
    public File createTargetFile(final String fileName)
    {
        final File cacheFolderRoot = EventSystem.sendSynchronousEvent(new GetCacheFolderEvent());
        final File cacheFolder = new File(cacheFolderRoot, saveFolderParam.getStringValue());

        return new File(cacheFolder, fileName + DiskLoaderConstants.JSON_EXTENSION);
    }


    @Override
    public void load(final Iterator<DataCiteJson> documents) throws LoaderException
    {
        try {
            super.load(documents);
        } catch (ExtractorException | TransformerException | LoaderException e) { // NOPMD, these exceptions don't need to be wrapped
            throw e;
        } catch (final RuntimeException e) { // NOPMD, every other exception must be wrapped
            throw new LoaderException(e);
        }
    }


    @Override
    public void init(final AbstractETL<?, ?> etl)
    {
        super.init(etl);

        // create empty file
        this.targetFile = createTargetFile(etl.getName());
        FileUtils.createEmptyFile(this.targetFile);

        // abort if the file could not be created or cleaned up
        if (!this.targetFile.exists() || this.targetFile.length() != 0)
            throw new IllegalStateException(String.format(DiskLoaderConstants.SAVE_FAILED_CANNOT_CREATE, this.targetFile));

        // create JSON writer
        try {
            writer = new JsonWriter(FileUtils.getWriter(targetFile, etl.getCharset()));
            writer.beginObject();

            // write the current timestamp
            writer.name(DiskLoaderConstants.HARVEST_DATE_JSON);
            writer.value(System.currentTimeMillis());

            // write the hash
            final String sourceHash = etl.getHash();

            if (sourceHash != null) {
                writer.name(DiskLoaderConstants.SOURCE_HASH_JSON);
                writer.value(sourceHash);
            }

            writer.name(DiskLoaderConstants.DOCUMENTS_JSON);
            writer.beginArray();
        } catch (final IOException e) {
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
        } catch (final IOException e) {
            throw new LoaderException(e);
        }

        if (!hasLoadedDocuments)
            FileUtils.deleteFile(this.targetFile);
    }


    @Override
    public void loadElement(final DataCiteJson document) throws LoaderException
    {
        if (document != null) {
            try {
                gson.toJson(document, document.getClass(), writer);
            } catch (final JsonIOException e) {
                throw new LoaderException(e);
            }
        }
    }
}
