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
package de.gerdiproject.harvest.scheduler.constants;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;


/**
 * This class offers constants that are used for parsing cron tabs.
 *
 * It was an immense test of mental strength to not name this class
 * "Cronstants".
 *
 * @author Robin Weiss
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CronConstants
{
    public static final int MINUTES_MIN_CRON = 0;
    public static final int MINUTES_MAX_CRON = 59;

    public static final int HOURS_MIN_CRON = 0;
    public static final int HOURS_MAX_CRON = 23;

    public static final int DAYS_MIN_CRON = 1;
    public static final int DAYS_MAX_CRON = 31;

    public static final int MONTHS_MIN_CRON = 1;
    public static final int MONTHS_MAX_CRON = 12;

    public static final int WEEK_DAYS_MIN_CRON = 0;
    public static final int WEEK_DAYS_MAX_CRON = 6;

    public static final String ERROR_NO_MONTHS =
        "Invalid cron month restriction '%s': The restriction of days and months causes this job to never be executed!";

    public static final String ERROR_CANNOT_PARSE = "Cannot parse cron field '%s'!";
    public static final String ERROR_OUT_OF_RANGE = "Cron field '%s' out of range [%d, %d]!";
    public static final String ERROR_PARSE_FREQUENCY = "Invalid frequency in cron field '%s'!";
    public static final String ERROR_PARSE_RANGE = "Invalid range in cron field '%s'!";
    public static final String ERROR_CRON_FORMAT =
        "Invalid cron tab '%s': A cron tab must consist of exactly five space-separated values!";

    public static final Map<Byte, Byte> MAX_DAYS_IN_MONTH_MAP = createDaysInMonthMap();


    /**
     * Creates a map that maps a cron month to the maximum number of days that
     * can appear in this month.
     *
     * @return a map of months to corresponding days
     */
    private static Map<Byte, Byte> createDaysInMonthMap()
    {
        final Map<Byte, Byte> map = new ConcurrentHashMap<>();
        map.put((byte) 1, (byte) 31);
        map.put((byte) 2, (byte) 28);
        map.put((byte) 3, (byte) 31);
        map.put((byte) 4, (byte) 30);
        map.put((byte) 5, (byte) 31);
        map.put((byte) 6, (byte) 30);
        map.put((byte) 7, (byte) 31);
        map.put((byte) 8, (byte) 31);
        map.put((byte) 9, (byte) 30);
        map.put((byte) 10, (byte) 31);
        map.put((byte) 11, (byte) 30);
        map.put((byte) 12, (byte) 31);

        return Collections.unmodifiableMap(map);
    }
}
