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
package de.gerdiproject.harvest.application;


import java.lang.reflect.ParameterizedType;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gerdiproject.harvest.application.constants.ApplicationConstants;
import de.gerdiproject.harvest.application.events.ContextDestroyedEvent;
import de.gerdiproject.harvest.application.events.ContextInitializedEvent;
import de.gerdiproject.harvest.application.events.ContextResetEvent;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.harvester.AbstractHarvester;
import de.gerdiproject.harvest.submission.AbstractSubmitter;
import de.gerdiproject.harvest.submission.elasticsearch.ElasticSearchSubmitter;


/**
 * This class registers a Logger and Harvester when the server is started. A
 * sub-class with the @WebListener annotation must be implemented in order for
 * the harvester micro service to work.
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
    protected static final Logger LOGGER = LoggerFactory.getLogger(ContextListener.class);


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

        // remove HarvesterXXX if it exists within the name
        int harvesterIndex = name.toLowerCase().lastIndexOf(ApplicationConstants.HARVESTER_NAME_SUB_STRING);

        if (harvesterIndex != -1)
            name = name.substring(0, harvesterIndex);

        return name + ApplicationConstants.HARVESTER_SERVICE_NAME_SUFFIX;
    }


    /**
     * Creates a list of submitter classes that can be chosen to transfer data to the search index.
     *
     * @return a harvested documents submitter
     */
    protected List<Class<? extends AbstractSubmitter>> getSubmitterClasses()
    {
        final List<Class<? extends AbstractSubmitter>> submitterClasses = new LinkedList<>();
        submitterClasses.add(ElasticSearchSubmitter.class);
        return submitterClasses;
    }


    //////////////////////////////
    // Event Callback Functions //
    //////////////////////////////

    /**
     * This method is called when the server is set up. Creates a logger and
     * harvester and sets them in the MainContext.
     *
     * @param sce the servlet context event that was initialized
     * @see de.gerdiproject.harvest.application.MainContext
     */
    @Override
    public void contextInitialized(ServletContextEvent sce)
    {
        EventSystem.addListener(ContextResetEvent.class, this::onContextReset);

        // init main context
        MainContext.init(
            getServiceName(),
            harvesterClass,
            getSubmitterClasses());

        EventSystem.sendEvent(new ContextInitializedEvent());
    }


    /**
     * This method is called when the server shuts down. Currently does nothing.
     *
     * @param sce the servlet context event that was destroyed
     */
    @Override
    public void contextDestroyed(ServletContextEvent sce)
    {
        EventSystem.sendEvent(new ContextDestroyedEvent());

        String goodbyeMsg = String.format(ApplicationConstants.CONTEXT_DESTROYED, getServiceName());
        System.out.println(goodbyeMsg); // NOPMD The logger does not work at this point
    }


    /**
     * This event listener is called when the harvester service is reset.
     *
     * @param event the event that triggered the callback
     */
    protected void onContextReset(ContextResetEvent event)
    {
        String resetMsg = String.format(ApplicationConstants.CONTEXT_RESET, getServiceName());
        LOGGER.info(resetMsg);

        EventSystem.reset();
        contextInitialized(null);
    }
}
