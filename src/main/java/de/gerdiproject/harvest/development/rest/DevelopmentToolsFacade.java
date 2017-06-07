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
import de.gerdiproject.harvest.development.DevelopmentTools;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * A RESTful interface. for developer options and tools. It provides REST
 * requests for caching web requests and automatically submitting or saving a
 * harvest result to disk.
 *
 * @see DevelopmentTools
 * @author row
 */
@Path("dev")
public class DevelopmentToolsFacade
{
    private final static String OPTION_YES_1 = "true";
    private final static String OPTION_YES_2 = "1";
    private final static String OPTION_YES_3 = "yes";
    private final static String NO_CHANGES = "Nothing was changed! Valid Form Parameters: auto_save, auto_submit, read_from_disk, write_to_disk";
    private final static String WRITE_TO_DISK_SET = "Write HTTP responses to disk: ";
    private final static String READ_FROM_DISK_SET = "Read HTTP requests from disk: ";
    private final static String AUTO_SUBMIT_SET = "Automatically submit to search index after harvest: ";
    private final static String AUTO_SAVE_SET = "Automatically save harvest result to disk: ";
    private final static String INFO = "- %s Advanced Tools -\n\n"
            + WRITE_TO_DISK_SET + "%b\n"
            + READ_FROM_DISK_SET + "%b\n"
            + AUTO_SUBMIT_SET + "%b\n"
            + AUTO_SAVE_SET + "%b\n\n"
            + "PUT\t\t\tChange any of the above options. Form Parameters: auto_save, auto_submit, read_from_disk, write_to_disk\n"
            + "POST/save\tSaves the search index to disk";


    /**
     * Displays an info string that shows the available REST calls and current
     * state of the developer options.
     *
     * @return an info string
     */
    @GET
    @Produces(
            {
                MediaType.TEXT_PLAIN
            })
    public String getInfo()
    {
        final DevelopmentTools devTools = DevelopmentTools.instance();
        return String.format(
                INFO,
                MainContext.getModuleName(),
                devTools.isWritingHttpToDisk(),
                devTools.isReadingHttpFromDisk(),
                devTools.isAutoSubmitting(),
                devTools.isAutoSaving()
        );
    }


    /**
     * Saves the harvest result to disk.
     *
     * @return an info message that describes the status of the operation
     */
    @POST
    @Path("save")
    @Produces(
            {
                MediaType.TEXT_PLAIN
            })
    public String saveToDisk()
    {
        return DevelopmentTools.instance().saveHarvestResultToDisk();
    }


    /**
     * Changes development options.
     *
     * @param autoSave if true, results are automatically written to disk after
     * a successful harvest
     * @param autoSubmit if true, results are automatically submitted to elastic
     * search after a successful harvest
     * @param readFromDisk if true, instead of sending HTTP requests, the local
     * file system is browsed for cached responses, that have been saved via the
     * 'writeToDisk' flag
     * @param writeToDisk if true, all HTTP responses are written to the local
     * file system. Failed responses result in an empty object
     * @return A string that describes which options have been changed.
     */
    @PUT
    @Produces(
            {
                MediaType.TEXT_PLAIN
            })
    @Consumes(
            {
                MediaType.APPLICATION_FORM_URLENCODED
            })
    public String setProperties(
            @FormParam("auto_save") String autoSave,
            @FormParam("auto_submit") String autoSubmit,
            @FormParam("read_from_disk") String readFromDisk,
            @FormParam("write_to_disk") String writeToDisk )
    {
        DevelopmentTools devTools = DevelopmentTools.instance();

        StringBuilder sb = new StringBuilder();

        if (autoSave != null && !autoSave.isEmpty())
        {
            boolean state = autoSave.equals( OPTION_YES_1 ) || autoSave.equals( OPTION_YES_2 ) || autoSave.equals( OPTION_YES_3 );
            devTools.setAutoSave( state );
            sb.append( AUTO_SAVE_SET ).append( state ).append( '\n' );
        }

        if (autoSubmit != null && !autoSubmit.isEmpty())
        {
            boolean state = autoSubmit.equals( OPTION_YES_1 ) || autoSubmit.equals( OPTION_YES_2 ) || autoSubmit.equals( OPTION_YES_3 );
            devTools.setAutoSubmit( state );
            sb.append( AUTO_SUBMIT_SET ).append( state ).append( '\n' );
        }

        if (readFromDisk != null && !readFromDisk.isEmpty())
        {
            boolean state = readFromDisk.equals( OPTION_YES_1 ) || readFromDisk.equals( OPTION_YES_2 ) || readFromDisk.equals( OPTION_YES_3 );
            devTools.setReadHttpFromDisk( state );
            sb.append( READ_FROM_DISK_SET ).append( state ).append( '\n' );
        }

        if (writeToDisk != null && !writeToDisk.isEmpty())
        {
            boolean state = writeToDisk.equals( OPTION_YES_1 ) || writeToDisk.equals( OPTION_YES_2 ) || writeToDisk.equals( OPTION_YES_3 );
            devTools.setWriteHttpToDisk( state );
            sb.append( WRITE_TO_DISK_SET ).append( state );
        }

        // if nothing was attempted to be changed, inform the user
        if (sb.length() == 0)
        {
            sb.append( NO_CHANGES );
        }

        return sb.toString();
    }
}
