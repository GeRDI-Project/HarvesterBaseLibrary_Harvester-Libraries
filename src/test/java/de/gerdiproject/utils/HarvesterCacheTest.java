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
package de.gerdiproject.utils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.gerdiproject.harvest.utils.cache.HarvesterCache;

/**
 * This class provides test cases for the {@linkplain HarvesterCache}.
 *
 * @author Robin Weiss
 */
public class HarvesterCacheTest
{
    @Before
    public void before()
    {

    }


    @After
    public void after()
    {

    }


    @Test
    public void test()
    {
        HarvesterCache cache = new HarvesterCache();
        cache.addDocument(doc);
        cache.applyChanges(isSuccessful, isAborted);
        cache.cacheDocument(doc);
        cache.getChangesCache();
        cache.getVersionsCache();
        cache.init(hash, harvestStartIndex, harvestEndIndex);
        cache.skipAllDocuments();
    }
}
