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
package de.gerdiproject.harvest.scheduler;

import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.scheduler.constants.SchedulerConstants;
import de.gerdiproject.harvest.scheduler.events.ScheduledTaskExecutedEvent;
import de.gerdiproject.harvest.state.StateMachine;

/**
 * This task is used by the {@linkplain Scheduler} in order to run a harvest in
 * a specified schedule.
 *
 * @author Robin Weiss
 */
public class HarvestingTimerTask extends TimerTask
{
    private final Logger LOGGER = LoggerFactory.getLogger(Scheduler.class);


    @Override
    public void run()
    {
        // start a harvest
        final String status = StateMachine.getCurrentState().startHarvest();

        // log the feedback
        LOGGER.info(String.format(SchedulerConstants.TASK_MESSAGE, status));

        // notify the Scheduler to calculate the next execution
        EventSystem.sendEvent(new ScheduledTaskExecutedEvent(this));
    }
}
