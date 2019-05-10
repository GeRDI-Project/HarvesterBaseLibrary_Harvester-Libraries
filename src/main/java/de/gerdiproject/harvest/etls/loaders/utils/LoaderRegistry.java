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
package de.gerdiproject.harvest.etls.loaders.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.parameters.StringParameter;
import de.gerdiproject.harvest.config.parameters.constants.ParameterMappingFunctions;
import de.gerdiproject.harvest.etls.loaders.ILoader;
import de.gerdiproject.harvest.etls.loaders.constants.LoaderConstants;
import de.gerdiproject.harvest.etls.loaders.events.CreateLoaderEvent;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.IEventListener;

/**
 * This class maps loader names to {@linkplain ILoader}s at runtime.
 * The active loader can be created via the {@linkplain CreateLoaderEvent}.
 *
 * @author Robin Weiss
 */
public class LoaderRegistry implements IEventListener
{
    private final Map<String, Class<? extends ILoader<?>>> loaderMap;
    private StringParameter loaderParam;


    /**
     * Constructor that defines a default active loader.
     */
    public LoaderRegistry()
    {
        this.loaderMap = new HashMap<>();

        // the loader parameter may only be changed while no ETL is running, and only to registered loader classes
        final Function<String, String> paramChangeGuard =
            ParameterMappingFunctions.createMapperForETLs(
                ParameterMappingFunctions.createStringListMapper(loaderMap.keySet()));

        this.loaderParam = new StringParameter(
            LoaderConstants.LOADER_TYPE_PARAM_KEY,
            LoaderConstants.PARAMETER_CATEGORY,
            null,
            paramChangeGuard);
    }


    /**
     * Registers an {@linkplain ILoader} class via its class name.
     *
     * @param loaderClass the class of the loader that is to be registered
     */
    public void registerLoader(final Class<? extends ILoader<?>> loaderClass)
    {
        final String loaderName = loaderClass.getSimpleName();
        loaderMap.put(loaderName, loaderClass);

        // register loader in configuration if none was set before
        if (!loaderParam.isRegistered() && loaderParam.getValue() == null) {
            loaderParam.setValue(loaderName);
            this.loaderParam = Configuration.registerParameter(loaderParam);
        }
    }


    @Override
    public void addEventListeners()
    {
        EventSystem.addSynchronousListener(CreateLoaderEvent.class, this::createLoader);
    }


    @Override
    public void removeEventListeners()
    {
        EventSystem.removeSynchronousListener(CreateLoaderEvent.class);
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
            return loaderClass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException
                     | IllegalAccessException
                     | IllegalArgumentException
                     | InvocationTargetException
                     | NoSuchMethodException
                     | SecurityException e) {
            return null;
        }
    }
}
