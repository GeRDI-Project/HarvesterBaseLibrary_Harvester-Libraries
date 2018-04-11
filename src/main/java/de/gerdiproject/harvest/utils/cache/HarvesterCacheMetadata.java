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
package de.gerdiproject.harvest.utils.cache;

import de.gerdiproject.harvest.harvester.AbstractHarvester;
import de.gerdiproject.json.GsonUtils;

/**
 * This object is part of the JSON representation of the
 * {@linkplain DocumentVersionsCache}. It contains information about the
 * {@linkplain AbstractHarvester} that is linked to the cache, and is used to
 * determine whether documents need to be harvested again.
 * 
 * @author Robin Weiss
 */
public class HarvesterCacheMetadata
{
    private String sourceHash;
    private int rangeFrom;
    private int rangeTo;


    /**
     * Constructor that initializes fields with invalid values.
     */
    public HarvesterCacheMetadata()
    {
        this.sourceHash = null;
        this.rangeFrom = Integer.MAX_VALUE;
        this.rangeTo = Integer.MIN_VALUE;
    }


    /**
     * Compares this metadata to checks if this meta requires an update which
     * may occur because the range of the new metadata is greater or the hash
     * value is different.
     * 
     * @param newMetadata the metadata to which this metadata is compared
     * 
     * @return true if this metadata is a strict subset of the newMetadata
     */
    public boolean isUpdateNeeded(final HarvesterCacheMetadata newMetadata)
    {
        return sourceHash == null
                || rangeFrom > newMetadata.rangeFrom
                || rangeTo < newMetadata.rangeTo
                || !sourceHash.equals(newMetadata.sourceHash);
    }


    /**
     * Copies the values of another {@link HarvesterCacheMetadata} object.
     * 
     * @param other the object of which the values are copied
     */
    public void set(final HarvesterCacheMetadata other)
    {
        this.rangeFrom = other.rangeFrom;
        this.rangeTo = other.rangeTo;
        this.sourceHash = other.sourceHash;
    }


    /**
     * Sets the source hash value.
     * 
     * @param sourceHash a value that serves as a version checksum of all
     *            possible harvested source data
     */
    public void setSourceHash(String sourceHash)
    {
        this.sourceHash = sourceHash;
    }


    /**
     * Sets the harvesting range start index.
     * 
     * @param rangeFrom the start index of the harvesting range
     */
    public void setRangeFrom(int rangeFrom)
    {
        this.rangeFrom = rangeFrom;
    }


    /**
     * Sets the harvesting range end index.
     * 
     * @param rangeTo the exclusive end index of the harvesting range
     */
    public void setRangeTo(int rangeTo)
    {
        this.rangeTo = rangeTo;
    }


    @Override
    public String toString()
    {
        return GsonUtils.getGson().toJson(this);
    }

}
