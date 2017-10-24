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
import de.gerdiproject.harvest.state.events.ChangeStateEvent;
import de.gerdiproject.harvest.state.impl.InitializationState;

public class StateMachine
{
    private static final StateMachine instance = new StateMachine();

    private IState currentState;


    private StateMachine()
    {
        currentState = new InitializationState();
    }

    /**
     * Init must be called explicitly, because this class must be referenced once, in order to work.
     */
    public static void init()
    {
        // register event listeners
        EventSystem.addListener(ChangeStateEvent.class, (ChangeStateEvent e) -> instance.setState(e.getState()));
    }


    private void setState(IState newState)
    {
        currentState.onStateLeave();
        currentState = newState;
        currentState.onStateEnter();
    }


    public static IState getCurrentState()
    {
        return instance.currentState;
    }
}
