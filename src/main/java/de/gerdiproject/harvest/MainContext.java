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


import java.nio.charset.Charset;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.impl.ChangeStateEvent;
import de.gerdiproject.harvest.harvester.AbstractHarvester;
import de.gerdiproject.harvest.state.impl.NotReadyState;
import de.gerdiproject.harvest.state.impl.ReadyState;
import de.gerdiproject.harvest.utils.CancelableFuture;


/**
 * This class provides static methods for retrieving the application name, the
 * dedicated harvester class and logger.
 *
 *
 * @author Robin Weiss
 */
public class MainContext
{
    private String moduleName;

    private static final String INIT_START = "Initializing Harvester...";
    private static final String INIT_FAILED = "Could not initialize Harvester!";
    private static final String INIT_SUCCESS = "%s initialized!";

    private static final Logger LOGGER = LoggerFactory.getLogger(MainContext.class);

    private AbstractHarvester harvester;
    private Charset charset;

    private static MainContext instance = new MainContext();


    /**
     * Singleton constructor.
     */
    private MainContext()
    {
    }


    /**
     * Returns the harvester singleton instance of this application
     *
     * @return this application's harvester class
     * @see de.gerdiproject.harvest.harvester.AbstractHarvester
     */
    public static AbstractHarvester getHarvester()
    {
        return instance.harvester;
    }


    /**
     * Returns the name of the application.
     *
     * @return the module name
     */
    public static String getModuleName()
    {
        return instance.moduleName;
    }

    /**
     * Retrieves the charset used for processing strings.
     * @return the charset that is used for processing strings
     */
    public static Charset getCharset()
    {
        return instance.charset;
    }


    /**
     * Sets up global parameters and the harvester.
     *
     * @param <T> an AbstractHarvester subclass
     * @param moduleName name of this application
     * @param harvesterClass an AbstractHarvester subclass
     * @param charset the default charset for processing strings
     * 
     * @see de.gerdiproject.harvest.harvester.AbstractHarvester
     */
    public static <T extends AbstractHarvester> void init(String moduleName, Class<T> harvesterClass, Charset charset)
    {
        // set global parameters
        instance.moduleName = moduleName;
        instance.charset = charset;

        // init harvester
        CancelableFuture<Boolean> initProcess = new CancelableFuture<>(() -> {
            LOGGER.info(INIT_START);
            instance.harvester = harvesterClass.newInstance();
            instance.harvester.init();
            return true;
        });
        initProcess.thenApply(onHarvesterInitializedSuccess)
        .exceptionally(onHarvesterInitializedFailed);
    }
    

    /**
     * This function is called when the asynchronous harvester initialization completes successfully.
     * It logs the success and changes the state machine's current state.
     */
    private static Function<Boolean, Boolean> onHarvesterInitializedSuccess = (Boolean state) -> {
    	
    	// log sucess
        LOGGER.info(String.format(INIT_SUCCESS, instance.harvester.getName()));

        // try to load configuration
        Configuration.loadFromDisk();

        // change state
        EventSystem.instance().sendEvent(new ChangeStateEvent(new ReadyState()));

        return true;
    };


    /**
     * This function is called when the asynchronous harvester fails to be initialized.
     * It logs the exception that caused the failure and changes the state machine's current state.
     */
    private static Function<Throwable, Boolean> onHarvesterInitializedFailed = (Throwable reason) -> {
    	
    	// log exception that caused the failure
        LOGGER.error(INIT_FAILED, reason.getCause());
        
        // change stage
        EventSystem.instance().sendEvent(new ChangeStateEvent(new NotReadyState()));

        return false;
    };
}
