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
package de.gerdiproject.harvest.rest;


import java.util.Set;

import javax.ws.rs.core.Application;


/**
 * Configuration file that registers all rest classes.
 *
 * @author Robin Weiss
 */
@javax.ws.rs.ApplicationPath("harvest")
public class RestResourceConfig extends Application
{
    @Override
    public Set<Class<?>> getClasses()
    {
        final Set<Class<?>> resources = new java.util.HashSet<>();
        addRestResourceClasses(resources);

        return resources;
    }


    /**
     * Do not modify addRestResourceClasses() method. It is automatically
     * populated with all resources defined in the project. If required, comment
     * out calling this method in getClasses().
     */
    private void addRestResourceClasses(final Set<Class<?>> resources)
    {
        resources.add(de.gerdiproject.harvest.etls.rest.ETLRestResource.class);
        resources.add(de.gerdiproject.harvest.config.rest.ConfigurationRestResource.class);
        resources.add(de.gerdiproject.harvest.scheduler.rest.SchedulerRestResource.class);
    }

}
