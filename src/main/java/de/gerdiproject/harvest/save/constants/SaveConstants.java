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
package de.gerdiproject.harvest.save.constants;

import de.gerdiproject.harvest.save.HarvestSaver;

/**
 * This static class is a collection of constants that are used by the {@linkplain HarvestSaver}.
 *
 * @author Robin Weiss
 */
public class SaveConstants
{
    public static final String HARVEST_DATE_JSON = "harvestDate";
    public static final String DURATION_JSON = "durationInSeconds";
    public static final String IS_FROM_DISK_JSON = "wasHarvestedFromDisk";
    public static final String HASH_JSON = "hash";
    public static final String DATA_JSON = "data";

    /**
     * Private constructor, because this is a static class.
     */
    private SaveConstants()
    {

    }
}