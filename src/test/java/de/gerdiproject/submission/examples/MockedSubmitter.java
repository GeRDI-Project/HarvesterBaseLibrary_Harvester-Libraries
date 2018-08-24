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
package de.gerdiproject.submission.examples;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.gerdiproject.harvest.IDocument;
import de.gerdiproject.harvest.submission.AbstractSubmitter;
import de.gerdiproject.json.datacite.DataCiteJson;

/**
 * @author Robin Weiss
 *
 */
public class MockedSubmitter extends AbstractSubmitter
{
    private List<Integer> submittedIndices = new LinkedList<>();


    public MockedSubmitter()
    {
        super();
    }


    @Override
    protected int getSizeOfDocument(String documentId, IDocument document)
    {
        return 1;
    }


    @Override
    protected void submitBatch(Map<String, IDocument> documents) throws Exception
    {
        documents.forEach((String docId, IDocument doc) ->
                          submittedIndices.add(Integer.valueOf(((DataCiteJson)doc).getPublicationYear())));

    }


    public URL getSubmissionUrl()
    {
        return super.url;
    }


    public int getMaxBatchSize()
    {
        return super.maxBatchSize;
    }


    public String getCredentials()
    {
        return super.credentials;
    }


    public List<Integer> getSubmittedIndices()
    {
        return submittedIndices;
    }
}
