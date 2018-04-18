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
package de.gerdiproject.harvest.submission.events;


import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.event.IEvent;

/**
 * This event aims to send all harvested documents to be processed and
 * submitted.
 *
 * @author Robin Weiss
 */
public class StartSubmissionEvent implements IEvent
{
    private final boolean canSubmitOutdatedDocs;
    private final boolean canSubmitFailedDocs;


    /**
     * Constructor that sets submission flags.
     * 
     * @param canSubmitOutdated true if partially harvested documents of failed
     *            or aborted harvests should be submitted
     * @param canSubmitFailedDocs true if the submission should be executed even
     *            if there are no changes
     */
    public StartSubmissionEvent(boolean canSubmitOutdated, boolean canSubmitFailedDocs)
    {
        this.canSubmitOutdatedDocs = canSubmitOutdated;
        this.canSubmitFailedDocs = canSubmitFailedDocs;
    }


    /**
     * Constructor that sets submission flags by retrieving them from the
     * {@linkplain Configuration}.
     */
    public StartSubmissionEvent()
    {
        final Configuration config = MainContext.getConfiguration();
        this.canSubmitOutdatedDocs = config.getParameterValue(ConfigurationConstants.SUBMIT_FORCED, Boolean.class);
        this.canSubmitFailedDocs = config.getParameterValue(ConfigurationConstants.SUBMIT_INCOMPLETE, Boolean.class);
    }


    /**
     * Returns true if the submission should be executed even if there are no
     * changes.
     * 
     * @return true if the submission should be executed even if there are no
     *         changes
     */
    public boolean canSubmitOutdatedDocuments()
    {
        return canSubmitOutdatedDocs;
    }


    /**
     * Returns true if partially harvested documents of failed or aborted
     * harvests should be submitted.
     * 
     * @return true if partially harvested documents of failed or aborted
     *         harvests should be submitted
     */
    public boolean isCanSubmitFailedDocuments()
    {
        return canSubmitFailedDocs;
    }


}
