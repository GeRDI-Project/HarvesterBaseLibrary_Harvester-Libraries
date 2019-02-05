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

import de.gerdiproject.harvest.config.parameters.IntegerParameter;
import de.gerdiproject.harvest.config.parameters.PasswordParameter;
import de.gerdiproject.harvest.config.parameters.StringParameter;
import de.gerdiproject.harvest.config.parameters.constants.ParameterMappingFunctions;
import de.gerdiproject.harvest.etls.AbstractIteratorETL;
import de.gerdiproject.harvest.etls.loaders.ILoader;

/**
 * This static class is a collection of constants that relate to {@linkplain ILoader}s.
 *
 * @author Robin Weiss
 */
public class LoaderConstants
{
    public static final String PARAMETER_CATEGORY = "Submission";

    public static final StringParameter URL_PARAM =
        new StringParameter(
        "url",
        PARAMETER_CATEGORY,
        null,
        ParameterMappingFunctions.createMapperForETLs(ParameterMappingFunctions::mapToUrlString));


    public static final StringParameter USER_NAME_PARAM =
        new StringParameter(
        "userName",
        PARAMETER_CATEGORY,
        null,
        ParameterMappingFunctions.createMapperForETLs(ParameterMappingFunctions::mapToString));

    public static final PasswordParameter PASSWORD_PARAM =
        new PasswordParameter(
        "password",
        PARAMETER_CATEGORY,
        null,
        ParameterMappingFunctions.createMapperForETLs(ParameterMappingFunctions::mapToString));

    public static final IntegerParameter MAX_BATCH_SIZE_PARAM =
        new IntegerParameter(
        "size",
        PARAMETER_CATEGORY,
        1048576,
        ParameterMappingFunctions.createMapperForETLs(ParameterMappingFunctions::mapToUnsignedInteger));

    public static final String LOADER_TYPE_PARAM_KEY = "loader";

    public static final String LOADED_PARTIAL_OK = "Loaded %d documents.";
    public static final String UNKNOWN_DOCUMENT_COUNT = "???";

    public static final String NO_URL_ERROR = "Loader Error: You need to set up a valid loader URL!";
    public static final String NO_DOCS_ERROR = "Loader Error: No documents were harvested!";
    public static final String CLEAN_LOAD_ERROR = "Loader Error: Unable to submit partially harvested documents while cancelling the harvest!";
    public static final String DOCUMENT_TOO_LARGE =
        "Loader Error: Size of document %s is %d bytes,"
        + " which is larger than the maximum permitted size of %d bytes.";

    public static final String NO_ITER_ETL_ERROR = "%s must be assigned to an " + AbstractIteratorETL.class.getSimpleName() + "!";


    /**
     * Private constructor, because this class just serves as a place to define
     * constants.
     */
    private LoaderConstants()
    {
    }
}
