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

import java.util.Iterator;

/**
 * This {@linkplain IExtractor} can extract an {@linkplain Iterator} in order
 * to be able to iterate lists or similar constructs.
 *
 * @author Robin Weiss
 */
public interface IIteratorExtractor <IN> extends IExtractor<Iterator<IN>>
{
    /**
     * Sets the interval of extracted elements to [startIndex, endIndex).
     *
     * @param startIndex the index of the first element to be retrieved (inclusive)
     * @param endIndex the index of the last element to be retrieved (exclusive)
     */
    void setRange(int startIndex, int endIndex);
}
