/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
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
package de.gerdiproject.harvest.state;


/**
 * An interface for states of the {@linkplain StateMachine}.
 *
 * @author Robin Weiss
 */
public interface IState
{
    /**
     * Returns a human readable name of the state.
     *
     * @return a human readable name of the state
     */
    String getName();


    /**
     * Returns a textual representation of the current progress
     * or status of the state.
     *
     * @return a textual representation of the current progress
     * or status of the state
     */
    String getStatusString();

    /**
     * Returns a minimalistic representation the state's progress.
     *
     * @return a minimalistic representation the state's progress
     */
    String getProgress();


    /**
     * This function is called when the {@linkplain StateMachine} transitions into
     * this state.
     */
    void onStateEnter();


    /**
     * This function is called when the {@linkplain StateMachine} transitions out of
     * this state.
     */
    void onStateLeave();


    /**
     * Attempts to start a harvesting-process.
     *
     * @return a status message about the success or failure of the operation
     */
    String startHarvest();


    /**
     * Attempts to abort a running process.
     *
     * @return a status message about the success or failure of the operation
     */
    String abort();


    /**
     * Attempts to pause a running process.
     *
     * @return a status message about the success or failure of the operation
     */
    String pause();


    /**
     * Attempts to resume a paused process.
     *
     * @return a status message about the success or failure of the operation
     */
    String resume();


    /**
     * Attempts to submit harvested documents.
     *
     * @return a status message about the success or failure of the operation
     */
    String submit();


    /**
     * Attempts to save harvested documents to disk.
     *
     * @return a status message about the success or failure of the operation
     */
    String save();
}
