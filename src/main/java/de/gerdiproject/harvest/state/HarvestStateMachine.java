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
package de.gerdiproject.harvest.state;

import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.impl.ChangeStateEvent;
import de.gerdiproject.harvest.state.impl.PreloadingState;

public class HarvestStateMachine
{
    private static final HarvestStateMachine instance = new HarvestStateMachine();

    private IHarvestState currentState;


    private HarvestStateMachine()
    {
        currentState = new PreloadingState();

        // register event listeners
        EventSystem.instance().addListener(ChangeStateEvent.class, (ChangeStateEvent e) -> setState(e.getState()));
    }


    public static HarvestStateMachine instance()
    {
        return instance;
    }


    private void setState(IHarvestState newState)
    {
        currentState.onStateLeave();
        currentState = newState;
        currentState.onStateEnter();
    }


    public IHarvestState getCurrentState()
    {
        return currentState;
    }
}
