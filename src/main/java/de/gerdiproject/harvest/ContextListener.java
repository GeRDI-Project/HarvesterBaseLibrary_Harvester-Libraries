/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
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
package de.gerdiproject.harvest;


import de.gerdiproject.harvest.config.parameters.AbstractParameter;
import de.gerdiproject.harvest.harvester.AbstractHarvester;
import de.gerdiproject.harvest.state.StateMachine;
import de.gerdiproject.harvest.submission.AbstractSubmitter;
import de.gerdiproject.harvest.submission.impl.ElasticSearchSubmitter;
import de.gerdiproject.harvest.utils.cache.DocumentsCache;
import de.gerdiproject.json.GsonUtils;

import java.lang.reflect.ParameterizedType;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;


/**
 * This class registers a Logger and Harvester when the server is started. A
 * subclass with the @WebListener annotation must be defined.
 *
 * @param <T> an AbstractHarvester sub-class
 *
 * @see javax.servlet.annotation.WebListener
 * @see de.gerdiproject.harvest.harvester.AbstractHarvester
 *
 * @author Robin Weiss
 */
public class ContextListener<T extends AbstractHarvester> implements ServletContextListener
{
    // this warning is suppressed, because the only generic Superclass MUST be T. The cast will always succeed.
    @SuppressWarnings("unchecked")
    private Class<T> harvesterClass =
        (Class<T>)((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];

    /**
     * Retrieves the name of this harvester service.
     *
     * @return the name of this harvester service
     */
    protected String getServiceName()
    {
        // get name of main harvester class
        String name = harvesterClass.getSimpleName();

        // make sure the name ends with "HarvesterService"
        if (name.endsWith("arvester"))
            name += "Service";
        else if (!name.endsWith("arvesterService"))
            name += "HarvesterService";

        return name;
    }

    /**
     * Retrieves the charset that is used for harvesting and file operations.
     * @return a charset
     */
    protected Charset getCharset()
    {
        return StandardCharsets.UTF_8;
    }

    /**
     * Creates a GsonBuilder that is to be shared across the service.
     * If you have custom  JSON (de-)serialization adapters, you can register
     * them to the GsonBuilder when overriding this method.
     * @see JsonDeserializer
     * @see JsonSerializer
     *
     * @return a GsonBuilder that will be used to initialize {@link GsonUtils}
     */
    protected GsonBuilder createGsonBuilder()
    {
        return new GsonBuilder();
    }

    /**
     * Returns additional parameters that are specific to the harvester implementation.
     *
     * @return a list of parameters, or null, if no additional parameters are needed
     */
    protected List<AbstractParameter<?>> getHarvesterSpecificParameters()
    {
        return null;
    }


    /**
     * Creates a means to submit documents to any place.
     *
     * @return a harvested documents submitter
     */
    protected AbstractSubmitter createSubmitter()
    {
        return new ElasticSearchSubmitter();
    }


    /**
     * This method is called when the server is set up. Creates a logger and
     * harvester and sets them in the MainContext.
     *
     * @param sce
     *            the servlet context event that was initialized
     * @see de.gerdiproject.harvest.MainContext
     */
    @Override
    public void contextInitialized(ServletContextEvent sce)
    {
        // init Json utilities
        GsonUtils.init(createGsonBuilder());

        // init state machine
        StateMachine.init();

        // init main context
        MainContext.init(
            getServiceName(),
            harvesterClass,
            getCharset(),
            getHarvesterSpecificParameters()
        );

        // init documents cache and sender
        DocumentsCache.init(createSubmitter());
    }


    /**
     * This method is called when the server shuts down. Currently does nothing.
     *
     * @param sce
     *            the servlet context event that was destroyed
     */
    @Override
    public void contextDestroyed(ServletContextEvent sce)
    {
    }

}
