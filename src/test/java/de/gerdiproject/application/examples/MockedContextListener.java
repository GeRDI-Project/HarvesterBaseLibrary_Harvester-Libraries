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
package de.gerdiproject.application.examples;

import java.util.List;

import de.gerdiproject.harvest.application.ContextListener;
import de.gerdiproject.harvest.submission.AbstractSubmitter;
import de.gerdiproject.utils.examples.harvestercache.MockedHarvester;

/**
 * This class is a mocked ContextListener.
 *
 * @author Robin Weiss
 */
public class MockedContextListener extends ContextListener<MockedHarvester>
{
    @Override
    public List<Class<? extends AbstractSubmitter>> getSubmitterClasses() // NOPMD not a useless override because the accessor changed
    {
        return super.getSubmitterClasses();
    }


    @Override
    public String getServiceName() // NOPMD not a useless override because the accessor changed
    {
        return super.getServiceName();
    }
}
