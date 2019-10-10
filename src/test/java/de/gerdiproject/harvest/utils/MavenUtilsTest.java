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
package de.gerdiproject.harvest.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import de.gerdiproject.harvest.AbstractObjectUnitTest;
import de.gerdiproject.harvest.application.MainContext;
import de.gerdiproject.harvest.utils.maven.MavenUtils;
import de.gerdiproject.json.datacite.DataCiteJson;

/**
 * This class provides test cases for {@linkplain MavenUtils}.
 *
 * @author Robin Weiss
 */
public class MavenUtilsTest extends AbstractObjectUnitTest<MavenUtils>
{
    @Override
    protected MavenUtils setUpTestObjects()
    {
        return new MavenUtils(MainContext.class);
    }


    /**
     * Tests if the jar names from classes of different jars differ as well.
     */
    @Test
    public void testJarNames()
    {
        final String harvestLibraryJarName = testedObject.getHarvesterJarName();
        final String jsonLibraryJarName = new MavenUtils(DataCiteJson.class).getHarvesterJarName();

        assertNotEquals("The method getHarvesterJarName() should return a JAR-name that depends on the JAR file to which a class belongs!",
                        harvestLibraryJarName,
                        jsonLibraryJarName);
    }


    /**
     * Tests if the list of dependencies is not empty.
     */
    @Test
    public void testDependencies()
    {
        assertFalse("The method getMavenVersionInfo() should not return an empty list!",
                    testedObject.getMavenVersionInfo(null).isEmpty());
    }
}
