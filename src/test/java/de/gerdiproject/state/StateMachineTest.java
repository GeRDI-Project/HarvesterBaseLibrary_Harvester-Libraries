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
package de.gerdiproject.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Test;

import de.gerdiproject.harvest.state.IState;
import de.gerdiproject.harvest.state.StateMachine;
import de.gerdiproject.state.examples.TestState;


/**
 * This class provides test cases for the {@linkplain StateMachine}.
 *
 * @author Robin Weiss
 */
public class StateMachineTest
{
    /**
     * Sets the current State to null after testing.
     */
    @After
    public void after()
    {
        StateMachine.setState(null);
    }


    /**
     * Tests if the default state of the {@linkplain StateMachine} is null.
     */
    @Test
    public void testDefaultState()
    {
        assertNull(StateMachine.getCurrentState());
    }


    /**
     * Tests if the current state of the {@linkplain StateMachine} is set properly.
     */
    @Test
    public void testStateChange()
    {
        final TestState testState = new TestState();
        StateMachine.setState(testState);

        assertEquals(testState, StateMachine.getCurrentState());
    }

    /**
     * Tests if the constructor of a state does not automatically
     * call the onStateEnter() function.
     */
    @Test
    public void testConstructor()
    {
        final TestState testState = new TestState();
        assert !testState.isActive();
    }


    /**
     * Tests if the onStateEnter() function of {@linkplain IState}
     * is called when the state is assigned to the {@linkplain StateMachine}.
     */
    @Test
    public void testStateEnte()
    {
        final TestState testState = new TestState();
        StateMachine.setState(testState);
        assert testState.isActive();
    }

    /**
     * Tests if the onStateLeave() function of {@linkplain IState}
     * is called when the state is removed from the {@linkplain StateMachine}.
     */
    @Test
    public void testStateEnterAndLeave()
    {
        final TestState testState = new TestState();
        StateMachine.setState(testState);
        StateMachine.setState(null);
        assert !testState.isActive();
    }
}
