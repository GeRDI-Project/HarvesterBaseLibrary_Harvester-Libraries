package de.gerdiproject.harvest.utils.cleaner;

import java.util.HashMap;
import java.util.Map;

public class CleanerFactory
{
    private static Map<Class<?>, Class<? extends ICleaner<?>>> cleanerMap = new HashMap<>();


    @SuppressWarnings("unchecked")  // due to the implementation of registerCleaner(), the Cast always succeeds
    public static <T> ICleaner<T> createCleaner(Class<T> cleanerClass)
    {
        try {
            return (ICleaner<T>) cleanerMap.get(cleanerClass).newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            return null;
        }
    }

    public static void registerCleaner(Class<?> cleanableClass, Class<? extends ICleaner<?>> cleanerClass)
    {
        cleanerMap.put(cleanableClass, cleanerClass);
    }
}
