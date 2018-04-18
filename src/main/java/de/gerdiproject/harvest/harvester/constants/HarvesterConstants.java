/**
 * Copyright © 2017 Robin Weiss (http://www.gerdi-project.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.gerdiproject.harvest.harvester.constants;

import de.gerdiproject.harvest.config.constants.ConfigurationConstants;

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
    public static final String HARVESTER_SKIPPED_OUTDATED = "Skipping %s, because no changes were detected!";
    public static final String HARVESTER_SKIPPED_SUBMIT =
        "Cancelling %s, because previous changes have not been submitted!%n"
        + "Either /submit the current index or set the '"
        + ConfigurationConstants.FORCE_HARVEST
        + "' flag to true when harvesting.";

    // LIST HARVESTER
    public static final String ERROR_NO_ENTRIES =
        "Cannot harvest %s - The source entries are empty or could not be retrieved!";
    public static final String LOG_OUT_OF_RANGE = "Skipping %s - Document indices out of range.";

    // REST
    public static final String REST_INFO = "- %s -%n%n%s%n%nRange:  %s-%s%n%n"
                                           + "GET/outdated Checks if there is unharvested metadata%n"
                                           + "POST         Starts the harvest%n"
                                           + "POST/abort   Aborts an ongoing harvest, saving, or submission%n"
                                           + "POST/submit  Submits harvested documents to a DataBase%n"
                                           + "POST/save    Saves harvested documents to disk";
    public static final String UNKNOWN_NUMBER = "???";
    public static final String MAX_RANGE_NUMBER = "%d (" + ConfigurationConstants.INTEGER_VALUE_MAX + ")";


    /**
     * Private constructor, because this class just serves as a place to define
     * constants.
     */
    private HarvesterConstants()
    {
    }
}
