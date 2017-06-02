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

import java.util.Set;
import javax.ws.rs.core.Application;

/**
 * Configuration file that registers all rest classes.
 * @author row
 */
@javax.ws.rs.ApplicationPath("resources")
public class ApplicationConfig extends Application
{
    @Override
    public Set<Class<?>> getClasses()
    {
        Set<Class<?>> resources = new java.util.HashSet<>();
        addRestResourceClasses(resources);
        
        return resources;
    }


    /**
     * Do not modify addRestResourceClasses() method.
     * It is automatically populated with
     * all resources defined in the project.
     * If required, comment out calling this method in getClasses().
     */
    private void addRestResourceClasses(Set<Class<?>> resources)
    {
        resources.add(de.gerdiproject.harvest.elasticsearch.rest.ElasticSearchSenderFacade.class);
        resources.add(de.gerdiproject.harvest.harvester.rest.HarvesterFacade.class );
        resources.add(de.gerdiproject.harvest.development.rest.DevelopmentToolsFacade.class );
        resources.add(de.gerdiproject.harvest.development.rest.DataCiteMapperFacade.class );
        resources.add(de.gerdiproject.harvest.config.rest.ConfigurationFacade.class );
    }
    
}
