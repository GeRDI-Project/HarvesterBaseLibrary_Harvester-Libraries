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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.config.parameters.BooleanParameter;
import de.gerdiproject.harvest.config.parameters.IntegerParameter;
import de.gerdiproject.harvest.config.parameters.ParameterCategory;
import de.gerdiproject.harvest.state.IState;
import de.gerdiproject.harvest.state.impl.ErrorState;
import de.gerdiproject.harvest.state.impl.IdleState;
import de.gerdiproject.harvest.state.impl.InitializationState;
import de.gerdiproject.harvest.state.impl.SubmittingState;

/**
 * This static class is a collection of constants that are used by harvesters.
 *
 * @author Robin Weiss
 */
public class HarvesterConstants
{
    // PARAMETERS
    public static final List<Class<? extends IState>> HARVESTER_PARAMETER_ALLOWED_STATES =
        Collections.unmodifiableList(
            Arrays.asList(
                InitializationState.class,
                ErrorState.class,
                IdleState.class,
                SubmittingState.class));

    public static final ParameterCategory PARAMETER_CATEGORY = new ParameterCategory(
        "Harvester", HARVESTER_PARAMETER_ALLOWED_STATES);

    public static final BooleanParameter FORCED_PARAM = new BooleanParameter(
        "forced",
        PARAMETER_CATEGORY,
        false);

    public static final IntegerParameter START_INDEX_PARAM = new IntegerParameter(
        "rangeFrom",
        PARAMETER_CATEGORY,
        0);

    public static final IntegerParameter END_INDEX_PARAM = new IntegerParameter(
        "rangeTo",
        PARAMETER_CATEGORY,
        Integer.MAX_VALUE);

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
        + FORCED_PARAM.getCompositeKey()
        + "' flag to true when harvesting.";

    // LIST HARVESTER
    public static final String ERROR_NO_ENTRIES =
        "Cannot harvest %s - The source entries are empty or could not be retrieved!";
    public static final String LOG_OUT_OF_RANGE = "Skipping %s - Document indices out of range.";

    // REST
    public static final String REST_INFO = "- %s -%n%n%s%n%nRange:  %s-%s%n%n"
                                           + "GET/outdated  Checks if there is unharvested metadata%n"
                                           + "POST          Starts the harvest%n"
                                           + "POST/abort    Aborts an ongoing harvest, saving, or submission%n"
                                           + "POST/submit   Submits harvested documents to a DataBase%n"
                                           + "POST/download Downloads harvested documents to disk%n"
                                           + "POST/reset    Attempts to re-initialize this service%n"
                                           + "%n"
                                           + "GET/config    Displays a table of parameters and a means of%n"
                                           + "              configuring them%n"
                                           + "GET/status    Additional GET requests for retrieving concrete%n"
                                           + "              harvester status values%n"
                                           + "GET/schedule  Displays a configurable set of cron jobs that%n"
                                           + "              can trigger harvests automatically%n"
                                           + "GET/log       Displays the server log. The query parameters%n"
                                           + "              'date', 'class', and 'level' can be used to%n"
                                           + "              filter the log with comma-separated values of%n"
                                           + "              dates, logger classes, and log levels.";
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
