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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.After;
import org.junit.Test;

import com.google.gson.Gson;

import de.gerdiproject.harvest.utils.data.DiskIO;
import de.gerdiproject.mocked.MockedObject;


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

    private static final Gson GSON = new Gson();


    /**
     * Removes test files.
     * @throws IOException thrown when the temporary files could not be deleted
     */
    @After
    public void after() throws IOException
    {
        boolean allDeleted = true;

        allDeleted &= !STRING_TEST_FILE.exists() || STRING_TEST_FILE.delete();
        allDeleted &= !OBJECT_TEST_FILE.exists() || OBJECT_TEST_FILE.delete();

        if (!allDeleted)
            throw new IOException();
    }


    /**
     * Tests if retrieved strings differ between {@linkplain DiskIO}s with different charsets.
     */
    @Test
    public void testCharsets()
    {
        DiskIO diskIoUtf8 = new DiskIO(GSON, StandardCharsets.UTF_8);
        diskIoUtf8.writeStringToFile(STRING_TEST_FILE, TEST_STRING);
        final String retrievedUtf8 = diskIoUtf8.getString(STRING_TEST_FILE);

        DiskIO diskIoAscii = new DiskIO(GSON, StandardCharsets.US_ASCII);
        final String retrievedAscii = diskIoAscii.getString(STRING_TEST_FILE);

        assertNotEquals(retrievedUtf8, retrievedAscii);
    }


    /**
     * Tests if strings are written to a file, and
     * if they are the same when being read.
     */
    @Test
    public void testWritingAndReadingStrings()
    {
        assertFalse(STRING_TEST_FILE.exists());

        DiskIO diskIo = new DiskIO(GSON, StandardCharsets.UTF_8);
        diskIo.writeStringToFile(STRING_TEST_FILE, TEST_STRING);

        assertTrue(STRING_TEST_FILE.exists());
        assertEquals(diskIo.getString(STRING_TEST_FILE), TEST_STRING);
    }


    /**
     * Tests if files are completely overwritten when strings are written.
     */
    @Test
    public void testOverwritingStrings()
    {
        DiskIO diskIo = new DiskIO(GSON, StandardCharsets.UTF_8);
        diskIo.writeStringToFile(STRING_TEST_FILE, TEST_STRING);

        assertTrue(STRING_TEST_FILE.exists());
        final String firstReadString = diskIo.getString(STRING_TEST_FILE);
        assertEquals(firstReadString, TEST_STRING);

        diskIo.writeStringToFile(STRING_TEST_FILE, TEST_STRING);

        final String secondReadString = diskIo.getString(STRING_TEST_FILE);
        assertEquals(firstReadString, secondReadString);
    }


    /**
     * Tests if objects are written to a file, an
     * if they are the same when being read.
     */
    @Test
    public void testWritingAndReadingObjects()
    {
        assertFalse(OBJECT_TEST_FILE.exists());

        MockedObject testObject = new MockedObject(OBJECT_TEST_STRING, OBJECT_TEST_INT);
        DiskIO diskIo = new DiskIO(GSON, StandardCharsets.UTF_8);
        diskIo.writeObjectToFile(OBJECT_TEST_FILE, testObject);

        assertTrue(OBJECT_TEST_FILE.exists());
        final MockedObject readObject = diskIo.getObject(OBJECT_TEST_FILE, testObject.getClass());
        assertEquals(readObject, testObject);
    }


    /**
     * Tests if files are completely overwritten when objects are written.
     */
    @Test
    public void testOverwritingObjects()
    {
        MockedObject testObject = new MockedObject(OBJECT_TEST_STRING, OBJECT_TEST_INT);
        DiskIO diskIo = new DiskIO(GSON, StandardCharsets.UTF_8);
        diskIo.writeObjectToFile(OBJECT_TEST_FILE, testObject);

        assertTrue(OBJECT_TEST_FILE.exists());
        final MockedObject firstReadObject = diskIo.getObject(OBJECT_TEST_FILE, testObject.getClass());

        assertEquals(firstReadObject, testObject);

        diskIo.writeObjectToFile(OBJECT_TEST_FILE, testObject);

        final MockedObject secondReadObject = diskIo.getObject(OBJECT_TEST_FILE, testObject.getClass());
        assertEquals(firstReadObject, secondReadObject);
    }
}
