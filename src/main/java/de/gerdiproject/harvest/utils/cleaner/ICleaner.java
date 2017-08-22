package de.gerdiproject.harvest.utils.cleaner;

/**
 * This interface is used for defining cleaning methods for documents
 * @author Robin Weiss
 *
 * @param <T> the type of the object that is to be cleaned
 */
public interface ICleaner<T>
{
    /**
     * Cleans an object to remove superfluous data before submitting it to ElasticSearch,
     * @param dirtyObject
     */
    T clean(T dirtyObject);
}
