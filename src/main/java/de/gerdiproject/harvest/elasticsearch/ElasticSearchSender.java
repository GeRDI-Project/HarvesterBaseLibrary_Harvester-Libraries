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
package de.gerdiproject.harvest.elasticsearch;


import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.utils.HttpRequester;
import de.gerdiproject.harvest.utils.SearchIndexFactory;
import de.gerdiproject.json.IJsonArray;
import de.gerdiproject.json.IJsonBuilder;
import de.gerdiproject.json.IJsonObject;
import de.gerdiproject.json.IJsonReader;
import de.gerdiproject.json.impl.JsonBuilder;
import de.gerdiproject.json.utils.JsonHelper;
import de.gerdiproject.logger.ILogger;

import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;


/**
 * This Singleton class serves as a communicator for an Elastic Search node. An
 * URL and optionally a username and password must be set up first. Afterwards,
 * the harvested database can be uploaded.
 *
 * @author row
 */
public class ElasticSearchSender
{
	private final static String ERROR_PREFIX = "Cannot send search index to Elastic Search: ";
	private final static String EMPTY_INDEX_ERROR = ERROR_PREFIX + "JSON 'data' array is empty";
	private final static String NO_URL_ERROR = ERROR_PREFIX + "You need to set up a valid  Elastic Search URL";

	private final static String MAPPINGS_URL = "%s/%s/_mapping/%s/";
	private final static String BASIC_MAPPING = "{\"properties\":{}}";

	private final static String BATCH_POST_INSTRUCTION = "{\"index\":{\"_id\":\"%s\"}}\n%s\n";
	private final static String BULK_SUBMISSION_URL = "%s/%s/%s/_bulk?pretty";

	private final static String URL_SET_OK = "Set ElasticSearch URL to '%s/%s/%s/'.";
	private final static String URL_SET_FAILED = "Elastic Search URL '%s' is malformed!";

	private final static String SUBMISSION_START = "Submitting documents to '%s'...";
	private final static String SUBMISSION_DONE = "SUBMISSION DONE!";
	private final static String SUBMIT_ERROR_INDICATOR = "\"status\" : 400";
	private final static String SUBMIT_PARTIAL_OK = "Succcessfully submitted documents %d to %d";
	private final static String SUBMIT_PARTIAL_FAILED = "There were errors while submitting documents %d to %d:";
	private final static String SUBMIT_PARTIAL_FAILED_FORMAT = "\n\t%s of document '%s': %s - %s'";

	private final static String RESUBMISSION_START = "Had to remove erroneous fields from %d documents! Re-submitting...";
	private final static String RESUBMISSION_OK = "Succcessfully submitted documents!";
	private final static String RESUBMISSION_FAILED = "Could not re-submit the following documents:";

	private final static String ID_JSON = "_id";
	private final static String INDEX_JSON = "index";
	private final static String ITEMS_JSON = "items";
	private final static String ERROR_JSON = "error";
	private final static String REASON_JSON = "reason";
	private final static String IDENTIFIER_JSON = "identifier";
	private final static String CAUSED_BY_JSON = "caused_by";
	private final static String TYPE_JSON = "type";
	private final static String PARSE_ERROR_PREFIX = "failed to parse [";

	private final static String NULL_JSON = "null";

	private final static int BULK_SUBMISSION_SIZE = 1024;

	private static ElasticSearchSender instance;

	private final IJsonBuilder jsonBuilder;
	private final HttpRequester httpRequester;
	private final ILogger logger;
	private final Base64.Encoder encoder;
	private final Base64.Decoder decoder;

	/**
	 * the bulk-POST URL for an ElasticSearch index and type
	 */
	private String baseUrl;

	/**
	 * the search index of ElasticSearch
	 */
	private String index;

	/**
	 * the document type of the harvested documents
	 */
	private String type;

	/**
	 * base-64 encoded user credentials (optional)
	 */
	private String credentials;

	/**
	 * user name (optional).
	 */
	private String userName;


	/**
	 * Returns the Singleton instance of this class.
	 *
	 * @return a Singleton instance of this class
	 */
	public static ElasticSearchSender instance()
	{
		if (instance == null)
		{
			instance = new ElasticSearchSender();
		}
		return instance;
	}


	/**
	 * Private constructor, because this is a singleton.
	 * 
	 * @throws NoSuchAlgorithmException
	 */
	private ElasticSearchSender()
	{
		logger = MainContext.getLogger();
		httpRequester = new HttpRequester();

		encoder = Base64.getEncoder();
		decoder = Base64.getDecoder();

		jsonBuilder = new JsonBuilder();
	}


	/**
	 * Creates a bulk-post URL for creating/updating search indices in Elastic
	 * Search.
	 *
	 * @param baseUrl
	 *            the main Elastic Search URL without index or type
	 * @param index
	 *            the index that will be updated/created
	 * @param type
	 *            the type that will be updated/created
	 *
	 * @return the generated bulk-post URL, or an error message, if the
	 *         operation does not succeed
	 */
	public String setUrl( String baseUrl, String index, String type )
	{
		// remove superfluous slashes
		index = index.replace( "/", "" );
		type = type.replace( "/", "" );

		if (baseUrl.charAt( baseUrl.length() - 1 ) == '/')
		{
			baseUrl = baseUrl.substring( 0, baseUrl.length() - 1 );
		}

		// assemble complete URL
		try
		{
			// test if URL is valid
			new URL( baseUrl );

			// assign properties
			this.baseUrl = baseUrl;
			this.index = index;
			this.type = type;

			return String.format( URL_SET_OK, this.baseUrl, this.index, this.type );

		}
		catch (MalformedURLException mue)
		{
			return logger.logError( String.format( URL_SET_FAILED, baseUrl ) );
		}
	}


	/**
	 * Assembles a bulk submission URL that is used for submitting multiple
	 * documents at once.
	 *
	 * @return an ElasticSearch bulk submission URL
	 */
	private String getBulkSubmissionUrl()
	{
		return String.format( BULK_SUBMISSION_URL, baseUrl, index, type );
	}


	/**
	 * Sets login credentials that will be used in subsequent Elastic Search
	 * requests. If userName is an empty string, the login credentials will be
	 * removed.
	 *
	 * @param userName
	 *            the login user name
	 * @param password
	 *            the password for the user
	 */
	public void setCredentials( String userName, String password )
	{
		if (userName == null || userName.isEmpty())
		{
			this.userName = null;
			credentials = null;
		}
		else
		{
			this.userName = userName;
			credentials = Base64.getEncoder().encodeToString( (userName + ":" + password).getBytes() );
		}
	}


	/**
	 * Updates or creates a search index in Elastic Search.
	 *
	 * @param documents
	 *            a JSON-array of searchable objects
	 * @return the HTTP response of the Elastic Search POST request, or an error
	 *         message if the operation does not succeed
	 */
	public String sendToElasticSearch( IJsonArray documents )
	{
		// if the type does not exist on ElasticSearch yet, initialize it
		validateAndCreateMappings();

		// check if a URL has been set up
		if (baseUrl == null)
		{
			return logger.logWarning( NO_URL_ERROR );
		}

		// check if entries exist
		if (documents == null || documents.isEmpty())
		{
			return logger.logWarning( EMPTY_INDEX_ERROR );
		}

		final String elasticSearchUrl = getBulkSubmissionUrl();
		logger.log( String.format( SUBMISSION_START, elasticSearchUrl ) );

		// build a string for bulk-posting to Elastic search
		StringBuilder bulkRequestBuilder = new StringBuilder();
		int from = 0;

		try
		{
			for (int i = 0, len = documents.size(); i < len; i++)
			{
				IJsonObject doc = documents.getJsonObject( i );
				String id = createDocumentID( doc );
				bulkRequestBuilder
						.append( String.format( BATCH_POST_INSTRUCTION, id, doc.toJsonString().replace( "\n", "" ) ) );

				// submit every 1024 posts, to decrease memory usage
				if (((i + 1) % BULK_SUBMISSION_SIZE) == 0)
				{

					// submit to elasticsearch
					String response = httpRequester
							.postRequest( elasticSearchUrl, bulkRequestBuilder.toString(), credentials );

					// handle response
					handleSubmissionResponse( response, from, i, documents );

					// reset the string builder and free memory
					bulkRequestBuilder = new StringBuilder();
					from = i + 1;

					System.gc();
				}
			}

			// send final POST request to Elastic search
			if (bulkRequestBuilder.length() > 0)
			{
				String response = httpRequester
						.postRequest( elasticSearchUrl, bulkRequestBuilder.toString(), credentials );

				// log response
				handleSubmissionResponse( response, from, documents.size() - 1, documents );
			}
		}
		catch (Exception e)
		{
			logger.logError( ERROR_PREFIX );
			return logger.logException( e );
		}

		return logger.log( SUBMISSION_DONE );
	}


	/**
	 * Checks if mappings exist for the index and type combination. If they do
	 * not, they are created on the lasticSearch node.
	 */
	public void validateAndCreateMappings()
	{
		String mappingsUrl = String.format( MAPPINGS_URL, baseUrl, index, type );
		IJsonObject mappings = httpRequester.getRawJsonFromUrl( mappingsUrl );

		if (mappings == null)
		{
			// create mappings on ElasticSearch
			httpRequester.putRequest( mappingsUrl, BASIC_MAPPING, credentials );
		}
	}


	/**
	 * Encodes the viewUrl in order to use it as an ID for submitting documents
	 * to Elasticsearch.
	 * 
	 * @param document
	 *            the searchable document of which an ID needs to be created
	 * @return an ID string
	 */
	private String createDocumentID( IJsonObject document )
	{
		// get view URL
		String url = document.getString( SearchIndexFactory.VIEW_URL_JSON );

		if (url != null)
		{
			// base64 encoding:
			String base64EncodedString = new String( encoder.encode( url.getBytes() ) );
			return base64EncodedString;
		}
		else
		{
			// check if the document already has an identifier
			String identifier = document.getString( IDENTIFIER_JSON, null );
			return identifier;
		}
	}


	private String documentIdToViewUrl( String documentId )
	{
		// decode base64
		String viewUrl = new String( decoder.decode( documentId ) );

		return viewUrl;
	}


	/**
	 * Handles the response from ElasticSearch that is sent after a bulk
	 * submission. If any document failed, it will be logged and attempted to be
	 * fixed.
	 * 
	 * @param response
	 *            the response string from ElasticSearch
	 * @param from
	 *            the index of the first document that was submitted in this
	 *            bulk
	 * @param to
	 *            the index of the first document that is not submitted anymore
	 * @param documents
	 *            all harvested documents
	 */
	private void handleSubmissionResponse( String response, int from, int to, IJsonArray documents )
	{
		// parse a json object from the response string
		IJsonArray responseArray = null;
		{
			IJsonReader reader = jsonBuilder.createReader( new StringReader( response ) );
			try
			{
				IJsonObject responseObj = reader.readObject();
				reader.close();
				responseArray = responseObj.getJsonArray( ITEMS_JSON );
			}
			catch (Exception e)
			{
				logger.logError( e.toString() );
				return;
			}
		}

		// if the server response is not JSON, it's probably an error
		if (responseArray == null)
		{
			logger.log( String.format( SUBMIT_PARTIAL_FAILED, from, to ) + response );
			return;
		}

		// if the server response is JSON and does not contain "status: 400",
		// there are no errors
		if (response.indexOf( SUBMIT_ERROR_INDICATOR ) == -1)
		{
			logger.log( String.format( SUBMIT_PARTIAL_OK, from, to ) );
			return;
		}

		boolean hasErrors = false;
		StringBuilder errorBuilder = new StringBuilder( String.format( SUBMIT_PARTIAL_FAILED, from, to ) );

		// collect and fix failed documents
		List<IJsonObject> fixedDocuments = new LinkedList<>();

		for (Object r : responseArray)
		{
			// get the json object that holds the response to a single document
			IJsonObject singleDocResponse = ((IJsonObject) r).getJsonObject( INDEX_JSON );

			// if document was transmitted successfully, check the next one
			if (singleDocResponse.getJsonObject( ERROR_JSON ) == null)
			{
				continue;
			}
			hasErrors = true;

			// log the error message
			String errorMessage = formatSubmissionError( singleDocResponse );
			errorBuilder.append( errorMessage );

			// attempt to fix the document
			IJsonObject fixedDoc = fixErroneousDocument( singleDocResponse, documents );
			if (fixedDoc != null)
			{
				fixedDocuments.add( fixedDoc );
			}
		}
		// log failed documents
		if (hasErrors)
		{
			logger.logError( errorBuilder.toString() );
		}

		// try to submit the documents again
		resubmitDocuments( fixedDocuments );
	}


	private String formatSubmissionError( IJsonObject elasticSearchResponse )
	{
		IJsonObject errorObject = elasticSearchResponse.getJsonObject( ERROR_JSON );
		final String viewUrl = documentIdToViewUrl( elasticSearchResponse.getString( ID_JSON ) );

		// get the reason of the submission failure
		final String submitFailedReason = (errorObject.isNull( REASON_JSON ))
				? NULL_JSON
				: errorObject.getString( REASON_JSON );
		final IJsonObject cause = errorObject.getJsonObject( CAUSED_BY_JSON );

		final String exceptionType = (cause.isNull( TYPE_JSON ))
				? NULL_JSON
				: cause.getString( TYPE_JSON );

		final String exceptionReason = (cause.isNull( REASON_JSON ))
				? NULL_JSON
				: cause.getString( REASON_JSON, NULL_JSON );

		// append document failure to error log
		return String.format(
				SUBMIT_PARTIAL_FAILED_FORMAT,
				submitFailedReason,
				viewUrl,
				exceptionType,
				exceptionReason );
	}


	/**
	 * Attempts to fix a document that could not be submitted to ElasticSearch.
	 * Searches the ElasticSearch response for error details and removes a field
	 * from the document if it caused the error.
	 * 
	 * @param elasticSearchResponse
	 *            the response from elastic search for one submitted document
	 * @param documents the documents that are to be submitted           
	 */
	private IJsonObject fixErroneousDocument( IJsonObject elasticSearchResponse, IJsonArray documents )
	{
		// get error object from response
		IJsonObject errorObject = elasticSearchResponse.getJsonObject( ERROR_JSON );

		// get reason for submission failure
		final String submitFailedReason = (errorObject != null) ? errorObject.getString( REASON_JSON, null ) : null;

		if (submitFailedReason == null)
		{
			return null;
		}

		// make sure it's a parsing error. these can be fixed by removing the
		// field that cannot be parsed
		if (submitFailedReason.startsWith( PARSE_ERROR_PREFIX ))
		{
			// decode document ID to get the viewUrl
			final String viewUrl = documentIdToViewUrl( elasticSearchResponse.getString( ID_JSON ) );

			// find document with matching ViewURL
			IJsonObject failedDocument = JsonHelper.findObjectInArray(
					documents,
					SearchIndexFactory.VIEW_URL_JSON,
					viewUrl );

			// make sure the failed document was found
			if (failedDocument != null)
			{
				// retrieve the field that could not be parsed
				String failedField = submitFailedReason.substring(
						PARSE_ERROR_PREFIX.length(),
						submitFailedReason.length() - 1 );

				// the error can be nested within JsonObjects. we need to find
				// the field inside the object tree
				String[] failedObjPath = failedField.split( "\\." );
				IJsonObject failedObj = failedDocument;
				for (int i = 0; i < failedObjPath.length - 1; i++)
				{
					failedObj = failedObj.getJsonObject( failedObjPath[i] );
				}

				// remove erroneous field from document
				failedObj.remove( failedObjPath[failedObjPath.length - 1] );
				
				
				// TODO: mark the corrected document as being changed

				return failedDocument;
			}
		}

		return null;
	}


	/**
	 * Submits a list of documents to ElasticSearch, logging the process as
	 * "Resubmitting".
	 * 
	 * @param documents
	 *            a list of documents that are to be submitted
	 */
	private void resubmitDocuments( List<IJsonObject> documents )
	{
		// did any document fail?
		if (documents.size() == 0)
		{
			return;
		}

		// log resubmission
		logger.log( String.format( RESUBMISSION_START, documents.size() ) );

		final String elasticSearchUrl = getBulkSubmissionUrl();
		final StringBuilder bulkRequestBuilder = new StringBuilder();

		// create bulk submission body
		for (IJsonObject fixedDoc : documents)
		{
			bulkRequestBuilder.append(
					String.format(
							BATCH_POST_INSTRUCTION,
							createDocumentID( fixedDoc ),
							fixedDoc.toJsonString().replace( "\n", "" ) ) );
		}

		// re-submit fixed documents
		String resubmitResponse = httpRequester
				.postRequest( elasticSearchUrl, bulkRequestBuilder.toString(), credentials );

		//
		if (resubmitResponse.indexOf( SUBMIT_ERROR_INDICATOR ) == -1)
		{
			logger.log( RESUBMISSION_OK );
		}
		else
		{
			// parse the re-submission response
			IJsonReader reader = jsonBuilder.createReader( new StringReader( resubmitResponse ) );
			IJsonArray resubmitResponseArray = null;

			try
			{
				resubmitResponseArray = reader.readObject().getJsonArray( ITEMS_JSON );
				reader.close();
			}
			catch (Exception e)
			{
				logger.logError( e.toString() );
			}

			// re-initialize the error string builder
			StringBuilder errorBuilder = new StringBuilder( RESUBMISSION_FAILED );

			for (Object r : resubmitResponseArray)
			{
				IJsonObject singleDocResponse = ((IJsonObject) r).getJsonObject( INDEX_JSON );
				IJsonObject errorObject = singleDocResponse.getJsonObject( ERROR_JSON );

				// did an error occur?
				if (errorObject != null)
				{
					// get error details
					final String viewUrl = documentIdToViewUrl( singleDocResponse.getString( ID_JSON ) );
					final String submitFailedReason = (errorObject.isNull( REASON_JSON ))
							? NULL_JSON
							: errorObject.getString( REASON_JSON );
					final IJsonObject cause = errorObject.getJsonObject( CAUSED_BY_JSON );

					final String exceptionType = (cause.isNull( TYPE_JSON ))
							? NULL_JSON
							: cause.getString( TYPE_JSON );
					final String exceptionReason = (cause.isNull( REASON_JSON ))
							? NULL_JSON
							: cause.getString( REASON_JSON );

					// append document failure to error log
					errorBuilder.append(
							String.format(
									SUBMIT_PARTIAL_FAILED_FORMAT,
									submitFailedReason,
									viewUrl,
									exceptionType,
									exceptionReason ) );
				}
			}
			// log re-submission errors
			logger.logError( errorBuilder.toString() );
		}
	}


	/**
	 * Returns the base url of an Elastic Search node, or null if the URL has
	 * not been set up.
	 *
	 * @return the base url of an Elastic Search node, or null if the URL has
	 *         not been set up
	 */
	public String getBaseUrl()
	{
		return baseUrl;
	}


	/**
	 * Returns the user name or null, if it is not set.
	 *
	 * @return the user name or null, if it is not set
	 */
	public String getUserName()
	{
		return userName;
	}


	/**
	 * Return the search index name.
	 *
	 * @return the search index name
	 */
	public String getIndex()
	{
		return index;
	}


	/**
	 * Returns the search document type.
	 *
	 * @return he search document type
	 */
	public String getType()
	{
		return type;
	}
}
