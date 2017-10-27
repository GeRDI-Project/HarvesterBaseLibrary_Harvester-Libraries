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
package de.gerdiproject.harvest.state.constants;

/**
 * This static class is a collection of constants, commonly used within State classes.
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

    public static final String INIT_IN_PROGRESS = "Please wait for the harvester to be initialized!";
    public static final String SUBMIT_IN_PROGRESS = "Please wait for all documents to be submitted!";
    public static final String HARVEST_IN_PROGRESS = "Please wait for the harvest to finish!";
    public static final String SAVE_IN_PROGRESS = "Please wait for the search index to be saved to disk!";
    public static final String NO_HARVEST_IN_PROGRESS = "No harvest is running!";
    public static final String NO_DOCUMENTS = "No documents to send! Did you start a harvest?";
    public static final String ERROR_DETAILED = "Cannot execute command, because the Harvester could not be initialized! Look at the logs for details.";
    public static final String ABORT_DETAILED = "Cannot execute command, because the %s-process is being aborted! Try again in a short moment.";

    public static final String RESUME_IN_PROGRESS = "The %s-process is already running!";
    public static final String PAUSE_IN_PROGRESS = "The %s-process is already paused!";

    public static final String READY = "%s is now ready!";
    public static final String HARVEST_STARTED = "Harvest started!";
    public static final String HARVEST_FINISHED_AT = "Harvest finished at %s";
    public static final String HARVEST_SAVED_AT = ", saved at %s";
    public static final String HARVEST_SUBMITTED_AT = ", submitted at %s";
    public static final String HARVEST_NOT_STARTED = "Ready to harvest";

    public static final String IDLE_STATUS = "Harvest:\t\t%s"
                                             + "%nSave to disk:\t%s"
                                             + "%nSubmit to DB:\t%s";

    public static final String HARVEST_DONE = "Harvest finished!";
    public static final String HARVEST_FAILED = "Harvest failed!";

    public static final String ABORT_STARTED = "Aborting %s-process...";
    public static final String ABORT_FINISHED = "%s-process aborted!";

    // PROGRESSING STATES
    public static final int UNKNOWN_NUMBER = -1;
    public static final String TIME_UNKNOWN = "unknown";
    public static final String DAYS_HOURS = "%dd %dh";
    public static final String HOURS_MINUTES = "%dh %dm";
    public static final String MINUTES_SECONDS = "%dm %ds";
    public static final String SECONDS = "%ds";

    public static final String PROGESS_TEXT = "%s: %3d%% (%d / %d)";
    public static final String PROGESS_TEXT_DETAILED = "%d / %d (%.2f%%)  Remaining Time: %s";

    /**
     * Private constructor, because this is a static class.
     */
    private StateConstants()
    {
    }
}
