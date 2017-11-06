/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
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
package de.gerdiproject.harvest.harvester.constants;

/**
 * This static class is a collection of constants that are used by harvesters.
 *
 * @author Robin Weiss
 */
public class HarvesterConstants
{
    // HASH GENERATION
    public static final String HASH_CREATION_FAILED = "Failed to create hash for %s!";
    public static final String OCTAT_FORMAT = "%02x";
    public static final String SHA_HASH_ALGORITHM = "SHA";

    // ALL HARVESTERS
    public static final String HARVESTER_START = "Starting %s...";
    public static final String HARVESTER_END = "%s finished!";
    public static final String HARVESTER_FAILED = "%s failed!";
    public static final String HARVESTER_ABORTED = "%s aborted!";

    // LIST HARVESTER
    public static final String ERROR_NO_ENTRIES = "Cannot harvest %s - The source entries are empty or could not be retrieved!";
    public static final String LOG_OUT_OF_RANGE = "Skipping %s - Document indices out of range.";

    // REST
    public static final String REST_INFO = "- %s -%n%n%s%n%nRange:\t\t%s-%s%n%n"
                                           + "POST\t\t\tStarts the harvest%n"
                                           + "POST/abort\t\tAborts an ongoing harvest%n"
                                           + "POST/submit\t\tSubmits harvested documents to a DataBase%n"
                                           + "POST/save\t\tSaves harvested documents to disk";

    /**
     * Private constructor, because this is a static class.
     */
    private HarvesterConstants()
    {

    }
}
