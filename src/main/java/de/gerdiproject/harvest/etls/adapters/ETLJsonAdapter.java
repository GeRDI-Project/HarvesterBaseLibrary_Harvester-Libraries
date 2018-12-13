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
package de.gerdiproject.harvest.etls.adapters;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import de.gerdiproject.harvest.etls.enums.ETLHealth;
import de.gerdiproject.harvest.etls.enums.ETLState;
import de.gerdiproject.harvest.etls.json.ETLJson;
import de.gerdiproject.harvest.etls.utils.TimestampedList;

/**
 * This adapter defines the (de-)serialization behavior of
 * {@linkplain ETLJson} objects.
 *
 * @author Robin Weiss
 */
public class ETLJsonAdapter implements JsonDeserializer<ETLJson>
{
    @Override
    public ETLJson deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
    {
        final JsonObject jsonObject = json.getAsJsonObject();

        final JsonElement nameEle = jsonObject.get("name");
        final String name = nameEle != null ? nameEle.getAsString() : null;

        final JsonElement versionHashEle = jsonObject.get("versionHash");
        final String versionHash = versionHashEle != null ? versionHashEle.getAsString() : null;

        final JsonElement harvestedCountEle = jsonObject.get("harvestedCount");
        final int harvestedCount = harvestedCountEle != null ? harvestedCountEle.getAsInt() : 0;

        final JsonElement maxDocumentCountEle = jsonObject.get("maxDocumentCount");
        final int maxDocumentCount = maxDocumentCountEle != null ? maxDocumentCountEle.getAsInt() : 1;

        final JsonElement statusHistoryEle = jsonObject.get("statusHistory");
        final Type statusHistoryType = new TypeToken<ETLState>() {} .getType();
        final TimestampedList<ETLState> statusHistory = context.deserialize(statusHistoryEle, statusHistoryType);

        final JsonElement healthHistoryEle = jsonObject.get("healthHistory");
        final Type healthHistoryType = new TypeToken<ETLHealth>() {} .getType();
        final TimestampedList<ETLHealth> healthHistory = context.deserialize(healthHistoryEle, healthHistoryType);

        return new ETLJson(name, statusHistory, healthHistory, harvestedCount, maxDocumentCount, versionHash);
    }
}
