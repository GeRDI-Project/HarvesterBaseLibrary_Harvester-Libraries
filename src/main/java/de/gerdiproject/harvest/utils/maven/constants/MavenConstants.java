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
package de.gerdiproject.harvest.utils.maven.constants;

import java.io.File;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * This class contains Maven and pom.xml related constants.
 *
 * @author Robin Weiss
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MavenConstants
{
    public static final String MAVEN_JAR_FILE_PATTERN = "^jar:.+/([^/]*?)\\.jar!.+$|^file:.+/([^/]+)/target/.+$";
    public static final String MAVEN_JAR_FILE_NAME_REPLACEMENT = "$1$2";

    public static final String DEPENDENCY_LIST_FORMAT = "%s%n%n%s";

    public static final String MAVEN_JAR_META_INF_FOLDER = "META-INF/maven/%s";
    public static final String DEFAULT_GERDI_NAMESPACE = "de.gerdi-project";
    public static final String JAR_PREFIX = "jar:";

    public static final String TARGET_FOLDER = File.separator + "target";
}
