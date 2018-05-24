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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.utils.maven.MavenUtils;
import de.gerdiproject.json.datacite.DataCiteJson;

/**
 * This class provides test cases for {@linkplain MavenUtils}.
 *
 * @author Robin Weiss
 *
 */
public class MavenUtilsTest
{
    /**
     * Tests if the jar names from classes of different jars differ as well.
     */
    @Test
    public void testJarNames()
    {
        final String harvestLibraryJarName = new MavenUtils(MainContext.class).getHarvesterJarName();
        final String jsonLibraryJarName = new MavenUtils(DataCiteJson.class).getHarvesterJarName();

        assertNotEquals(harvestLibraryJarName, jsonLibraryJarName);
    }


    /**
     * Tests if the list of dependencies is not empty.
     */
    @Test
    public void testDependencies()
    {
        final MavenUtils utils = new MavenUtils(MainContext.class);
        assertFalse(utils.getMavenVersionInfo(null).isEmpty());
    }
}
