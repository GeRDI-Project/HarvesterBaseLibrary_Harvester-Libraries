/**
 * Copyright © 2017 Robin Weiss (http://www.gerdi-project.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.gerdiproject.harvest.utils.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.file.NoSuchFileException;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import de.gerdiproject.harvest.utils.data.constants.DataOperationConstants;
import de.gerdiproject.harvest.utils.file.FileUtils;
import lombok.Setter;

/**
 * This class provides methods for reading files from disk.
 *
 * @author Robin Weiss
 */
public class DiskIO implements IDataRetriever
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DiskIO.class);
    private final Gson gson;

    @Setter
    private Charset charset;


    /**
     * Constructor that sets the GSON (de-)serializer for reading and
     * writing JSON objects.
     *
     * @param gson the GSON (de-)serializer for reading and writing JSON objects
     * @param charset the charset of the files to be read and written
     */
    public DiskIO(final Gson gson, final Charset charset)
    {
        this.gson = gson;
        this.charset = charset;
    }


    /**
     * Constructor that copies over settings from another {@linkplain DiskIO}.
     *
     * @param other the {@linkplain DiskIO} of which the settings are copied
     */
    public DiskIO(final DiskIO other)
    {
        this.gson = other.gson;
        this.charset = other.charset;
    }


    /**
     * Writes a string to a file on disk.
     * @param filePath
     *      the complete path to the file
     * @param fileContent
     *      the string that is to be written to the file
     *
     * @return a String that describes the status of the operation
     */
    public String writeStringToFile(final String filePath, final String fileContent)
    {
        return writeStringToFile(new File(filePath), fileContent);
    }


    /**
     * Writes a string to a file on disk.
     * @param file the file to which the String is written
     * @param fileContent
     *      the string that is to be written to the file
     *
     * @return a String that describes the status of the operation
     */
    public String writeStringToFile(final File file, final String fileContent)
    {
        final String filePath = file.getAbsolutePath();

        String statusMessage;
        boolean isSuccessful = false;

        // create directories
        final boolean isDirectoryCreated = file.getParentFile().exists() || file.getParentFile().mkdirs();

        if (isDirectoryCreated) {
            // write content to file
            try
                (BufferedWriter writer = FileUtils.getWriter(file,  charset)) {
                writer.write(fileContent);

                // set status message
                isSuccessful = true;
                statusMessage = String.format(DataOperationConstants.SAVE_OK, filePath);

            } catch (IOException | SecurityException e) {
                LOGGER.warn(String.format(DataOperationConstants.SAVE_FAILED, filePath), e);
                statusMessage = String.format(DataOperationConstants.SAVE_FAILED, filePath);
            }
        } else {
            statusMessage = String.format(
                                DataOperationConstants.SAVE_FAILED_NO_FOLDERS,
                                filePath);
        }

        // log the status
        if (isSuccessful)
            LOGGER.trace(statusMessage);
        else
            LOGGER.warn(statusMessage);

        return statusMessage;
    }


    /**
     * Attempts to transform an object to a JSON object and writes it to a file on disk.
     * @param filePath
     *      the complete path to the file
     * @param obj
     *      the object that is to be written to the file
     *
     * @return a String that describes the status of the operation
     */
    public String writeObjectToFile(final String filePath, final Object obj)
    {
        final String jsonString = (obj == null) ? "{}" : gson.toJson(obj);
        return writeStringToFile(filePath, jsonString);
    }


    /**
     * Attempts to transform an object to a JSON object and writes it to a file on disk.
     * @param file
     *      the file to which the object should be written
     * @param obj
     *      the object that is to be written to the file
     *
     * @return a String that describes the status of the operation
     */
    public String writeObjectToFile(final File file, final Object obj)
    {
        final String jsonString = (obj == null) ? "{}" : gson.toJson(obj);
        return writeStringToFile(file, jsonString);
    }


    /**
     * Tries to parse the content of a specified file as a string.
     *
     * @param file the file that is to be parsed
     * @return a string, or null if the file could not be parsed
     */
    public String getString(final File file)
    {
        String fileContent = null;

        try
            (BufferedReader reader = FileUtils.getReader(file, charset)) {
            fileContent = reader.lines().collect(Collectors.joining("\n"));

        } catch (final NoSuchFileException e) { // NOPMD if the file is not found, do not log anything

        } catch (final IOException e) {
            LOGGER.warn(String.format(DataOperationConstants.LOAD_FAILED, file.getAbsolutePath()), e);
        }

        return fileContent;
    }


    /**
     * Tries to parse the content of a specified file as an object.
     *
     * @param file a JSON file representing the object
     * @param targetClass the class of the object that is read
     * @param <T> the type of the object that is to be read
     *
     * @return an object, or null if the file could not be parsed
     */
    public <T> T getObject(final File file, final Class<T> targetClass)
    {
        T object = null;

        try
            (Reader reader = FileUtils.getReader(file, charset)) {
            object = gson.fromJson(reader, targetClass);

        } catch (final NoSuchFileException e) { // NOPMD if the file is not found, do not log anything

        } catch (IOException | IllegalStateException | JsonIOException | JsonSyntaxException e) {
            LOGGER.warn(String.format(DataOperationConstants.LOAD_FAILED, file.getAbsolutePath()), e);
        }

        return object;
    }


    /**
     * Tries to parse the content of a specified file as an object.
     *
     * @param file a JSON file representing the object
     * @param targetType the type of the object that is read
     * @param <T> the type of the object that is to be read
     *
     * @return an object, or null if the file could not be parsed
     */
    public <T> T getObject(final File file, final Type targetType)
    {
        T object = null;

        try
            (Reader reader = FileUtils.getReader(file, charset)) {
            object = gson.fromJson(reader, targetType);

        } catch (final NoSuchFileException e) { // NOPMD if the file is not found, do not log anything

        } catch (IOException | IllegalStateException | JsonIOException | JsonSyntaxException e) {
            LOGGER.warn(String.format(DataOperationConstants.LOAD_FAILED, file.getAbsolutePath()), e);
        }

        return object;
    }


    @Override
    public String getString(final String filePath)
    {
        return getString(new File(filePath));
    }


    @Override
    public <T> T getObject(final String filePath, final Class<T> targetClass)
    {
        return getObject(new File(filePath), targetClass);
    }


    @Override
    public <T> T getObject(final String filePath, final Type targetType)
    {
        return getObject(new File(filePath), targetType);
    }


    @Override
    public Document getHtml(final String filePath)
    {
        Document htmlResponse = null;

        final String fileContent = getString(filePath);

        if (fileContent != null)
            htmlResponse = Jsoup.parse(fileContent);

        return htmlResponse;
    }
}
