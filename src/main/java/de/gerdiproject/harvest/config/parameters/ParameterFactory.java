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
import de.gerdiproject.harvest.state.impl.SavingState;
import de.gerdiproject.harvest.state.impl.SubmittingState;


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

        AbstractParameter<?> autoSave = createAutoSave();
        AbstractParameter<?> autoSubmit = createAutoSubmit();
        AbstractParameter<?> readFromDisk = createReadFromDisk();
        AbstractParameter<?> writeToDisk = createWriteToDisk();
        AbstractParameter<?> submitUrl = createSubmissionUrl();
        AbstractParameter<?> submitSize = createSubmissionSize();
        AbstractParameter<?> submitName = createSubmissionUserName();
        AbstractParameter<?> submitPassword = createSubmissionPassword();
        AbstractParameter<?> deleteUnfinishedSaves = createDeleteUnfinishedSaves();
        AbstractParameter<?> submitIncompleteHarvests = createSubmitIncomplete();
        AbstractParameter<?> submitOutdated = createSubmitOutdated();
        AbstractParameter<?> persistCache = createPersistCache();

        params.put(autoSave.getKey(), autoSave);
        params.put(autoSubmit.getKey(), autoSubmit);
        params.put(submitUrl.getKey(), submitUrl);
        params.put(submitName.getKey(), submitName);
        params.put(submitPassword.getKey(), submitPassword);
        params.put(submitSize.getKey(), submitSize);
        params.put(submitIncompleteHarvests.getKey(), submitIncompleteHarvests);
        params.put(submitOutdated.getKey(), submitOutdated);
        params.put(readFromDisk.getKey(), readFromDisk);
        params.put(writeToDisk.getKey(), writeToDisk);
        params.put(deleteUnfinishedSaves.getKey(), deleteUnfinishedSaves);
        params.put(persistCache.getKey(), persistCache);

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
     * Creates a flag-parameter for changing the automatic saving of harvested
     * documents to disk.
     *
     * @return a flag-parameter for the automatic saving of harvested documents
     *         to disk
     */
    public static BooleanParameter createAutoSave()
    {
        final List<Class<? extends IState>> allowedStates = Arrays.asList(
                                                                InitializationState.class,
                                                                ErrorState.class,
                                                                IdleState.class,
                                                                HarvestingState.class,
                                                                SavingState.class,
                                                                SubmittingState.class);
        return new BooleanParameter(ConfigurationConstants.AUTO_SAVE, allowedStates, true);
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
                                                                SavingState.class,
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
                                                                SavingState.class,
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
                                                                SavingState.class,
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
                                                                SavingState.class,
                                                                SubmittingState.class);
        return new BooleanParameter(ConfigurationConstants.FORCE_HARVEST, allowedStates, false);
    }


    /**
     * Creates a flag-parameter for changing whether saved documents should be
     * deleted when the save process is aborted or fails.
     *
     * @return a flag-parameter for whether cached documents should be deleted
     *         when the save process is aborted or fails
     */
    public static AbstractParameter<?> createDeleteUnfinishedSaves()
    {
        final List<Class<? extends IState>> allowedStates = Arrays.asList(
                                                                InitializationState.class,
                                                                ErrorState.class,
                                                                IdleState.class,
                                                                HarvestingState.class,
                                                                SubmittingState.class);
        return new BooleanParameter(ConfigurationConstants.DELETE_UNFINISHED_SAVE, allowedStates, true);
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
                                                                HarvestingState.class,
                                                                SavingState.class);
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
                                                                HarvestingState.class,
                                                                SavingState.class);
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
                                                                HarvestingState.class,
                                                                SavingState.class);
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
                                                                HarvestingState.class,
                                                                SavingState.class);
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
                                                                HarvestingState.class,
                                                                SavingState.class);
        return new BooleanParameter(ConfigurationConstants.SUBMIT_FORCED, allowedStates, false);
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
                                                                HarvestingState.class,
                                                                SavingState.class);
        return new BooleanParameter(ConfigurationConstants.SUBMIT_INCOMPLETE, allowedStates, false);
    }




    /**
     * TEMPORARY WORK-AROUND SAI-1186<br><br>
     *
     * If false, the cached documents are deleted on startup, which causes no
     * performance impact in the initialization phase and when harvesting the last document.
     * This should be set to false for harvesters that harvest more than 30.000 documents.
     *
     * @return a flag-parameter for changing whether cached documents should be persisted
     */
    private static AbstractParameter<?> createPersistCache()
    {
        final List<Class<? extends IState>> allowedStates = Arrays.asList(
                                                                ErrorState.class,
                                                                IdleState.class,
                                                                SubmittingState.class,
                                                                SavingState.class);
        return new BooleanParameter(ConfigurationConstants.PERSIST_CACHE, allowedStates, true);
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
