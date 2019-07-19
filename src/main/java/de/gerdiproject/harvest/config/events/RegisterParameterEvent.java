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
import de.gerdiproject.harvest.event.ISynchronousEvent;

/**
 * This event aims to register a new parameter in the {@linkplain Configuration}.
 * The return value of the callback function is the parameter as it appears in the configuration.
 * This guarantees that multiple registrations of the same parameter will always return
 * the correct object reference.
 *
 * @author Robin Weiss
 */
public class RegisterParameterEvent implements ISynchronousEvent<AbstractParameter<?>>
{
    private final AbstractParameter<?> param;


    /**
     * Constructor that sets up the payload.
     *
     * @param param the parameter that is to be registered in the {@linkplain Configuration}.
     */
    public RegisterParameterEvent(final AbstractParameter<?> param)
    {
        this.param = param;
    }


    /**
     * Returns the parameter that is to be registered in the {@linkplain Configuration}.
     *
     * @return the parameter that is to be registered in the {@linkplain Configuration}.
     */
    public AbstractParameter<?> getParameter()
    {
        return param;
    }
}
