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

import java.nio.charset.StandardCharsets;

import org.junit.Test;

import de.gerdiproject.AbstractUnitTest;
import de.gerdiproject.harvest.utils.HashGenerator;

/**
 * This class provides test cases for the {@linkplain HashGenerator}.
 *
 * @author Robin Weiss
 *
 */
public class HashGeneratorTest extends AbstractUnitTest<HashGenerator>
{
    private static final String INPUT_VALUE = "test";
    private static final String HASHED_INPUT_VALUE = "a94a8fe5ccb19ba61c4c0873d391e987982fbbd3";
    private static final String ASSERT_MESSAGE = "The method getShaHash(\"" + INPUT_VALUE + "\") should return:" + HASHED_INPUT_VALUE;


    @Override
    protected HashGenerator setUpTestObjects()
    {
        return new HashGenerator(StandardCharsets.UTF_8);
    }


    /**
     * Tests if the getShaHash() method returns the expected SHA hash of an input value.
     */
    @Test
    public void testHashValue()
    {
        assertEquals(ASSERT_MESSAGE,
                     HASHED_INPUT_VALUE,
                     testedObject.getShaHash(INPUT_VALUE));
    }
}
