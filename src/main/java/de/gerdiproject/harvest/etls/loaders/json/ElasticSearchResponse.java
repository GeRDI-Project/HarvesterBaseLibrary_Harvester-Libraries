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

import java.util.List;


/**
 * This JSON object represents the response of an ElasticSearch submission.
 *
 * @author Robin Weiss
 */
public class ElasticSearchResponse
{
    private int took;
    private boolean errors;
    private List<ElasticSearchIndexWrapper> items;


    public int getTook()
    {
        return took;
    }


    public boolean hasErrors()
    {
        return errors;
    }


    public List<ElasticSearchIndexWrapper> getItems()
    {
        return items;
    }


    public void setTook(int took)
    {
        this.took = took;
    }


    public void setErrors(boolean errors)
    {
        this.errors = errors;
    }


    public void setItems(List<ElasticSearchIndexWrapper> items)
    {
        this.items = items;
    }
}
