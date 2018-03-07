/**
 * Copyright © 2017 Robin Weiss (http://www.gerdi-project.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.gerdiproject.harvest.state.impl;

import de.gerdiproject.harvest.application.constants.StatusConstants;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.harvester.events.HarvesterInitializedEvent;
import de.gerdiproject.harvest.state.IState;
import de.gerdiproject.harvest.state.constants.StateConstants;
import de.gerdiproject.harvest.state.constants.StateEventHandlerConstants;

/**
 * This state represents the initialization of harvesters at the beginning of the server start.
 *
 * @author Robin Weiss
 */
public class InitializationState implements IState
{
    @Override
    public void onStateEnter()
    {
        EventSystem.addListener(HarvesterInitializedEvent.class, StateEventHandlerConstants.ON_HARVESTER_INITIALIZED);
    }


    @Override
    public void onStateLeave()
    {
        EventSystem.removeListener(HarvesterInitializedEvent.class, StateEventHandlerConstants.ON_HARVESTER_INITIALIZED);
    }


    @Override
    public String getStatusString()
    {
        return StateConstants.INIT_STATUS;
    }


    @Override
    public String startHarvest()
    {
        return StateConstants.CANNOT_START_PREFIX + StateConstants.INIT_IN_PROGRESS;
    }


    @Override
    public String abort()
    {
        return String.format(
                   StateConstants.CANNOT_ABORT_PREFIX + StateConstants.INIT_IN_PROGRESS,
                   StateConstants.INIT_PROCESS);
    }


    @Override
    public String pause()
    {
        return String.format(
                   StateConstants.CANNOT_PAUSE_PREFIX + StateConstants.INIT_IN_PROGRESS,
                   StateConstants.INIT_PROCESS);
    }


    @Override
    public String resume()
    {
        return String.format(
                   StateConstants.CANNOT_RESUME_PREFIX + StateConstants.INIT_IN_PROGRESS,
                   StateConstants.INIT_PROCESS);
    }


    @Override
    public String submit()
    {
        return StateConstants.CANNOT_SUBMIT_PREFIX + StateConstants.INIT_IN_PROGRESS;
    }


    @Override
    public String save()
    {
        return StateConstants.CANNOT_SAVE_PREFIX + StateConstants.INIT_IN_PROGRESS;
    }


    @Override
    public String getProgress()
    {
        return StatusConstants.NOT_AVAILABLE;
    }


    @Override
    public String getName()
    {
        return StateConstants.INIT_PROCESS;
    }
}
