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
package de.gerdiproject.harvest.submission.elasticsearch.json;

import com.google.gson.annotations.SerializedName;

import de.gerdiproject.harvest.submission.elasticsearch.constants.ElasticSearchConstants;

/**
 * This JSON object is part of an ElasticSearch submission response if an error appears.
 *
 * @author Robin Weiss
 */
public class ElasticSearchError
{
    private String type;
    private String reason;

    @SerializedName("caused_by")
    private ElasticSearchErrorCause causedBy;


    public String getType()
    {
        return type;
    }


    public void setType(String type)
    {
        this.type = type;
    }


    public String getReason()
    {
        return reason;
    }


    public void setReason(String reason)
    {
        this.reason = reason;
    }


    public ElasticSearchErrorCause getCausedBy()
    {
        return causedBy;
    }


    public void setCausedBy(ElasticSearchErrorCause causedBy)
    {
        this.causedBy = causedBy;
    }


    @Override
    public String toString()
    {
        String errorText = String.format(
                               ElasticSearchConstants.DOCUMENT_SUBMIT_ERROR_REASON,
                               type,
                               reason);

        if (causedBy != null)
            errorText += String.format(
                             ElasticSearchConstants.DOCUMENT_SUBMIT_ERROR_CAUSE,
                             causedBy.getType(),
                             causedBy.getReason()
                         );

        return errorText;
    }
}
