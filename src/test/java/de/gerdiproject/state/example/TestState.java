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
package de.gerdiproject.state.example;

import javax.ws.rs.core.Response;

import de.gerdiproject.harvest.state.IState;
import de.gerdiproject.state.StateMachineTest;

/**
 * This class serves as a test state for {@linkplain StateMachineTest}.
 * It returns null on all calls, but provides a getter function for testing
 * if the onStateEnter() and onStateLeave() functions have been called.
 *
 * @author Robin Weiss
 *
 */
public class TestState implements  IState
{
    private boolean isActive = false;


    /**
     * Returns true if the onStateEnter() function was called, but
     * the onStateLeave() function was not.
     *
     * @return true if the state is currently active
     */
    public boolean isActive()
    {
        return isActive;
    }


    @Override
    public String getName()
    {
        return null;
    }


    @Override
    public String getStatusString()
    {
        return null;
    }


    @Override
    public Response getProgress()
    {
        return null;
    }


    @Override
    public void onStateEnter()
    {
        isActive = true;
    }


    @Override
    public void onStateLeave()
    {
        isActive = false;
    }


    @Override
    public Response startHarvest()
    {
        return null;
    }


    @Override
    public Response abort()
    {
        return null;
    }


    @Override
    public Response submit()
    {
        return null;
    }


    @Override
    public Response save()
    {
        return null;
    }


    @Override
    public Response isOutdated()
    {
        return null;
    }


    @Override
    public Response reset()
    {
        return null;
    }
}
