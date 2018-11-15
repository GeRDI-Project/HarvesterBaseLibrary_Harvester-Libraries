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


import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gerdiproject.harvest.application.constants.ApplicationConstants;
import de.gerdiproject.harvest.application.events.ContextDestroyedEvent;
import de.gerdiproject.harvest.application.events.ContextInitializedEvent;
import de.gerdiproject.harvest.application.events.ResetContextEvent;
import de.gerdiproject.harvest.etls.AbstractETL;
import de.gerdiproject.harvest.etls.loaders.DiskLoader;
import de.gerdiproject.harvest.etls.loaders.ElasticSearchLoader;
import de.gerdiproject.harvest.etls.loaders.ILoader;
import de.gerdiproject.harvest.event.EventSystem;


/**
 * This class registers a Logger and Harvester when the server is started. A
 * sub-class with the @WebListener annotation must be implemented in order for
 * the harvester micro service to work.
 *
 * @see javax.servlet.annotation.WebListener
 * @see de.gerdiproject.harvest.etls.AbstractETL
 *
 * @author Robin Weiss
 */
public abstract class ContextListener implements ServletContextListener
{
    protected static final Logger LOGGER = LoggerFactory.getLogger(ContextListener.class);


    /**
     * Retrieves the name of this harvester service.
     *
     * @return the name of this harvester service
     */
    protected String getServiceName()
    {
        return getRepositoryName() + ApplicationConstants.HARVESTER_SERVICE_NAME_SUFFIX;
    }


    /**
     * Retrieves the name of the repository that is to be harvested.
     *
     * @return the name of the repository that is to be harvested
     */
    protected String getRepositoryName()
    {
        final String className = getClass().getSimpleName();
        final int suffixIndex = className.lastIndexOf(ContextListener.class.getSimpleName());

        return suffixIndex == -1
               ? className
               : className.substring(0, suffixIndex);
    }


    /**
     * Creates a list of {@linkplain AbstractETL} implementations that can be chosen to extract, transform and load
     * data from the targeted repository.
     *
     * @return a list of {@linkplain AbstractETL}s
     */
    protected abstract List<? extends AbstractETL<?, ?>> createETLs();


    /**
     * Creates a list of {@linkplain ILoader} implementations that can be chosen to transfer data to the search index.
     *
     * @return a list of {@linkplain ILoader}
     */
    protected List<Class<? extends ILoader<?>>> getLoaderClasses()
    {
        final List<Class<? extends ILoader<?>>> loaderClasses = new LinkedList<>();
        loaderClasses.add(ElasticSearchLoader.class);
        loaderClasses.add(DiskLoader.class);
        return loaderClasses;
    }


    //////////////////////////////
    // Event Callback Functions //
    //////////////////////////////

    /**
     * This method is called when the server is set up. Creates a logger and
     * harvester and sets them in the MainContext.
     *
     * @param sce the servlet context event that was  initialized
     * @see de.gerdiproject.harvest.application.MainContext
     */
    @Override
    public void contextInitialized(ServletContextEvent sce)
    {
        EventSystem.addListener(ResetContextEvent.class, this::onResetContext);

        // init main context
        MainContext.init(
            getClass(),
            this::getRepositoryName,
            this::createETLs,
            getLoaderClasses());

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
        MainContext.destroy();

        String goodbyeMsg = String.format(ApplicationConstants.CONTEXT_DESTROYED, getServiceName());
        System.out.println(goodbyeMsg); // NOPMD The logger does not work at this point
    }


    /**
     * This event listener is called when the harvester service is reset.
     *
     * @param event the event that triggered the callback
     */
    protected void onResetContext(ResetContextEvent event)
    {
        String resetMsg = String.format(ApplicationConstants.CONTEXT_RESET, getServiceName());
        LOGGER.info(resetMsg);

        EventSystem.reset();
        contextInitialized(null);
    }
}
