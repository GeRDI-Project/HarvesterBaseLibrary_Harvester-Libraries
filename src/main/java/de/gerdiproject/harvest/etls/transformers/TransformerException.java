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
package de.gerdiproject.harvest.etls.transformers;

/**
 * This exception is thrown when an {@linkplain ITransformer} fails to transform
 * elements.
 *
 * @author Robin Weiss
 */
public class TransformerException extends RuntimeException
{
    private static final long serialVersionUID = 7676375273362447887L;


    /**
     * Forwarded super-class constructor
     *
     * @param message a message describing the cause of the exception
     */
    public TransformerException(String message)
    {
        super(message);
    }


    /**
     * Constructor that forwards a super class constructor.
     *
     * @param e the exception that caused the failure
     */
    public TransformerException(Throwable e)
    {
        super(e);
    }
}
