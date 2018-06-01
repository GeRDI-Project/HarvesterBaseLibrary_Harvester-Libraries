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
package de.gerdiproject.event.example;

import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.ISynchronousEvent;

/**
 * This is an exemplary synchronous event used for testing the {@linkplain EventSystem}.
 *
 * @author Robin Weiss
 */
public class TestSynchronousEvent implements ISynchronousEvent<Object>
{
    private final Object payload;

    /**
     * Constructor for the event with a null-payload.
     */
    public TestSynchronousEvent()
    {
        this.payload = null;
    }


    /**
     * Constructor for the event with a specified payload
     *
     * @param payload a payload transported by the event
     */
    public TestSynchronousEvent(Object payload)
    {
        this.payload = payload;
    }


    /**
     * Returns the payload of the event.
     *
     * @return the payload of the event
     */
    public Object getPayload()
    {
        return payload;
    }
}
