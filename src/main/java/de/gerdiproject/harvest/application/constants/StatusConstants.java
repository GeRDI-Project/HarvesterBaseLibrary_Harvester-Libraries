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
package de.gerdiproject.harvest.application.constants;

import de.gerdiproject.harvest.application.rest.StatusRestResource;
import de.gerdiproject.harvest.state.constants.StateConstants;

/**
 * This static class is a collection of constants that are used by the
 * {@linkplain StatusRestResource}.
 *
 * @author Robin Weiss
 */
public class StatusConstants
{
    public static final String FAILED_HARVEST_HEALTH_CHECK = String.format(
                                                                 StateConstants.HARVEST_STATUS,
                                                                 "failed");

    public static final String REST_INFO = "- %s Extended REST Interface -%n%n"
                                           + "GET          Returns this overview of possible HTTP calls.%n"
                                           + "GET/state    Returns plain text describing what the service is doing.%n"
                                           + "GET/health   Returns plain text that serves as a health check.%n"
                                           + "GET/progress Returns two slash-separated numbers, representing the%n"
                                           + "             progress of the current task, or N/A if idle.%n"
                                           + "GET/max-documents Returns the max number of documents that can be%n"
                                           + "                  harvested considering the harvesting range.%n"
                                           + "GET/data-provider Returns the name of the data provider that is harvested.%n"
                                           + "GET/harvested-documents Returns the number of harvested and currently%n"
                                           + "                        cached documents.%n"
                                           + "GET/versions     Returns a list of GeRDI Maven dependencies.%n"
                                           + "GET/versions-all Returns a list of all Maven dependencies.";


    /**
     * Private constructor, because this class just serves as a place to define
     * constants.
     */
    private StatusConstants()
    {
    }
}
