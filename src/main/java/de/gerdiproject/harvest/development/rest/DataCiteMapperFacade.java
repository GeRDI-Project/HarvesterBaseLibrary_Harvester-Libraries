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
package de.gerdiproject.harvest.development.rest;


import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.development.DataCiteMapper;
import de.gerdiproject.harvest.development.DevelopmentTools;
import de.gerdiproject.harvest.elasticsearch.ElasticSearchSender;
import de.gerdiproject.harvest.harvester.AbstractHarvester;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;


/**
 * A RESTful interface. for developer options and tools. It provides REST
 * requests for converting harvested documents to the DataCite schema.
 *
 * @see DataCiteMapper
 * @author Robin Weiss
 */
@Path ("datacite")
public class DataCiteMapperFacade
{
	private final static String STATUS_NO_HARVEST = "Needs to finish harvesting first";
	private final static String STATUS_NOT_CONVERTED = "Ready for conversion";
	private final static String STATUS_DONE = "Documents converted";

	private final static String SAVE_FAILED = "Documents must be converted prior to saving them. Use a POST request to start the conversion.";
	private final static String SUBMIT_FAILED = "Documents must be converted prior to submitting them. Use a POST request to start the conversion.";
	private final static String FILE_PATH = "harvestedIndices/dataCite/%s_datacite_%d.json";

	private final static String INFO = "- %s DataCite Mapper -\n\n"
			+ "Status:\t\t%s\n"
			+ "POST/save\tSaves the converted documents to disk\n"
			+ "POST/submit\tSubmits the converted documents to ElasticSearch";

	private final DataCiteMapper dataCiteMapper = DataCiteMapper.instance();


	/**
	 * Displays an info string that shows the available REST calls and current
	 * state of the developer options.
	 *
	 * @return an info string
	 */
	@GET
	@Produces ({
			MediaType.TEXT_PLAIN
	})
	public String getInfo()
	{
		String status = STATUS_NO_HARVEST;
		AbstractHarvester harvester = MainContext.getHarvester();

		if (dataCiteMapper.isFinished())
		{
			status = STATUS_DONE;
		}
		else if (dataCiteMapper.isConverting())
		{
			status = dataCiteMapper.getProgressString();
		}
		else if (harvester.isHarvestFinished())
		{
			status = STATUS_NOT_CONVERTED;
		}

		return String.format(
				INFO,
				MainContext.getModuleName(),
				status );
	}


	/**
	 * Starts converting harvested documents to the DataCite schema.
	 *
	 * @return an info message that describes the status of the operation
	 */
	@POST
	@Produces ({
			MediaType.TEXT_PLAIN
	})
	public String startConversion()
	{
		return dataCiteMapper.startConversion();
	}


	/**
	 * Saves the conversion result to disk.
	 *
	 * @return an info message that describes the status of the operation
	 */
	@POST
	@Path ("save")
	@Produces ({
			MediaType.TEXT_PLAIN
	})
	public String saveToDisk()
	{
		if (!dataCiteMapper.isFinished())
		{
			return SAVE_FAILED;
		}
		String filePath = String.format( FILE_PATH, MainContext.getModuleName(), System.currentTimeMillis() );
		return DevelopmentTools.instance().saveJsonToDisk( dataCiteMapper.getConvertedDocuments(), filePath );
	}


	/**
	 * Submits the conversion result to ElasticSearch.
	 *
	 * @return an info message that describes the status of the operation
	 */
	@POST
	@Path ("submit")
	@Produces ({
			MediaType.TEXT_PLAIN
	})
	public String submitToElasticSearch()
	{
		if (!dataCiteMapper.isFinished())
		{
			return SUBMIT_FAILED;
		}
		return ElasticSearchSender.instance().sendToElasticSearch( dataCiteMapper.getConvertedDocuments() );
	}
}
