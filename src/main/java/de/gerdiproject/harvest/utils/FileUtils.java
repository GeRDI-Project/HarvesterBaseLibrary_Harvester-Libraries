package de.gerdiproject.harvest.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.json.IJson;
import de.gerdiproject.json.IJsonBuilder;
import de.gerdiproject.json.IJsonReader;
import de.gerdiproject.json.impl.JsonBuilder;

/**
 * This class offers methods for file operations.
 * @author Robin Weiss
 *
 */
public class FileUtils
{
    private final static String SAVE_OK = "Saved file: '%s'";
    private final static String SAVE_FAILED = "Could not save file '%s': %s";
    private final static String SAVE_FAILED_NO_FOLDERS = "Failed to create directories!";
    private final static String LOAD_FAILED = "Could not load file '%s': %s";

    private final static Logger LOGGER = LoggerFactory.getLogger(FileUtils.class);
    private final static IJsonBuilder JSON_BUILDER = new JsonBuilder();

    /**
     * Writes a string to a file on disk.
     * @param filePath
     *      the complete path to the file
     * @param fileContent
     *      the string that is to be written to the file
     *
     * @return a String that describes the status of the operation
     */
    public static String writeToDisk(String filePath, String fileContent)
    {
        String statusMessage;
        boolean isSuccessful = false;

        try {
            File file = new File(filePath);

            // create directories
            boolean isDirectoryCreated = file.getParentFile().exists() || file.getParentFile().mkdirs();

            if (!isDirectoryCreated)
                statusMessage = String.format(SAVE_FAILED, filePath, SAVE_FAILED_NO_FOLDERS);
            else {
                // write content to file
                BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
                writer.write(fileContent);

                writer.close();

                // set status message
                isSuccessful = true;
                statusMessage = String.format(SAVE_OK, filePath);
            }
        } catch (IOException | SecurityException e) {
            statusMessage = String.format(SAVE_FAILED, filePath, e.getMessage());
        }

        // log the status
        if (isSuccessful)
            LOGGER.info(statusMessage);
        else
            LOGGER.error(statusMessage);

        return statusMessage;

    }

    /**
     * Reads a file.
     * @param filePath
     *      the complete path to the file that is to be read
     * @return a reader
     *      that can parse the file
     * @throws FileNotFoundException
     *      this exception is thrown if the specified file does not exist
     */
    public static Reader readFromDisk(String filePath) throws FileNotFoundException
    {
        // try to read from disk
        return new InputStreamReader(new FileInputStream(filePath), MainContext.getCharset());
    }

    /**
     * Opens a file at the specified path and returns the content as a string.
     *
     * @param filePath
     *            the filepath to a HTML file
     * @return a text, or null if the file could not be parsed
     */
    public static String readStringFromDisk(String filePath)
    {
        String fileContent = null;

        try {
            BufferedReader reader = new BufferedReader(readFromDisk(filePath));
            fileContent = reader.lines().collect(Collectors.joining("\n"));

            reader.close();
        } catch (IOException e) {
            LOGGER.warn(String.format(LOAD_FAILED, filePath, e.toString()));
        }

        return fileContent;
    }

    /**
     * Opens a file at the specified path and tries to parse the content as a
     * JSON object or array.
     *
     * @param filePath
     *            the filepath to a HTML file
     * @return a JSON object or array, or null if the file could not be parsed
     */
    public static IJson readJsonFromDisk(String filePath)
    {
        IJson fileContent = null;

        try {
            IJsonReader reader = JSON_BUILDER.createReader(readFromDisk(filePath));
            fileContent = reader.read();

            reader.close();
        } catch (IOException | IllegalStateException | ParseException e) {
            LOGGER.warn(String.format(LOAD_FAILED, filePath, e.toString()));
        }

        return fileContent;
    }

    /**
     * Opens a file at the specified path and tries to parse the content as a
     * HTML document.
     *
     * @param filePath
     *            the filepath to a HTML file
     * @return a HTML document, or null if the file could not be parsed
     */
    public static Document readHtmlFromDisk(String filePath)
    {
        Document htmlResponse = null;

        String fileContent = readStringFromDisk(filePath);

        if (fileContent != null)
            htmlResponse = Jsoup.parse(fileContent);

        return htmlResponse;
    }

}
