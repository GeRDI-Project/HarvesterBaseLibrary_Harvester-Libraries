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

import java.io.File;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * This static class is a collection of constants that are used for ContextListeners and application related classes.
 *
 * @author Robin Weiss
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApplicationConstants
{
    public static final String INIT_SERVICE = "Initializing Harvester Service...";
    public static final String INIT_SERVICE_FAILED = "Initialization of the Harvester Service failed!";
    public static final String INIT_SERVICE_SUCCESS = "%s is now ready!";

    public static final String CONTEXT_DESTROYED = "%s undeployed!";
    public static final String CONTEXT_RESET = "Resetting %s...";

    public static final String HARVESTER_SERVICE_NAME_SUFFIX = "HarvesterService";
    public static final String HARVESTER_NAME_SUB_STRING = "harvester";

    public static final String INIT_FIELD = "Initializing %s...";
    public static final String INIT_FIELD_SUCCESS = "Successfully initialized %s!";

    public static final String LOG_DEPLOYMENT_TYPE = "Deployment Type: %s";
    public static final String DEPLOYMENT_TYPE = "DEPLOYMENT_TYPE";

    public static final String CACHE_ROOT_DIR_OTHER = new File("").getAbsolutePath();

    public static final String CACHE_DIR_JETTY = "debug/";
    public static final String CACHE_DIR_UNIT_TESTS = "debug/unit-tests/";
    public static final String CACHE_DIR_OTHER = "";
}
