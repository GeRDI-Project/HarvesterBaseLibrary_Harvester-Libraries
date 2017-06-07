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
package de.gerdiproject.harvest.harvester;

import de.gerdiproject.json.IJsonArray;
import de.gerdiproject.json.IJsonObject;
import de.gerdiproject.logger.ILogger;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 *
 * @author row
 */
public abstract class AbstractJsonArrayHarvester extends AbstractHarvester
{
    protected final static String ERROR_NO_ENTRIES = ": Could not harvest. The entries are empty or could not be retrieved";
    protected final static String LOG_OUT_OF_RANGE = ": Harvesting indices out of range. Harvesting will be skipped";
    
    protected IJsonArray entries;
    protected final int numberOfDocumentsPerEntry;


    /**
     * Forwarding the superclass constructor.
     *
     * @param numberOfDocumentsPerEntry the number of documents that are
     * expected to be harvested from each entry
     */
    public AbstractJsonArrayHarvester( int numberOfDocumentsPerEntry )
    {
        super();
        this.numberOfDocumentsPerEntry = numberOfDocumentsPerEntry;
    }


    /**
     * Retrieves a JSON-array of entries that are to be searched.
     *
     * @return A JsonArray of entries
     */
    abstract protected IJsonArray getEntries();


    /**
     * Harvests a single entry, adding exactly one document to the search index.
     *
     * @param entry the entry that is to be read
     *
     * @return a list of search documents, created via the SearchIndexFactory
     * @see de.gerdiproject.harvest.utils.SearchIndexFactory
     */
    abstract protected List<IJsonObject> harvestEntry( IJsonObject entry );


    @Override
    protected boolean harvestInternal( int from, int to )
    {
        if(from == to)
        {
            logger.log( name + LOG_OUT_OF_RANGE);
            
            return true;
        }
        else if(entries == null || entries.isEmpty())
        {
            logger.logError( name + ERROR_NO_ENTRIES);
            
            return false;
        }
        
        // indices of entries that are to be harvested
        int firstEntryIndex = from / numberOfDocumentsPerEntry;
        int lastEntryIndex = (to - 1) / numberOfDocumentsPerEntry;

        // indices of documents that are harvested from one entry
        int startIndex = from % numberOfDocumentsPerEntry;
        int endIndex = to % numberOfDocumentsPerEntry;

        // the endIndex is in [1, docsPerEntry]
        if (endIndex == 0)
        {
            endIndex = numberOfDocumentsPerEntry;
        }

        // harvest the first entry
        final List<IJsonObject> firstEntryDocuments = harvestEntry( entries.getJsonObject( firstEntryIndex ) );

        // check if only a single entry needs to be harvested
        if (firstEntryIndex == lastEntryIndex)
        {
            // add only a part of the documents to the index
            for (int i = startIndex; i < endIndex; i++)
            {
                addDocumentToIndex( firstEntryDocuments.get( i ) );
            }
        }
        else
        {
            // add only a part of the first entry documents to the index
            for (int i = startIndex; i < numberOfDocumentsPerEntry; i++)
            {
                addDocumentToIndex( firstEntryDocuments.get( i ) );
            }

            // harvest all entries that are to be harvested wholly
            for (int i = firstEntryIndex + 1; i < lastEntryIndex; i++)
            {
                addDocumentsToIndex( harvestEntry( entries.getJsonObject( i ) ) );
            }
            
            // harvest last entry
            final List<IJsonObject> lastEntryDocuments = harvestEntry( entries.getJsonObject( lastEntryIndex ) );

            // add only a part of the last entry documents to the index
            for (int i = 0; i < endIndex; i++)
            {
                addDocumentToIndex( lastEntryDocuments.get( i ) );
            }
        }
        return true;
    }


    @Override
    public void init( ILogger logger )
    {
        entries = getEntries();
        super.init( logger );
    }


    @Override
    protected int calculateTotalNumberOfDocumentsInternal()
    {
        return entries.size() * numberOfDocumentsPerEntry;
    }


    @Override
    protected String calculateHashInternal()
    {
        try
        {
            final MessageDigest md = MessageDigest.getInstance( SHA_HASH_ALGORITHM );
            md.update( entries.toString().getBytes() );

            final byte[] digest = md.digest();

            final StringWriter buffer = new StringWriter( digest.length * 2 );
            final PrintWriter pw = new PrintWriter( buffer );

            for (byte b : digest)
            {
                pw.printf( OCTAT_FORMAT, b );
            }

            return buffer.toString();
        }
        catch (NoSuchAlgorithmException | NullPointerException e)
        {
            logger.logError( HASH_CREATE_FAILED );
            return null;
        }
    }

}
