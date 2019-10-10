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
package de.gerdiproject.harvest;

import java.io.File;

import de.gerdiproject.harvest.utils.file.FileUtils;

/**
 * This class serves as a base for all unit tests that generate files for the purpose of testing.
 * It provides some convenience functions before and after tests for cleaning up
 * possible temporary files and removing event listeners.
 *
 * @author Robin Weiss
 */
public abstract class AbstractFileSystemUnitTest<T> extends AbstractObjectUnitTest<T>
{
    private static final String CLEANUP_ERROR = "Could not delete temporary test directory: " + TEST_FOLDER;

    protected final File testFolder = new File(TEST_FOLDER, getTestedClass().getSimpleName());


    /**
     * Removes the test folder and validates if it really
     * has been deleted.
     * Also creates an instance of the tested object.
     *
     * @throws InstantiationException thrown when the test folder could not be deleted
     */
    @Override
    public void before() throws InstantiationException
    {
        // clean up temp files before tests to prevent wrong preconditions
        FileUtils.deleteFile(TEST_FOLDER);

        if (TEST_FOLDER.exists())
            throw new InstantiationException(CLEANUP_ERROR);

        super.before();
    }


    @Override
    public void after()
    {
        super.after();

        // clean up temp files after tests to free up some space
        FileUtils.deleteFile(TEST_FOLDER);
    }
}
