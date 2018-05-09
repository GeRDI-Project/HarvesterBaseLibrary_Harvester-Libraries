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
package de.gerdiproject.harvest.state.constants;

/**
 * This static class is a collection of constants, mostly used within State classes.
 *
 * @author Robin Weiss
 */
public class StateConstants
{
    public static final String INIT_PROCESS = "initialization";
    public static final String IDLE_PROCESS = "idling";
    public static final String ERROR_PROCESS = "error";
    public static final String HARVESTING_PROCESS = "harvesting";
    public static final String SAVE_PROCESS = "saving";
    public static final String SUBMIT_PROCESS = "submitting";
    public static final String ABORTING_PROCESS = "aborting";

    public static final String INIT_STATUS = "Initializing harvester...";
    public static final String SUBMITTING_STATUS = "Submitting documents...";
    public static final String SAVING_STATUS = "Saving documents to disk...";
    public static final String ERROR_STATUS = "Harvester could not be initialized! Look at the logs for details.";
    public static final String ABORT_STATUS = "Aborting %s-process...";

    public static final String CANNOT_START_PREFIX = "Cannot start harvest: ";
    public static final String CANNOT_ABORT_PREFIX = "Cannot abort %s: ";
    public static final String CANNOT_PAUSE_PREFIX = "Cannot pause %s: ";
    public static final String CANNOT_RESUME_PREFIX = "Cannot resume %s: ";
    public static final String CANNOT_SUBMIT_PREFIX = "Cannot submit documents: ";
    public static final String CANNOT_SAVE_PREFIX = "Cannot save documents: ";
    public static final String CANNOT_PROCESS_PREFIX = "Cannot process request: ";

    public static final String INIT_IN_PROGRESS = "Please wait for the harvester to be initialized!";
    public static final String SUBMIT_IN_PROGRESS = "Please wait for all documents to be submitted!";
    public static final String HARVEST_IN_PROGRESS = "Please wait for the harvest to finish!";
    public static final String SAVE_IN_PROGRESS = "Please wait for the search index to be saved to disk!";
    public static final String NO_HARVEST_IN_PROGRESS = "No harvest is running!";
    public static final String NO_DOCUMENTS = "No documents to send! Did you start a harvest?";
    public static final String ERROR_DETAILED = "Cannot process request, because the Harvester could not be initialized! Look at the logs for details.";
    public static final String ABORT_DETAILED = "Cannot process request, because the %s-process is being aborted! Try again in a short moment.";
    public static final String UNKNOWN_ERROR = "Cannot process request due to an unknown error!";
    public static final String CANNOT_RESET = "Cannot reset the service! Please, abort the %s-process first!";

    public static final String RESUME_IN_PROGRESS = "The %s-process is already running!";
    public static final String PAUSE_IN_PROGRESS = "The %s-process is already paused!";

    public static final String READY = "%s is now ready!";
    public static final String HARVEST_STARTED = "Harvest started!";

    public static final String HARVEST_STATUS = "Harvest:     %s";
    public static final String SAVE_STATUS = "Storage:     %s";
    public static final String SUBMIT_STATUS = "Submission:  %s";
    public static final String IDLE_STATUS = HARVEST_STATUS + "%n"
                                             + SAVE_STATUS + "%n"
                                             + SUBMIT_STATUS;

    public static final String HARVEST_DONE = "Harvest finished!";
    public static final String HARVEST_FAILED = "Harvest failed!";

    public static final String ABORT_STARTED = "Aborting %s-process...";
    public static final String ABORT_FINISHED = "%s-process aborted!";

    public static final String SAVING_DONE = "Saving finished!";

    public static final String RESET_STARTED = "Resetting the Harvester Service!";
    public static final String RESET_STARTED_PROBLEMATIC = "Resetting the Harvester Service! This may cause stability issues,"
                                                           + " because the service is neither in the " + IDLE_PROCESS + "- nor the " + ERROR_PROCESS + " state!";

    // PROGRESSING STATES
    public static final String TIME_UNKNOWN = "unknown";
    public static final String DAYS_HOURS = "%dd %dh";
    public static final String HOURS_MINUTES = "%dh %dm";
    public static final String MINUTES_SECONDS = "%dm %ds";
    public static final String SECONDS = "%ds";

    public static final String PROGESS_TEXT = "%s: %3d%% (%d / %d)";
    public static final String PROGESS_TEXT_SIMPLE = "%d/%d";
    public static final String PROGESS_TEXT_DETAILED = "%d / %d (%.2f%%)  Remaining Time: %s";
    public static final String PROGESS_TEXT_NO_MAX_VALUE = "%d documents";
    public static final String RETRY_AFTER_HEADER = "Retry-After";


    /**
     * Private constructor, because this class just serves
     * as a place to define constants.
     */
    private StateConstants()
    {
    }
}
