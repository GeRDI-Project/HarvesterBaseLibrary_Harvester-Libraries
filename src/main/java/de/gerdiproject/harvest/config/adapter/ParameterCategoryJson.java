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
package de.gerdiproject.harvest.config.adapter;

import java.util.LinkedList;
import java.util.List;

import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.parameters.AbstractParameter;
import de.gerdiproject.harvest.config.parameters.ParameterCategory;
import de.gerdiproject.harvest.state.IState;
import de.gerdiproject.harvest.state.impl.AbortingState;
import de.gerdiproject.harvest.state.impl.ErrorState;
import de.gerdiproject.harvest.state.impl.HarvestingState;
import de.gerdiproject.harvest.state.impl.IdleState;
import de.gerdiproject.harvest.state.impl.InitializationState;
import de.gerdiproject.harvest.state.impl.SubmittingState;

/**
 * This class serves as a convenient JSON representation of {@linkplain ParameterCategory}s.
 * It is used in order to (de-)serialize the {@linkplain Configuration}.
 *
 * @author Robin Weiss
 */
class ParameterCategoryJson
{
    private final List<ParameterJson> parameters;
    private final List<String> allowedStates;


    /**
     * Constructor that requires the {@linkplain ParameterCategory} that is
     * to be represented by this class.
     *
     * @param category the {@linkplain ParameterCategory} that is
     * to be represented by this class.
     */
    public ParameterCategoryJson(ParameterCategory category)
    {
        this.parameters = new LinkedList<>();
        this.allowedStates = new LinkedList<>();

        category.getAllowedStates().forEach((Class<? extends IState>state) -> allowedStates.add(state.getSimpleName()));
    }


    /**
     * Converts an {@linkplain AbstractParameter} to a {@linkplain ParameterJson} and
     * adds it to the list of parameters that belong to this category.
     *
     * @param parameter an {@linkplain AbstractParameter} that is to be added
     */
    public void addParameter(AbstractParameter<?> parameter)
    {
        parameters.add(new ParameterJson(parameter));
    }


    /**
     * Retrieves all {@linkplain AbstractParameter}s that belong to this category.
     *
     * @param categoryName the name of the category of which the parameters are retrieved
     *
     * @return all {@linkplain AbstractParameter}s that belong to this category
     */
    public List<AbstractParameter<?>> getParameters(String categoryName)
    {
        final ParameterCategory category = getCategory(categoryName);

        final List<AbstractParameter<?>> deserializedParameters = new LinkedList<>();

        for (ParameterJson jsonParam : parameters) {
            final AbstractParameter<?> deserializedParam = jsonParam.toAbstractParameter(category);

            if (deserializedParam != null)
                deserializedParameters.add(jsonParam.toAbstractParameter(category));
        }

        return deserializedParameters;
    }


    /**
     * Retrieves the {@linkplain ParameterCategory} that is represented by this class.
     *
     * @param categoryName the name of the retrieved category
     *
     * @return the {@linkplain ParameterCategory} that is represented by this class
     */
    private ParameterCategory getCategory(String categoryName)
    {
        final List<Class<? extends IState>> stateClasses = new LinkedList<>();

        for (String stateClassSimpleName : allowedStates) {
            if (stateClassSimpleName.equals(AbortingState.class.getSimpleName()))
                stateClasses.add(AbortingState.class);

            else if (stateClassSimpleName.equals(ErrorState.class.getSimpleName()))
                stateClasses.add(ErrorState.class);

            else if (stateClassSimpleName.equals(HarvestingState.class.getSimpleName()))
                stateClasses.add(HarvestingState.class);

            else if (stateClassSimpleName.equals(IdleState.class.getSimpleName()))
                stateClasses.add(IdleState.class);

            else if (stateClassSimpleName.equals(InitializationState.class.getSimpleName()))
                stateClasses.add(InitializationState.class);

            else if (stateClassSimpleName.equals(SubmittingState.class.getSimpleName()))
                stateClasses.add(SubmittingState.class);
        }

        return new ParameterCategory(categoryName, stateClasses);
    }
}
