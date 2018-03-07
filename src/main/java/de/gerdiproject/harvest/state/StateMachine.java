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
package de.gerdiproject.harvest.state;

import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.state.events.ChangeStateEvent;
import de.gerdiproject.harvest.state.impl.InitializationState;

/**
 * This singleton state machine controls REST input by delegating it to the current state.
 * Edge-case scenarios are more easily avoided this way, as all REST-triggered functions
 * have a clearly defined behavior at any given state.
 *
 * @author Robin Weiss
 */
public class StateMachine
{
    private static final StateMachine instance = new StateMachine();

    private IState currentState;


    /**
     * Private constructor for the singleton instance.
     * The default state is the {@linkplain InitializationState}.
     */
    private StateMachine()
    {
        currentState = new InitializationState();
        currentState.onStateEnter();
    }

    /**
     * Init must be called explicitly, because this class must be referenced once in order to work.
     */
    public static void init()
    {
        // register event listeners
        EventSystem.addListener(ChangeStateEvent.class, (ChangeStateEvent e) -> instance.setState(e.getState()));
    }


    /**
     * Changes the state.
     *
     * @param newState the new state
     */
    private void setState(IState newState)
    {
        currentState.onStateLeave();
        currentState = newState;
        currentState.onStateEnter();
    }


    /**
     * Retrieves the currently active state.
     *
     * @return the currently active state
     */
    public static IState getCurrentState()
    {
        return instance.currentState;
    }
}
