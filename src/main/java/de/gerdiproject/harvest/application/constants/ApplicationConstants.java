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


/**
 * This static class is a collection of constants that are used for ContextListeners and application related classes.
 *
 * @author Robin Weiss
 */
public class ApplicationConstants
{
    public static final String INIT_HARVESTER_START = "Initializing Harvester...";
    public static final String INIT_HARVESTER_FAILED = "Could not initialize Harvester!";
    public static final String INIT_HARVESTER_SUCCESS = "%s initialized!";

    public static final String CONTEXT_DESTROYED = "%s undeployed!";
    public static final String CONTEXT_RESET = "Resetting %s...";

    public static final String HARVESTER_SERVICE_NAME_SUFFIX = "HarvesterService";
    public static final String HARVESTER_NAME_SUB_STRING = "harvester";

    /**
     * Private constructor, because this is a static class.
     */
    private ApplicationConstants()
    {

    }
}
