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
import de.gerdiproject.harvest.config.parameters.LoaderParameter;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.IEventListener;
import de.gerdiproject.harvest.harvester.loaders.ILoader;
import de.gerdiproject.harvest.submission.constants.SubmissionConstants;
import de.gerdiproject.harvest.submission.events.CreateLoaderEvent;
import de.gerdiproject.harvest.submission.events.GetLoaderNamesEvent;

/**
 * This class maps loader names to {@linkplain ILoader}s at runtime.
 * The active loader can be created via the {@linkplain CreateLoaderEvent}.
 *
 * @author Robin Weiss
 */
public class LoaderFactory implements IEventListener
{
    private final Map<String, Class<? extends ILoader<?>>> loaderMap;
    private LoaderParameter loaderParam;


    /**
     * Constructor that defined a default active submitter.
     */
    public LoaderFactory()
    {
        this.loaderMap = new HashMap<>();
        this.loaderParam = SubmissionConstants.SUBMITTER_TYPE_PARAM.copy();
    }


    /**
     * Registers a submitter class via an identifier.
     * @param submitter the submitter that is to be registered
     */
    public void registerLoader(Class<? extends ILoader<?>> loaderClass)
    {
        final String loaderName = loaderClass.getSimpleName();
        loaderMap.put(loaderName, loaderClass);

        // register submitter in configuration if none was set before
        if (!loaderParam.isRegistered() && loaderParam.getValue() == null) {
            loaderParam.setValue(loaderName, null);
            this.loaderParam = Configuration.registerParameter(loaderParam);
        }
    }


    @Override
    public void addEventListeners()
    {
        EventSystem.addSynchronousListener(CreateLoaderEvent.class, this::createLoader);
        EventSystem.addSynchronousListener(GetLoaderNamesEvent.class, this::getLoaderNames);
    }


    @Override
    public void removeEventListeners()
    {
        EventSystem.removeSynchronousListener(CreateLoaderEvent.class);
        EventSystem.removeSynchronousListener(GetLoaderNamesEvent.class);
    }


    /**
     * Returns all registered loader names.
     *
     * @return a set of registered loader names
     */
    private Set<String> getLoaderNames()
    {
        return loaderMap.keySet();
    }


    //////////////////////////////
    // Event Callback Functions //
    //////////////////////////////

    /**
     * Event Callback: Instantiates the loader class that is stored in the map under
     * its simple class name.
     *
     * @return an instance of the configured loader class.
     */
    private ILoader<?> createLoader()
    {
        final Class<? extends ILoader<?>> loaderClass = loaderMap.get(loaderParam.getValue());

        try {
            return loaderClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            return null;
        }
    }


}
