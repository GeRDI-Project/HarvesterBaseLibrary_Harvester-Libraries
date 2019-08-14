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
package de.gerdiproject.harvest.etls.json;

import java.util.List;

import de.gerdiproject.harvest.etls.AbstractETL;
import de.gerdiproject.harvest.etls.enums.ETLHealth;
import de.gerdiproject.harvest.etls.enums.ETLState;
import de.gerdiproject.harvest.etls.utils.ETLManager;
import de.gerdiproject.harvest.etls.utils.TimestampedEntry;
import lombok.Value;

/**
 * This class represents a JSON object containing details
 * of an {@linkplain AbstractETL} or the {@linkplain ETLManager}.
 *
 * @author Robin Weiss
 */
@Value
public class ETLJson
{
    private final String name;
    private final List<TimestampedEntry<ETLState>> stateHistory;
    private final List<TimestampedEntry<ETLHealth>> healthHistory;
    private final int harvestedCount;
    private final int maxDocumentCount;
    private final String versionHash;
}
