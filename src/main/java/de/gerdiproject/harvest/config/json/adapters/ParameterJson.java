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

import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.parameters.AbstractParameter;
import de.gerdiproject.harvest.config.parameters.BooleanParameter;
import de.gerdiproject.harvest.config.parameters.IntegerParameter;
import de.gerdiproject.harvest.config.parameters.PasswordParameter;
import de.gerdiproject.harvest.config.parameters.StringParameter;

/**
 * This class serves as a convenient JSON representation of {@linkplain AbstractParameter}s.
 * It is used in order to (de-)serialize the {@linkplain Configuration}.
 *
 * @author Robin Weiss
 */
class ParameterJson
{
    private final String key;
    private final String value;
    private final String type;


    /**
     * Constructs a JSON representation using an {@linkplain AbstractParameter}.
     *
     * @param parameter the {@linkplain AbstractParameter} of which a JSON object is to be created
     */
    public ParameterJson(final AbstractParameter<?> parameter)
    {
        this.key = parameter.getKey();
        this.value = parameter.getValue() == null ? null : parameter.getValue().toString();
        this.type = parameter.getClass().getSimpleName();
    }


    /**
     * Creates an {@linkplain AbstractParameter} out of this JSON representation.
     *
     * @param category the category of the {@linkplain AbstractParameter} that is to be created
     *
     * @return an {@linkplain AbstractParameter} that mirrors this class
     */
    public AbstractParameter<?> toAbstractParameter(final String category)
    {
        AbstractParameter<?> param = null;

        if (type.equals(BooleanParameter.class.getSimpleName()))
            param = new BooleanParameter(key, category, Boolean.valueOf(value));

        else if (type.equals(IntegerParameter.class.getSimpleName()))
            param = new IntegerParameter(key, category, value == null ? 0 : Integer.parseInt(value));

        else if (type.equals(StringParameter.class.getSimpleName()))
            param = new StringParameter(key, category, value);

        else if (type.equals(PasswordParameter.class.getSimpleName()))
            param = new PasswordParameter(key, category, value);

        return param;
    }
}
