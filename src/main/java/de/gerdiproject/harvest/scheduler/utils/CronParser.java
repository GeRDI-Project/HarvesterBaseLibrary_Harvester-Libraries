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
package de.gerdiproject.harvest.scheduler.utils;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import de.gerdiproject.harvest.scheduler.constants.CronConstants;
import de.gerdiproject.harvest.scheduler.enums.CronMonth;
import de.gerdiproject.harvest.scheduler.enums.CronWeekDay;

/**
 * This class provides static functions for parsing cron tabs.
 *
 * @author Robin Weiss
 */
public class CronParser
{
    /**
     * Private constructor, because this is just a collection of static methods.
     */
    private CronParser()
    {
    }


    /**
     * This method parses the "minutes" of a cron tab and generates a byte array
     * of all viable minutes.
     *
     * @param cronTabMinutes the cron tab minutes field
     *
     * @return a byte array of minutes that match the cron tab
     *
     * @throws IllegalArgumentException thrown when the cron tab is of an
     *             illegal format
     */
    public static byte[] parseMinutes(String cronTabMinutes) throws IllegalArgumentException
    {
        return parseCronField(
                   cronTabMinutes,
                   CronConstants.MINUTES_MIN_CRON,
                   CronConstants.MINUTES_MAX_CRON);
    }


    /**
     * This method parses the "hours" of a cron tab and generates a byte array
     * of all viable hours.
     *
     * @param cronTabHours the cron tab hours field
     *
     * @return a byte array of hours that match the cron tab
     *
     * @throws IllegalArgumentException thrown when the cron tab is of an
     *             illegal format
     */
    public static byte[] parseHours(String cronTabHours) throws IllegalArgumentException
    {
        return parseCronField(
                   cronTabHours,
                   CronConstants.HOURS_MIN_CRON,
                   CronConstants.HOURS_MAX_CRON);
    }


    /**
     * This method parses the "days" of a cron tab and generates a byte array of
     * all viable month days.
     *
     * @param cronTabMonthDays the cron tab days field
     *
     * @return a byte array of month days that match the cron tab
     *
     * @throws IllegalArgumentException thrown when the cron tab is of an
     *             illegal format
     */
    public static byte[] parseMonthDays(String cronTabMonthDays) throws IllegalArgumentException
    {
        return parseCronField(
                   cronTabMonthDays,
                   CronConstants.DAYS_MIN_CRON,
                   CronConstants.DAYS_MAX_CRON);
    }


    /**
     * This method parses the "week days" of a cron tab and generates a byte
     * array of all viable week days, converting any 7 to a 0 to enforce the
     * range of 0-6 even tough 1-7 is also viable.
     *
     * @param cronTabWeekDays the cron tab week days field
     *
     * @return a byte array of week days [0-6] that match the cron tab
     *
     * @throws IllegalArgumentException thrown when the cron tab is of an
     *             illegal format
     */
    public static byte[] parseWeekDays(String cronTabWeekDays) throws IllegalArgumentException
    {
        final byte[] weekDays = parseCronField(
                                    cronTabWeekDays,
                                    CronConstants.WEEK_DAYS_MIN_CRON,
                                    CronConstants.WEEK_DAYS_MAX_CRON + 1);

        // in cron, both 0 and 7 represent a Sunday. For simplicity's sake, we convert the 7 to a 0
        if (weekDays[weekDays.length - 1] == CronConstants.WEEK_DAYS_MAX_CRON + 1) {
            byte[] weekDaysWithoutSeven;

            if (weekDays[0] == 0) {
                weekDaysWithoutSeven = new byte[weekDays.length - 1];
                System.arraycopy(weekDays, 0, weekDaysWithoutSeven, 0, weekDaysWithoutSeven.length);
            } else {
                weekDaysWithoutSeven = new byte[weekDays.length];
                System.arraycopy(weekDays, 0, weekDaysWithoutSeven, 1, weekDays.length - 1);
            }

            return weekDaysWithoutSeven;
        } else
            return weekDays;
    }


    /**
     * This method parses the "months" of a cron tab and generates a byte array
     * of all viable months. By taking week days and month days into
     * consideration, all months that are impossible to match, are removed.
     *
     * @param cronTabMonths the cron tab months field
     * @param monthDays month days that have been parsed from the same cron tab
     * @param weekDays week days that have been parsed from the same cron tab
     *
     * @return a byte array of months that match the cron tab
     *
     * @throws IllegalArgumentException thrown when the cron tab is of an
     *             illegal format
     */
    public static byte[] parseMonths(String cronTabMonths, byte[] monthDays, byte[] weekDays) throws IllegalArgumentException
    {
        byte[] months = parseCronField(
                            cronTabMonths,
                            CronConstants.MONTHS_MIN_CRON,
                            CronConstants.MONTHS_MAX_CRON);

        final boolean isWeekDayUnRestricted = weekDays[0] == CronConstants.WEEK_DAYS_MIN_CRON
                                              && weekDays[weekDays.length - 1] == CronConstants.WEEK_DAYS_MAX_CRON;

        // if there is no weekday restriction, remove month/day combinations that are impossible
        if (isWeekDayUnRestricted) {
            months = removeInvalidMonths(monthDays, months);

            if (months.length == 0)
                throw new IllegalArgumentException(
                    String.format(CronConstants.ERROR_NO_MONTHS, cronTabMonths));
        }

        return months;
    }


    /**
     * Removes all months from an array, that are impossible to match, due to a
     * given array of days.<br>
     * e.g. days = {31}, months = {1,2,3,4,5,6,7,8} => filtered months =
     * {1,3,5,7,8}
     *
     * @param days an array of days of a cron tab
     * @param months an array of months of a cron tab
     *
     * @return an array of months that can have the specified days
     */
    private static byte[] removeInvalidMonths(byte[] days, byte[] months)
    {
        byte earliestDay = days[0];


        // every month can have at least 29 days
        if (earliestDay <= 29)
            return months;

        // check if at least one month is defined that has enough days, considering leap years
        int removedCount = 0;

        for (int i = 0; i < months.length; i++) {
            if (CronUtils.getDaysInMonth(months[i], 2016) < earliestDay) {
                months[i] = -1;
                removedCount++;
            }
        }

        // if no months had to be removed, return the original array
        if (removedCount == 0)
            return months;

        // if all months had to be removed, return empty array
        if (removedCount == months.length)
            return new byte[0];

        // sort array, so the -1 entries are at the beginning
        Arrays.sort(months);

        // copy all entries that are valid to a new array
        byte[] culledMonths = new byte[months.length - removedCount];
        System.arraycopy(months, removedCount, culledMonths, 0, months.length - removedCount);

        return culledMonths;
    }


    /**
     * Parses a single field of a cron tab and generates a byte array. Each byte
     * represents one time element that is covered by the cron field.
     *
     * @param field one of five fields of a cron tab
     * @param minVal the minimum possible value of the field
     * @param maxVal the maximum possible value of the field
     *
     * @return a byte array, where each byte represents one time element that is
     *         covered by the cron field
     *
     * @throws IllegalArgumentException thrown when the field is of an illegal
     *             format
     */
    private static byte[] parseCronField(String field, int minVal, int maxVal) throws IllegalArgumentException
    {
        byte[] values;

        // check if all possible values are to be added
        if (field.equals("*")) {
            final int len = 1 + maxVal - minVal;
            values = new byte[len];

            for (int i = 0; i < len; i++)
                values[i] = (byte)(minVal + i);
        }

        // check if there are multiple values
        else if (field.contains(",")) {
            final Set<Byte> tempValues = new TreeSet<>();
            String[] subFields = field.split(",");

            // parse recursively
            for (String subField : subFields) {
                final byte[] subValues = parseCronField(subField, minVal, maxVal);

                // add values to a set to avoid duplicates
                for (byte subValue : subValues)
                    tempValues.add(subValue);
            }

            // convert set to array
            int i = 0;
            values = new byte[tempValues.size()];

            for (Byte v : tempValues)
                values[i++] = v;
        }

        // check if there are frequencies
        else if (field.contains("/")) {
            try {
                String[] frequency = field.split("/");

                if (frequency.length != 2)
                    throw new IllegalArgumentException(String.format(CronConstants.ERROR_PARSE_FREQUENCY, field));

                // parse frequency parts recursively
                byte[] freqRange = parseCronField(frequency[0], minVal, maxVal);
                byte freqInterval = parseCronField(frequency[1], minVal, maxVal)[0];

                // get the frequency start index, considering the wrap-around
                int freqRangeFrom = freqRange[0];

                // if only one number describes the range, the max range is assumed
                int freqRangeTo = freqRange.length == 1
                                  ? maxVal
                                  : freqRange[freqRange.length - 1];

                // calculate the byte array length
                int len = 1 + ((freqRangeTo - freqRangeFrom) / freqInterval);
                values = new byte[len];

                // fill the byte array
                for (int i = 0; i < len; i++)
                    values[i] = (byte)(freqRangeFrom + i * freqInterval);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(String.format(CronConstants.ERROR_PARSE_FREQUENCY, field));
            }
        }

        // check if there are ranges
        else if (field.contains("-")) {
            String[] range = field.split("-");

            if (range.length != 2)
                throw new IllegalArgumentException(String.format(CronConstants.ERROR_PARSE_RANGE, field));

            // parse two range values recursively
            byte rangeFrom = parseCronField(range[0], minVal, maxVal)[0];
            byte rangeTo = parseCronField(range[1], minVal, maxVal)[0];

            return parseCronField("*", rangeFrom, rangeTo);

        } else {
            byte singleValue = -1;

            try {
                singleValue = Byte.parseByte(field);
            } catch (NumberFormatException e) { // NOPMD continue with other tests
            }

            // if no number could be parsed, try to parse a month name
            if (singleValue == -1) {
                try {
                    singleValue = (byte) CronMonth.valueOf(field).ordinal();
                } catch (IllegalArgumentException e) { // NOPMD continue with other tests
                }
            }

            // if it was not a month name, try to parse a week day

            if (singleValue == -1) {
                try {
                    singleValue = (byte) CronWeekDay.valueOf(field).ordinal();
                } catch (IllegalArgumentException e) { // NOPMD continue with other tests
                    throw new IllegalArgumentException(String.format(CronConstants.ERROR_CANNOT_PARSE, field));
                }

            }

            if (singleValue < minVal || singleValue > maxVal)
                throw new IllegalArgumentException(
                    String.format(CronConstants.ERROR_OUT_OF_RANGE, field, minVal, maxVal));

            values = new byte[1];
            values[0] = singleValue;
        }

        return values;
    }


}
