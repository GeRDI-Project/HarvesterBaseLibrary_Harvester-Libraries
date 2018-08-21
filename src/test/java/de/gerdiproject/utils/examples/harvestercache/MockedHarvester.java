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
import java.util.Collection;
import java.util.List;

import de.gerdiproject.harvest.IDocument;
import de.gerdiproject.harvest.harvester.AbstractListHarvester;
import de.gerdiproject.json.datacite.DataCiteJson;
import de.gerdiproject.json.datacite.Title;

/**
 * This class serves as a mocked Harvester that can harvest a specified
 * list of strings.
 *
 * @author Robin Weiss
 */
public class MockedHarvester extends AbstractListHarvester<String>
{
    protected final List<String> mockedEntries;
    private final String cacheFolder;


    /**
     * This constructor accepts a list of harvestable strings.
     *
     * @param mockedEntries a list of strings to be harvested
     * @param cacheFolder the folder where documents are cached
     */
    public MockedHarvester(final List<String> mockedEntries, final File cacheFolder)
    {
        super(1);
        this.mockedEntries = mockedEntries;
        this.cacheFolder = cacheFolder + "/";
    }


    /**
     * This constructor generates a short list to be used as entries.
     * @param cacheFolder the folder where documents are cached
     */
    public MockedHarvester(final File cacheFolder)
    {
        this(Arrays.asList("mockedEntry1", "mockedEntry2", "mockedEntry3"), cacheFolder);
    }

    public String getTemporaryCacheFolder()
    {
        return cacheFolder + "documents_temp/";
    }


    public String getStableCacheFolder()
    {
        return cacheFolder + "documents/";
    }


    @Override
    protected Collection<String> loadEntries()
    {
        return mockedEntries;
    }


    @Override
    protected List<IDocument> harvestEntry(String entry)
    {
        DataCiteJson mockedDocument = new DataCiteJson("source: " + entry);
        mockedDocument.setTitles(Arrays.asList(new Title("title: " + entry)));
        return Arrays.asList(mockedDocument);
    }
}
