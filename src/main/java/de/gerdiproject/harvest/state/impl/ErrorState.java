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

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.harvester.events.HarvesterInitializedEvent;
import de.gerdiproject.harvest.state.IState;
import de.gerdiproject.harvest.state.constants.StateConstants;
import de.gerdiproject.harvest.state.events.ChangeStateEvent;

/**
 * This state indicates it is waiting for a harvest to start.
 *
 * @author Robin Weiss
 */
public class ErrorState implements IState
{
    /**
     * Switches the state to {@linkplain IdleState} if the initialization was successful.
     * Otherwise, the state is switched to the {@linkplain ErrorState}
     */
    private static final Consumer<HarvesterInitializedEvent> ON_HARVESTER_INITIALIZED =
    (HarvesterInitializedEvent e) -> {

        if (e.isSuccessful())
            EventSystem.sendEvent(new ChangeStateEvent(new IdleState()));
    };


    @Override
    public String getProgressString()
    {
        return StateConstants.ERROR_STATUS;
    }


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
    public String startHarvest()
    {
        return StateConstants.ERROR_DETAILED;
    }


    @Override
    public String abort()
    {
        return StateConstants.ERROR_DETAILED;
    }


    @Override
    public String pause()
    {
        return StateConstants.ERROR_DETAILED;
    }


    @Override
    public String resume()
    {
        return StateConstants.ERROR_DETAILED;
    }


    @Override
    public List<String> getAllowedParameters()
    {
        return Arrays.asList();
    }


    @Override
    public String submit()
    {
        return StateConstants.ERROR_DETAILED;
    }


    @Override
    public String save()
    {
        return StateConstants.ERROR_DETAILED;
    }


    @Override
    public String getName()
    {
        return StateConstants.ERROR_PROCESS;
    }
}
