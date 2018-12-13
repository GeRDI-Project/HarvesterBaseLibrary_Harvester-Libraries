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
package de.gerdiproject.harvest.config.events;

import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.parameters.AbstractParameter;
import de.gerdiproject.harvest.event.IEvent;

/**
 * This event aims to unregister a parameter from the {@linkplain Configuration}.
 *
 * @author Robin Weiss
 *
 */
public class UnregisterParameterEvent implements IEvent
{
    private final AbstractParameter<?> param;


    /**
     * Constructor that sets up the payload.
     *
     * @param param the parameter that is to be unregistered from the {@linkplain Configuration}.
     */
    public UnregisterParameterEvent(AbstractParameter<?> param)
    {
        this.param = param;
    }


    /**
     * Returns the parameter that is to be unregistered from the {@linkplain Configuration}.
     *
     * @return the parameter that is to be unregistered from the {@linkplain Configuration}.
     */
    public AbstractParameter<?> getParameter()
    {
        return param;
    }
}
