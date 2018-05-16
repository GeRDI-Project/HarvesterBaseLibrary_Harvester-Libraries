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
package de.gerdiproject.harvest.utils.logger;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.utils.logger.constants.LoggerConstants;

/**
 * This class provides static methods for configuring the logger.
 *
 * @author Robin Weiss
 */
public class LoggerUtils
{
    private static org.slf4j.Logger LOGGER = LoggerFactory.getLogger(LoggerUtils.class);

    /**
     * Private constructor, because this class only provides static methods.
     */
    private LoggerUtils()
    {

    }


    /**
     * Retrieves filtered log messages from the harvester service log file.
     *
     * @param dateFilters the log dates in YYYY-MM-DD format of the log messages,
     *         or null if this filter should not be applied
     * @param levelFilters the log levels of the log messages,
     *         or null if this filter should not be applied
     * @param classFilters the logger names of the log messages,
     *         or null if this filter should not be applied
     *
     * @return all log messages that fit the filter criteria
     */
    public static String getLog(List<String> dateFilters, List<String> levelFilters, List<String> classFilters)
    {
        final StringBuilder logBuilder = new StringBuilder();
        final String logFilePath = String.format(LoggerConstants.LOG_FILE_PATH, MainContext.getModuleName());

        try
            (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                    new FileInputStream(logFilePath), MainContext.getCharset()))) {

            if (dateFilters == null && levelFilters == null && classFilters == null)
                logBuilder.append(reader.lines().collect(Collectors.joining("\n")));
            else {

                String line;
                boolean shouldAddLine = false;

                while ((line = reader.readLine()) != null) {
                    final Matcher lineMatch = LoggerConstants.PARSE_LOG_PATTERN.matcher(line);

                    if (lineMatch.matches())
                        shouldAddLine =
                            (dateFilters == null || dateFilters.contains(lineMatch.group(1)))
                            && (classFilters == null || classFilters.contains(lineMatch.group(2)))
                            && (levelFilters == null || levelFilters.contains(lineMatch.group(3)));

                    if (shouldAddLine)
                        logBuilder.append(line).append('\n');
                }
            }
        } catch (IOException e) {
            LOGGER.error(String.format(LoggerConstants.ERROR_READING_LOG, logFilePath), e);
            return null;
        }

        return logBuilder.toString();
    }


    /**
     * Initializes the root logger by assigning a file to it and setting
     * a standard logging pattern.
     *
     * @param logFileName the name of the log file
     */
    public static void init(final String logFileName)
    {
        final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        final FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
        fileAppender.setContext(loggerContext);
        fileAppender.setFile(String.format(LoggerConstants.LOG_FILE_PATH, logFileName));

        final PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern(LoggerConstants.LOG_PATTERN);
        encoder.start();

        fileAppender.setEncoder(encoder);
        fileAppender.start();

        LoggerConstants.ROOT_LOGGER.addAppender(fileAppender);
    }
}
