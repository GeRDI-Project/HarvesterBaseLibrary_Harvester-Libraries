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
package de.gerdiproject.harvest.state.events;

import de.gerdiproject.harvest.event.IEvent;
import de.gerdiproject.harvest.state.StateMachine;
import de.gerdiproject.harvest.state.IState;

/**
 * This event causes a state transition of the {@linkplain StateMachine}.
 *
 * @author Robin Weiss
 */
public class ChangeStateEvent implements IEvent
{
    private final IState state;

    /**
     * Simple Constructor.
     *
     * @param state the state that is to be loaded by the {@linkplain StateMachine}
     */
    public ChangeStateEvent(IState state)
    {
        this.state = state;
    }

    /**
     * Returns the state that is to be loaded by the {@linkplain StateMachine}.
     *
     * @return the state that is to be loaded
     */
    public IState getState()
    {
        return state;
    }
}
