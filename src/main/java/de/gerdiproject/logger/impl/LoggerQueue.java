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
import java.util.LinkedList;


/**
 * A logger that stores all messages in a queue. It provides a method for
 * logging all queued messages using a different logger implementation.
 *
 * @author row
 */
public class LoggerQueue implements ILogger
{
	private final static String SEPARATOR = "%";
	private final static String PROGRESS_SEPARATOR = "#sep#";

	private final LinkedList<String> messageQueue;


	/**
	 * Simple constructor that initializes the message queue.
	 */
	public LoggerQueue()
	{
		messageQueue = new LinkedList<>();
	}


	/**
	 * Logs all stored messages using a different logger implementation
	 *
	 * @param otherLogger
	 *            a logger which is to log all queued messages
	 */
	public void logQueuedMessages( ILogger otherLogger )
	{
		while (!messageQueue.isEmpty())
		{
			final String rawMessage = messageQueue.pop();

			final int separatorIndex = rawMessage.indexOf( SEPARATOR );
			final LogType messageType = LogType.valueOf( rawMessage.substring( 0, separatorIndex ) );
			final String message = rawMessage.substring( separatorIndex + 1 );

			switch (messageType)
			{
				case LOG:
					otherLogger.log( message );
					break;
				case WARNING:
					otherLogger.logWarning( message );
					break;
				case ERROR:
					otherLogger.logError( message );
					break;
				case PROGRESS:
					String[] params = message.split( PROGRESS_SEPARATOR );
					otherLogger.logProgress(
							params[0],
							Integer.parseInt( params[1] ),
							Integer.parseInt( params[2] ) );
					break;
			}
		}
	}


	@Override
	public String log( String message )
	{
		messageQueue.offer( LogType.LOG + SEPARATOR + message );
		return message;
	}


	@Override
	public String logWarning( String message )
	{
		messageQueue.offer( LogType.WARNING + SEPARATOR + message );

		return message;
	}


	@Override
	public String logError( String message )
	{
		messageQueue.offer( LogType.ERROR + SEPARATOR + message );

		return message;
	}


	@Override
	public String logProgress( String prefix, int currentValue, int maxValue )
	{
		messageQueue.offer(
				LogType.PROGRESS + SEPARATOR + prefix + PROGRESS_SEPARATOR + currentValue + PROGRESS_SEPARATOR
						+ maxValue );

		int progressInPercent = Math.min( (int) Math.ceil( (100f * currentValue) / maxValue ), 100 );
		return String.valueOf( progressInPercent );
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

	/**
	 * The type of the logged message.
	 */
	private enum LogType
	{
		LOG, WARNING, ERROR, PROGRESS
	}
}
