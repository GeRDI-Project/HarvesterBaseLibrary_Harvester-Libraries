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
package de.gerdiproject.harvest.utils.logger.constants;


import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import de.gerdiproject.harvest.utils.logger.HarvesterLog;

/**
 * This static class is a collection of constants, used by the {@linkplain HarvesterLog}.
 *
 * @author Robin Weiss
 */
public class LoggerConstants
{
    public static final Logger ROOT_LOGGER = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    public static final Level DEFAULT_LOG_LEVEL = Level.ALL;

    public static final String LOG_FILE_PATH = "/var/log/harvester/%s.log";
    public static final String LOG_PATTERN = "%date %logger{0} %level %msg%n";
    public static final Pattern PARSE_LOG_PATTERN = Pattern.compile("^([^ ]+) [^ ]+ ([^ ]+) (\\w+?) [\\d\\D]*$");
    public static final String ERROR_READING_LOG = "Could not read log file: %s";



    /**
     * Private constructor, because this class just serves
     * as a place to define constants.
     */
    private LoggerConstants()
    {
    }
}
