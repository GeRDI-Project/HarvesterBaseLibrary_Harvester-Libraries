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
package de.gerdiproject.harvest.utils;

/**
 * This interface represents a lmbda expression that accepts no arguments and returns
 * nothing.
 *
 * @author https://stackoverflow.com/users/5299539/charana
 */
@FunctionalInterface
public interface Procedure
{

    /**
     * Executes the code specified by a lambda expression.
     */
    void run();


    /**
     * Runs a specified {@linkplain Procedure} after this one.
     *
     * @param after the {@linkplain Procedure} to run after this one
     *
     * @return a new {@linkplain Procedure} that sequentially executes
     * both {@linkplain Procedure}s
     */
default Procedure andThen(Procedure after) {
        return () -> {
            this.run();
            after.run();
        };
    }


    /**
     * Runs a specified {@linkplain Procedure} before this one.
     *
     * @param before the {@linkplain Procedure} to run before this one
     *
     * @return a new {@linkplain Procedure} that sequentially executes
     * both {@linkplain Procedure}s
     */
default Procedure compose(Procedure before) {
        return () -> {
            before.run();
            this.run();
        };
    }
}