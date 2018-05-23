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

import de.gerdiproject.harvest.utils.HashGenerator;

/**
 * This class provides test cases for the {@linkplain HashGenerator}.
 *
 * @author Robin Weiss
 *
 */
public class HashGeneratorTest
{
    @Test
    public void testHashValue()
    {
        final HashGenerator generator = new HashGenerator(StandardCharsets.UTF_8);
        assertEquals(generator.getShaHash("test"), "a94a8fe5ccb19ba61c4c0873d391e987982fbbd3");
    }
}
