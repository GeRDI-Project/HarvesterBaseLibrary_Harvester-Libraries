package de.gerdiproject.harvest.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import org.jsoup.nodes.Document;
import com.google.gson.JsonElement;

import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.utils.data.DiskIO;
import de.gerdiproject.json.IJson;
import de.gerdiproject.json.impl.GsonArray;
import de.gerdiproject.json.impl.GsonObject;

/**
 * Deprecated: Use {@link DiskIO} instead
 * This class offers methods for file operations.
 * @author Robin Weiss
 *
 */
@Deprecated
public class FileUtils
{
    private final static DiskIO DISK_IO = new DiskIO();

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
        return DISK_IO.writeStringToFile(filePath, fileContent);
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
        return DISK_IO.getString(filePath);
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
        JsonElement ele = DISK_IO.getJson(filePath);
        IJson readJson = null;

        if (ele != null && ele.isJsonArray())
            readJson = new GsonArray(ele.getAsJsonArray());

        else if (ele != null && ele.isJsonObject())
            readJson = new GsonObject(ele.getAsJsonObject());

        return readJson;
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
        return DISK_IO.getHtml(filePath);
    }

}
