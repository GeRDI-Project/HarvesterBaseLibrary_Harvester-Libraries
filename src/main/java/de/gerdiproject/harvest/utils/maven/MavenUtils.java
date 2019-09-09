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
package de.gerdiproject.harvest.utils.maven;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import de.gerdiproject.harvest.utils.maven.constants.MavenConstants;
import lombok.Getter;

/**
 * This class contains utility functions for retrieving Maven values.
 *
 * @author Robin Weiss
 */
public class MavenUtils
{
    /**
     * -- GETTER --
     * Returns the name of the main jar of this service.
     * @return the name of the main jar of this service
     */
    @Getter
    private final String harvesterJarName;


    /**
     * Initializes the class by retrieving the jar name of the jar that contains a specified
     * class.
     *
     * @param mainJarClass a class of the harvester jar
     */
    public MavenUtils(final Class<?> mainJarClass)
    {
        URL contextListenerResource =
            mainJarClass.getResource(mainJarClass.getSimpleName() + ".class");

        // fallback, use HarvesterLibrary as main jar
        if (contextListenerResource == null)
            contextListenerResource = getClass().getResource(mainJarClass.getSimpleName() + ".class");

        if (contextListenerResource == null)
            this.harvesterJarName = null;
        else {
            this.harvesterJarName = contextListenerResource.toString().replaceAll(
                                        MavenConstants.MAVEN_JAR_FILE_PATTERN,
                                        MavenConstants.MAVEN_JAR_FILE_NAME_REPLACEMENT);
        }
    }


    /**
     * Returns a list of dependencies of this service's classpath.
     *
     * @param groupId a Maven groupId that can be used to filter the Maven projects,
     *         or null if no filter is to be applied
     *
     * @return a list of Maven dependencies, or null if no versions could be retrieved
     */
    public List<String> getMavenVersionInfo(final String groupId)
    {
        final List<String> dependencyList = new LinkedList<>();
        final String projectFilter = String.format(
                                         MavenConstants.MAVEN_JAR_META_INF_FOLDER,
                                         groupId == null ? "" : groupId);

        try {
            // retrieve all resources that match 'projectFilter'
            final Enumeration<URL> gerdiMavenLibraries =
                Thread
                .currentThread()
                .getContextClassLoader()
                .getResources(projectFilter);

            // retrieve only the jar names from the resources
            while (gerdiMavenLibraries.hasMoreElements()) {
                final String resourcePath = gerdiMavenLibraries.nextElement().toString();

                if (resourcePath.startsWith(MavenConstants.JAR_PREFIX)) {
                    dependencyList.add(resourcePath.replaceAll(
                                           MavenConstants.MAVEN_JAR_FILE_PATTERN,
                                           MavenConstants.MAVEN_JAR_FILE_NAME_REPLACEMENT));
                }
            }
        } catch (final IOException e) {
            return null;
        }

        // sort dependencies
        Collections.sort(dependencyList, String.CASE_INSENSITIVE_ORDER);

        return dependencyList;
    }
}
