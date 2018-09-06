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
package de.gerdiproject.harvest.harvester;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import de.gerdiproject.harvest.IDocument;
import de.gerdiproject.harvest.application.MainContext;
import de.gerdiproject.harvest.application.events.ContextDestroyedEvent;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.harvester.constants.HarvesterConstants;
import de.gerdiproject.harvest.utils.FileUtils;
import de.gerdiproject.harvest.utils.cache.constants.CacheConstants;


/**
 * This harvester writes a collection of entries to a stream, caching them in a
 * file on disk and counting them at the same time. You can conveniently use
 * this harvester instead of an {@linkplain AbstractListHarvester} if the server
 * is low on memory. However, what is saved in heap space, will add to the size
 * on disk instead.
 *
 * @author Robin Weiss
 */
public abstract class AbstractStreamHarvester<T> extends AbstractHarvester
{
    // this warning is suppressed, because the only generic Superclass MUST be T. The cast will always succeed.
    @SuppressWarnings("unchecked")
    private final Class<T> entryClass = (Class<T>)((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];

    private final AtomicInteger entryCount;
    protected final int numberOfDocumentsPerEntry;

    protected final Gson gson;

    protected File streamFile;


    /**
     * Event listener that deletes the cache file when the service is
     * undeployed.
     */
    private final Consumer<ContextDestroyedEvent> onContextDestroyed = (ContextDestroyedEvent e) -> deleteEntryStreamFile();


    /**
     * Forwarding the superclass constructor.
     *
     * @param harvesterName a unique name of the harvester
     * @param numberOfDocumentsPerEntry the number of documents that are
     *            expected to be harvested from each entry
     */
    public AbstractStreamHarvester(String harvesterName, int numberOfDocumentsPerEntry)
    {
        super(harvesterName);
        this.numberOfDocumentsPerEntry = numberOfDocumentsPerEntry;
        this.entryCount = new AtomicInteger();
        this.gson = createGsonBuilder().create();

        EventSystem.addListener(ContextDestroyedEvent.class, onContextDestroyed);
    }


    /**
     * Forwarding the superclass constructor.
     *
     * @param numberOfDocumentsPerEntry the number of documents that are
     *            expected to be harvested from each entry
     */
    public AbstractStreamHarvester(int numberOfDocumentsPerEntry)
    {
        this(null, numberOfDocumentsPerEntry);
    }


    @Override
    public void init(boolean isMainHarvester, String moduleName)
    {
        super.init(isMainHarvester, moduleName);
        streamFile = initStreamFile(moduleName);
    }


    /**
     * Initializes the file were entries are cached.
     *
     * @param moduleName the name of the harvester service
     *
     * @return a file were entries are cached
     */
    protected File initStreamFile(String moduleName)
    {
        String filePath = String.format(CacheConstants.CACHE_ENTRY_STREAM_PATH, MainContext.getServiceName(), getName());

        File cacheFile = new File(filePath);
        FileUtils.createEmptyFile(cacheFile);

        return cacheFile;
    }


    /**
     * Writes JSON entries into a stream and counts them. Each entry of the
     * stream will later be treated by the harvestEntry() function.
     *
     * @param addEntryToStream use this consumer function to add entries to the
     *            stream
     */
    protected abstract void loadEntries(Consumer<T> addEntryToStream);


    /**
     * Harvests a single entry, adding between zero and
     * 'numberOfDocumentsPerEntry' entries to the search index.
     *
     * @param entry the entry that is to be read
     *
     * @return a list of search documents, or null if no documents could be
     *         retrieved from the entry
     */
    protected abstract List<IDocument> harvestEntry(T entry);


    @Override
    protected boolean harvestInternal(int from, int to) throws Exception // NOPMD - we want the inheriting class to be able to throw any exception
    {
        if (from == to) {
            logger.warn(String.format(HarvesterConstants.LOG_OUT_OF_RANGE, name));
            return true;

        } else if (entryCount.get() == 0) {
            logger.error(String.format(HarvesterConstants.ERROR_NO_ENTRIES, name));
            return false;
        }

        // indices of entries that are to be harvested
        int firstEntryIndex = from / numberOfDocumentsPerEntry;
        int lastEntryIndex = (to - 1) / numberOfDocumentsPerEntry;

        // indices of documents that are harvested from one entry
        int startIndex = from % numberOfDocumentsPerEntry;
        int endIndex = to % numberOfDocumentsPerEntry;

        // the endIndex must be in [1, docsPerEntry]
        if (endIndex == 0)
            endIndex = numberOfDocumentsPerEntry;

        // open reader to entry stream
        JsonReader entryReader = createEntryStreamReader();
        entryReader.beginArray();

        // harvest the first entry
        int i = 0;

        while (entryReader.hasNext()) {
            // abort harvest, if it is flagged for cancellation
            if (isAborting) {
                currentHarvestingProcess.cancel(false);
                return false;
            }

            // skip entries that come before the firstEntryIndex
            if (i >= firstEntryIndex) {
                // get entry
                final T entry = gson.fromJson(entryReader, entryClass);

                // get convert entry to document(s)
                final List<IDocument> docs = harvestEntry(entry);

                int j = (i == firstEntryIndex) ? startIndex : 0;
                int jEnd = (i == lastEntryIndex) ? endIndex : numberOfDocumentsPerEntry;

                // add all harvested documents to the cache
                if (docs != null) {
                    while (j < jEnd && j < docs.size())
                        addDocument(docs.get(j++));
                }

                // if less docs were harvested than expected,
                // skip the correct amount of documents
                while (j++ < jEnd)
                    addDocument(null);

                // finish iteration after harvesting the last index
                if (i == lastEntryIndex)
                    break;
            }

            i++;
        }

        // close reader
        if (!isAborting)
            entryReader.endArray();

        entryReader.close();

        return true;
    }


    @Override
    public void update()
    {
        // delete left-over cache file
        deleteEntryStreamFile();

        try {
            writeEntriesToStream();
        } catch (IOException e) {
            logger.error(String.format(CacheConstants.ENTRY_STREAM_WRITE_ERROR, e));
        }

        super.update();
    }


    /**
     * Resets the entry count and loads source entries via a stream of JSON
     * objects into a cache file.
     *
     * @throws IOException this exception is thrown if a read or write function
     *             of the entry stream fails
     */
    private void writeEntriesToStream() throws IOException
    {
        entryCount.set(0);

        // prepare the writer
        final JsonWriter entryWriter = createEntryStreamWriter();
        entryWriter.beginArray();

        // write entries to stream
        final Consumer<T> addFunction = createAddEntryToStreamFunction(entryWriter);
        loadEntries(addFunction);

        // close writer
        entryWriter.endArray();
        entryWriter.close();
    }


    /**
     * Creates a {@linkplain JsonWriter} for the stream of JSON entries that are
     * to be harvested. The writer is used to cache and count all source entries
     * that will later be converted to searchable documents.
     *
     * @return a JsonWriter for the stream of JSON entries
     *
     * @throws FileNotFoundException this exception is thrown if the file could
     *             not be created or found
     */
    private JsonWriter createEntryStreamWriter() throws FileNotFoundException
    {
        if (streamFile == null)
            throw new FileNotFoundException();

        return new JsonWriter(new OutputStreamWriter(new FileOutputStream(streamFile), getCharset()));
    }


    /**
     * Creates a {@linkplain JsonReader} for the stream of JSON entries that are
     * to be harvested. The reader is used to process the cached entries and
     * convert them to searchable documents.
     *
     * @return a JsonReader for the stream of JSON entries that are cached in a
     *         file
     *
     * @throws FileNotFoundException this exception is thrown if the file could
     *             not be created or found
     */
    private JsonReader createEntryStreamReader() throws FileNotFoundException
    {
        if (streamFile == null)
            throw new FileNotFoundException();

        return new JsonReader(new InputStreamReader(new FileInputStream(streamFile), getCharset()));
    }


    /**
     * Attempts to delete the entry stream cache file.
     */
    private void deleteEntryStreamFile()
    {
        if (streamFile != null && streamFile.exists()) {
            boolean deleteSuccess;

            try {
                deleteSuccess = streamFile.delete();
            } catch (SecurityException e) {
                deleteSuccess = false;
            }

            if (deleteSuccess)
                logger.info(String.format(CacheConstants.DELETE_FILE_SUCCESS, streamFile.getName()));
            else
                logger.error(String.format(CacheConstants.DELETE_FILE_FAILED, streamFile.getName()));
        }
    }


    /**
     * Creates a consuming function that can be passed as function argument. The
     * function adds an entry to a specified {@linkplain JsonWriter} and
     * increments the entry count.
     *
     * @param entryWriter the writer to which the entry is written as a JSON
     *            object
     *
     * @return a consuming function that adds an entry to a specified
     *         {@linkplain JsonWriter} and increments the entry count
     */
    private Consumer<T> createAddEntryToStreamFunction(JsonWriter entryWriter)
    {
        return (T entry) -> {
            gson.toJson(entry, entry.getClass(), entryWriter);
            entryCount.incrementAndGet();
        };
    }


    @Override
    protected int initMaxNumberOfDocuments()
    {
        return entryCount.get() * numberOfDocumentsPerEntry;
    }
}
