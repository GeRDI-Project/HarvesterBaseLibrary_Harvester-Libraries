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
package de.gerdiproject.harvest.submission;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.parameters.SubmitterParameter;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.IEventListener;
import de.gerdiproject.harvest.submission.constants.SubmissionConstants;
import de.gerdiproject.harvest.submission.events.GetSubmitterIdsEvent;
import de.gerdiproject.harvest.submission.events.StartSubmissionEvent;

/**
 * This class maps submitter names to {@linkplain AbstractSubmitter}s at runtime.
 * The active submitter can be selected a parameter change.
 *
 * @author Robin Weiss
 */
public class SubmitterManager implements IEventListener
{
    private Map<String, AbstractSubmitter> submitterMap;
    private SubmitterParameter submitterParam;


    /**
     * Constructor that defined a default active submitter.
     */
    public SubmitterManager()
    {
        this.submitterMap = new HashMap<>();
        this.submitterParam = SubmissionConstants.SUBMITTER_TYPE_PARAM.copy();
    }


    /**
     * Registers a submitter class via an identifier.
     * @param submitter the submitter that is to be registered
     */
    public void registerSubmitter(AbstractSubmitter submitter)
    {
        submitterMap.put(submitter.getId(), submitter);

        // register submitter in configuration if none was set before
        if (!submitterParam.isRegistered() && submitterParam.getValue() == null) {
            submitterParam.setValue(submitter.getId(), null);
            this.submitterParam = Configuration.registerParameter(submitterParam);
        }
    }


    @Override
    public void addEventListeners()
    {
        EventSystem.addSynchronousListener(StartSubmissionEvent.class, this::onStartSubmission);
        EventSystem.addSynchronousListener(GetSubmitterIdsEvent.class, this::getSubmitterIDs);
    }


    @Override
    public void removeEventListeners()
    {
        EventSystem.removeSynchronousListener(StartSubmissionEvent.class);
        EventSystem.removeSynchronousListener(GetSubmitterIdsEvent.class);
    }


    /**
     * Returns all registered submitter identifiers.
     *
     * @return a set of registered submitter identifiers
     */
    private Set<String> getSubmitterIDs()
    {
        return submitterMap.keySet();
    }


    //////////////////////////////
    // Event Callback Functions //
    //////////////////////////////

    /**
     * Event callback: When a submission starts, submit the cache file via the
     * {@linkplain AbstractSubmitter}.
     */
    private String onStartSubmission()
    {
        AbstractSubmitter submitter = submitterMap.get(submitterParam.getValue());

        if (submitter == null)
            return SubmissionConstants.NO_SUBMITTER_CONFIGURED;

        try {
            return submitter.submitAll();
        } catch (IllegalStateException e) {
            return e.getMessage();
        }
    }
}
