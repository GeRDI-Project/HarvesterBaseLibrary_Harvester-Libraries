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
package de.gerdiproject;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;

import org.junit.After;
import org.junit.Before;

import de.gerdiproject.harvest.event.IEventListener;
import de.gerdiproject.harvest.utils.FileUtils;

/**
 * This class serves as a base for all unit tests that generate files for the purpose of testing.
 * It provides some convenience functions before and after tests for cleaning up
 * possible temporary files and removing event listeners.
 *
 * @author Robin Weiss
 */
public abstract class AbstractFileSystemUnitTest<T> extends AbstractUnitTest
{
    private static final File TEST_FOLDER = new File("mocked");
    private static final String CLEANUP_ERROR = "Could not delete temporary test diectory: " + TEST_FOLDER;

    protected final File testFolder = getTestSubFolder();
    protected T testedObject;


    /**
     * Removes the test folder and validates if it really
     * has been deleted.
     * Also creates an instance of the tested object.
     *
     * @throws IOException thrown when the test folder could not be deleted
     */
    @Before
    public void before() throws IOException
    {
        FileUtils.deleteFile(TEST_FOLDER);

        if (TEST_FOLDER.exists())
            throw new IOException(CLEANUP_ERROR);

        testedObject = setUpTestObjects();
    }


    /**
     * Removes event listeners of the tested object if applicable.
     * Deletes the test folder to free up some space.
     */
    @After
    public void after()
    {
        if (testedObject instanceof IEventListener)
            ((IEventListener) testedObject).removeEventListeners();

        testedObject = null;
        FileUtils.deleteFile(TEST_FOLDER);
    }


    /**
     * Sets up all objects that are required for the following test
     * and returns the main tested object.
     *
     * @return the main tested object
     */
    protected abstract T setUpTestObjects();

    /**
     * Sets up a folder as a child of the global test folder.
     * The sub-folder is named after the tested class.
     *
     * @return a dedicated folder for the tested object
     */
    private File getTestSubFolder()
    {
        @SuppressWarnings("unchecked")
        Class<T> testedObjectClass = (Class<T>)((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];

        return new File(TEST_FOLDER, testedObjectClass.getSimpleName());
    }
}
