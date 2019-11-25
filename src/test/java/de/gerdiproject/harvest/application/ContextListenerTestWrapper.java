/*
 *  Copyright © 2019 Robin Weiss (http://www.gerdi-project.de/)
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
package de.gerdiproject.harvest.application;

import java.io.File;
import java.util.Arrays;
import java.util.function.Supplier;

import de.gerdiproject.harvest.application.events.ContextInitializedEvent;
import de.gerdiproject.harvest.application.events.ResetContextEvent;
import de.gerdiproject.harvest.etls.AbstractETL;
import de.gerdiproject.harvest.event.EventSystem;
import lombok.RequiredArgsConstructor;

/**
 * This class wraps a {@linkplain ContextListener} implementation and modifies
 * the {@linkplain MainContext} initialization by only providing a single specified,
 * lazily initialized {@linkplain AbstractETL}.
 * The somewhat complex setup allows to test the {@linkplain AbstractETL} individually,
 * mocking data where necessary.
 *
 * @author Robin Weiss
 */
@RequiredArgsConstructor
public class ContextListenerTestWrapper <T extends AbstractETL<?, ?>>
{
    private final ContextListener wrappedContextListener;
    private final Supplier<T> etlSupplier;
    private T etl;


    /**
     * Retrieves the {@linkplain AbstractETL} lazily.
     * This allows the ETL to be initialized as if the harvester were run
     * regularily.
     *
     * @return the supplied {@linkplain AbstractETL}
     */
    public T getEtl()
    {
        if (etl == null)
            etl = etlSupplier.get();

        return etl;
    }


    /**
     * This method mirrors the wrapped {@linkplain ContextListener#contextInitialized(javax.servlet.ServletContextEvent)}
     * method with the exception of providing only a list of a single lazily initialized {@linkplain AbstractETL}.
     * This is required to guarantee the correct path to the unit test cache folder even when executing the tests
     * in Eclipse or from a different folder.
     */
    public void initializeContext()
    {
        EventSystem.addListener(ResetContextEvent.class, wrappedContextListener::onResetContext);

        // init main context
        MainContext.init(
            wrappedContextListener.getClass(),
            wrappedContextListener::getRepositoryName,
            () -> Arrays.asList(getEtl()),
            wrappedContextListener.getLoaderClasses());

        EventSystem.sendEvent(new ContextInitializedEvent());
    }


    /**
     * Returns a path to the {@linkplain Configuration} file in the temporary test folder.
     * The path can be used to copy over an optional configuration to properly test the
     * {@linkplain AbstractETL}.
     *
     * @return a path to the {@linkplain Configuration} file in the temporary test folder
     */
    public File getConfigFile()
    {
        return MainContextUtils.getConfigurationFile(
                   wrappedContextListener.getServiceName(),
                   wrappedContextListener.getClass());
    }
}
