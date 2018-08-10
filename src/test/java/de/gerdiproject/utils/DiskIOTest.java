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
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;

import de.gerdiproject.harvest.utils.FileUtils;
import de.gerdiproject.harvest.utils.data.DiskIO;
import de.gerdiproject.utils.examples.diskio.MockedObject;


/**
 * This class provides test cases for {@linkplain DiskIO}s.
 *
 * @author Robin Weiss
 */
public class DiskIOTest
{
    private static final String TEST_STRING = "Döner macht schöner!";
    private static final File STRING_TEST_FILE = new File("mocked/testDiskIoString.json");

    private static final File OBJECT_TEST_FILE = new File("mocked/testDiskIoObject.json");
    private static final int OBJECT_TEST_INT = 1337;
    private static final String OBJECT_TEST_STRING = "Test String in Map";
    private static final String CLEAN_UP_ERROR = "Could not remove test file: ";

    private DiskIO diskIoUtf8;


    /**
     * Removes test files and verifies that they have been deleted.
     * Creates a UTF-8 {@linkplain DiskIO} for testing.
     *
     * @throws IOException thrown when temporary files could not be deleted
     */
    @Before
    public void before() throws IOException
    {
        FileUtils.deleteFile(STRING_TEST_FILE);
        FileUtils.deleteFile(OBJECT_TEST_FILE);

        if (STRING_TEST_FILE.exists())
            throw new IOException(CLEAN_UP_ERROR + STRING_TEST_FILE.getAbsolutePath());

        if (OBJECT_TEST_FILE.exists())
            throw new IOException(CLEAN_UP_ERROR + STRING_TEST_FILE.getAbsolutePath());

        diskIoUtf8 = new DiskIO(new Gson(), StandardCharsets.UTF_8);
    }


    /**
     * Removes test files.
     */
    @After
    public void after()
    {
        diskIoUtf8 = null;

        FileUtils.deleteFile(STRING_TEST_FILE);
        FileUtils.deleteFile(OBJECT_TEST_FILE);
    }


    /**
     * Tests if retrieved strings differ between {@linkplain DiskIO}s with different charsets.
     */
    @Test
    public void testCharsets()
    {
        diskIoUtf8.writeStringToFile(STRING_TEST_FILE, TEST_STRING);
        final String retrievedUtf8 = diskIoUtf8.getString(STRING_TEST_FILE);

        final DiskIO diskIoAscii = new DiskIO(new Gson(), StandardCharsets.US_ASCII);
        final String retrievedAscii = diskIoAscii.getString(STRING_TEST_FILE);

        assertNotEquals(retrievedUtf8, retrievedAscii);
    }


    /**
     * Tests if writing strings to a file, causes the file
     * to be created.
     */
    @Test
    public void testWritingStrings()
    {
        diskIoUtf8.writeStringToFile(STRING_TEST_FILE, TEST_STRING);

        assert STRING_TEST_FILE.exists();
    }


    /**
     * Tests if strings thate are written to a file, are the same when being read.
     */
    @Test
    public void testReadingStrings()
    {
        diskIoUtf8.writeStringToFile(STRING_TEST_FILE, TEST_STRING);

        assertEquals(diskIoUtf8.getString(STRING_TEST_FILE), TEST_STRING);
    }


    /**
     * Tests if strings overwrite files instead of being appended to them.
     */
    @Test
    public void testOverwritingStrings()
    {
        diskIoUtf8.writeStringToFile(STRING_TEST_FILE, TEST_STRING);
        final String firstReadString = diskIoUtf8.getString(STRING_TEST_FILE);

        diskIoUtf8.writeStringToFile(STRING_TEST_FILE, TEST_STRING);
        final String secondReadString = diskIoUtf8.getString(STRING_TEST_FILE);

        assertEquals(firstReadString, secondReadString);
    }


    /**
     * Tests if objects that are written to a file, cause the file to be created.
     */
    @Test
    public void testWritingObjects()
    {
        final MockedObject testObject = new MockedObject(OBJECT_TEST_STRING, OBJECT_TEST_INT);
        diskIoUtf8.writeObjectToFile(OBJECT_TEST_FILE, testObject);

        assertTrue(OBJECT_TEST_FILE.exists());
    }


    /**
     * Tests if objects that are written to a file, are the same when being read.
     */
    @Test
    public void testReadingObjects()
    {
        final MockedObject testObject = new MockedObject(OBJECT_TEST_STRING, OBJECT_TEST_INT);
        diskIoUtf8.writeObjectToFile(OBJECT_TEST_FILE, testObject);

        final MockedObject readObject = diskIoUtf8.getObject(OBJECT_TEST_FILE, testObject.getClass());
        assertEquals(readObject, testObject);
    }


    /**
     * Tests if files are completely written anew when objects are written to disk.
     */
    @Test
    public void testOverwritingObjects()
    {
        // write object
        final MockedObject writtenObject = new MockedObject(OBJECT_TEST_STRING, OBJECT_TEST_INT);
        diskIoUtf8.writeObjectToFile(OBJECT_TEST_FILE, writtenObject);

        // write the same object again
        final MockedObject firstReadObject = diskIoUtf8.getObject(OBJECT_TEST_FILE, writtenObject.getClass());
        diskIoUtf8.writeObjectToFile(OBJECT_TEST_FILE, writtenObject);

        // read the object
        final MockedObject secondReadObject = diskIoUtf8.getObject(OBJECT_TEST_FILE, writtenObject.getClass());
        assertEquals(firstReadObject, secondReadObject);
    }
}
