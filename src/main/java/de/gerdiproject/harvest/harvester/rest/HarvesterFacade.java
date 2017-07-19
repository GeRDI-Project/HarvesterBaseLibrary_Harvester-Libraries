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
package de.gerdiproject.harvest.harvester.rest;


import de.gerdiproject.harvest.harvester.AbstractHarvester;
import de.gerdiproject.json.IJsonObject;
import de.gerdiproject.harvest.MainContext;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;


/**
 * A facade for the harvester that serves as a RESTful interface. It provides
 * REST requests that manipulate the harvester in order to prepare and send
 * search indices to Elastic Search.
 *
 * @see de.gerdiproject.harvest.harvester.AbstractHarvester
 * @author Robin Weiss
 */
@Path("harvest")
public class HarvesterFacade
{
    private static final String INFO = "- %s -\n\nStatus:\t\t%s\n\nRange:\t\t%d-%d\n%s\n\n"
                                       + "GET/result \t\tReturns the search index\n"
                                       + "POST/save\t\tSaves the search index to disk\n"
                                       + "POST\t\t\tCreates a search index\n"
                                       + "POST/abort\t\tAborts an ongoing harvest\n"
                                       + "PUT \t\t\tSets x-www-form-urlencoded parameters for the harvester (%s).\n"
                                       + "PUT/range\t\tSets the start and end index of the harvest (from, to). default: 0, %d\n";
    private static final String PROPERTY = "%s:\t%s\n";
    private static final String HARVEST_NOT_STARTED = "Not yet harvested!";
    private static final String HARVEST_IN_PROGRESS = "Harvested %d / %d (%.2f%%)  Remaining Time: %s";
    private static final String DAYS_HOURS = "%dd %dh";
    private static final String HOURS_MINUTES = "%dh %dm";
    private static final String MINUTES_SECONDS = "%dm %ds";
    private static final String SECONDS = "%ds";
    private static final String CANNOT_ESTIMATE = "unknown";
    private static final String HARVEST_DONE = "Harvested finished at %s";
    private static final String HARVEST_STARTED = "Harvesting started";
    private static final String HARVEST_ALREADY_STARTED = "Another harvest is already in progress";
    private static final String HARVEST_ABORTED = "Harvesting aborted";
    private static final String HARVEST_ABORTED_FAILED = "No harvest is in progress";
    private static final String NO_CHANGES = "No changes were made. Valid Form Parameters: %s";
    private static final String SET_OK = "Set property '%s' to '%s'\n";
    private static final String SET_FAILED = "Cannot set '%s'! It is not a valid property\n";

    private static final String FORM_PARAM_FROM = "from";
    private static final String FORM_PARAM_TO = "to";
    private static final String RANGE_SET_FAILED_RANGE = "Invalid Harvesting range: %s - %s\nRange should be between 0 and %d";
    private static final String RANGE_SET_FAILED_PARAMS = "Invalid Parameters! All x-www-form-urlencoded parameters must be set."
                                                          + " Use\n'from' to set the start index (default: 0)\n'to' to set the end index (default: %d)";
    private static final String RANGE_SET_OK = "Harvesting Range set from %d to %d.";

    private final AbstractHarvester harvester = MainContext.getHarvester();


    /**
     * Starts a harvest using the harvester that is registered in the
     * MainContext.
     *
     * @param formParams
     *            optional parameters encompass "from" and "to" to set the
     *            harvest range
     * @return a status string that describes the success or failure of the
     *         harvest
     */
    @POST
    @Produces({
        MediaType.TEXT_PLAIN
    })
    public String startHarvest(final MultivaluedMap<String, String> formParams)
    {
        // start the harvest
        if (harvester.isHarvesting())
            return HARVEST_ALREADY_STARTED;

        else {
            harvester.harvest();
            return HARVEST_STARTED;
        }
    }


    /**
     * Sets the harvest range, specifying the index of the first and last
     * document to be harvested.
     *
     * @param formParams
     *            should include 'from' and 'to'
     * @return a message describing if the range setting was successful or not
     */
    @PUT
    @Path("range")
    @Consumes({
        MediaType.APPLICATION_FORM_URLENCODED
    })
    @Produces({
        MediaType.TEXT_PLAIN
    })
    public String setRange(final MultivaluedMap<String, String> formParams)
    {
        List<String> fromParam = formParams.getOrDefault(FORM_PARAM_FROM, null);
        List<String> toParam = formParams.getOrDefault(FORM_PARAM_TO, null);

        int maxRange = harvester.getTotalNumberOfDocuments();

        if (fromParam != null && toParam != null) {
            try {
                // get and convert form params to integer
                int from = Integer.parseInt(fromParam.get(0));
                int to = Integer.parseInt(toParam.get(0));

                // make sure the specified range is valid
                if (to < from || from < 0 || to > maxRange)
                    throw new NumberFormatException();

                harvester.setRange(from, to);
                return String.format(RANGE_SET_OK, from, to);

            } catch (NumberFormatException e) {
                // a parameter could not be cast to int, or is out of range
                return String.format(
                           RANGE_SET_FAILED_RANGE,
                           fromParam.get(0),
                           toParam.get(0),
                           maxRange);
            }
        }

        return String.format(RANGE_SET_FAILED_PARAMS, maxRange);
    }


    /**
     * Sets one or more properties of the registered harvester.
     *
     * @param formParams
     *            a generic list of x-www-form-urlencoded key value pairs
     * @return a status text describing successful or unsuccessful changes of
     *         properties
     */
    @PUT
    @Consumes({
        MediaType.APPLICATION_FORM_URLENCODED
    })
    @Produces({
        MediaType.TEXT_PLAIN
    })
    public String setProperties(final MultivaluedMap<String, String> formParams)
    {
        StringBuilder sb = new StringBuilder();
        List<String> validProperties = harvester.getValidProperties();
        boolean hasChanges = false;

        for (Entry<String, List<String>> entry : formParams.entrySet()) {
            // retrieve the property key
            String key = entry.getKey();

            // check if property is among the valid properties of the harvester
            if (validProperties.contains(key)) {
                hasChanges = true;

                // a value can appear multiple times. process all values
                for (String value : entry.getValue()) {
                    // set harvester property and add the change to the response
                    // string
                    harvester.setProperty(key, value);
                    sb.append(String.format(SET_OK, key, value));
                }
            } else
                sb.append(String.format(SET_FAILED, key));
        }

        // if no property changes were made, inform the user
        if (!hasChanges)
            sb.append(String.format(NO_CHANGES, String.join(", ", harvester.getValidProperties())));

        return sb.toString();
    }


    @GET
    @Path("isLatest")
    @Produces({
        MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON
    })
    public String isLatestVersion()
    {
        final String latestChecksum = ""; // TODO get from Elasticsearch or from
        // prospector(?)
        final String currentChecksum = harvester.getHash(true);

        return String.valueOf(currentChecksum != null && !currentChecksum.equals(latestChecksum));
    }


    /**
     * Displays a text that describes the status and possible REST calls.
     *
     * @return a text describing the harvesting status and available RESTful
     *         calls
     */
    @GET
    @Produces({
        MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON
    })
    public String getInfo()
    {
        // get harvest date
        Date harvestDate = harvester.getHarvestDate();

        String status;

        if (harvester.isHarvesting()) {
            int numberOfHarvestedDocuments = harvester.getNumberOfHarvestedDocuments();
            int totalNumberOfDocuments = harvester.getHarvestEndIndex() - harvester.getHarvestStartIndex();

            // calculate completion in percent and estimate completion time
            double progressInPercent = 100.0 * numberOfHarvestedDocuments / totalNumberOfDocuments;
            String estimatedCompletion = getDurationText(harvester.estimateRemainingSeconds());

            status = String.format(
                         HARVEST_IN_PROGRESS,
                         numberOfHarvestedDocuments,
                         totalNumberOfDocuments,
                         progressInPercent,
                         estimatedCompletion);

        } else if (harvestDate != null)
            status = String.format(HARVEST_DONE, harvestDate.toString());

        else
            status = HARVEST_NOT_STARTED;

        // get harvesting range
        int from = harvester.getHarvestStartIndex();
        int to = harvester.getHarvestEndIndex();

        // get harvester name
        String name = MainContext.getModuleName();

        // get properties and values
        StringBuilder sb = new StringBuilder();
        harvester.getValidProperties().forEach((String s) -> {
            // convert first char to upper case
            String propertyName = s.substring(0, 1).toUpperCase() + s.substring(1);
            sb.append(String.format(PROPERTY, propertyName, harvester.getProperty(s)));
        });

        // get valid properties
        String validProps = String.join(", ", harvester.getValidProperties());

        // get valid harvesting range
        int maxRange = harvester.getTotalNumberOfDocuments();

        return String.format(INFO, name, status, from, to, sb.toString(), validProps, maxRange);
    }


    /**
     * Creates a duration string out of a specified number of seconds
     *
     * @param durationInSeconds
     *            the duration in seconds (duh!)
     * @return a formatted duration string, or "unknown" if the duration is
     *         negative
     */
    private String getDurationText(long durationInSeconds)
    {
        String durationText;

        if (durationInSeconds < 0)
            durationText = CANNOT_ESTIMATE;

        else if (durationInSeconds <= 60)
            durationText = String.format(SECONDS, durationInSeconds);

        else if (durationInSeconds <= 3600) {
            long minutes = durationInSeconds / 60;
            long seconds = durationInSeconds - minutes * 60;
            durationText = String.format(MINUTES_SECONDS, minutes, seconds);

        } else if (durationInSeconds <= 86400) {
            long hours = durationInSeconds / 3600;
            long minutes = durationInSeconds / 60 - hours * 60;
            durationText = String.format(HOURS_MINUTES, hours, minutes);

        } else {
            long days = durationInSeconds / 86400;
            long hours = durationInSeconds / 3600 - days * 24;
            durationText = String.format(DAYS_HOURS, days, hours);
        }

        return durationText;
    }


    /**
     * Displays the search index or an empty object if nothing was harvested.
     *
     * @return the search index or an empty object if nothing was harvested
     */
    @GET
    @Path("result")
    @Produces({
        MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON
    })
    public String getResult()
    {
        IJsonObject result = harvester.getHarvestResult();

        return (result != null)
               ? result.toJsonString()
               : "{}";
    }


    /**
     * Displays the search index or an empty object if nothing was harvested.
     *
     * @return the search index or an empty object if nothing was harvested
     */
    @POST
    @Path("abort")
    @Produces({
        MediaType.TEXT_PLAIN
    })
    public String abortHarvest()
    {
        if (harvester.isHarvesting()) {
            harvester.abortHarvest();
            return HARVEST_ABORTED;

        } else
            return HARVEST_ABORTED_FAILED;
    }
}
