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

import de.gerdiproject.harvest.config.parameters.AbstractParameter;
import de.gerdiproject.harvest.event.IEvent;

/**
 * This event is dispatched when the value of a parameter has changed.
 *
 * @author Robin Weiss
 */
public class ParameterChangedEvent implements IEvent
{
    private final AbstractParameter<?> param;


    /**
     * Constructor that requires the signal payload.
     *
     * @param param the parameter that has changed
     */
    public ParameterChangedEvent(AbstractParameter<?> param)
    {
        this.param = param;
    }


    /**
     * Retrieves the parameter that has changed.
     *
     * @return the parameter that has changed
     */
    public AbstractParameter<?> getParameter()
    {
        return param;
    }
}
