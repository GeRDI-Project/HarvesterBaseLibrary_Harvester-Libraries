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

/**
 * This JSON object is part of an ElasticSearch submission response.
 *
 * @author Robin Weiss
 */
public class ElasticSearchShard
{
    private int total;
    private int successful;
    private int failed;


    public int getTotal()
    {
        return total;
    }


    public int getSuccessful()
    {
        return successful;
    }


    public int getFailed()
    {
        return failed;
    }


    public void setTotal(int total)
    {
        this.total = total;
    }


    public void setSuccessful(int successful)
    {
        this.successful = successful;
    }


    public void setFailed(int failed)
    {
        this.failed = failed;
    }
}
