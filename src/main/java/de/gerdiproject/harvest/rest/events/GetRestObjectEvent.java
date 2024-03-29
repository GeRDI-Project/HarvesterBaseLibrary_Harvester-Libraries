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
package de.gerdiproject.harvest.rest.events;

import de.gerdiproject.harvest.event.ISynchronousEvent;
import de.gerdiproject.harvest.rest.AbstractRestObject;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * This class is a stricted version of {@linkplain ISynchronousEvent}s that only allow
 * {@linkplain AbstractRestObject}s to be returned.
 *
 * @author Robin Weiss
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GetRestObjectEvent <T extends AbstractRestObject<T, ?>> implements ISynchronousEvent<T>
{
}
