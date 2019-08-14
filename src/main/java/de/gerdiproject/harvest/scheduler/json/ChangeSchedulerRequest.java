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

import de.gerdiproject.harvest.scheduler.Scheduler;
import lombok.Value;

/**
 * This JSON object represents a REST request that can delete/add/change
 * a crontab of the {@linkplain Scheduler}.
 *
 * @author Robin Weiss
 */
@Value
public class ChangeSchedulerRequest
{
    /**
     * -- GETTER --
     * Returns the crontab that is to be changed.
     * @return the crontab that is to be changed
     */
    private final String cronTab;
}
