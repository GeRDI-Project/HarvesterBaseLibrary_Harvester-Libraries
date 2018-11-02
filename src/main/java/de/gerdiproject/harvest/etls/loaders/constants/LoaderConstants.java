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

/**
 * This static class is a collection of constants that relate to the submission
 * of documents.
 *
 * @author Robin Weiss
 */
public class LoaderConstants
{
    public static final String SUBMISSION_START = "Submitter '%s' is valid.";
    public static final String SUBMISSION_DONE_ALL_OK = "Submission done! All documents were submitted!";
    public static final String SUBMISSION_DONE_SOME_FAILED = "Submission done! Failed to submit %d documents!";
    public static final String SUBMISSION_DONE_ALL_FAILED = "Submission failed!";
    public static final String SUBMISSION_INTERRUPTED = "Submission interrupted unexpectedly!";

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

    public static final String SUBMIT_PARTIAL_OK = " Submitted %d documents.";
    public static final String SUBMIT_PARTIAL_FAILED = "Error submitting documents %s to %s.";
    public static final String UNKNOWN_DOCUMENT_COUNT = "???";

    public static final String REGISTER_ERROR = "Could not register submitter: %s";
    public static final String NO_URL_ERROR = "Cannot submit documents: You need to set up a valid submission URL!";
    public static final String NO_DOCS_ERROR = "Cannot submit documents: There are no documents to submit!";
    public static final String DOCUMENT_TOO_LARGE =
        "Cannot submit document %s, because its submission size is %d bytes,"
        + " which is larger than the maximum permitted size of %d bytes.";

    public static final String NO_ITER_ETL_ERROR = "%s must be assigned to an " + AbstractIteratorETL.class.getSimpleName() + "!";
    public static final String NO_SUBMITTER_CONFIGURED = "No Submitter was configured! You can change it by sending a PUT request to .../config";


    /**
     * Private constructor, because this class just serves as a place to define
     * constants.
     */
    private LoaderConstants()
    {
    }
}
