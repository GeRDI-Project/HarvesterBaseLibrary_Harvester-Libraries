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
package de.gerdiproject.harvest.elasticsearch.rest;


import de.gerdiproject.harvest.harvester.AbstractHarvester;
import de.gerdiproject.harvest.IDocument;
import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.elasticsearch.ElasticSearchSender;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;


/**
 * A facade for ElasticSearchSender that serves as a RESTful interface. It
 * provides REST requests that manipulate the ElasticSearchSender in order to
 * prepare and send search indices to Elastic Search.
 *
 * @see de.gerdiproject.harvest.elasticsearch.ElasticSearchSender
 * @author Robin Weiss
 */
@Path("elasticsearch")
public class ElasticSearchSenderFacade
{
    private static final String NO_CHANGES = "Nothing was changed! Valid Form Parameters: url, index, type, username, password\n";
    private static final String MISSING_PARAM_URL = "Missing www-form-urlencoded parameter 'url'. This must be a Elastic Search base URL";
    private static final String MISSING_PARAM_INDEX = "Missing www-form-urlencoded parameter 'index'. This must be the targeted search index on the Elastic Search node";
    private static final String MISSING_PARAM_TYPE = "Missing www-form-urlencoded parameter 'type'. This must be the targeted type on the Elastic Search node";
    private static final String AUTHORIZATION_OK = "Set Elastic Search user to '%s'";
    private static final String MISSING_PARAM_PASSWORD = "Could not set user to '%s'. Password is missing";
    private static final String URL_MISSING = "No URL has been set up";
    private static final String INFO = "- %s ElasticSearch Interface -%n%nUrl:\t%s/%s/%s";
    private static final String INFO_USER = "\nUser:\t";
    private static final String INFO_REST = "\n\nPOST\tSends the harvested search index to Elastic Search\n"
                                            + "PUT\tSets up the Elastic Search URL and optionally authorization. Form Parameters: url, index, type, username, password\n";


    /**
     * GET: Displays info about the ElasticSearchSender and form navigation.
     *
     * @return the Elastic Search URL and possible HTTP requests.
     */
    @GET
    @Produces({
        MediaType.TEXT_PLAIN
    })
    public String getInfo()
    {
        // get URL or default text
        String url = ElasticSearchSender.instance().getBaseUrl();
        url = (url == null) ? URL_MISSING : url;

        String index = ElasticSearchSender.instance().getIndex();
        String type = ElasticSearchSender.instance().getType();

        // get user name
        String userName = ElasticSearchSender.instance().getUserName();

        // add title and URL
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(INFO, MainContext.getModuleName(), url, index, type));

        // add user name if it exists
        if (userName != null)
            sb.append(INFO_USER).append(userName);

        // add REST method info
        sb.append(INFO_REST);

        return sb.toString();
    }


    /**
     * POST: Submits a search index to Elastic Search. If the search index has
     * not been harvested yet, it will be harvested automatically before being
     * sent.
     *
     * @return the Elastic Search HTTP response, or an error code if the
     *         operation does not succeed
     */
    @POST
    @Produces({
        MediaType.TEXT_PLAIN
    })
    public String submitSearchIndex()
    {
        AbstractHarvester harvester = MainContext.getHarvester();

        // harvest the data, if it has not been done yet
        if (!harvester.isFinished() && !harvester.isHarvesting())
            harvester.harvest();

        // retrieve harvested search index
        // TODO List<IDocument> harvestedDocuments = harvester.getHarvestedDocuments();

        // send search index to Elastic Search
        // TODO String status = ElasticSearchSender.instance().sendToElasticSearch(harvestedDocuments);

        // TODO return status;
        return null;
    }


    /**
     * Sets properties of the ElasticSearchSender that are required for
     * uploading search indices.
     *
     * @param url
     *            the main Elastic Search URL without index or type
     * @param index
     *            the Elastic Search index that will be updated/created
     * @param type
     *            the Elastic Search type within the index that will be
     *            updated/created
     * @param username
     *            (optional) the user name, if access is restricted
     * @param password
     *            (optional) the password, if access is restricted
     *
     * @return confirmation or error messages concerning the property changes
     */
    @PUT
    @Produces({
        MediaType.TEXT_PLAIN
    })
    @Consumes({
        MediaType.APPLICATION_FORM_URLENCODED
    })
    public String setProperties(
        @FormParam("url") String url,
        @FormParam("index") String index,
        @FormParam("type") String type,
        @FormParam("username") String username,
        @FormParam("password") String password)
    {
        StringBuilder sb = new StringBuilder();

        // check if form parameters are valid
        boolean hasUrl = url != null && !url.isEmpty();
        boolean hasIndex = index != null && !index.isEmpty();
        boolean hasType = type != null && !type.isEmpty();

        // ignore "index" and "type" if "url" is not among the form parameters
        if (hasUrl || hasIndex || hasType) {
            // verify that "url" was submitted
            if (!hasUrl)
                sb.append(MISSING_PARAM_URL).append('\n');

            // verify that "index" was submitted
            if (!hasIndex)
                sb.append(MISSING_PARAM_INDEX).append('\n');

            // verify that "type" was submitted
            if (!hasType)
                sb.append(MISSING_PARAM_TYPE).append('\n');

            // set up Elastic Search URL if all required parameters are there
            if (hasUrl && hasIndex && hasType) {
                String status = ElasticSearchSender.instance().setUrl(url, index, type);
                sb.append(status).append('\n');
            }
        }

        // set up credentials if both "username" and "password" are form
        // parameters
        if (username != null) {
            if (!username.isEmpty() && (password == null || password.isEmpty()))
                sb.append(String.format(MISSING_PARAM_PASSWORD, username)).append('\n');
            else {
                ElasticSearchSender.instance().setCredentials(username, password);
                sb.append(String.format(AUTHORIZATION_OK, username)).append('\n');
            }
        }

        // if nothing was attempted to be changed, inform the user
        if (sb.length() == 0)
            sb.append(NO_CHANGES);

        return sb.toString();
    }
}
