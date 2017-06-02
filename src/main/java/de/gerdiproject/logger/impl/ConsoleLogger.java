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
package de.gerdiproject.logger.impl;


import de.gerdiproject.logger.ILogger;


/**
 * A simple logger implementation that logs messages to the console.
 *
 * @author row
 */
public class ConsoleLogger implements ILogger
{
	private final String PROGESS_TEXT = "\r%s: %3d%% (%d / %d)";


	@Override
	public String log( String message )
	{
		System.out.println( message );

		return message;
	}


	@Override
	public String logWarning( String message )
	{
		String formattedMessage = "Warning: " + message;
		System.out.println( formattedMessage );

		return formattedMessage;
	}


	@Override
	public String logError( String message )
	{
		String formattedMessage = "Error: " + message;
		System.err.println( formattedMessage );

		return formattedMessage;
	}


	@Override
	public String logProgress( String prefix, int currentValue, int maxValue )
	{
		int progressInPercent = Math.min( (int) Math.ceil( (100f * currentValue) / maxValue ), 100 );

		String formattedMessage = String.format( PROGESS_TEXT, prefix, progressInPercent, currentValue, maxValue );

		System.out.print( formattedMessage );
		System.out.flush();

		return formattedMessage;
	}


	@Override
	public String logException( Throwable ex )
	{
		final StringBuilder errorBuilder = new StringBuilder( ex.toString() );
		StackTraceElement[] stackTrace = ex.getStackTrace();

		for (StackTraceElement ele : stackTrace)
		{
			errorBuilder.append( '\n' ).append( ele );
		}
		return logError( errorBuilder.toString() );
	}
}
