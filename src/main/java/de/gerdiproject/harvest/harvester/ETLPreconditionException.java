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
package de.gerdiproject.harvest.harvester;

/**
 * This exception signifies that a harvester has not started harvesting,
 * because a pre-condition failed.
 *
 * @author Robin Weiss
 */
public class ETLPreconditionException extends RuntimeException
{
    private static final long serialVersionUID = 502142825678604791L;


    /**
     * Constructor that requires a message.
     *
     * @param message a message explaining which pre-condition failed
     */
    public ETLPreconditionException(final String message)
    {
        super(message);
    }
}
