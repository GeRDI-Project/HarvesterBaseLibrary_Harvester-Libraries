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
package de.gerdiproject.harvest.etls.constants;

import de.gerdiproject.harvest.config.parameters.BooleanParameter;
import de.gerdiproject.harvest.config.parameters.constants.ParameterConstants;
import de.gerdiproject.harvest.config.parameters.constants.ParameterMappingFunctions;
import de.gerdiproject.harvest.etls.AbstractIteratorETL;
import de.gerdiproject.harvest.etls.extractors.AbstractIteratorExtractor;
import de.gerdiproject.harvest.etls.loaders.AbstractIteratorLoader;
import de.gerdiproject.harvest.etls.transformers.AbstractIteratorTransformer;
import de.gerdiproject.harvest.etls.utils.ETLManager;
import de.gerdiproject.harvest.utils.file.constants.FileConstants;

/**
 * This static class is a collection of constants that are used by harvesters.
 *
 * @author Robin Weiss
 */
public class ETLConstants
{
    // PARAMETERS
    public static final String PARAMETER_CATEGORY = "AllETLs";

    public static final BooleanParameter FORCED_PARAM =
        new BooleanParameter(
        "forced",
        PARAMETER_CATEGORY,
        true,
        ParameterMappingFunctions.createMapperForETLs(ParameterMappingFunctions::mapToBoolean));

    public static final BooleanParameter ENABLED_PARAM =
        new BooleanParameter(
        "enabled",
        PARAMETER_CATEGORY,
        true,
        ParameterMappingFunctions.createMapperForETLs(ParameterMappingFunctions::mapToBoolean));

    public static final BooleanParameter CONCURRENT_PARAM =
        new BooleanParameter(
        "concurrentHarvest",
        PARAMETER_CATEGORY,
        false,
        ParameterMappingFunctions.createMapperForETLs(ParameterMappingFunctions::mapToBoolean));


    public static final String START_INDEX_PARAM_KEY = "rangeFrom";
    public static final int START_INDEX_PARAM_DEFAULT_VALUE = 0;

    public static final String END_INDEX_PARAM_KEY = "rangeTo";
    public static final int END_INDEX_PARAM_DEFAULT_VALUE = Integer.MAX_VALUE;


    // HASH GENERATION
    public static final String HASH_CREATION_FAILED = "Failed to create hash for %s!";
    public static final String OCTET_FORMAT = "%02x";
    public static final String SHA_HASH_ALGORITHM = "SHA";

    // ALL ETLs
    public static final String ETL_STARTED = "Starting %s...";
    public static final String ETL_FINISHED = "%s finished!";
    public static final String ETL_FAILED = "%s failed!";
    public static final String ETL_ABORTED = "%s aborted!";
    public static final String ETL_START_FAILED = "%s could not be started due to an unexpected error!";

    public static final String ETL_SKIPPED_DISABLED = "Skipping %s, because it is disabled!";
    public static final String ETL_SKIPPED_NO_CHANGES = "Did not start harvest, because no changes were detected!";

    // IteratorETL
    public static final String ERROR_NO_ENTRIES =
        "Cannot harvest %s - The source entries are empty or could not be retrieved!";
    public static final String ETL_SKIPPED_OUT_OF_RANGE = "Skipping %s - Document indices out of range.";

    // REST
    public static final String ALLOWED_REQUESTS =
        "GET/.json     Returns detailed information about all ETLs.\n"
        + "GET/etl       Returns detailed information about an ETL.\n"
        + "              The query parameter 'name' must be the name of the ETL.\n"
        + "GET/health    Returns plain text that serves as a health check.\n"
        + "GET/outdated  Checks if there is unharvested metadata.\n"
        + "GET/config    Displays a table of parameters and a means of\n"
        + "              configuring them.\n"
        + "GET/schedule  Displays a configurable set of cron jobs that\n"
        + "              can trigger harvests automatically.\n"
        + "GET/log       Displays the server log. The query parameters\n"
        + "              'date', 'class', and 'level' can be used to\n"
        + "              filter the log with comma-separated values of\n"
        + "              dates, logger classes, and log levels.\n"
        + "GET/harvest-timestamp  Displays the date of the last harvest.\n"
        + "GET/versions     Returns a list of GeRDI Maven dependencies.\n"
        + "GET/versions-all Returns a list of all Maven dependencies.\n"
        + "\n"
        + "POST          Starts the harvest.\n"
        + "POST/abort    Aborts an ongoing harvest.\n"
        + "POST/reset    Attempts to re-initialize this service.";


    public static final String ETL_NAME_QUERY = "name";
    public static final String ETL_NAME_QUERY_ERROR_EMPTY = "Missing query parameter 'name'!";
    public static final String ETL_NAME_QUERY_ERROR_UNKNOWN = "Unknown ETL name '%s'!";

    public static final String UNKNOWN_NUMBER = "???";
    public static final String ETL_PRETTY = "%s : %s [Health: %s]%n";
    public static final String PROGRESS = " % 3d%% (%d / %d)";
    public static final String PROGRESS_NO_BOUNDS = " (%d / ???)";
    public static final String NAME_TOTAL = "---\nOVERALL";

    public static final String MAX_RANGE_NUMBER = "%d (" + ParameterConstants.INTEGER_VALUE_MAX + ")";

    public static final String BUSY_ERROR_MESSAGE = "The harvesters are currently processing another request!";

    public static final String EXTRACTOR_CREATE_ERROR = "Could not create EXTRACTOR for %s!";
    public static final String TRANSFORMER_CREATE_ERROR = "Could not create TRANSFORMER for %s!";
    public static final String LOADER_CREATE_ERROR = "Could not create LOADER for %s!";

    public static final String ETL_PROCESSING_ERROR = "Error iterating through ETL components!";
    public static final String DUPLICATE_ETL_REGISTERED_ERROR = "Did not register %s, because it was already registered!";

    public static final String INVALID_ITER_EXTRACTOR_ERROR = AbstractIteratorETL.class.getSimpleName() + " instances must use subclasses of " + AbstractIteratorExtractor.class.getSimpleName() + " as Extractors!";
    public static final String INVALID_ITER_TRANSFORMER_ERROR = AbstractIteratorETL.class.getSimpleName() + " instances must use subclasses of " + AbstractIteratorTransformer.class.getSimpleName() + " as Transformers!";
    public static final String INVALID_ITER_LOADER_ERROR = AbstractIteratorETL.class.getSimpleName() + " instances must use subclasses of " + AbstractIteratorLoader.class.getSimpleName() + " as Loaders!";

    public static final String INIT_INVALID_STATE = "ETLs must not be initialized more than once!";
    public static final String ABORT_INVALID_STATE = "Cannot abort a harvest when it is '%s'!";

    public static final String BUSY_HARVESTING = "Cannot start harvest: Please wait for the current harvest to finish, or abort it!";
    public static final String ETLS_FAILED_UNKNOWN_ERROR = "Harvesting interrupted by unexpected error!";
    public static final String PREPARE_ETLS = "Preparing ETLs for harvest.";
    public static final String PREPARE_ETLS_FAILED = "Cannot start harvest: No ETL could be prepared!";
    public static final String START_ETLS = "Starting ETLs.";

    public static final String ETL_MANAGER_CACHE_PATH = FileConstants.CACHE_FOLDER_PATH + "state.json";
    public static final String ETL_MANAGER_LOADED = "Loaded " + ETLManager.class.getSimpleName() + " from cache file: %s.";
    public static final String ETL_LOADING_FAILED = "Could not load %s state from cache!";

    public static final String REMAINING_TIME_UNKNOWN = "Remaining Time : ???";
    public static final String REMAINING_TIME = "Remaining Time: %1$02d:%2$tM:%2$tS";

    public static final String ETL_DISABLED = "disabled";

    public static final String ABORT_START = "Aborting harvest...";
    public static final String ABORT_FINISHED = "All ETLs have been aborted!";
    public static final String ABORT_HARVEST_FAILED_NO_HARVEST = "Cannot abort harvest: No harvest is currently running!";

    public static final String HARVEST_STARTED = "Harvest started!";
    public static final String HARVEST_IN_PROGRESS = "Please wait for the harvest to finish!";
    public static final String HARVEST_FINISHED = "Harvest finished!";
    public static final String HARVEST_FAILED = "Harvest failed!";


    /**
     * Private constructor, because this class just serves as a place to define
     * constants.
     */
    private ETLConstants()
    {
    }
}
