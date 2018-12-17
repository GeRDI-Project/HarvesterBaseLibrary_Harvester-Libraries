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
package de.gerdiproject.harvest.etls.loaders.constants;

import java.util.regex.Pattern;

import de.gerdiproject.harvest.etls.loaders.ElasticSearchLoader;

/**
 * This static class is a collection of constants that are related to the {@linkplain ElasticSearchLoader}.
 *
 * @author Robin Weiss
 */
public class ElasticSearchConstants
{
    public static final String BATCH_INDEX_INSTRUCTION = "{\"index\":{\"_id\":\"%s\"}}%n%s%n";
    public static final String BATCH_DELETE_INSTRUCTION = "{\"delete\":{\"_id\":\"%s\"}}%n";
    public static final String BULK_SUBMISSION_URL_SUFFIX = "_bulk";

    public static final String INVALID_URL_ERROR = "Invalid Elasticsearch API URL: %s";

    // INVALID FIELD HANDLING
    public static final String DOCUMENTS_RESUBMIT = "Resubmitting documents after removing invalid fields.";
    public static final Pattern PARSE_ERROR_REASON_PATTERN = Pattern.compile("failed to parse \\[([^.]+)[A-Za-z.]*\\]");
    public static final String CANNOT_FIX_INVALID_DOCUMENT_ERROR = "Could not remove invalid field '%s' from Document %s!";

    // DATE RANGE FIX
    public static final String DATE_REGEX = "\"value\":(\"\\d\\d\\d\\d-\\d\\d-\\d\\dT\\d\\d:\\d\\d[^\"/]*Z\")";
    public static final String DATE_REPLACEMENT = "\"value\":\\{\"gte\":$1,\"lte\":$1\\}";

    public static final String DATE_RANGE_REGEX =
        "\"value\":\"(\\d\\d\\d\\d-\\d\\d-\\d\\dT\\d\\d:\\d\\d[^\"]*?Z)?/(\\d\\d\\d\\d-\\d\\d-\\d\\dT\\d\\d:\\d\\d[^\"]*?Z)?\"";
    public static final String DATE_RANGE_REPLACEMENT = "\"value\":\\{\"gte\":\"$1\",\"lte\":\"$2\"\\}";
    public static final String EMPTY_DATE_RANGE_REGEX = "(\"gte\":\"\",)|(,\"lte\":\"\")";
    public static final String EMPTY_DATE_RANGE_REPLACEMENT = "";

    // SERVER RESPONSE JSON
    public static final String LOAD_DOCUMENT_ERROR = "Loader Error: Document %s%n%s";
    public static final String LOAD_DOCUMENT_ERROR_REASON = "  %s: %s";
    public static final String LOAD_DOCUMENT_ERROR_CAUSE = ", caused by%n  %s: %s";

    public static final String BASIC_AUTH_PREFIX = "Basic ";


    /**
     * Private constructor, because this class just serves as a place to define
     * constants.
     */
    private ElasticSearchConstants()
    {
    }
}
