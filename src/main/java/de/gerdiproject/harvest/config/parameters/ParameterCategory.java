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
package de.gerdiproject.harvest.config.parameters;

import java.util.List;

import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.state.IState;


/**
 * This class represents a category of {@linkplain AbstractParameter}s.
 *
 * @author Robin Weiss
 */
public class ParameterCategory
{
    private final String name;

    private final List<Class<? extends IState>> allowedStates;

    /**
     * Constructor that requires the unique name and allowed states of the category.
     *
     * @param name the unique name of the category
     * @param allowedStates the list of states during which parameters of that category may be changed
     *
     * @throws IllegalArgumentException thrown if the name contains invalid characters
     */
    public ParameterCategory(String name, List<Class<? extends IState>> allowedStates) throws IllegalArgumentException
    {
        if (!name.matches(ConfigurationConstants.VALID_PARAM_NAME_REGEX))
            throw new IllegalArgumentException(String.format(ConfigurationConstants.INVALID_CATEGORY_NAME, name));

        this.name = name;
        this.allowedStates = allowedStates;
    }


    /**
     * Return the unique name of the category.
     *
     * @return the unique name of the category
     */
    public String getName()
    {
        return name;
    }


    /**
     * Returns the list of states during which parameters of that category may be changed.
     *
     * @return the list of states during which parameters of that category may be changed
     */
    public List<Class<? extends IState>> getAllowedStates()
    {
        return allowedStates;
    }


    @Override
    public boolean equals(Object other)
    {
        return other instanceof ParameterCategory
               && name.equals(((ParameterCategory)other).name);
    }


    @Override
    public int hashCode()
    {
        return name.hashCode();
    }
}
