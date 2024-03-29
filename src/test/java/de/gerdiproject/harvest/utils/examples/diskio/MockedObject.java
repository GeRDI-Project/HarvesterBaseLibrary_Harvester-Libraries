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
package de.gerdiproject.harvest.utils.examples.diskio;

/**
 * This is some arbitrary object used for testing JSON (de-)serialization.
 *
 * @author Robin Weiss
 */
public class MockedObject
{
    public final String testStr;
    public final int testInt;

    /**
     * Constructor that sets stuff.
     *
     * @param testStr some stuff
     * @param testInt some more stuff
     */
    public MockedObject(String testStr, int testInt)
    {
        this.testStr = testStr;
        this.testInt = testInt;
    }


    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof MockedObject))
            return false;

        final MockedObject other = (MockedObject) obj;
        return testInt == other.testInt && testStr.equals(other.testStr);
    }


    @Override
    public int hashCode()
    {
        return testStr.hashCode() + testInt;
    }
}
