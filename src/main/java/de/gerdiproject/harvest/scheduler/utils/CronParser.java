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
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * This class provides static functions for parsing cron tabs.
 *
 * @author Robin Weiss
 */
@SuppressWarnings("PMD.GodClass") // not a god class, it only contains cron parsing logic
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CronParser
{
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
    public static byte[] parseMinutes(final String cronTabMinutes) throws IllegalArgumentException
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
    public static byte[] parseHours(final String cronTabHours) throws IllegalArgumentException
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
    public static byte[] parseMonthDays(final String cronTabMonthDays) throws IllegalArgumentException
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
    public static byte[] parseWeekDays(final String cronTabWeekDays) throws IllegalArgumentException
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
    public static byte[] parseMonths(final String cronTabMonths, final byte[] monthDays, final byte[] weekDays) throws IllegalArgumentException
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
    private static byte[] removeInvalidMonths(final byte[] days, final byte[] months)
    {
        final byte earliestDay = days[0];


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
        final byte[] culledMonths = new byte[months.length - removedCount];
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
    private static byte[] parseCronField(final String field, final int minVal, final int maxVal) throws IllegalArgumentException
    {
        final byte[] values;

        // check if all possible values are to be added
        if ("*".equals(field))
            values = parseAllValuesField(minVal, maxVal);

        else if (field.contains(","))
            values = parseMultiValueField(field, minVal, maxVal);

        else if (field.contains("/"))
            values = parseFrequencyField(field, minVal, maxVal);

        else if (field.contains("-"))
            values = parseRangeField(field, minVal, maxVal);

        else {
            values = new byte[1];
            values[0] = parseSingleValueField(field, minVal, maxVal);
        }

        return values;
    }

    /**
     * Parses a cron tab field that describes a single value.
     *
     * @param field a cron tab field that contains a number, weekday or month
     * @param minVal the minimum possible value of the field
     * @param maxVal the maximum possible value of the field
     *
     * @return a byte array, where each byte represents one time element that is
     *         covered by the cron field
     *
     * @throws IllegalArgumentException if the single value could not be parsed
     */
    private static byte parseSingleValueField(final String field, final int minVal, final int maxVal) throws IllegalArgumentException
    {
        byte singleValue = -1;

        try {
            singleValue = Byte.parseByte(field);
        } catch (final NumberFormatException e) { // NOPMD continue with other tests
        }

        // if no number could be parsed, try to parse a month name
        if (singleValue == -1) {
            try {
                singleValue = (byte) CronMonth.valueOf(field).ordinal();
            } catch (final IllegalArgumentException e) { // NOPMD continue with other tests
            }
        }

        // if it was not a month name, try to parse a week day

        if (singleValue == -1) {
            try {
                singleValue = (byte) CronWeekDay.valueOf(field).ordinal();
            } catch (final IllegalArgumentException e) { // NOPMD the message of the exception is changed
                throw new IllegalArgumentException(String.format(CronConstants.ERROR_CANNOT_PARSE, field)); // NOPMD (see above)
            }

        }

        if (singleValue < minVal || singleValue > maxVal)
            throw new IllegalArgumentException(
                String.format(CronConstants.ERROR_OUT_OF_RANGE, field, minVal, maxVal));

        return singleValue;
    }


    /**
     * Parses a cron tab field that describes the total range of a value.
     *
     * @param minVal the minimum possible value of the field
     * @param maxVal the maximum possible value of the field
     *
     * @return a byte array, where each byte represents one time element that is
     *         covered by the cron field
     */
    private static byte[] parseAllValuesField(final int minVal, final int maxVal)
    {
        final int len = 1 + maxVal - minVal;
        final byte[] values = new byte[len];

        for (int i = 0; i < len; i++)
            values[i] = (byte)(minVal + i);

        return values;
    }


    /**
     * Parses a cron tab field that describes a frequency value.
     *
     * @param field a cron tab field that contains a slash
     * @param minVal the minimum possible value of the field
     * @param maxVal the maximum possible value of the field
     *
     * @return a byte array, where each byte represents one time element that is
     *         covered by the cron field
     *
     * @throws IllegalArgumentException if the frequency could not be parsed properly
     */
    private static byte[] parseFrequencyField(final String field, final int minVal, final int maxVal) throws IllegalArgumentException
    {
        try {
            final String[] frequency = field.split("/");

            if (frequency.length != 2)
                throw new IllegalArgumentException(String.format(CronConstants.ERROR_PARSE_FREQUENCY, field));

            // parse frequency parts recursively
            final byte[] freqRange = parseCronField(frequency[0], minVal, maxVal);
            final byte freqInterval = parseSingleValueField(frequency[1], minVal, maxVal);

            // get the frequency start index, considering the wrap-around
            final int freqRangeFrom = freqRange[0];

            // if only one number describes the range, the max range is assumed
            final int freqRangeTo = freqRange.length == 1
                                    ? maxVal
                                    : freqRange[freqRange.length - 1];

            // calculate the byte array length
            final int len = 1 + ((freqRangeTo - freqRangeFrom) / freqInterval);
            final byte[] values = new byte[len];

            // fill the byte array
            for (int i = 0; i < len; i++)
                values[i] = (byte)(freqRangeFrom + i * freqInterval);

            return values;
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException(String.format(CronConstants.ERROR_PARSE_FREQUENCY, field)); // NOPMD intended exception
        }
    }


    /**
     * Parses a cron tab field that describes a value range.
     *
     * @param field a cron tab field that contains a dash
     * @param minVal the minimum possible value of the field
     * @param maxVal the maximum possible value of the field
     *
     * @return a byte array, where each byte represents one time element that is
     *         covered by the cron field
     */
    private static byte[] parseRangeField(final String field, final int minVal, final int maxVal)
    {
        final String[] range = field.split("-");

        if (range.length != 2)
            throw new IllegalArgumentException(String.format(CronConstants.ERROR_PARSE_RANGE, field));

        // parse two range values recursively
        final byte rangeFrom = parseSingleValueField(range[0], minVal, maxVal);
        final byte rangeTo = parseSingleValueField(range[1], minVal, maxVal);

        return parseAllValuesField(rangeFrom, rangeTo);
    }


    /**
     * Parses a cron tab field that contains multiple comma-separated values.
     *
     * @param field a cron tab field that contains colons
     * @param minVal the minimum possible value of the field
     * @param maxVal the maximum possible value of the field
     *
     * @return a byte array, where each byte represents one time element that is
     *         covered by the cron field
     */
    private static byte[] parseMultiValueField(final String field, final int minVal, final int maxVal)
    {
        final Set<Byte> tempValues = new TreeSet<>();
        final String[] subFields = field.split(",");

        // parse recursively
        for (final String subField : subFields) {
            final byte[] subValues = parseCronField(subField, minVal, maxVal);

            // add values to a set to avoid duplicates
            for (final byte subValue : subValues)
                tempValues.add(subValue);
        }

        // convert set to array
        int i = 0;
        final byte[] values = new byte[tempValues.size()];

        for (final Byte v : tempValues)
            values[i++] = v;

        return values;
    }


}
