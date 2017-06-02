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

import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.development.DevelopmentTools;
import de.gerdiproject.harvest.elasticsearch.ElasticSearchSender;
import de.gerdiproject.harvest.utils.CancelableFuture;
import de.gerdiproject.harvest.utils.HttpRequester;
import de.gerdiproject.harvest.utils.SearchIndexFactory;
import de.gerdiproject.json.IJsonArray;
import de.gerdiproject.json.IJsonBuilder;
import de.gerdiproject.json.IJsonObject;
import de.gerdiproject.json.impl.JsonBuilder;
import de.gerdiproject.logger.ILogger;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Harvester classes provide methods for harvesting databases and returning a
 * search index as a JSON object.
 *
 * @author row
 */
public abstract class AbstractHarvester
{
    protected static final String HASH_CREATE_FAILED = "Failed to create hash";
    protected static final String OCTAT_FORMAT = "%02x";
    protected static final String SHA_HASH_ALGORITHM = "SHA";

    private final static String HARVESTER_START = "+++ Starting %s +++";
    private final static String HARVESTER_END = "--- %s finished ---";
    private final static String HARVESTER_FAILED = "!!! %s FAILED !!!";
    private static final String HARVEST_DONE = "HARVEST FINISHED!";
    private static final String HARVEST_FAILED = "HARVEST FAILED!";

    private final HashMap<String, String> properties;
    private final IJsonArray harvestedDocuments;

    private Date harvestStartedDate;
    private Date harvestFinishedDate;
    protected CancelableFuture<Boolean> currentHarvestingProcess;

    private final AtomicInteger totalNumberOfDocuments;
    private final AtomicInteger numberOfHarvestedDocuments;

    private final AtomicInteger harvestStartIndex;
    private final AtomicInteger harvestEndIndex;

    protected String name;
    protected String hash;
    protected ILogger logger;
    protected final HttpRequester httpRequester;
    protected final SearchIndexFactory searchIndexFactory;
    protected final IJsonBuilder jsonBuilder;

    /**
     * The main harvesting method. The overridden implementation should add
     * documents to the search index by calling addDocumentToIndex().
     *
     * @param startIndex the index of the first document to be harvested
     * @param endIndex the index of the last document to be harvested
     * @return true, if everything was harvested
     * @see de.gerdiproject.harvest.utils.SearchIndexFactory
     */
    abstract protected boolean harvestInternal( int startIndex, int endIndex );


    /**
     * Returns a list of properties that can be set.
     *
     * @return a list of properties that can be set via setProperty()
     */
    abstract public List<String> getValidProperties();


    /**
     * Calculates the total number of harvested documents.
     *
     * @return the total number of documents that are to be harvested
     */
    abstract protected int calculateTotalNumberOfDocumentsInternal();


    /**
     * Computes a hash value of the files that are to be harvested, which is
     * used for checking if the files have changed.
     *
     * @return a hash as a checksum of the data which is to be harvested 
     */
    abstract protected String calculateHashInternal();


    /**
     * Initializes the Harvester, setting up missing values and the logger.
     *
     * @param logger the logger that is to be used
     */
    public void init( ILogger logger )
    {
        // set logger
        this.logger = logger;

        // calculate hash
        hash = calculateHashInternal();

        // calculate number of documents
        totalNumberOfDocuments.set( calculateTotalNumberOfDocumentsInternal() );

        int oldEndIndex = harvestEndIndex.get();
        int newEndIndex = totalNumberOfDocuments.get();

        // if the end index is out of bounds, set it correctly
        if (oldEndIndex == 0 || oldEndIndex > newEndIndex)
        {
            harvestEndIndex.set( newEndIndex );
        }

        name = getName();
    }


    /**
     * Constructor that can share one documents array with other harvesters.
     *
     * @param harvestedDocuments the list to which harvested documents are added
     */
    public AbstractHarvester( IJsonArray harvestedDocuments )
    {
        properties = new HashMap<>();
        httpRequester = new HttpRequester();
        searchIndexFactory = new SearchIndexFactory();
        
        currentHarvestingProcess = null;
        harvestStartedDate = null;
        harvestFinishedDate = null;
        totalNumberOfDocuments = new AtomicInteger();
        numberOfHarvestedDocuments = new AtomicInteger();
        this.harvestedDocuments = harvestedDocuments;

        harvestStartIndex = new AtomicInteger( 0 );
        harvestEndIndex = new AtomicInteger( 0 );
        jsonBuilder = new JsonBuilder();
    }
    
    /**
     * Simple constructor that creates its own documents array.
     */
    public AbstractHarvester( )
    {
        properties = new HashMap<>();
        httpRequester = new HttpRequester();
        searchIndexFactory = new SearchIndexFactory();
        
        currentHarvestingProcess = null;
        harvestStartedDate = null;
        harvestFinishedDate = null;
        totalNumberOfDocuments = new AtomicInteger();
        numberOfHarvestedDocuments = new AtomicInteger();

        harvestStartIndex = new AtomicInteger( 0 );
        harvestEndIndex = new AtomicInteger( 0 );
        jsonBuilder = new JsonBuilder();
        this.harvestedDocuments = jsonBuilder.createArray();
    }


    /**
     * Retrieves the value of a property.
     *
     * @param key the name of the property
     * @return the property value
     */
    public String getProperty( String key )
    {
        return properties.get( key );
    }


    /**
     * Sets the value of a property.
     *
     * @param key the property name
     * @param value the new property value
     */
    public void setProperty( String key, String value )
    {
        properties.put( key, value );
    }


    /**
     * Creates and returns the search index and some metadata as a JSON object.
     *
     * @return the JSON object that was harvested via harvestInternal(), or null
     * if no harvest has been finished successfully
     */
    public final IJsonObject getHarvestResult()
    {
    	java.sql.Date sqlDate = new java.sql.Date( harvestStartedDate.getTime() );
        int harvestTime
                = (harvestFinishedDate != null && harvestStartedDate != null)
                        ? (int) ((harvestFinishedDate.getTime() - harvestStartedDate.getTime()) / 1000)
                        : -1;
                        
        synchronized (harvestedDocuments)
        {
        	return searchIndexFactory.createSearchIndex( harvestedDocuments, sqlDate, hash, harvestTime );
        }
    }
    
    /**
     * Returns the harvested documents.
     * 
     * @return the harvested documents
     */
    public final IJsonArray getHarvestedDocuments()
    {
        synchronized (harvestedDocuments)
        {
        	return harvestedDocuments;
        }
    }


    /**
     * Returns the timestamp of the last successful harvest.
     *
     * @return the time and date at which the last harvest finished, or null if
     * no harvest has been finished successfully
     */
    public final java.util.Date getHarvestDate()
    {
        return harvestFinishedDate;
    }


    /**
     * Returns the timestamp of the beginning of the last harvest.
     *
     * @return the time and date at which the last harvest started, or null if
     * no harvest has been started yet
     */
    public final java.util.Date getHarvestStartDate()
    {
        return harvestStartedDate;
    }


    /**
     * Adds a document to the search index and logs the progress. If the
     * document is null, it is not added to the search index, but the progress
     * is incremented regardlessly.
     *
     * @param document the document that is to be added to the search index
     */
    protected void addDocumentToIndex( IJsonObject document )
    {
        if (document != null)
        {
            synchronized (harvestedDocuments)
            {
                harvestedDocuments.add( document );
            }
        }

        // increment harvest count
        final int harvestedDocs = numberOfHarvestedDocuments.incrementAndGet();
        final int from = harvestStartIndex.get();

        // log progress
        logger.logProgress(
                name,
                from + harvestedDocs,
                harvestEndIndex.get()
        );
    }


    /**
     * Adds multiple documents to the search index and logs the progress. If a
     * document is null, it is not added to the search index, but the progress
     * is incremented regardlessly.
     *
     * @param documents the documents that are to be added to the search index
     */
    protected void addDocumentsToIndex( Iterable<IJsonObject> documents )
    {
        documents.forEach( (IJsonObject doc) -> addDocumentToIndex( doc ) );
    }


    /**
     * Returns the total number of documents that can possibly be harvested.
     *
     * @return the total number of documents that can possibly be harvested
     */
    public final int getTotalNumberOfDocuments()
    {
        return totalNumberOfDocuments.get();
    }


    /**
     * Returns the index of the first document that is to be harvested.
     *
     * @return the index of the first document that is to be harvested
     */
    public final int getHarvestStartIndex()
    {
        return harvestStartIndex.get();
    }


    /**
     * Returns the index of the first document that is not to be harvested
     * anymore.
     *
     * @return the index of the first document that is not to be harvested
     * anymore
     */
    public final int getHarvestEndIndex()
    {
        return harvestEndIndex.get();
    }


    /**
     * Sets the harvesting range.
     *
     * @param from the index of the first document to be harvested
     * @param to the index of the first document that is not to be harvested
     * anymore
     */
    public void setRange( int from, int to )
    {
        harvestStartIndex.set( from );
        harvestEndIndex.set( to );
    }


    /**
     * Returns the number of harvested documents.
     *
     * @return the number of harvested documents
     */
    public int getNumberOfHarvestedDocuments()
    {
        return numberOfHarvestedDocuments.get();
    }


    /**
     * Returns true if a harvest is in progress.
     *
     * @return true if a harvest is in progress
     */
    public boolean isHarvesting()
    {
        return currentHarvestingProcess != null;
    }


    /**
     * Estimates the completion date of an ongoing harvest by regarding the
     * already passed time.
     *
     * @return a Date or null, if no harvest is in progress
     */
    public long estimateRemainingSeconds()
    {
        // get number of harvested documents
        long documentCount = getNumberOfHarvestedDocuments();

        // only estimate if some progress was made
        if (isHarvesting() && documentCount > 0)
        {
            Date now = new Date();

            // calculate how many milliseconds the harvest has been going on
            long milliSecondsUntilNow = now.getTime() - harvestStartedDate.getTime();

            // estimate how many milliseconds the harvest will take
            long milliSecondsTotal
                    = milliSecondsUntilNow
                    * (harvestEndIndex.get() - harvestStartIndex.get())
                    / documentCount;

            return (milliSecondsTotal - milliSecondsUntilNow) / 1000l;
        }
        return -1;
    }


    /**
     * Starts an asynchronous harvest with the implemented harvestInternal()
     * method and saves the result and date for this session
     */
    public final void harvest()
    {
        logger.log( String.format( HARVESTER_START, name ) );
        synchronized (harvestedDocuments)
        {
            harvestedDocuments.clear();
        }
        numberOfHarvestedDocuments.set( 0 );
        harvestStartedDate = new Date();

        // start asynchronous harvest
        currentHarvestingProcess = new CancelableFuture<>(
                () -> harvestInternal( harvestStartIndex.get(), harvestEndIndex.get() )
        );
        currentHarvestingProcess.thenApply( (isSuccessful) ->
        {
            endHarvest();
            return isSuccessful;
        }
        ).exceptionally( throwable ->
        {
            failHarvest( throwable.getCause() );
            return false;
        } );

    }


    /**
     * Finishes the harvesting process, allowing a new harvest to be started.
     */
    private void endHarvest()
    {
        currentHarvestingProcess = null;
        harvestFinishedDate = new Date();

        logger.log( String.format( HARVESTER_END, name ) );

        // do some things, only if this is the main harvester
        if (MainContext.getHarvester() == this)
        {
            final DevelopmentTools devTools = DevelopmentTools.instance();
            
            // save to disk if auto-save is enabled
            if (devTools.isAutoSaving())
            {
                devTools.saveHarvestResultToDisk();
            }
            
            // log complete harvest end
            logger.log( HARVEST_DONE );

            // send to elastic search if auto-submission is enabled
            if (devTools.isAutoSubmitting())
            {
                ElasticSearchSender.instance().sendToElasticSearch( harvestedDocuments );
            }
        }
    }


    /**
     * Cleans up a failed harvesting process, allowing a new harvest to be
     * started.
     */
    private void failHarvest( Throwable reason )
    {
    	logger.logException( reason );
        currentHarvestingProcess = null;

        // save to disk if auto-save is enabled
        final DevelopmentTools devTools = DevelopmentTools.instance();
        if (devTools.isAutoSaving() && MainContext.getHarvester() == this)
        {
            devTools.saveHarvestResultToDisk();
        }

        // log failure
        logger.logWarning( String.format( HARVESTER_FAILED, name ) );

        if (MainContext.getHarvester() == this)
        {
            logger.logError( HARVEST_FAILED );
        }
    }


    /**
     * Aborts the harvesting process, allowing a new harvest to be started.
     */
    public void abortHarvest()
    {
        if (currentHarvestingProcess != null)
        {
            currentHarvestingProcess.cancel( true );
        }
    }


    /**
     * Returns the checksum hash of the entries which are to be harvested.
     *
     * @param recalculate if true, recalculates the hash value
     * @return the checksum hash of the entries which are to be harvested
     */
    public String getHash( boolean recalculate )
    {
        if (recalculate)
        {
            hash = calculateHashInternal();
        }
        return hash;
    }


    /**
     * Returns a readable name of the harvester.
     *
     * @return the name of the harvester
     */
    public String getName()
    {
        return getClass().getSimpleName();
    }


    /**
     * Checks if a harvest has finished already.
     *
     * @return true if a harvest was completed
     */
    public boolean isHarvestFinished()
    {
        return !isHarvesting() && harvestFinishedDate != null;
    }
}
