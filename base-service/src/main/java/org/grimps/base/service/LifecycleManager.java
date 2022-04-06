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

package org.grimps.base.service;

import org.grimps.base.BaseException;
import org.grimps.base.InternalErrorException;
import org.grimps.base.config.Configurable;
import org.grimps.base.config.Configuration;
import org.grimps.base.utils.Utils;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility Class that implements the lifecycle of service and provide utility methods to initialize and destroy services
 */
public class LifecycleManager {
    public static final String CONFIG_ATTRIBUTE_SERVICE_SEQUENCE = "service-init-sequence";
    private static final XLogger logger = XLoggerFactory.getXLogger(LifecycleManager.class);

    /**
     * Load the class for given name and run it through the initialization process.
     *
     * @param className Name of the class to initialize
     * @param <T>       Class of service
     * @return Initialized instance of class if successful, otherwise an exception may be thrown.
     * @see Utils#loadClass(String)
     * @see #createService(Class)
     */
    public static <T> T createService(String className) {
        logger.entry(className);
        return (T) logger.exit(LifecycleManager.<T>createService(Utils.<T>loadClass(className)));
    }

    /**
     * Load the class for given name and run it through the initialization process using the given configuration.
     *
     * @param className     Name of the class to initialize
     * @param configuration Configuration to be used to initialize
     * @param <T>           Class of service
     * @return Initialized instance of class if successful, otherwise an exception may be thrown.
     * @see Utils#loadClass(String)
     * @see #createService(Class, Configuration)
     */
    public static <T> T createService(String className, Configuration configuration) {
        logger.entry(className);
        return logger.exit(createService(Utils.<T>loadClass(className), configuration));
    }

    /**
     * Create an instance of the given class and return it after initialization.
     *
     * @param serviceClass Class instance of service to be initialized.
     * @param <T>          Name of class to initialize
     * @return Initialized instance of class.
     * @see #createService(Class, Configuration)
     */
    public static <T> T createService(Class<T> serviceClass) {
        logger.entry(serviceClass);
        return logger.exit(createService(serviceClass, null));
    }

    /**
     * Creates an instance of given class and initializes it with specified configuration.
     *
     * @param serviceClass         Class to be initialized.
     * @param serviceConfiguration Configuration to be used for initialization.
     * @param <T>                  Name of class to initialize
     * @return Initialized instance of class.
     * @throws NullPointerException   In case provided service class is null
     * @throws InternalErrorException In case a new instance of given class could not be created.
     * @see #createService(Object, Configuration)
     * @see Class#newInstance()
     */
    public static <T> T createService(Class<T> serviceClass, Configuration serviceConfiguration) {
        //TODO: careful about configuration containing sensitive data.
        logger.entry(serviceClass, serviceConfiguration);
        T serviceInstance = null;
        if (serviceClass == null) {
            throw logger.throwing(XLogger.Level.WARN, new NullPointerException("Can not initialize a service using class null"));
        }
        try {
            logger.debug("Trying to create a new instance of {} using default constructor", serviceClass);
            serviceInstance = serviceClass.newInstance();
            logger.debug("Created a new instance {}", serviceInstance);
        } catch (Exception exception) {
            throw logger.throwing(XLogger.Level.WARN, new InternalErrorException("Failed to create a new instance of service class " + serviceClass, exception));
        }
        if (serviceInstance == null)
            throw logger.throwing(XLogger.Level.WARN, new InternalErrorException("Failed to create a new instance of " + serviceClass));
        return logger.exit(createService(serviceInstance, serviceConfiguration));
    }

    /**
     * Initializes the given instance of service. The initialization process involves
     * <ol>
     * <li>In case the service instance implements {@link Configurable} and a not null configuration was passed,
     * {@link Configurable#configure(Configuration)} is called with the configuration passed.</li>
     * <li>If the service instance implements {@link Lifecycle}, {@link Lifecycle#initialize()} is called on the given instance
     * of service.</li>
     * </ol>
     * In case the initialization fails (throws an exception), the service is {@link Lifecycle#destroy() destroyed} before
     * throwing an error.
     *
     * @param serviceInstance      Instance of service to be initialized.
     * @param serviceConfiguration Configuration to use for {@link Configurable configuration}
     * @param <T>                  Name of class being instantiated and initialized.
     * @return Initialized instance of service.
     * @throws NullPointerException   In case no service instance was provided for creation.
     * @throws InternalErrorException In case the service could not be configured or initialized.
     */
    public static <T> T createService(T serviceInstance, Configuration serviceConfiguration) {
        logger.entry(serviceInstance, serviceConfiguration);
        if (serviceInstance == null)
            throw logger.throwing(XLogger.Level.WARN, new NullPointerException("No service instance was provided for creation"));
        if (serviceInstance instanceof Configurable && serviceConfiguration != null) {
            logger.debug("Service needs to be configured");
            try {
                logger.debug("Trying to configure service");
                ((Configurable) serviceInstance).configure(serviceConfiguration);
                logger.debug("Configured service");
            } catch (RuntimeException exception) {
                // TODO: configuration being printed out. Need to review this in case a toString prints all org.grimps.service.config data.
                throw logger.throwing(XLogger.Level.WARN, new InternalErrorException("Failed to configure service " + serviceInstance + " using configuration " + serviceConfiguration + ".", exception));
            }
        } else {
            logger.debug("Service will not be configured since either it is not an instanceof Configurable or provided service configuration {} is null", serviceConfiguration);
        }
        if (serviceInstance instanceof Lifecycle) {
            logger.debug("Service needs to be initialized");
            try {
                ((Lifecycle) serviceInstance).initialize();
                logger.debug("Initialized service");
            } catch (BaseException exception) {
                throw exception;
            } catch (RuntimeException exception) {
                try {
                    logger.debug("Trying to destroy instance after initialization failed.");
                    ((Lifecycle) serviceInstance).destroy();
                    logger.debug("Destroyed instance after initialization failed.");
                } catch (Exception destroyException) {
                    logger.catching(destroyException);
                }
                throw logger.throwing(XLogger.Level.WARN, new InternalErrorException("Failed to initialize service " + serviceInstance + ".", exception));
            }
        }
        return logger.exit(serviceInstance);
    }

    /**
     * Destroys the given service and does not suppress any exception that occurs during the process.
     *
     * @param service Service instance to destroy
     * @param <T>     Name of class of service.
     * @see #destroyService(T, boolean)
     */
    public static <T> void destroyService(T service) {
        logger.entry(service);
        destroyService(service, false);
        logger.exit();
    }

    /**
     * Destroys the given service instance by invoking {@link Lifecycle#destroy()} if the service implements {@link Lifecycle}
     * interface. Depending on whether suppressAllException is passed as either false or true,
     * throws an exception or just logs the error respectively during the process.
     *
     * @param service              Service instance to destroy
     * @param suppressAllException True to just log exception, false to throw the error.
     * @param <T>                  Name of class of service.
     * @throws InternalErrorException incase {@link Lifecycle#destroy()} throws ane error and exception suppression is false.
     * @throws NullPointerException   in case no service instance was passed and supressAllException is false.
     */
    public static <T> void destroyService(T service, boolean suppressAllException) {
        logger.entry(service);
        if (service != null) {
            if (service instanceof Lifecycle) {
                try {
                    logger.debug("Trying to destroy service {}", service);
                    ((Lifecycle) service).destroy();
                    logger.debug("Destroyed service");
                } catch (RuntimeException exception) {
                    logger.catching(XLogger.Level.WARN, exception);
                    if (!suppressAllException)
                        throw logger.throwing(new InternalErrorException("Failed to destroy service " + service, exception));
                }
            }
        } else {
            if (!suppressAllException)
                throw logger.throwing(XLogger.Level.WARN, new NullPointerException("No service was provided to be destroyed"));
        }
        logger.exit();
    }

    /**
     * Locates implementation of given serviceClass and create a service from the same using provided configuration.
     * <p/>
     * In case the provided Configuration contains {@link #CONFIG_ATTRIBUTE_SERVICE_SEQUENCE} attribute with name of
     * given serviceClass and a list of class name, the same is used to define the sequence in which the services should
     * be created.
     * <p/>
     * In case of any error during initialization of a service, the method with throws the same exception with partial
     * set of service initialized.
     *
     * @param serviceClass           Class of which implementation needs to be located, initialized and returned.
     * @param configMgrConfiguration Configuration to be used for loading service.
     * @param <T>                    Name of service class
     * @return List of initialized service instance that implement the given serviceClass. In case no loaded service could be located
     * empty list will be returned.
     * @see Utils#loadServices(Class)
     * @see LifecycleManager#createService(Object, Configuration)
     */
    public static <T> List<T> loadServices(Class<T> serviceClass, Configuration configMgrConfiguration) {
        logger.entry(serviceClass, configMgrConfiguration);
        List<T> loadedServices = Utils.loadServices(serviceClass);
        if (loadedServices != null) {
            if (configMgrConfiguration != null && configMgrConfiguration.containsProperty(CONFIG_ATTRIBUTE_SERVICE_SEQUENCE)) {
                Configuration serviceInitSequenceConfiguration = configMgrConfiguration.subset(CONFIG_ATTRIBUTE_SERVICE_SEQUENCE);
                if (serviceInitSequenceConfiguration != null && serviceClass != null) {
                    String serviceClassName = serviceClass.getName();
                    logger.trace("Processing service sequence for {}", serviceClassName);
                    if (serviceInitSequenceConfiguration.containsProperty(serviceClassName)) {
                        List<String> initializationSequence = serviceInitSequenceConfiguration.getProperty(serviceClassName, List.class);
                        logger.trace("Located service sequence as {}", initializationSequence);
                        if (initializationSequence != null && !initializationSequence.isEmpty()) {
                            List<T> revisedLoadedServices = new ArrayList<>();
                            Set<T> unprocessedLoadedServices = new HashSet<>(loadedServices);
                            for (String initializeService : initializationSequence) {
                                logger.trace("Processing initialization service item {}", initializeService);
                                T locatedLoadedService = null;
                                for (T loadedService : loadedServices) {
                                    logger.trace("Checking whether service instance {} matches item being processed", loadedService);
                                    if (loadedService != null && loadedService.getClass().getName().equalsIgnoreCase(initializeService)) {
                                        logger.debug("Located service instance matching initialization service item being processed.");
                                        locatedLoadedService = loadedService;
                                        break;
                                    }
                                }
                                if (locatedLoadedService != null) {
                                    revisedLoadedServices.add(locatedLoadedService);
                                    unprocessedLoadedServices.remove(locatedLoadedService);
                                } else {
                                    logger.debug("No service instance matching initialization service item {} could be located.", initializeService);
                                }
                                logger.trace("Processed initialization service.");
                            }
                            logger.debug("Adding the remaining initialization items {} to revised loaded list {}", unprocessedLoadedServices, revisedLoadedServices);
                            revisedLoadedServices.addAll(unprocessedLoadedServices);
                            logger.debug("Using the revised service initialization sequence.");
                            loadedServices = revisedLoadedServices;
                        }
                    } else {
                        logger.debug("No service initialization sequence has been provided for {}", serviceClassName);
                    }
                }
            } else {
                logger.debug("No service initialization sequence configuration is available. Default random initialization sequence will be performed.");
            }
            logger.debug("Loaded service being processed {}", loadedServices);
            for (T loadedService : loadedServices) {
                logger.trace("Processing loaded service {}", loadedService);
                if (loadedService != null) {
                    logger.debug("Creating and initializing service {} using configuration {}", loadedService, configMgrConfiguration);
                    LifecycleManager.createService(loadedService, configMgrConfiguration);
                }
            }
        } else {
            logger.debug("No services could be located for initialization.");
        }
        return logger.exit(loadedServices);
    }

}
