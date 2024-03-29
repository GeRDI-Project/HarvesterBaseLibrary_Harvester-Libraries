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
package de.gerdiproject.harvest.scheduler.json;

import java.util.Set;

import de.gerdiproject.harvest.scheduler.rest.SchedulerRestResource;
import lombok.Value;

/**
 * This class represents a JSON response from a .json GET request of the {@linkplain SchedulerRestResource}.
 *
 * @author Robin Weiss
 */
@Value
public class SchedulerResponse
{
    /**
     * -- GETTER --
     * Returns all cron tabs of scheduled harvests.
     * @return a set of all cron tabs of scheduled harvests
     */
    private final Set<String> scheduledHarvests;
}
