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
package de.gerdiproject.harvest.config.parameters;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.state.IState;
import de.gerdiproject.harvest.state.impl.ErrorState;
import de.gerdiproject.harvest.state.impl.HarvestingState;
import de.gerdiproject.harvest.state.impl.IdleState;
import de.gerdiproject.harvest.state.impl.InitializationState;
import de.gerdiproject.harvest.state.impl.SubmittingState;
import de.gerdiproject.harvest.submission.elasticsearch.constants.ElasticSearchConstants;


/**
 * This factory creates {@linkplain AbstractParameter}s that are stored in the
 * {@linkplain Configuration}.
 *
 * @author Robin Weiss
 */
public final class ParameterFactory
{
    /**
     * Creates a map of default parameters.
     *
     * @return a map of default parameters.
     */
    public static Map<String, AbstractParameter<?>> createDefaultParameters()
    {
        Map<String, AbstractParameter<?>> params = new LinkedHashMap<>();

        AbstractParameter<?> autoSubmit = createAutoSubmit();
        AbstractParameter<?> readFromDisk = createReadFromDisk();
        AbstractParameter<?> writeToDisk = createWriteToDisk();
        AbstractParameter<?> submitter = createSubmitter();
        AbstractParameter<?> submitUrl = createSubmissionUrl();
        AbstractParameter<?> submitSize = createSubmissionSize();
        AbstractParameter<?> submitName = createSubmissionUserName();
        AbstractParameter<?> submitPassword = createSubmissionPassword();
        AbstractParameter<?> submitIncompleteHarvests = createSubmitIncomplete();
        AbstractParameter<?> submitOutdated = createSubmitOutdated();

        params.put(submitter.getKey(), submitter);
        params.put(autoSubmit.getKey(), autoSubmit);
        params.put(submitUrl.getKey(), submitUrl);
        params.put(submitName.getKey(), submitName);
        params.put(submitPassword.getKey(), submitPassword);
        params.put(submitSize.getKey(), submitSize);
        params.put(submitIncompleteHarvests.getKey(), submitIncompleteHarvests);
        params.put(submitOutdated.getKey(), submitOutdated);
        params.put(readFromDisk.getKey(), readFromDisk);
        params.put(writeToDisk.getKey(), writeToDisk);

        return params;
    }


    /**
     * Creates a map of harvester specific parameters.
     *
     * @param harvesterParams a list of harvester specific parameters
     *
     * @return a map of harvester specific parameters
     */
    public static Map<String, AbstractParameter<?>> createHarvesterParameters(List<AbstractParameter<?>> harvesterParams)
    {
        Map<String, AbstractParameter<?>> params = new LinkedHashMap<>();

        // create start-and end-index
        AbstractParameter<?> harvestStartIndex = createHarvestStartIndex();
        AbstractParameter<?> harvestEndIndex = createHarvestEndIndex();
        AbstractParameter<?> forceHarvest = createForceHarvest();

        // add indices to parameters
        params.put(harvestStartIndex.getKey(), harvestStartIndex);
        params.put(harvestEndIndex.getKey(), harvestEndIndex);
        params.put(forceHarvest.getKey(), forceHarvest);

        if (harvesterParams != null) {
            for (AbstractParameter<?> hParam : harvesterParams)
                params.put(hParam.getKey(), hParam);
        }

        return params;
    }


    /**
     * Creates a parameter for changing the target of the submitted documents.
     *
     * @return a parameter for changing the target of the submitted documents
     */
    public static SubmitterParameter createSubmitter()
    {
        final List<Class<? extends IState>> allowedStates = Arrays.asList(
                                                                InitializationState.class,
                                                                ErrorState.class,
                                                                IdleState.class,
                                                                HarvestingState.class);
        return new SubmitterParameter(ConfigurationConstants.SUBMITTER_TYPE, allowedStates, ElasticSearchConstants.SUBMITTER_ID);
    }


    /**
     * Creates a flag-parameter for changing the automatic submission of
     * harvested documents.
     *
     * @return a flag-parameter for the automatic submission of harvested
     *         documents
     */
    public static BooleanParameter createAutoSubmit()
    {
        final List<Class<? extends IState>> allowedStates = Arrays.asList(
                                                                InitializationState.class,
                                                                ErrorState.class,
                                                                IdleState.class,
                                                                HarvestingState.class,
                                                                SubmittingState.class);
        return new BooleanParameter(ConfigurationConstants.AUTO_SUBMIT, allowedStates, false);
    }


    /**
     * Creates a flag-parameter for changing whether all HTTP responses should
     * be cached on disk.
     *
     * @return a flag-parameter for whether all HTTP responses should be cached
     *         on disk
     */
    public static BooleanParameter createWriteToDisk()
    {
        final List<Class<? extends IState>> allowedStates = Arrays.asList(
                                                                ErrorState.class,
                                                                IdleState.class,
                                                                SubmittingState.class);
        return new BooleanParameter(ConfigurationConstants.WRITE_HTTP_TO_DISK, allowedStates, false);
    }


    /**
     * Creates a flag-parameter for changing whether documents should be
     * harvested from cached HTTP responses from disk.
     *
     * @return a flag-parameter for whether documents should be harvested from
     *         cached HTTP responses from disk
     */
    public static BooleanParameter createReadFromDisk()
    {
        final List<Class<? extends IState>> allowedStates = Arrays.asList(
                                                                ErrorState.class,
                                                                IdleState.class,
                                                                SubmittingState.class);
        return new BooleanParameter(ConfigurationConstants.READ_HTTP_FROM_DISK, allowedStates, false);
    }


    /**
     * Creates a flag-parameter for changing whether documents should be
     * harvested, even if they did not change since the last harvest.
     *
     * @return a flag-parameter for whether cached documents should be retained
     *         after a new harvest
     */
    public static AbstractParameter<?> createForceHarvest()
    {
        final List<Class<? extends IState>> allowedStates = Arrays.asList(
                                                                InitializationState.class,
                                                                ErrorState.class,
                                                                IdleState.class,
                                                                SubmittingState.class);
        return new BooleanParameter(ConfigurationConstants.FORCE_HARVEST, allowedStates, false);
    }


    /**
     * Creates a parameter for changing the URL to which the harvested documents
     * are being posted.
     *
     * @return a parameter for the URL to which the harvested documents are
     *         being posted
     */
    public static UrlParameter createSubmissionUrl()
    {
        final List<Class<? extends IState>> allowedStates = Arrays.asList(
                                                                InitializationState.class,
                                                                ErrorState.class,
                                                                IdleState.class,
                                                                HarvestingState.class);
        return new UrlParameter(ConfigurationConstants.SUBMISSION_URL, allowedStates, null);
    }


    /**
     * Creates a parameter for changing the user name for sending documents.
     *
     * @return a parameter for the user name for sending documents
     */
    public static StringParameter createSubmissionUserName()
    {
        final List<Class<? extends IState>> allowedStates = Arrays.asList(
                                                                InitializationState.class,
                                                                ErrorState.class,
                                                                IdleState.class,
                                                                HarvestingState.class);
        return new StringParameter(ConfigurationConstants.SUBMISSION_USER_NAME, allowedStates, null);
    }


    /**
     * Creates a parameter for changing the user password for sending documents.
     *
     * @return a parameter for the user password for sending documents
     */
    public static PasswordParameter createSubmissionPassword()
    {
        final List<Class<? extends IState>> allowedStates = Arrays.asList(
                                                                InitializationState.class,
                                                                ErrorState.class,
                                                                IdleState.class,
                                                                HarvestingState.class);
        return new PasswordParameter(ConfigurationConstants.SUBMISSION_PASSWORD, allowedStates);
    }


    /**
     * Creates a parameter for changing the maximum size of the JSON-String of a
     * single POST-request to submit documents.
     *
     * @return a parameter for the maximum size of the JSON-String of a single
     *         POST-request to submit documents
     */
    public static IntegerParameter createSubmissionSize()
    {
        final List<Class<? extends IState>> allowedStates = Arrays.asList(
                                                                InitializationState.class,
                                                                ErrorState.class,
                                                                IdleState.class,
                                                                HarvestingState.class);
        return new IntegerParameter(ConfigurationConstants.SUBMISSION_SIZE, allowedStates, 1048576);
    }


    /**
     * Creates a flag-parameter for changing whether documents should be
     * re-submitted even if their version did not change.
     *
     * @return a flag-parameter for changing whether documents should be
     *         re-submitted even if their version did not change
     */
    private static BooleanParameter createSubmitOutdated()
    {
        final List<Class<? extends IState>> allowedStates = Arrays.asList(
                                                                InitializationState.class,
                                                                ErrorState.class,
                                                                IdleState.class,
                                                                HarvestingState.class);
        return new BooleanParameter(ConfigurationConstants.SUBMIT_OUTDATED, allowedStates, false);
    }


    /**
     * Creates a flag-parameter for changing whether documents can be submitted
     * even if their respective harvester failed or was aborted.
     *
     * @return a flag-parameter for changing whether documents can be submitted
     *         even if their respective harvester failed or was aborted.
     */
    private static BooleanParameter createSubmitIncomplete()
    {
        final List<Class<? extends IState>> allowedStates = Arrays.asList(
                                                                InitializationState.class,
                                                                ErrorState.class,
                                                                IdleState.class,
                                                                HarvestingState.class);
        return new BooleanParameter(ConfigurationConstants.SUBMIT_INCOMPLETE, allowedStates, false);
    }


    /**
     * Creates a parameter for changing the index of the first document that is
     * to be harvested.
     *
     * @return a parameter for the index of the first document that is to be
     *         harvested
     */
    public static IntegerParameter createHarvestStartIndex()
    {
        return new IntegerParameter(
                   ConfigurationConstants.HARVEST_START_INDEX,
                   ConfigurationConstants.HARVESTER_PARAM_ALLOWED_STATES,
                   0);
    }


    /**
     * Creates a parameter for changing the index of the first document that is
     * not to be harvested anymore.
     *
     * @return a parameter for the index of the first document that is not to be
     *         harvested anymore
     */
    public static IntegerParameter createHarvestEndIndex()
    {
        return new IntegerParameter(
                   ConfigurationConstants.HARVEST_END_INDEX,
                   ConfigurationConstants.HARVESTER_PARAM_ALLOWED_STATES,
                   Integer.MAX_VALUE);
    }


    /**
     * Private constructor, because this is a static factory.
     */
    private ParameterFactory()
    {

    }
}
