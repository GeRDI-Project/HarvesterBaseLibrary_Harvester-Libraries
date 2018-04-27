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

import java.util.Calendar;
import java.util.Date;

import de.gerdiproject.harvest.scheduler.constants.CronConstants;


/**
 * This class contains static utility functions that work on cron tabs and cron
 * related date.
 *
 * @author Robin Weiss
 */
public class CronUtils
{
    /**
     * Private constructor, because this is just a collection of static methods.
     */
    private CronUtils()
    {
    }


    /**
     * Uses the cron tab to calculate the first matching date after the current
     * date + one minute.
     *
     * @param cronTab the crontab of which the next date is to be generated
     *
     * @return the first cron-matching date after the current date + one minute
     */
    public static Date getNextMatchingDate(String cronTab) throws IllegalArgumentException
    {
        final Calendar cal = Calendar.getInstance();
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.MINUTE, cal.get(Calendar.MINUTE) + 1);

        return getNextMatchingDate(cronTab, cal.getTime());
    }


    /**
     * Uses the cron tab to calculate the first date that is equal or later than
     * a specified date.
     *
     * @param cronTab the crontab of which the next date is to be generated
     * @param earliestPossibleDate the earliest date that is allowed to match
     *
     * @return the first cron-matching date after the current date + one minute
     */
    public static Date getNextMatchingDate(String cronTab, Date earliestPossibleDate) throws IllegalArgumentException
    {
        String[] cronFields = cronTab.split(" ");

        if (cronFields.length != 5)
            throw new IllegalArgumentException(
                String.format(CronConstants.ERROR_CRON_FORMAT, cronTab));

        final byte[] minutes = CronParser.parseMinutes(cronFields[0]);
        final byte[] hours = CronParser.parseHours(cronFields[1]);
        final byte[] monthDays = CronParser.parseMonthDays(cronFields[2]);
        final byte[] weekDays = CronParser.parseWeekDays(cronFields[4]);
        final byte[] months = CronParser.parseMonths(cronFields[3], monthDays, weekDays);

        // check if month is not *
        boolean isMonthDayRestricted = monthDays[0] == CronConstants.DAYS_MIN_CRON
                                       && monthDays[monthDays.length - 1] == CronConstants.DAYS_MAX_CRON;

        // check if the week day is not *
        boolean isWeekDayRestricted = weekDays[0] == CronConstants.WEEK_DAYS_MIN_CRON
                                      && weekDays[weekDays.length - 1] == CronConstants.WEEK_DAYS_MAX_CRON;

        // retrieve times from current time stamp
        final Calendar cal = Calendar.getInstance();
        cal.setTime(earliestPossibleDate);

        final int currYear = cal.get(Calendar.YEAR);
        final byte currMonth = (byte)(cal.get(Calendar.MONTH) + 1);
        final byte currMonthDay = (byte) cal.get(Calendar.DAY_OF_MONTH);
        final byte currWeekDay = (byte)(cal.get(Calendar.DAY_OF_WEEK) - 1);
        final byte currHour = (byte) cal.get(Calendar.HOUR_OF_DAY);
        final byte currMinute = (byte) cal.get(Calendar.MINUTE);

        // find the next minute that matches the cron tab
        byte nextMinute = getNextMatch(minutes, currMinute);

        // check if the next matching minute is within the current (false) or next hour (true)
        boolean isOverflowing = nextMinute < currMinute;

        // find the next matching hour
        byte nextHour = getNextMatch(hours, isOverflowing ? (byte)(currHour + 1) : currHour);

        // if the hour has changed, the minutes reset to the earliest match
        if (nextHour != currHour)
            nextMinute = minutes[0];

        // check if the next matching hour is within the current (false) or next day (true)
        isOverflowing = nextHour < currHour || nextHour == currHour && isOverflowing;

        // both month day and week day restrictions are summarized as a day of a month
        byte nextDay;
        {
            final int daysInThisMonth = getDaysInMonth(currMonth, currYear);

            // find the next matching day of the month
            byte nextMonthDay = getNextMatch(monthDays, isOverflowing ? (byte)(currMonthDay + 1) : currMonthDay);

            // if the next possible day falls into the next month, choose the earliest month day
            if (nextMonthDay > currMonthDay && daysInThisMonth < nextMonthDay)
                nextMonthDay = monthDays[0];

            nextDay = nextMonthDay;

            // check if week days are (also) restricted
            if (isWeekDayRestricted) {
                // find the next matching week day
                byte nextWeekDay = getNextMatch(weekDays, currWeekDay);

                // calculate how many days pass until the next viable week day
                int daysUntilNextWeekDay = nextWeekDay > currWeekDay || nextWeekDay == currWeekDay && !isOverflowing
                                           ? nextWeekDay - currWeekDay
                                           : nextWeekDay + 1 + CronConstants.WEEK_DAYS_MAX_CRON - currWeekDay;

                // calculate the day of the month of the matching week day
                byte nextMonthDay2 = (byte)(currMonthDay + daysUntilNextWeekDay);

                // wrap the day of the month, if the week day falls into the next month
                if (nextMonthDay2 > currMonthDay && daysInThisMonth < nextMonthDay2)
                    nextMonthDay2 -= daysInThisMonth;

                // check if the next days are in the next matching month
                boolean isMonthDayWrapped = nextMonthDay < currMonthDay || nextMonthDay > daysInThisMonth;
                boolean isWeekDayWrapped = nextMonthDay2 < currMonthDay;


                // check if the next week day comes before the next month day, if both are restricted
                if (!isMonthDayRestricted
                    || isMonthDayWrapped == isWeekDayWrapped && nextMonthDay2 < nextMonthDay
                    || isMonthDayWrapped)
                    nextDay = nextMonthDay2;
            }
        }

        // check if the next matching day is within the current (false) or next month (true)
        isOverflowing =
            nextDay < currMonthDay || nextDay == currMonthDay && isOverflowing;

        // get next matching month
        byte nextMonth = getNextMatch(months, isOverflowing ? (byte)(currMonth + 1) : currMonth);

        // check if the next matching month is within the current (false) or next year (true)
        isOverflowing =
            nextMonth < currMonth || nextMonth == currMonth && isOverflowing;

        // increment the year if the next month falls out if range
        int nextYear = isOverflowing ? currYear + 1 : currYear;

        // if we have only a month day restriction, calculate the next fitting month and year
        // this is not trivial, because multiple years can be skipped for Feb. the 29th
        if (!isWeekDayRestricted) {
            while (getDaysInMonth(nextMonth, nextYear) < nextDay) {
                byte prevMonth = nextMonth;
                nextMonth = getNextMatch(months, ++nextMonth);

                if (nextMonth <= prevMonth)
                    nextYear++;
            }
        }

        // reset all previous values if either the year or month has changed
        if (nextMonth != currMonth || nextYear != currYear) {
            nextMinute = minutes[0];
            nextHour = hours[0];

            // calculate the earliest (week-)day of the month/year combination
            if (isWeekDayRestricted) {

                // get first week day of the month
                cal.clear();
                cal.set(Calendar.YEAR, nextYear);
                cal.set(Calendar.MONTH, nextMonth - 1);
                byte firstDayInMonth = (byte)(cal.get(Calendar.DAY_OF_WEEK) - 1);

                // get the first matching day of the month
                byte nextWeekDay = getNextMatch(weekDays, firstDayInMonth);
                nextDay = (byte)(firstDayInMonth <= nextWeekDay
                                 ? 1 + nextWeekDay - firstDayInMonth
                                 : 1 + 7 + nextWeekDay - firstDayInMonth);
            }

            // if the month days are also restricted, choose what comes earlier
            if (!isWeekDayRestricted || isMonthDayRestricted && monthDays[0] < nextDay)
                nextDay = monthDays[0];
        }

        // assemble the next matching date and return it
        cal.set(nextYear, nextMonth - 1, nextDay, nextHour, nextMinute, 0);
        return cal.getTime();
    }


    /**
     * Calculates the amount of days of a month in a specified year.
     *
     * @param month the month of which the number of days are returned
     * @param year the year of the month
     *
     * @return the amount of days of a month in a specified year
     */
    public static int getDaysInMonth(byte month, int year)
    {
        int days = CronConstants.MAX_DAYS_IN_MONTH_MAP.get(month);

        // if the month is February check if we have a leap year
        if (month == 2 && year % 4 == 0 && !(year % 100 == 0 && year % 400 != 0))
            days++;

        return days;
    }


    /**
     * Searches through an timeArray for a value that is higher or equal to a
     * specified minTime. If no match could be found, the first element of the
     * timeArray is returned.
     *
     * @param timeArray an array of cron tab time values
     * @param minTime the minimum time to be returned
     *
     * @return a value that is higher or equal to minTime, or the first element
     *         of the timeArray
     */
    private static byte getNextMatch(byte[] timeArray, byte minTime)
    {
        final int len = timeArray.length;
        int i = 0;

        // search for an element that is greater or equal
        while (i < len && timeArray[i] < minTime)
            i++;

        // assign the found element, or the first one of the array
        return timeArray[i % len];
    }
}
