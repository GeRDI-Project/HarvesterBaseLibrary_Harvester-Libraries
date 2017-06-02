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
package de.gerdiproject.logger;

/**
 * Logs general info, warnings and errors. Needs to be set up in the
 * MainContext.
 *
 * @see de.gerdiproject.harvest.MainContext
 * @author row
 */
public interface ILogger
{
    /**
     * Logs general info that is not crucial.
     *
     * @param message the message which is to be logged
     *
     * @return the logged message
     */
    public String log( String message );


    /**
     * Logs progress.
     *
     * @param prefix a prefix describing what is progressing
     * @param currentValue the current value describing the progress
     * @param maxValue the maximum value of the progress
     *
     * @return the logged message
     */
    public String logProgress( String prefix, int currentValue, int maxValue );


    /**
     * Logs warnings that may effect functionality negatively.
     *
     * @param message the message which is to be logged
     *
     * @return the logged message
     */
    public String logWarning( String message );


    /**
     * Logs errors that indicate fatal behavior.
     *
     * @param message the message which is to be logged
     *
     * @return the logged message
     */
    public String logError( String message );
    
    /**
     * Logs an exception and its stack trace.
     *
     * @param ex the exception which is to be logged
     *
     * @return the logged message
     */
    public String logException( Throwable ex );
}
