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
package de.gerdiproject.utils.examples.harvestercache;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import de.gerdiproject.AbstractFileSystemUnitTest;
import de.gerdiproject.application.ContextListenerTest;
import de.gerdiproject.harvest.application.ContextListener;
import de.gerdiproject.harvest.etls.StaticIteratorETL;
import de.gerdiproject.harvest.etls.extractors.AbstractIteratorExtractor;
import de.gerdiproject.harvest.etls.transformers.AbstractIteratorTransformer;
import de.gerdiproject.json.datacite.DataCiteJson;
import de.gerdiproject.json.datacite.Title;

/**
 * This class serves as a mocked Harvester that can harvest a specified
 * list of strings.
 *
 * @author Robin Weiss
 */
public class MockedETL extends StaticIteratorETL<String, DataCiteJson>
{
    protected final List<String> mockedEntries;
    private final String cacheFolder;



    /**
     * This constructor is used by {@linkplain ContextListenerTest}.
     *
     */
    public MockedETL()
    {
        this(new File(AbstractFileSystemUnitTest.TEST_FOLDER, ContextListener.class.getSimpleName()));
    }


    /**
     * This constructor accepts a list of harvestable strings.
     *
     * @param mockedEntries a list of strings to be harvested
     * @param cacheFolder the folder where documents are cached
     */
    public MockedETL(final List<String> mockedEntries, final File cacheFolder)
    {
        super(new MockedExtractor(), new MockedTransformer());

        this.mockedEntries = mockedEntries;
        this.cacheFolder = cacheFolder + "/";
    }


    /**
     * This constructor generates a short list to be used as entries.
     * @param cacheFolder the folder where documents are cached
     */
    public MockedETL(final File cacheFolder)
    {
        this(Arrays.asList("mockedEntry1", "mockedEntry2", "mockedEntry3"), cacheFolder);
    }

    /**
     * Returns a unique identifier, used for Unit Tests of caching classes.
     *
     * @return a unique harvester identifier
     */
    public String getId()
    {
        return getName();
    }


    /**
     * Returns a temporary cache folder path, used for Unit Tests of caching classes.
     * @return a temporary cache folder path
     */
    public String getTemporaryCacheFolder()
    {
        return cacheFolder + "documents_temp/";
    }


    /**
     * Returns a permanent cache folder path, used for Unit Tests of caching classes.
     * @return a permanent cache folder path
     */
    public String getStableCacheFolder()
    {
        return cacheFolder + "documents/";
    }


    private static class MockedExtractor extends AbstractIteratorExtractor<String>
    {
        private List<String> mockedList = Arrays.asList("mockedEntry1", "mockedEntry2", "mockedEntry3");

        @Override
        public Iterator<String> extract()
        {
            return mockedList.iterator();
        }


        @Override
        public String getUniqueVersionString()
        {
            return null;
        }

        @Override
        public int size()
        {
            return mockedList.size();
        }


        @Override
        protected Iterator<String> extractAll()
        {
            return mockedList.iterator();
        }
    }


    private static class MockedTransformer extends AbstractIteratorTransformer<String, DataCiteJson>
    {
        @Override
        protected DataCiteJson transformElement(String source)
        {
            DataCiteJson mockedDocument = new DataCiteJson("source: " + source);
            mockedDocument.setTitles(Arrays.asList(new Title("title: " + source)));
            return mockedDocument;
        }
    }


    public void setHash(String sourceHash)
    {
        this.hash = sourceHash;
    }
}
