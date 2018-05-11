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
import java.util.Enumeration;

import de.gerdiproject.harvest.utils.maven.constants.MavenConstants;

/**
 * This class contains utility functions for retrieving Maven values.
 *
 * @author Robin Weiss
 */
public class MavenUtils
{
    /**
     * Returns a well-formatted string that contains artifactIDs and versions of GeRDI
     * Maven libraries used within this service.
     *
     * @return a well-formatted Maven versions string, or null if no versions could be retrieved
     */
    public static String getMavenVersionInfo()
    {

        final StringBuilder sb = new StringBuilder();

        try {
            final Enumeration<URL> gerdiMavenLibraries =
                MavenUtils.class
                .getClassLoader()
                .getResources(MavenConstants.MAVEN_JAR_META_INF_FOLDER);

            while (gerdiMavenLibraries.hasMoreElements()) {
                final String jarName = gerdiMavenLibraries.nextElement().toString();
                sb.append(jarName.replaceAll(
                              MavenConstants.MAVEN_JAR_FILE_PATTERN,
                              MavenConstants.MAVEN_JAR_FILE_NAME_REPLACEMENT));
            }
        } catch (IOException e) {
            return null;
        }

        return sb.toString();
    }


    /**
     * Private constructor, because this class offers only static functions.
     */
    private MavenUtils()
    {
    }
}
