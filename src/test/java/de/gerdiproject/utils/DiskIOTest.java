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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import com.google.gson.Gson;

import de.gerdiproject.AbstractFileSystemUnitTest;
import de.gerdiproject.harvest.utils.data.DiskIO;
import de.gerdiproject.utils.examples.diskio.MockedObject;


/**
 * This class provides test cases for {@linkplain DiskIO}s.
 *
 * @author Robin Weiss
 */
public class DiskIOTest extends AbstractFileSystemUnitTest<DiskIO>
{
    private static final String TEST_STRING = "Döner macht schöner!";
    private static final int OBJECT_TEST_INT = 1337;
    private static final String OBJECT_TEST_STRING = "Test String in Map";

    private final File testStringFile = new File(testFolder, "testDiskIoString.json");
    private final File testObjectFile = new File(testFolder, "testDiskIoObject.json");


    @Override
    protected DiskIO setUpTestObjects()
    {
        return new DiskIO(new Gson(), StandardCharsets.UTF_8);
    }


    /**
     * Tests if retrieved strings differ between {@linkplain DiskIO}s with different charsets.
     */
    @Test
    public void testCharsets()
    {
        testedObject.writeStringToFile(testStringFile, TEST_STRING);
        final String retrievedUtf8 = testedObject.getString(testStringFile);

        final DiskIO diskIoAscii = new DiskIO(new Gson(), StandardCharsets.US_ASCII);
        final String retrievedAscii = diskIoAscii.getString(testStringFile);

        assertNotEquals("Reading '" + TEST_STRING + "' in different charsets should result in different strings!",
                        retrievedUtf8,
                        retrievedAscii);
    }


    /**
     * Tests if writing strings to a file, causes the file
     * to be created.
     */
    @Test
    public void testWritingStrings()
    {
        testedObject.writeStringToFile(testStringFile, TEST_STRING);

        assertTrue("The method writeStringToFile() should create a new file!",
                   testStringFile.exists());
    }


    /**
     * Tests if strings that are written to a file, are the same when being read.
     */
    @Test
    public void testReadingStrings()
    {
        testedObject.writeStringToFile(testStringFile, TEST_STRING);

        assertEquals("The method getString() should return the same string that was written in writeStringToFile()!",
                     testedObject.getString(testStringFile),
                     TEST_STRING);
    }


    /**
     * Tests if strings overwrite files instead of being appended to them.
     */
    @Test
    public void testOverwritingStrings()
    {
        testedObject.writeStringToFile(testStringFile, TEST_STRING);
        final String firstReadString = testedObject.getString(testStringFile);

        testedObject.writeStringToFile(testStringFile, TEST_STRING);
        final String secondReadString = testedObject.getString(testStringFile);

        assertEquals("Calling writeStringToFile() multiple times should simply overwrite the file content!",
                     firstReadString,
                     secondReadString);
    }


    /**
     * Tests if objects that are written to a file, cause the file to be created.
     */
    @Test
    public void testWritingObjects()
    {
        final MockedObject testObject = new MockedObject(OBJECT_TEST_STRING, OBJECT_TEST_INT);
        testedObject.writeObjectToFile(testObjectFile, testObject);

        assertTrue("The method writeObjectToFile() should create a new file!",
                   testObjectFile.exists());
    }


    /**
     * Tests if objects that are written to a file, are the same when being read.
     */
    @Test
    public void testReadingObjects()
    {
        final MockedObject testObject = new MockedObject(OBJECT_TEST_STRING, OBJECT_TEST_INT);
        testedObject.writeObjectToFile(testObjectFile, testObject);

        final MockedObject readObject = testedObject.getObject(testObjectFile, testObject.getClass());
        assertEquals("The method getObject() should return the same string that was written in writeObjectToFile()!",
                     readObject,
                     testObject);
    }


    /**
     * Tests if files are completely written anew when objects are written to disk.
     */
    @Test
    public void testOverwritingObjects()
    {
        // write object
        final MockedObject writtenObject = new MockedObject(OBJECT_TEST_STRING, OBJECT_TEST_INT);
        testedObject.writeObjectToFile(testObjectFile, writtenObject);

        // write the same object again
        final MockedObject firstReadObject = testedObject.getObject(testObjectFile, writtenObject.getClass());
        testedObject.writeObjectToFile(testObjectFile, writtenObject);

        // read the object
        final MockedObject secondReadObject = testedObject.getObject(testObjectFile, writtenObject.getClass());
        assertEquals("Calling writeObjectToFile() multiple times should simply overwrite the file content!",
                     firstReadObject,
                     secondReadObject);
    }
}
