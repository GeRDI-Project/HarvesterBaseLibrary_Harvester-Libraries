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

import de.gerdiproject.harvest.utils.FileUtils;

/**
 * This class serves as a base for all unit tests that generate files for the purpose of testing.
 * It provides some convenience functions before and after tests for cleaning up
 * possible temporary files and removing event listeners.
 *
 * @author Robin Weiss
 */
public abstract class AbstractFileSystemUnitTest<T> extends AbstractUnitTest<T>
{
    private static final File TEST_FOLDER = new File("mocked");
    private static final String CLEANUP_ERROR = "Could not delete temporary test diectory: " + TEST_FOLDER;

    protected final File testFolder = new File(TEST_FOLDER, getTestedClass().getSimpleName());


    /**
     * Removes the test folder and validates if it really
     * has been deleted.
     * Also creates an instance of the tested object.
     *
     * @throws IOException thrown when the test folder could not be deleted
     */
    @Override
    public void before() throws InstantiationException
    {
        FileUtils.deleteFile(TEST_FOLDER);

        if (TEST_FOLDER.exists())
            throw new InstantiationException(CLEANUP_ERROR);

        super.before();
    }


    /**
     * Removes event listeners of the tested object if applicable.
     * Deletes the test folder to free up some space.
     */
    @Override
    public void after()
    {
        super.after();

        FileUtils.deleteFile(TEST_FOLDER);
    }
}
