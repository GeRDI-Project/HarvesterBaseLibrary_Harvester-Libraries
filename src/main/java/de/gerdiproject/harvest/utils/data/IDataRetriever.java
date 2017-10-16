package de.gerdiproject.harvest.utils.data;

import java.lang.reflect.Type;

import org.jsoup.nodes.Document;

import com.google.gson.JsonElement;

/**
 * This interface describes a means for retrieving data from an arbitrary source.
 * @author Robin Weiss
 *
 */
public interface IDataRetriever
{
    /**
     * Tries to parse the content from a specified path as a string.
     *
     * @param path
     *            the path to a HTML file
     * @return a text, or null if the file could not be parsed
     */
    String getString(String path);

    /**
     * Tries to parse the content from a specified path as a
     * JSON object or array.
     *
     * @param path the path to a JSON file
     * @return a JSON object or array, or null if the file could not be parsed
     */
    JsonElement getJson(String path);

    /**
     * Tries to parse the content from a specified path as a
     * JSON object.
     *
     * @param path the path to a JSON file
     * @param targetClass the class of the object that is read from disc
     * @param <T> the type of the object that is to be read
     *
     * @return an object, or null if the file could not be parsed
     */
    <T> T getObject(String path, Class<T> targetClass);

    /**
     * Tries to parse the content from a specified path as a
     * JSON object.
     *
     * @param path the path to a JSON file
     * @param targetType the type of the object that is read from disc
     * @param <T> the type of the object that is to be read
     *
     * @return an object, or null if the file could not be parsed
     */
    <T> T getObject(String path, Type targetType);

    /**
     * Tries to parse the content from a specified path as a
     * HTML document.
     *
     * @param path the path to a HTML file
     * @return a HTML document, or null if the file could not be parsed
     */
    Document getHtml(String path);
}
