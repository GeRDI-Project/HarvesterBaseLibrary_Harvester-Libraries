package de.gerdiproject.harvest.utils.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

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
 * This class provides methods for reading files from the web.
 * @author Robin Weiss
 *
 */
public class WebDataRetriever implements IDataRetriever
{
    private static final String ERROR_JSON = "Could not load and parse '%s': %s";
    private static final Logger LOGGER = LoggerFactory.getLogger(DiskIO.class);



    @Override
    public String getString(String url)
    {
        String responseText = null;

        try {
            // send web request
            InputStream response = new URL(url).openStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(response, MainContext.getCharset()));

            // read the first line of the response
            String line = reader.readLine();

            // make sure we got a response
            if (line != null) {
                StringBuilder responseBuilder = new StringBuilder(line);

                // read subsequent lines of the response
                line = reader.readLine();

                while (line != null) {
                    // add linebreak before appending the next line
                    responseBuilder.append('\n').append(line);
                    line = reader.readLine();
                }

                responseText = responseBuilder.toString();
            }

            // close the response reader
            reader.close();

        } catch (Exception e) {
            LOGGER.warn(String.format(ERROR_JSON, url, e.toString()));
        }

        return responseText;
    }

    @Override
    public JsonElement getJson(String url)
    {
        JsonElement jsonResponse = null;

        try {
            // send web request
            InputStream response = new URL(url).openStream();
            InputStreamReader reader = new InputStreamReader(response, MainContext.getCharset());
            JsonParser parser = new JsonParser();

            // parse the json object
            jsonResponse = parser.parse(reader);

            reader.close();

        } catch (Exception e) {
            LOGGER.warn(String.format(ERROR_JSON, url, e.toString()));
        }

        return jsonResponse;
    }

    @Override
    public <T> T getObject(String url, Class<T> targetClass)
    {
        T object = null;

        try {
            InputStream response = new URL(url).openStream();
            InputStreamReader reader = new InputStreamReader(response, MainContext.getCharset());

            object = GsonUtils.getGson().fromJson(reader, targetClass);
            reader.close();

        } catch (IOException | IllegalStateException | JsonIOException | JsonSyntaxException e) {
            LOGGER.warn(String.format(ERROR_JSON, url, e.toString()));
        }

        return object;
    }

    @Override
    public Document getHtml(String url)
    {
        Document htmlResponse = null;

        try {
            // send web request
            InputStream response = new URL(url).openStream();

            // parse the html object
            htmlResponse = Jsoup.parse(response, MainContext.getCharset().displayName(), url);

            response.close();

        } catch (Exception e) {
            LOGGER.warn(String.format(ERROR_JSON, url, e.toString()));
        }

        return htmlResponse;
    }
}
