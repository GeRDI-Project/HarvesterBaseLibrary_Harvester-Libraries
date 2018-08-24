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
import java.util.function.Consumer;

import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.config.events.GlobalParameterChangedEvent;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.IEventListener;
import de.gerdiproject.harvest.submission.events.GetSubmitterIdsEvent;

/**
 * This class maps submitter names to {@linkplain AbstractSubmitter}s at runtime.
 * The active submitter can be selected a parameter change.
 *
 * @author Robin Weiss
 */
public class SubmitterManager implements IEventListener
{
    private Map<String, AbstractSubmitter> submitterMap;
    private AbstractSubmitter activeSubmitter;


    /**
     * Constructor that defined a default active submitter.
     */
    public SubmitterManager()
    {
        this.submitterMap = new HashMap<>();
        this.activeSubmitter = null;
    }


    /**
     * Registers a submitter class via an identifier.
     * @param submitter the submitter that is to be registered
     */
    public void registerSubmitter(AbstractSubmitter submitter)
    {
        submitterMap.put(submitter.getId(), submitter);
    }


    @Override
    public void addEventListeners()
    {
        EventSystem.addListener(GlobalParameterChangedEvent.class, onGlobalParameterChanged);
        EventSystem.addSynchronousListener(GetSubmitterIdsEvent.class, this::getSubmitterIDs);

    }


    @Override
    public void removeEventListeners()
    {
        EventSystem.removeListener(GlobalParameterChangedEvent.class, onGlobalParameterChanged);
        EventSystem.removeSynchronousListener(GetSubmitterIdsEvent.class);
    }


    /**
     * Returns all registered submitter identifiers.
     *
     * @return a set of registered submitter identifiers
     */
    private Set<String> getSubmitterIDs(GetSubmitterIdsEvent event) // NOPMD event callbacks always need their argument
    {
        return submitterMap.keySet();
    }


    /**
     * Changes the currently active submitter.
     *
     * @param submitterId the identifier of the new active submitter
     */
    private void setActiveSubmitter(String submitterId)
    {
        if (submitterMap.containsKey(submitterId) && (activeSubmitter == null || !submitterId.equals(activeSubmitter.getId()))) {
            final AbstractSubmitter oldSubmitter = activeSubmitter;
            activeSubmitter = submitterMap.get(submitterId);

            if (oldSubmitter != null) {
                oldSubmitter.removeEventListeners();
                activeSubmitter.setValues(oldSubmitter);
            }

            activeSubmitter.addEventListeners();
        }
    }


    //////////////////////////////
    // Event Callback Functions //
    //////////////////////////////

    /**
     * Event callback for changing global parameter names.
     */
    private Consumer<GlobalParameterChangedEvent> onGlobalParameterChanged = (GlobalParameterChangedEvent event) -> {
        final String key = event.getParameter().getKey();

        // check if the correct parameter was set
        if (key.equals(ConfigurationConstants.SUBMITTER_TYPE))
            setActiveSubmitter(event.getParameter().getStringValue());
    };

}
