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
package de.gerdiproject.harvest.rest.constants;

/**
 * This class provides constants that are used by generic REST requests.
 *
 * @author Robin Weiss
 */
public class RestConstants
{
    public static final String WAIT_FOR_INIT = "Please wait for the service to be initialized!";
    public static final String CANNOT_PROCESS_PREFIX = "Cannot process request: ";
    public static final String UNKNOWN_ERROR = "Cannot process request due to an unknown error!";
    public static final String INIT_ERROR_DETAILED = "Cannot process request, because the Harvester could not be initialized! Look at the logs for details.";

    public static final String RETRY_AFTER_HEADER = "Retry-After";
    public static final String RESET_STARTED = "Resetting the Harvester Service!";

    public static final String JSON_INVALID_FORMAT_ERROR = "Invalid JSON object:%n%s";
    public static final String INVALID_REQUEST_ERROR = "Unsupported HTTP request method!";
    public static final String NO_JSON_BODY_ERROR = "You must specify a JSON body!";
    public static final String NO_FORM_PARAMETERS_BODY_ERROR = "You must specify form parameters!";
    public static final String REST_GET_TEXT = "- %s %s -%n%n%s%n%nAllowed Requests:%n";
    public static final String LINE_START_REGEX = "(^|\n)(\\w)";
    public static final String LINE_START_REPLACEMENT = "$1  $2";

    public static final String NOT_AVAILABLE = "N/A";

    /**
     * Private constructor because this class only offers static constants.
     */
    private RestConstants()
    {

    }
}
