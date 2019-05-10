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
package de.gerdiproject.harvest.application.events;

import de.gerdiproject.harvest.event.AbstractSucceededOrFailedEvent;

/**
 * This event signifies that the harvester service initialization process is over.
 *
 * @author Robin Weiss
 */
public class ServiceInitializedEvent extends AbstractSucceededOrFailedEvent
{
    /**
     * Simple Constructor.
     *
     * @param isSuccessful if true, the harvester service was initialized and is ready to go
     */
    public ServiceInitializedEvent(final boolean isSuccessful)
    {
        super(isSuccessful);
    }
}
