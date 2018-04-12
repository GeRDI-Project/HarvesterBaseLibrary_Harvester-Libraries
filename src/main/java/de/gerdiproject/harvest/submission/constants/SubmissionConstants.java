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
package de.gerdiproject.harvest.submission.constants;

/**
 * This static class is a collection of constants that relate to the submission
 * of documents.
 *
 * @author Robin Weiss
 */
public class SubmissionConstants
{
    public static final String SUBMISSION_START = "Submitting documents to: %s";
    public static final String SUBMISSION_DONE_ALL_OK = "Submission done! All documents were submitted!";
    public static final String SUBMISSION_DONE_SOME_FAILED = "Submission done! Failed to submit %d documents!";
    public static final String SUBMISSION_DONE_ALL_FAILED = "Submission failed!";
    public static final String SUBMISSION_INTERRUPTED = "Submission interrupted unexpectedly!";


    public static final String SUBMIT_PARTIAL_OK = " Submitted documents %d to %d.";
    public static final String SUBMIT_PARTIAL_FAILED = "Error submitting documents %s to %s.";
    public static final String UNKNOWN_DOCUMENT_COUNT = "???";

    public static final String NO_URL_ERROR = "Cannot submit documents: You need to set up a valid submission URL!";
    public static final String NO_DOCS_ERROR = "There are no documents to submit!";
    public static final String DOCUMENT_TOO_LARGE = "Cannot submit document, because its submission size is %d bytes,"
                                                    + " which is larger than the maximum permitted size of %d bytes. Failed document: %s";


    /**
     * Private constructor, because this class just serves as a place to define
     * constants.
     */
    private SubmissionConstants()
    {
    }
}
