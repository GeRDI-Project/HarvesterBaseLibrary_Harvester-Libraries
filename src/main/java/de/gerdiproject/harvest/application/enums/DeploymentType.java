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
package de.gerdiproject.harvest.application.enums;

/**
 * This enumeration depicts how the harvester service is being deployed.
 *
 * @author Robin Weiss
 */
public enum DeploymentType {

    /**
     * The harvester service is deployed in a Docker container.
     */
    DOCKER,

    /**
     * The harvester service is deployed via the -Drun=jetty
     * Maven argument.
     */
    JETTY,

    /**
     * Ther harvester service is being unit-tested.
     */
    UNIT_TEST,

    /**
     * The deployment type is unknown. The harvester service
     * was possibly deployed on a non-Docker server without
     * using Maven commands.
     */
    OTHER
}
