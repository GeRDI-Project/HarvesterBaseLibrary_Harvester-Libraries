/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
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
package de.gerdiproject.harvest.utils;



/**
 * This class provides methods for displaying (harvested) Strings.
 *
 * @author Robin Weiss
 */
public class HarvesterStringUtils
{
    private static final String PROGESS_TEXT = "\r%s: %3d%% (%d / %d)";

    /**
     * Creates a formatted string that represents the progress of a process.
     *
     * @param prefix
     *            this string will be prepended to the message
     * @param currentValue
     *            the current progress value
     * @param maxValue
     *            the maximum value the process can reach
     * @return a formatted string
     */
    public static final String formatProgress(String prefix, int currentValue, int maxValue)
    {
        int progressInPercent = Math.min((int) Math.ceil((100f * currentValue) / maxValue), 100);

        return String.format(
                   PROGESS_TEXT,
                   prefix,
                   progressInPercent,
                   currentValue,
                   maxValue);
    }
}
