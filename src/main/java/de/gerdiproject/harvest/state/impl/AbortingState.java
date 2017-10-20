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
import de.gerdiproject.harvest.event.impl.StartAbortingEvent;
import de.gerdiproject.harvest.event.impl.ChangeStateEvent;
import de.gerdiproject.harvest.event.impl.AbortingFinishedEvent;
import de.gerdiproject.harvest.state.IState;
import de.gerdiproject.harvest.state.constants.StateConstants;

/**
 * This state indicates it is waiting for a harvest to start.
 *
 * @author Robin Weiss
 */
public class AbortingState implements IState
{
    /**
     * Switches the state to {@linkplain IdleState} if the abortion (bad phrasing!) was successful.
     */
    private static final Consumer<AbortingFinishedEvent> ON_PROCESS_ABORTED =
    (AbortingFinishedEvent e) -> {
        EventSystem.sendEvent(new ChangeStateEvent(new IdleState()));
    };

    private final String processName;

    /**
     * Constructs the state with the name of the aborted process.
     * @param processName the name of the process that is aborted
     */
    public AbortingState(String processName)
    {
        this.processName = processName;
    }


    @Override
    public void onStateEnter()
    {
        EventSystem.addListener(AbortingFinishedEvent.class, ON_PROCESS_ABORTED);
        EventSystem.sendEvent(new StartAbortingEvent());
    }


    @Override
    public void onStateLeave()
    {
        EventSystem.removeListener(AbortingFinishedEvent.class, ON_PROCESS_ABORTED);
    }


    @Override
    public String getProgressString()
    {
        return String.format(StateConstants.ABORT_STATUS, processName);
    }


    @Override
    public String startHarvest()
    {
        return String.format(StateConstants.ABORT_DETAILED, processName);
    }


    @Override
    public String abort()
    {
        return String.format(StateConstants.ABORT_DETAILED, processName);
    }


    @Override
    public String pause()
    {
        return String.format(StateConstants.ABORT_DETAILED, processName);
    }


    @Override
    public String resume()
    {
        return String.format(StateConstants.ABORT_DETAILED, processName);
    }

    @Override
    public List<String> getAllowedParameters()
    {
        return Arrays.asList();
    }

    @Override
    public String submit()
    {
        return String.format(StateConstants.ABORT_DETAILED, processName);
    }


    @Override
    public String save()
    {
        return String.format(StateConstants.ABORT_DETAILED, processName);
    }


    @Override
    public String getName()
    {
        return StateConstants.ABORTING_PROCESS;
    }
}
