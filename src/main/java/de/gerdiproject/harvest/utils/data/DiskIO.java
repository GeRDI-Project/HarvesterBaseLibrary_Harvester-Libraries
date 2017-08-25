package de.gerdiproject.harvest.utils.data;

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
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.json.GsonUtils;

/**
 * This class provides methods for reading files from disk.
 * @author Robin Weiss
 *
 */
public class DiskIO implements IDataRetriever
{
    private static final String SAVE_OK = "Saved file: '%s'";
    private static final String SAVE_FAILED = "Could not save file '%s': %s";
    private static final String SAVE_FAILED_NO_FOLDERS = "Failed to create directories!";
    private static final String LOAD_FAILED = "Could not load file '%s': %s";

    private static final Logger LOGGER = LoggerFactory.getLogger(DiskIO.class);

    /**
     * Writes a string to a file on disk.
     * @param filePath
     *      the complete path to the file
     * @param fileContent
     *      the string that is to be written to the file
     *
     * @return a String that describes the status of the operation
     */
    public String writeStringToFile(String filePath, String fileContent)
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
     * Writes a JSON element to a file on disk.
     * @param filePath
     *      the complete path to the file
     * @param json
     *      the JSON element that is to be written to the file
     *
     * @return a String that describes the status of the operation
     */
    public String writeJsonToFile(String filePath, JsonElement json)
    {
        // deliberately write an empty object to disk
        String jsonString = (json == null) ? "{}" : GsonUtils.getGson().toJson(json);
        return writeStringToFile(filePath, jsonString);
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
    public String writeObjectToFile(String filePath, Object obj)
    {
        String jsonString = (obj == null) ? "null" : GsonUtils.getGson().toJson(obj);
        return writeStringToFile(filePath, jsonString);
    }


    @Override
    public String getString(String filePath)
    {
        String fileContent = null;

        try {
            BufferedReader reader = new BufferedReader(createDiskReader(filePath));
            fileContent = reader.lines().collect(Collectors.joining("\n"));

            reader.close();
        } catch (IOException e) {
            LOGGER.warn(String.format(LOAD_FAILED, filePath, e.toString()));
        }

        return fileContent;
    }


    @Override
    public JsonElement getJson(String filePath)
    {
        JsonElement fileContent = null;

        try {
            Reader reader = createDiskReader(filePath);
            JsonParser parser = new JsonParser();

            fileContent = parser.parse(reader);
            reader.close();
        } catch (IOException | IllegalStateException | JsonIOException | JsonSyntaxException e) {
            LOGGER.warn(String.format(LOAD_FAILED, filePath, e.toString()));
        }

        return fileContent;
    }


    @Override
    public <T> T getObject(String filePath, Class<T> targetClass)
    {
        T object = null;

        try {
            Reader reader = createDiskReader(filePath);
            object = GsonUtils.getGson().fromJson(reader, targetClass);
            reader.close();

        } catch (IOException | IllegalStateException | JsonIOException | JsonSyntaxException e) {
            LOGGER.warn(String.format(LOAD_FAILED, filePath, e.toString()));
        }

        return object;
    }


    @Override
    public Document getHtml(String filePath)
    {
        Document htmlResponse = null;

        String fileContent = getString(filePath);

        if (fileContent != null)
            htmlResponse = Jsoup.parse(fileContent);

        return htmlResponse;
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
    private Reader createDiskReader(String filePath) throws FileNotFoundException
    {
        // try to read from disk
        return new InputStreamReader(new FileInputStream(filePath), MainContext.getCharset());
    }
}
