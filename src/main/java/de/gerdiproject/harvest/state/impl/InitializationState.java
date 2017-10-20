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
package de.gerdiproject.harvest.state.impl;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.impl.ChangeStateEvent;
import de.gerdiproject.harvest.event.impl.HarvesterInitializedEvent;
import de.gerdiproject.harvest.state.IState;
import de.gerdiproject.harvest.state.constants.StateConstants;

/**
 * This state represents the initialization of harvesters at the beginning of the server start.
 *
 * @author Robin Weiss
 */
public class InitializationState implements IState
{
    /**
     * Switches the state to {@linkplain IdleState} if the initialization was successful.
     * Otherwise, the state is switched to the {@linkplain ErrorState}
     */
    private static final Consumer<HarvesterInitializedEvent> ON_HARVESTER_INITIALIZED =
    (HarvesterInitializedEvent e) -> {

        if (e.isSuccessful())
            EventSystem.sendEvent(new ChangeStateEvent(new IdleState()));
        else
            EventSystem.sendEvent(new ChangeStateEvent(new ErrorState()));
    };


    @Override
    public void onStateEnter()
    {
        EventSystem.addListener(HarvesterInitializedEvent.class, ON_HARVESTER_INITIALIZED);
    }


    @Override
    public void onStateLeave()
    {
        EventSystem.removeListener(HarvesterInitializedEvent.class, ON_HARVESTER_INITIALIZED);
    }


    @Override
    public String getProgressString()
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
    public List<String> getAllowedParameters()
    {
        return Arrays.asList(ConfigurationConstants.AUTO_SAVE,
                             ConfigurationConstants.AUTO_SUBMIT,
                             ConfigurationConstants.HARVEST_START_INDEX,
                             ConfigurationConstants.HARVEST_END_INDEX,
                             ConfigurationConstants.SUBMISSION_URL,
                             ConfigurationConstants.SUBMISSION_USER_NAME,
                             ConfigurationConstants.SUBMISSION_PASSWORD,
                             ConfigurationConstants.SUBMISSION_SIZE
                            );
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
    public String getName()
    {
        return StateConstants.INIT_PROCESS;
    }
}
