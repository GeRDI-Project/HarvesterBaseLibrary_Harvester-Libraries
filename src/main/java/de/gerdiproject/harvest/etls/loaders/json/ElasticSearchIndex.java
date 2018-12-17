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
import lombok.Data;

/**
 * This JSON object is part of an ElasticSearch submission response.
 *
 * @author Robin Weiss
 */
@Data
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
}
