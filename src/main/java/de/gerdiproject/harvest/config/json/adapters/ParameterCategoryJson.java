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
package de.gerdiproject.harvest.config.json.adapters;

import java.util.LinkedList;
import java.util.List;

import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.parameters.AbstractParameter;

/**
 * This class serves as a convenient JSON representation of {@linkplain ParameterCategory}s.
 * It is used in order to (de-)serialize the {@linkplain Configuration}.
 *
 * @author Robin Weiss
 */
class ParameterCategoryJson
{
    private final List<ParameterJson> parameters;


    /**
     * Constructor that sets up a new parameter list.
     */
    public ParameterCategoryJson()
    {
        this.parameters = new LinkedList<>();
    }


    /**
     * Converts an {@linkplain AbstractParameter} to a {@linkplain ParameterJson} and
     * adds it to the list of parameters that belong to this category.
     *
     * @param parameter an {@linkplain AbstractParameter} that is to be added
     */
    public void addParameter(final AbstractParameter<?> parameter)
    {
        parameters.add(new ParameterJson(parameter));
    }


    /**
     * Retrieves all {@linkplain AbstractParameter}s that belong to this category.
     *
     * @param category the name of the category of which the parameters are retrieved
     *
     * @return all {@linkplain AbstractParameter}s that belong to this category
     */
    public List<AbstractParameter<?>> getParameters(final String category)
    {
        final List<AbstractParameter<?>> deserializedParameters = new LinkedList<>();

        for (final ParameterJson jsonParam : parameters) {
            final AbstractParameter<?> deserializedParam = jsonParam.toAbstractParameter(category);

            if (deserializedParam != null)
                deserializedParameters.add(deserializedParam);
        }

        return deserializedParameters;
    }
}
