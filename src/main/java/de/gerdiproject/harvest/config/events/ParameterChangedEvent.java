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
import lombok.Value;

/**
 * This event is dispatched when the value of a parameter has changed.
 *
 * @author Robin Weiss
 */
@Value
public class ParameterChangedEvent implements IEvent
{
    /**
     * -- GETTER --
     * Retrieves the parameter that has changed.
     * @return the parameter that has changed
     */
    private final AbstractParameter<?> parameter;

    /**
     * -- GETTER --
     * Retrieves the value of the parameter prior to its change.
     * @return the value of the parameter prior to its change
     */
    private final Object oldValue;
}
