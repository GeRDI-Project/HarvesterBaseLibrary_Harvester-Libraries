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
package de.gerdiproject.harvest.etls.loaders.json;

import com.google.gson.annotations.SerializedName;

import de.gerdiproject.harvest.etls.loaders.constants.ElasticSearchConstants;

/**
 * This JSON object is part of an ElasticSearch submission response.
 *
 * @author Robin Weiss
 */
public class ElasticSearchIndex
{
    @SerializedName("_index")
    private String index;


    @SerializedName("_type")
    private String type;


    @SerializedName("_id")
    private String id;

    @SerializedName("_version")
    private int version;

    @SerializedName("_shards")
    private ElasticSearchShard shards;

    private String result;
    private String status;
    private boolean created;
    private ElasticSearchError error;


    /**
     * Returns a human readable version of a possible error.
     *
     * @return a human readable version of the error or an empty string, if no
     *         errors occurred
     */
    public String getErrorText()
    {
        if (error != null)
            return String.format(
                       ElasticSearchConstants.LOAD_DOCUMENT_ERROR,
                       id,
                       error.toString());
        else
            return "";
    }


    public String getIndex()
    {
        return index;
    }


    public String getType()
    {
        return type;
    }


    public String getId()
    {
        return id;
    }


    public String getResult()
    {
        return result;
    }


    public String getStatus()
    {
        return status;
    }


    public boolean isCreated()
    {
        return created;
    }


    public int getVersion()
    {
        return version;
    }


    public ElasticSearchShard getShards()
    {
        return shards;
    }


    public ElasticSearchError getError()
    {
        return error;
    }


    public void setIndex(String index)
    {
        this.index = index;
    }


    public void setType(String type)
    {
        this.type = type;
    }


    public void setId(String id)
    {
        this.id = id;
    }


    public void setVersion(int version)
    {
        this.version = version;
    }


    public void setShards(ElasticSearchShard shards)
    {
        this.shards = shards;
    }


    public void setResult(String result)
    {
        this.result = result;
    }


    public void setStatus(String status)
    {
        this.status = status;
    }


    public void setCreated(boolean created)
    {
        this.created = created;
    }


    public void setError(ElasticSearchError error)
    {
        this.error = error;
    }
}
