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
import de.gerdiproject.logger.ILogger;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author row
 */
public abstract class AbstractCompositeHarvester extends AbstractHarvester
{
    // fields and members
    protected final AbstractHarvester[] subHarvesters;


    public AbstractCompositeHarvester( IJsonArray harvestedDocuments, AbstractHarvester[] subHarvesters )
    {
        super( harvestedDocuments );

        this.subHarvesters = subHarvesters;
    }


    @Override
    public void init( ILogger logger )
    {
        // init sub-harvesters first, in order to properly calculate document hash and count
        for (AbstractHarvester subHarvester : subHarvesters)
        {
            subHarvester.init( logger );
        }

        super.init( logger );
    }


    @Override
    public void setRange( int from, int to )
    {
        super.setRange( from, to );

        boolean isBelowRange = true;
        boolean isAboveRange = false;
        int numberOfProcessedDocs = 0;

        for (AbstractHarvester subHarvester : subHarvesters)
        {
            int numberOfSubDocs = subHarvester.getTotalNumberOfDocuments();
            numberOfProcessedDocs += numberOfSubDocs;

            if (isAboveRange)
            {
                // above range: this harvester will be skipped completely
                subHarvester.setRange( 0, 0 );
            }
            else if (from < numberOfProcessedDocs)
            {
                int startIndex = (isBelowRange)
                        ? from - (numberOfProcessedDocs - numberOfSubDocs)
                        : 0;

                boolean isLastEntry = (to < numberOfProcessedDocs);

                int endIndex = (isLastEntry)
                        ? numberOfSubDocs - (numberOfProcessedDocs - to)
                        : numberOfSubDocs;

                subHarvester.setRange( startIndex, endIndex );

                isBelowRange = false;
                isAboveRange = isLastEntry;
            }
            else
            {
                // below range: this harvester will be skipped completely
                subHarvester.setRange( 0, 0 );
            }
        }
    }


	@Override
    @SuppressWarnings("rawtypes")
    protected boolean harvestInternal( int from, int to )
    {
        CompletableFuture[] subProcesses = new CompletableFuture[subHarvesters.length];
        
        int i = 0;
        
        // the range can be ignored at this point, because it was already set in
        // the subharvesters via the overriden setRange() method
        for (AbstractHarvester subHarvester : subHarvesters)
        {
            subHarvester.harvest();
            subProcesses[i++] = subHarvester.currentHarvestingProcess;
        }
        
        // wait for all sub-harvesters to complete
        try
        {
            CompletableFuture.allOf( subProcesses ).get();
        }
        catch (InterruptedException | ExecutionException ex)
        {
            logger.logWarning( "Interrupted Harvester!");
            return false;
        }

        return true;
    }


    @Override
    protected int calculateTotalNumberOfDocumentsInternal()
    {
        int total = 0;
        for (AbstractHarvester subHarvester : subHarvesters)
        {
            total += subHarvester.getTotalNumberOfDocuments();
        }
        return total;
    }


    @Override
    protected String calculateHashInternal()
    {
        // for now, concatenate all hashes
        final StringBuilder hashBuilder = new StringBuilder();
        for (AbstractHarvester subHarvester : subHarvesters)
        {
            hashBuilder.append( subHarvester.getHash( false ) );
        }

        // generate hash of all concatenated hashes
        try
        {
            final MessageDigest md = MessageDigest.getInstance( SHA_HASH_ALGORITHM );
            md.update( hashBuilder.toString().getBytes() );

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


    @Override
    public int getNumberOfHarvestedDocuments()
    {
        int totalNumber = 0;
        for (AbstractHarvester subHarvester : subHarvesters)
        {
            totalNumber += subHarvester.getNumberOfHarvestedDocuments();
        }
        return totalNumber;
    }


    @Override
    public boolean isHarvestFinished()
    {
        for (AbstractHarvester subHarvester : subHarvesters)
        {
            if (!subHarvester.isHarvestFinished())
            {
                return false;
            }
        }
        return true;
    }


    @Override
    public void abortHarvest()
    {
        for (AbstractHarvester subHarvester : subHarvesters)
        {
            subHarvester.abortHarvest();
        }
        super.abortHarvest(); 
    }
    
    

}
