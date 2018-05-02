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
package de.gerdiproject.harvest.scheduler.events;

import javax.ws.rs.core.Response;

import de.gerdiproject.harvest.event.ISynchronousEvent;
import de.gerdiproject.harvest.scheduler.Scheduler;

/**
 * Synchronous event for adding a task to the {@linkplain Scheduler} and returning an HTTP response.
 *
 * @author Robin Weiss
 */
public class AddSchedulerTaskEvent implements ISynchronousEvent<Response>
{
    private final String cronTab;


    /**
     * Constructor that sets a cron string as payload.
     *
     * @param cronTab a cron tab
     */
    public AddSchedulerTaskEvent(String cronTab)
    {
        this.cronTab = cronTab;
    }


    /**
     * Returns a cron tab that is used for scheduling tasks.
     *
     * @return a cron tab string
     */
    public String getCronTab()
    {
        return cronTab;
    }
}
