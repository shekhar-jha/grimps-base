/*
 * Copyright 2017 Shekhar Jha
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

package org.grimps.base.utils;

import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.*;

/**
 * Utility Methods
 */
public class Utils {

    private static final XLogger logger = XLoggerFactory.getXLogger(Utils.class);

    /**
     * Returns whether the given string is null or empty
     *
     * @param object String to be validated
     * @return true if String is null or empty, false otherwise.
     */
    public static boolean isEmpty(String object) {
        return object == null || object.isEmpty();
    }

    /**
     * Load the given class name.
     *
     * @param className Name of the class to be loaded.
     * @return Class if located.
     * @see #loadClass(String, boolean)
     */
    public static <T> Class<T> loadClass(String className) {
        logger.entry(className);
        return logger.exit(Utils.<T>loadClass(className, false));
    }

    /**
     * Load the given class. In case of any error will locating or loading class, if {@code ignoreError} is true,
     * no exception is thrown and null is returned.
     *
     * @param className   Name of the class that needs to be located and loaded.
     * @param ignoreError If true, return null in case of an error, otherwise throw appropriate error.
     * @return Instance of class if located, null otherwise.
     */
    public static <T> Class<T> loadClass(String className, boolean ignoreError) {
        logger.entry(className);
        Class<T> mappedClass = null;
        if (!isEmpty(className)) {
            try {
                mappedClass = (Class<T>) Class.forName(className);
            } catch (ClassNotFoundException exception) {
                logger.catching(XLogger.Level.DEBUG, exception);
            }
            if (mappedClass == null) {
                ClassLoader threadClassLoader = Thread.currentThread().getContextClassLoader();
                if (threadClassLoader != null) {
                    try {
                        mappedClass = (Class<T>) threadClassLoader.loadClass(className);
                    } catch (ClassNotFoundException exception) {
                        logger.catching(XLogger.Level.DEBUG, exception);
                    }
                }
            }
            if (mappedClass == null && !ignoreError) {
                throw logger.throwing(XLogger.Level.DEBUG, new InternalError("Failed to locate class "
                        + className + " using Class.forName() and Thread.currentThread().getContextClassLoader().loadClass()"));
            }
        } else if (!ignoreError) {
            throw logger.throwing(XLogger.Level.DEBUG, new NullPointerException("Can not locate class with name " + className));
        }
        return logger.exit(mappedClass);
    }

    /**
     * Load the given resource(s) and return a list of URLs pointing to the resource(s).
     *
     * @param resourceName Name of the resource to be loaded.
     * @return List of URL of the resource.
     * @see #loadResources(Class, String)
     */
    public static List<URL> loadResources(String resourceName) {
        logger.entry(resourceName);
        return logger.exit(loadResources(Utils.class, resourceName));
    }

    /**
     * Load the given resource(s) and return a list of URLs pointing to the resource(s).
     *
     * @param classContext The class in whose context resource should be loaded (associated classloader is used for loading class)
     * @param resourceName Name of the resource to be loaded.
     * @return List of URL of the resource. If null resource name is passed empty list is returned.
     */
    public static List<URL> loadResources(Class<?> classContext, String resourceName) {
        logger.entry(resourceName);
        List<URL> resourceURLs = new ArrayList<>();
        Set<String> resourceURLRepresentations = new HashSet<String>();
        if (!isEmpty(resourceName)) {
            classContext = (classContext == null) ? Utils.class : classContext;
            logger.trace("Trying to locate resource using {}.getClassLoader().getResources({})", classContext, resourceName);
            try {
                Enumeration<URL> resources = classContext.getClassLoader().getResources(resourceName);
                logger.trace("Located resources as {}", resources);
                if (resources != null) {
                    while (resources.hasMoreElements()) {
                        URL resourceURLValue = resources.nextElement();
                        resourceURLRepresentations.add(resourceURLValue.toExternalForm());
                        logger.debug("Adding resource as {}", resourceURLValue);
                        resourceURLs.add(resourceURLValue);
                    }
                }
            } catch (IOException exception) {
                logger.catching(exception);
            }
            logger.debug("Located resources as {}", resourceURLs);
            ClassLoader threadClassLoader = Thread.currentThread().getContextClassLoader();
            if (threadClassLoader != null) {
                try {
                    logger.debug("Trying to locate resource using {}.getResource({})", threadClassLoader, resourceName);
                    Enumeration<URL> resources = threadClassLoader.getResources(resourceName);
                    logger.debug("Located resource as {}", resources);
                    if (resources != null) {
                        while (resources.hasMoreElements()) {
                            URL resourceURLValue = resources.nextElement();
                            if (!resourceURLRepresentations.contains(resourceURLValue.toExternalForm())) {
                                logger.debug("Adding resource as {}", resourceURLValue);
                                resourceURLs.add(resourceURLValue);
                            } else {
                                logger.trace("Skipping resource {} since it was already loaded.", resourceURLValue);
                            }
                        }
                    }
                } catch (IOException exception) {
                    logger.catching(exception);
                }
            } else {
                logger.debug("No Thread class loader could be located to load the resource");
            }
            logger.debug("Located resources as {}", resourceURLs);
            File file = new File(resourceName);
            if (file.exists() && file.isFile() && file.canRead()) {
                logger.debug("Located resource as file {}", file);
                try {
                    URI fileURI = file.toURI();
                    URL fileURL = fileURI.toURL();
                    logger.debug("Adding resource as {}", fileURL);
                    resourceURLs.add(fileURI.toURL());
                } catch (Exception exception) {
                    logger.debug("Skipping file location of resource since it could not be translated to URI.", exception);
                }
            } else {
                logger.debug("Resource does not refer to a valid file location or it may not be readable.");
            }
        } else {
            logger.debug("Resource name {} is null or empty", resourceName);
        }
        return logger.exit(resourceURLs);
    }

    /**
     * Load the given resource and return a URL pointing to the resource.
     *
     * @param resourceName Name of the resource to be loaded.
     * @return URL of the resource.
     */
    public static URL loadResource(String resourceName) {
        logger.entry(resourceName);
        List<URL> resources = loadResources(resourceName);
        if (resources != null && resources.size() > 0) {
            return logger.exit(resources.get(0));
        } else {
            logger.trace("No resource could be loaded.");
        }
        return logger.exit(null);
    }

    /**
     * Creates a deep clone of the input.
     *
     * @param input The object to be cloned.
     * @param <T>   Data type of the object being cloned.
     * @return A new instance of T class with same data as input.
     * @throws UnsupportedOperationException In case of any error.
     */
    public static <T> T cloneObject(T input) {
        logger.entry(input);
        if (input.getClass().isPrimitive() || input.getClass().isEnum())
            return input;
        byte[] readObject = null;
        try (ByteArrayOutputStream objectCollector = new ByteArrayOutputStream();
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(objectCollector)) {
            objectOutputStream.writeObject(input);
            readObject = objectCollector.toByteArray();
        } catch (Exception exception) {
            throw logger.throwing(new UnsupportedOperationException("Failed to clone input " + input, exception));
        }
        try (ByteArrayInputStream objectProvider = new ByteArrayInputStream(readObject);
             ObjectInputStream objectInputStream = new ObjectInputStream(objectProvider)) {
            Object result = objectInputStream.readObject();
            return (T) logger.exit(result);
        } catch (Exception exception) {
            throw logger.throwing(new UnsupportedOperationException("Failed to clone input " + input, exception));
        }
    }

    /**
     * Merges the values from the newMap to the original map. In case both the values are map, it merges the two maps.
     *
     * @param original Original map to which values must be added
     * @param newMap   Input map from which values to be added to original map.
     * @return Original map with merged values.
     */
    public static Map deepMerge(Map original, Map newMap) {
        logger.entry(original, newMap);
        if (original == null)
            return logger.exit(newMap);
        if (newMap == null)
            return logger.exit(original);
        for (Object key : newMap.keySet()) {
            if (newMap.get(key) instanceof Map && original.get(key) instanceof Map) {
                logger.trace("Merging two maps for key {}", key);
                Map originalChild = (Map) original.get(key);
                Map newChild = (Map) newMap.get(key);
                original.put(key, deepMerge(originalChild, newChild));
            } else if (newMap.get(key) instanceof List && original.get(key) instanceof List) {
                logger.trace("Merging two lists for key {}", key);
                List<?> output = new ArrayList<>();
                output.addAll((List) newMap.get(key));
                output.addAll((List) original.get(key));
                logger.trace("New value {}", output);
                original.put(key, output);
            } else {
                logger.trace("Replacing original value with new value for key {}", key);
                original.put(key, newMap.get(key));
            }
        }
        return logger.exit(original);
    }


    /**
     * Returns Enum type for given value if available, null otherwise. This utility function is primarily to catch
     * exception triggered by {@code Enum.valueOf} method
     *
     * @param enumType Enum type that needs to be created.
     * @param value    String value of enum.
     * @param <T>      Type of enum
     * @return Instance of enum that matches given value if located, null otherwise.
     */
    public static <T extends Enum<T>> T getEnumValueOf(Class<T> enumType, String value) {
        try {
            T transformedValue = Enum.valueOf(enumType, value);
            return transformedValue;
        } catch (IllegalArgumentException exception) {
            logger.catching(XLogger.Level.TRACE, exception);
            return null;
        }
    }

    /**
     * Simple method to close an AutoCloseable class and catch any error triggered.
     *
     * @param closeable Object on which {@code close} must be called.
     * @param <T>       Data type of input
     * @return null.
     */
    public static <T extends AutoCloseable> T close(T closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception exception) {
                logger.catching(exception);
            }
        }
        return null;
    }

    /**
     * Returns whether a and b objects are equal
     *
     * @param a input
     * @param b output
     * @return true if a equals b, false otherwise.
     */
    public static boolean equals(Object a, Object b) {
        if (a == b)
            return true;
        if (a != null && b != null)
            return a.equals(b);
        else
            return false;
    }

    /**
     * Casts the given object to specified class if possible, returns null otherwise. This is a method that ensures that
     * ClassCastExceptions are not triggered.
     *
     * @param value      The object instance that needs to be cast
     * @param valueClass Class to which input must be cast.
     * @param <T>        Data type of result
     * @return instance of the object cast to specified value if possible, null otherwise.
     */
    public static <T> T castOrReturnNull(Object value, Class<T> valueClass) {
        if (valueClass != null && value != null && valueClass.isAssignableFrom(value.getClass())) {
            return valueClass.cast(value);
        } else {
            return null;
        }
    }

    /**
     * Extracts the value between the given prefix and suffix in the given input. In case any of the parameter is null
     * null is returned.
     *
     * @param input  The input that must be parsed.
     * @param prefix The start of the input string.
     * @param suffix
     * @return Content of string between prefix and suffix if available, null otherwise.
     */
    public static String extractValue(String input, String prefix, String suffix) {
        logger.entry(input, prefix, suffix);
        if (input == null || prefix == null || suffix == null) {
            logger.debug("Input {}, prefix {} or suffix {} was null. Can not process further", input, prefix, suffix);
            return logger.exit(null);
        }
        int keywordEndLocation = input.indexOf(suffix);
        if (keywordEndLocation != -1) {
            int attributeNameStartLocation = prefix.length();
            if (keywordEndLocation - attributeNameStartLocation > 0) {
                return logger.exit(input.substring(attributeNameStartLocation, keywordEndLocation));
            } else {
                logger.debug("Even though the prefix {} and suffix {} were located in {}, there is no attribute name provided. Skipping transformation.", prefix, suffix, input);
            }
        } else {
            logger.trace("Even though the keyword {} was located the corresponding suffix {} was not located. Skipping {} transformation", prefix, suffix, input);
        }
        return logger.exit(null);
    }

    /**
     * Returns all the classes and interfaces present in given class's hierarchy.
     *
     * @param classToProcess The class for which classes and interfaces need to be extracted.
     * @return Set of classes associated with given class. If no class is passed, empty set is returned.
     */
    public static Set<Class<?>> getAssociatedClasses(Class<?> classToProcess) {
        if (classToProcess == null)
            return new HashSet<>();
        Set<Class<?>> associatedClasses = new HashSet<>();
        associatedClasses.add(classToProcess);
        do {
            Class<?>[] interfaces = classToProcess.getInterfaces();
            if (interfaces != null && interfaces.length > 0) {
                for (Class<?> interfaceItem : interfaces) {
                    associatedClasses.addAll(getAssociatedClasses(interfaceItem));
                }
            }
            Class<?> superClass = classToProcess.getSuperclass();
            if (superClass == null) // Break point for call for interfaces.
                break;
            associatedClasses.add(superClass);
            classToProcess = superClass;
        } while (!Object.class.equals(classToProcess));
        return associatedClasses;
    }

    /**
     * Returns a list of implementation of given class using the standard Java Service Loader model.
     *
     * @param serviceClass Class implementation to load
     * @param <T>          Class implementation to load
     * @return List of instances of class if available. If no implementation could be located, empty list will be returned.
     * @see ServiceLoader#load(Class)
     */
    public static <T> List<T> loadServices(Class<T> serviceClass) {
        logger.entry(serviceClass);
        List<T> services = new ArrayList<>();
        if (serviceClass != null) {
            ServiceLoader<T> serviceProvider = ServiceLoader.load(serviceClass);
            logger.debug("Ensuring service loader {} is not null", serviceProvider);
            if (serviceProvider != null) {
                Iterator<T> iterator = serviceProvider.iterator();
                while (iterator.hasNext()) {
                    T service = iterator.next();
                    if (service != null) {
                        logger.debug("Located service {}", service);
                        services.add(service);
                    }
                }
            }
        }
        return logger.exit(services);
    }
}
