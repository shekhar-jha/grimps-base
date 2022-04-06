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

package org.grimps.service.manager;

import org.grimps.base.ValidationException;
import org.grimps.base.config.Configuration;
import org.grimps.base.service.*;
import org.grimps.base.utils.Utils;
import org.grimps.service.config.ConfigurationService;
import org.grimps.service.config.ConfigurationUtils;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import java.net.URI;
import java.util.*;

public class ServiceManager implements Lifecycle, ContextManager {

    public static final String SERVICE_MANAGER_SERVICE_ID = "ServiceManager";
    public static final String SERVICES_CONFIG_ID = "services";
    private static final XLogger logger = XLoggerFactory.getXLogger(ServiceManager.class);
    private URI configurationURI;
    private ConfigurationService configurationService;
    private List<Object> services = new ArrayList<>();
    private Map<Class<?>, Object> classServices = new HashMap<>();
    private Map<Feature, ServiceFactory> serviceFactories = new HashMap<>();
    private Map<String, Object> nameServices = new HashMap<>();
    private Map<ServiceFactory.ServiceDetail, Object> serviceDetailBasedServices = new HashMap<>();
    private Stack<Object> contextStack = new Stack<>();
    private boolean initialized;

    public ServiceManager() {
        logger.entry();
        logger.exit();
    }

    public void configure(URI configurationLocation) {
        if (initialized)
            throw logger.throwing(new ServiceException(ServiceException.ServiceErrorCode.already_initialized,
                    "The service manager is already initialized. Please destroy the service before re-configuring service."));
        this.configurationURI = configurationLocation;
    }

    @Override
    public void initialize() {
        logger.entry();
        configurationService = new ConfigurationService();
        if (configurationURI != null) {
            logger.trace("Configuring configuration service using {}", configurationURI);
            configurationService.configure(configurationURI);
        }
        logger.trace("Initializing configuration service {}", configurationService);
        configurationService.initialize();
        classServices.put(ConfigurationService.class, configurationService);
        Configuration serviceManagerConfiguration = ConfigurationUtils.verifyConfiguration(configurationService.getConfiguration(SERVICE_MANAGER_SERVICE_ID),
                SERVICE_MANAGER_SERVICE_ID);
        Configuration servicesConfiguration = ConfigurationUtils.verifyConfiguration(serviceManagerConfiguration.subset(SERVICES_CONFIG_ID), SERVICE_MANAGER_SERVICE_ID, SERVICES_CONFIG_ID);
        List<ValidationException.ValidationError> validationErrors = new ArrayList<>();
        logger.debug("Extracting service definitions from service configuration");
        Map<String, ServiceDefinition> serviceDefinitions = new HashMap<>();
        for (String serviceIdentifier : servicesConfiguration.getPropertyNames()) {
            Configuration serviceDefinitionConfiguration = ConfigurationUtils.verifyConfiguration(servicesConfiguration.subset(serviceIdentifier),
                    SERVICE_MANAGER_SERVICE_ID, SERVICES_CONFIG_ID, serviceIdentifier);
            if (serviceDefinitionConfiguration.getProperty("enabled", true, Boolean.class)) {
                ServiceDefinition serviceDefinitionObject = extractServiceDefinition(serviceIdentifier, serviceDefinitionConfiguration);
                logger.trace("Generated service definition {}", serviceDefinitionObject);
                if (serviceDefinitionObject != null) {
                    serviceDefinitions.put(serviceIdentifier, serviceDefinitionObject);
                } else {
                    validationErrors.add(new ValidationException.ValidationError("serviceDefinition", ValidationException.VALIDATION_ERROR_CODE.INVALID_TYPE, "Failed to extract service definition for " + serviceIdentifier + " from service configuration."));
                }
            } else {
                logger.info("Skipping service initialization for service {} since it is not enabled.", serviceIdentifier);
            }
        }
        Map<String, ServiceDependencyNode> dependencies = new HashMap<>();
        for (String serviceIdentifier : serviceDefinitions.keySet()) {
            ServiceDefinition serviceDefinition = serviceDefinitions.get(serviceIdentifier);
            if (serviceDefinition != null && serviceDefinition.dependencies != null) {
                ServiceDependencyNode serviceNode = dependencies.get(serviceIdentifier);
                if (serviceNode == null) {
                    serviceNode = new ServiceDependencyNode(serviceIdentifier);
                    dependencies.put(serviceIdentifier, serviceNode);
                }
                for (Object dependencyServiceIdentifier : serviceDefinition.dependencies) {
                    if (dependencyServiceIdentifier instanceof String) {
                        if (serviceDefinitions.containsKey(dependencyServiceIdentifier)) {
                            ServiceDependencyNode dependencyNode = dependencies.get(dependencyServiceIdentifier);
                            if (dependencyNode == null) {
                                dependencyNode = new ServiceDependencyNode((String) dependencyServiceIdentifier);
                                dependencies.put((String) dependencyServiceIdentifier, dependencyNode);
                            }
                            dependencyNode.addDependedBy(serviceNode);
                            serviceNode.addDependsOn(dependencyNode);
                        } else {
                            logger.warn("The definition for dependency {} specified for service {} is not available.", dependencyServiceIdentifier, serviceIdentifier);
                            validationErrors.add(new ValidationException.ValidationError("dependency", ValidationException.VALIDATION_ERROR_CODE.REQUIRED, "The definition for dependency " + dependencyServiceIdentifier
                                    + " specified for service " + serviceIdentifier + " is not available."));
                        }
                    } else {
                        logger.warn("The dependency {} specified for service {} is not a String as expected.", dependencyServiceIdentifier, serviceIdentifier);
                        validationErrors.add(new ValidationException.ValidationError("dependency", ValidationException.VALIDATION_ERROR_CODE.INVALID_TYPE, "The dependency "
                                + dependencyServiceIdentifier + " specified for service " + serviceIdentifier + " is not a String as expected"));
                    }
                }
            }
        }
        logger.trace("Service dependencies {}", dependencies);
        List<String> dependencyInitializationSequence = new ArrayList<>();
        int numberOfItemsProcessed;
        int passes = 1;
        do {
            logger.trace("Pass : {}", passes++);
            numberOfItemsProcessed = 0;
            Set<String> dependencyNodesToBeRemoved = new HashSet<>();
            for (Map.Entry<String, ServiceDependencyNode> dependencyNodeEntry : dependencies.entrySet()) {
                logger.trace("Checking whether node {} is root", dependencyNodeEntry.getValue());
                if (dependencyNodeEntry.getValue().dependedBy.isEmpty()) {
                    logger.trace("Identified a root node {} for which all the services that depend on have been removed.", dependencyNodeEntry.getValue());
                    dependencyInitializationSequence.add(0, dependencyNodeEntry.getKey());
                    numberOfItemsProcessed++;
                    dependencyNodesToBeRemoved.add(dependencyNodeEntry.getKey());
                    for (ServiceDependencyNode childNodeEntry : dependencyNodeEntry.getValue().dependsOn) {
                        logger.trace("Trying to remove {} from dependedBy of {}", dependencyNodeEntry, childNodeEntry);
                        if (!childNodeEntry.dependedBy.remove(dependencyNodeEntry.getValue())) {
                            validationErrors.add(new ValidationException.ValidationError("dependency", ValidationException.VALIDATION_ERROR_CODE.REQUIRED, "Expected entry " + dependencyNodeEntry.getKey() + " as dependedBy of " + childNodeEntry.serviceName));
                        }
                    }
                }
            }
            for (String dependencyNodeToBeRemoved : dependencyNodesToBeRemoved) {
                dependencies.remove(dependencyNodeToBeRemoved);
            }
        } while (numberOfItemsProcessed > 0 && dependencies.size() > 0);
        if (dependencies.size() > 0) {
            validationErrors.add(new ValidationException.ValidationError("dependency", ValidationException.VALIDATION_ERROR_CODE.ERROR_CODE, "Failed to process all the dependencies. Remaining dependencies that can not be processed " + dependencies));
        }
        if (validationErrors.size() > 0) {
            logger.warn("Failed to initialize service {}", validationErrors);
            throw logger.throwing(new ValidationException("Failed to initialize services due to dependency resolution errors.").addValidationErrors(validationErrors));
        }
        logger.trace("Processed dependencies to identify initialization sequence as {}", dependencyInitializationSequence);
        {
            List<String> serviceInitializationSequence = serviceManagerConfiguration.getProperty("service-sequence", List.class);
            if (serviceInitializationSequence != null) {
                int entryIndex = 0;
                for (String serviceIdentifier : serviceInitializationSequence) {
                    if (Utils.isEmpty(serviceIdentifier) || !serviceDefinitions.containsKey(serviceIdentifier)) {
                        logger.debug("Skipping service {} from the initialization sequence since there is no associated service definition.", serviceIdentifier);
                    } else {
                        dependencyInitializationSequence.add(entryIndex++, serviceIdentifier);
                    }
                }
            }
        }
        logger.trace("Initialization sequence is {} after processing 'service-sequence'", dependencyInitializationSequence);
        for (String serviceIdentifier : serviceDefinitions.keySet()) {
            if (dependencyInitializationSequence.indexOf(serviceIdentifier) == -1) {
                logger.debug("Adding service {} to sequence since it is not part of initialization sequence", serviceIdentifier);
                dependencyInitializationSequence.add(serviceIdentifier);
            }
        }
        logger.trace("Service initialization sequence {}", dependencyInitializationSequence);
        if (dependencyInitializationSequence != null && !dependencyInitializationSequence.isEmpty()) {
            Map<String, Object> serviceInstances = new HashMap<>();
            for (String serviceDefinition : dependencyInitializationSequence) {
                logger.trace("Processing service {}", serviceDefinition);
                try {
                    if (!serviceInstances.containsKey(serviceDefinition)) {
                        ServiceDefinition serviceDefinitionObject = serviceDefinitions.get(serviceDefinition);
                        if (serviceDefinitionObject != null) {
                            Object serviceInstance = registerService(serviceDefinitionObject);
                            logger.trace("Registered service {}", serviceInstance);
                            if (serviceInstance != null) {
                                serviceInstances.put(serviceDefinition, serviceInstance);
                            } else {
                                validationErrors.add(new ValidationException.ValidationError("service", ValidationException.VALIDATION_ERROR_CODE.REQUIRED, "No service instance was returned for service " + serviceDefinition + "."));
                            }
                        } else {
                            validationErrors.add(new ValidationException.ValidationError("service", ValidationException.VALIDATION_ERROR_CODE.REQUIRED, "No service definition could be extracted for " + serviceDefinition + " service. Skipping service registration."));
                        }
                    } else {
                        logger.debug("Service {} has already been registered. Skipping this item", serviceDefinition);
                    }
                } catch (Exception exception) {
                    logger.catching(XLogger.Level.WARN, exception);
                    validationErrors.add(new ValidationException.ValidationError("service", ValidationException.VALIDATION_ERROR_CODE.INVALID_VALUE, "Failed to initialize and register service " + serviceDefinition + " due to internal error. Error: " + exception));
                }
                logger.trace("Processed service.");
            }
        } else {
            logger.info("No sequence of service initialization could be determined. Please specify 'dependencies' for service or 'service-sequence' with sequence in which service must be initialized.");
        }
        List<ServiceFactory> serviceFactories = Utils.loadServices(ServiceFactory.class);
        logger.trace("Service factories {}", serviceFactories);
        if (serviceFactories != null) {
            for (ServiceFactory serviceFactory : serviceFactories) {
                try {
                    logger.trace("Processing service factory {}", serviceFactory);
                    if (serviceFactory != null) {
                        registerServiceFactory(serviceFactory);
                    } else {
                        logger.trace("Skipping service factory registration since it is null");
                    }
                    logger.trace("Processed service factory");
                } catch (Exception exception) {
                    logger.catching(XLogger.Level.WARN, exception);
                    validationErrors.add(new ValidationException.ValidationError("serviceFactory", ValidationException.VALIDATION_ERROR_CODE.INVALID_VALUE, "Failed to initialize and register service factory " + serviceFactory + " due to internal error. Error: " + exception));
                }
            }
        } else {
            logger.debug("No service factory could be loaded (LifecycleManager.loadServices).");
        }
        if (validationErrors.size() > 0) {
            this.destroy();
            logger.warn("Failed to initialize identified services due to errors {}", validationErrors);
            throw logger.throwing(new ValidationException("Failed to initialize identified services " + dependencyInitializationSequence + " in given sequence").addValidationErrors(validationErrors));
        }
        initialized = true;
        logger.exit();
    }

    @Override
    public void enable() {
        logger.entry();
        if (Context.getContext() == null) {
            ServiceManagerContext context = new ServiceManagerContext();
            context.serviceManager = this;
            context.enable();
        } else {
            logger.trace("Pushing on the context stack.");
            contextStack.push(new Object());
        }
        logger.exit();
    }

    @Override
    public void disable() {
        logger.entry();
        if (contextStack.isEmpty()) {
            Context context = Context.getContext();
            if (context != null) {
                if (context instanceof ServiceManagerContext) {
                    ((ServiceManagerContext) context).disable();
                } else {
                    logger.warn("Encountered context {} which is not an instance of ServiceManagerContext", context);
                }
            } else {
                logger.debug("No context was located for disabling.");
            }
        } else {
            logger.trace("Popping the context stack.");
            contextStack.pop();
        }
        logger.exit();
    }

    public ServiceDefinition extractServiceDefinition(String serviceIdentifier, Configuration serviceDefinitionConfiguration) {
        logger.entry(serviceIdentifier, serviceDefinitionConfiguration);
        return logger.exit(extractServiceDefinition(serviceIdentifier, serviceDefinitionConfiguration, true));
    }

    public ServiceDefinition extractServiceDefinition(String serviceIdentifier, Configuration serviceDefinitionConfiguration, boolean suppressError) {
        logger.entry(serviceDefinitionConfiguration);
        ServiceDefinition serviceDefinitionObject = new ServiceDefinition(serviceIdentifier);
        List<ValidationException.ValidationError> validationErrors = new ArrayList<>();
        String singletonType = serviceDefinitionConfiguration.getProperty("singleton", SINGLETON_LEVEL.VERSION.name(), String.class);
        logger.trace("Service singleton strategy read as {} using attribute {}", singletonType, "singleton");
        String singletonErrorHandling = serviceDefinitionConfiguration.getProperty("handle-multiple-instance", SINGLETON_STRATEGY_ERROR_HANDLING.REPLACE.name(), String.class);
        logger.trace("Service singleton error handling read as {} using attribute {}", singletonErrorHandling, "handle-multiple-instance");
        SINGLETON_LEVEL singletonLevel = Utils.getEnumValueOf(SINGLETON_LEVEL.class, singletonType);
        logger.trace("Service singleton strategy mapped to {}", singletonLevel);
        if (singletonLevel == null) {
            validationErrors.add(new ValidationException.ValidationError("singleton", ValidationException.VALIDATION_ERROR_CODE.INVALID_VALUE, "The singleton strategy specified using singleton as " + singletonType + " is incorrect."));
        }
        SINGLETON_STRATEGY_ERROR_HANDLING errorHandling = Utils.getEnumValueOf(SINGLETON_STRATEGY_ERROR_HANDLING.class, singletonErrorHandling);
        logger.trace("Service singleton errorhandling mapped to {}", errorHandling);
        if (errorHandling == null) {
            validationErrors.add(new ValidationException.ValidationError("handle-multiple-instance", ValidationException.VALIDATION_ERROR_CODE.INVALID_VALUE, "The error handling approach in case multiple singleton are encountered as specified using " + singletonErrorHandling + " is incorrect."));
        }
        serviceDefinitionObject.singletonStrategy = new SINGLETON_APPROACH(singletonLevel, errorHandling);
        logger.trace("Setting singleton strategy as {}", serviceDefinitionObject.singletonStrategy);
        String className = serviceDefinitionConfiguration.getProperty("className");
        logger.trace("Service definition class was read as {} using attribute 'className'");
        if (Utils.isEmpty(className)) {
            validationErrors.add(new ValidationException.ValidationError("className", ValidationException.VALIDATION_ERROR_CODE.REQUIRED, "The name of class of service that should be instantiated."));
        } else {
            serviceDefinitionObject.serviceClass = Utils.loadClass(className, true);
            logger.trace("Service definition class was loaded as {}", serviceDefinitionObject.serviceClass);
            if (serviceDefinitionObject.serviceClass == null) {
                validationErrors.add(new ValidationException.ValidationError("className", ValidationException.VALIDATION_ERROR_CODE.INVALID_VALUE, "The class name " + className + " could not be located."));
            }
        }
        ServiceFactory.ServiceDetail serviceDetail = null;
        Configuration serviceVersionDetailsConfiguration = serviceDefinitionConfiguration.subset("lookup-details");
        if (serviceVersionDetailsConfiguration != null) {
            String name = serviceVersionDetailsConfiguration.getProperty("name", String.class);
            String version = serviceVersionDetailsConfiguration.getProperty("version", String.class);
            if (Utils.isEmpty(name))
                validationErrors.add(new ValidationException.ValidationError("name", ValidationException.VALIDATION_ERROR_CODE.INVALID_VALUE, "The service version detail does not contain name."));
            if (Utils.isEmpty(version))
                validationErrors.add(new ValidationException.ValidationError("version", ValidationException.VALIDATION_ERROR_CODE.INVALID_VALUE, "The service version detail does not contain version."));
            if (!Utils.isEmpty(name) && !Utils.isEmpty(version)) {
                serviceDetail = new ServiceFactory.ServiceDetail(name, version);
            }
        }
        serviceDefinitionObject.serviceDetail = serviceDetail;
        Configuration serviceConfiguration;
        String serviceConfigurationIdentifier = serviceDefinitionConfiguration.getProperty("configuration-name", String.class);
        logger.trace("Service definition configuration identifier was loaded as {} using attribute 'configuration-name'", serviceConfigurationIdentifier);
        if (Utils.isEmpty(serviceConfigurationIdentifier) || (serviceConfiguration = configurationService.getConfiguration(serviceConfigurationIdentifier)) == null) {
            logger.trace("Since configuration for given configuration-name could not be located, trying to locate configuration using service identifier {}", serviceIdentifier);
            if ((serviceConfiguration = configurationService.getConfiguration(serviceIdentifier)) == null) {
                validationErrors.add(new ValidationException.ValidationError("configuration-name", ValidationException.VALIDATION_ERROR_CODE.REQUIRED, "The specified configuration-name "
                        + serviceConfigurationIdentifier + " can not be resolved to a configuration"));
            } else {
                logger.debug("Service configuration for {} was read using service definition identifier since no configuration name was located or {} could not be resolved.", serviceIdentifier, serviceConfigurationIdentifier);
            }
        }
        if (serviceConfiguration == null) {
            serviceConfiguration = serviceDefinitionConfiguration.subset("configuration");
            logger.trace("Looked up service configuration as part of definition as {}", serviceConfiguration);
        }
        logger.trace("Service configuration {}", serviceConfiguration);
        serviceDefinitionObject.serviceConfiguration = serviceConfiguration;
        if (!validationErrors.isEmpty()) {
            logger.debug("Validation errors {}", validationErrors);
            if (!suppressError) {
                throw logger.throwing(new ValidationException("Service " + serviceIdentifier + " could not be processed and initialized").addValidationErrors(validationErrors));
            }
        }
        //serviceDefinitionObject.initializationScript = serviceDefinitionConfiguration.getProperty("init-script", String.class);
        //logger.trace("Service initialization script {}", serviceDefinitionObject.initializationScript);
        serviceDefinitionObject.dependencies = serviceDefinitionConfiguration.getProperty("dependencies", List.class);
        logger.trace("Service definition");
        return logger.exit(serviceDefinitionObject);
    }

    public boolean canClassBeRegistered(Class serviceClass, SINGLETON_APPROACH approach, ServiceDefinition serviceDefinition) {
        logger.entry(serviceClass, approach, serviceDefinition);
        if (serviceClass != null && approach != null && approach.singletonLevel == SINGLETON_LEVEL.CLASS) {
            if (classServices.containsKey(serviceClass)) {
                switch (approach.singletonStrategyErrorHandling) {
                    case ERROR:
                        throw logger.throwing(new ValidationException("An existing instance of service of class "
                                + serviceClass + " exists. The service " + serviceDefinition + " can not be registered."));
                    case IGNORE:
                        logger.debug("Ignoring registration of service definition {} since an existing instance of same service class already exists.", serviceDefinition);
                        return logger.exit(false);
                    case REPLACE:
                        logger.debug("Continuing the processing since existing instance {} will be overwritten by new instance to be created.", classServices.get(serviceClass));
                        return logger.exit(true);
                    default:
                        logger.debug("Unsupported error handling strategy read as {}. Skipping service registration.", approach.singletonStrategyErrorHandling);
                        return logger.exit(false);
                }
            } else {
                logger.debug("Service can be registered using class since there is no existing service associated with class.");
                return logger.exit(true);
            }
        } else {
            logger.debug("Singleton level {} of approach {} for class {} is not correct. Service will not be registered.", approach != null ? approach.singletonLevel : "null", approach, serviceClass);
            return logger.exit(false);
        }
    }

    public boolean canNameBeRegistered(String name, SINGLETON_APPROACH approach, ServiceDefinition serviceDefinition) {
        logger.entry(name, approach, serviceDefinition);
        if (!Utils.isEmpty(name) && approach != null && approach.singletonLevel == SINGLETON_LEVEL.NAME) {
            if (nameServices.containsKey(name)) {
                switch (serviceDefinition.singletonStrategy.singletonStrategyErrorHandling) {
                    case ERROR:
                        throw logger.throwing(new ValidationException("An existing instance of service for name "
                                + name + " exists. The service " + serviceDefinition + " can not be registered."));
                    case IGNORE:
                        logger.debug("The new service with definition {} will not be registered since an existing service with same name is already registered.", serviceDefinition);
                        return logger.exit(false);
                    case REPLACE:
                        logger.debug("Continuing the processing since existing instance with name {} will be overwritten by new instance to be created.", name);
                        return logger.exit(true);
                    default:
                        logger.debug("Unsupported error handling strategy read as {}. Skipping service registration.", approach.singletonStrategyErrorHandling);
                        return logger.exit(false);
                }
            } else {
                logger.trace("Service can be registered using name since there is no existing service associated with name.");
                return logger.exit(true);
            }
        } else {
            logger.debug("Singleton level {} of approach {} for name {} is not correct. Service will not be registered.", approach != null ? approach.singletonLevel : "null", approach, name);
            return logger.exit(false);
        }
    }

    public boolean canNameAndVersionBeRegistered(String name, String version, SINGLETON_APPROACH approach, ServiceDefinition serviceDefinition) {
        logger.entry(name, version, approach, serviceDefinition);
        if (!Utils.isEmpty(name) && !Utils.isEmpty(version) && approach != null && approach.singletonLevel == SINGLETON_LEVEL.VERSION) {
            ServiceFactory.ServiceDetail serviceDetail = new ServiceFactory.ServiceDetail(name, version);
            if (serviceDetailBasedServices.containsKey(serviceDetail)) {
                switch (approach.singletonStrategyErrorHandling) {
                    case ERROR:
                        throw logger.throwing(new ValidationException("An existing instance of service for name "
                                + name + " and version " + version + " exists. The service "
                                + serviceDefinition + " can not be registered."));
                    case IGNORE:
                        logger.debug("The new service with definition {} will not be registered since an existing service with same name and version is already registered.", serviceDefinition);
                        return logger.exit(false);
                    case REPLACE:
                        logger.debug("Continuing the processing since existing instance will be overwritten by new instance to be created.");
                        return logger.exit(true);
                    default:
                        logger.debug("Unsupported error handling strategy read as {}. Skipping service registration.", approach.singletonStrategyErrorHandling);
                        return logger.exit(false);
                }
            } else {
                logger.trace("Service can be registered using name and version since there is no existing service associated with name.");
                return logger.exit(true);
            }
        } else {
            logger.debug("Singleton level {} of approach {} for name {} and version {} is not correct. Service will not be registered.",
                    (approach != null ? approach.singletonLevel : "null"), approach, name, version);
            return logger.exit(false);
        }
    }

    public Configuration extractConfigurationForService(Object serviceInstance, ServiceDefinition serviceDefinition) {
        logger.entry(serviceDefinition);
        Configuration serviceConfiguration = serviceDefinition.serviceConfiguration;
        logger.trace("Configuration as part of service definition {}", serviceDefinition);
        Configuration genericServiceConfiguration = null;
        ServiceFactory.ServiceDetail serviceDetail = serviceDefinition.serviceDetail;
        if (serviceInstance instanceof ServiceFactory) {
            try {
                serviceDetail = ((ServiceFactory) serviceInstance).getServiceDetail();
                logger.trace("Service detail for service factory {}", serviceDetail);
            } catch (Exception exception) {
                logger.catching(XLogger.Level.DEBUG, exception);
            }
        }
        if (genericServiceConfiguration == null && serviceDetail != null) {
            genericServiceConfiguration = configurationService.getConfiguration(serviceDetail.toString());
            logger.trace("Retrieved service configuration using service detail {} as {}", serviceDetail, genericServiceConfiguration);
        }
        Configuration applicableConfiguration = serviceConfiguration;
        if (genericServiceConfiguration != null) {
            if (serviceConfiguration != null) {
                logger.trace("Creating composite configuration with service definition configuration as primary configuration.");
                applicableConfiguration = configurationService.getCompositeConfiguration(serviceConfiguration, genericServiceConfiguration);
            } else
                applicableConfiguration = genericServiceConfiguration;
        }
        return logger.exit(applicableConfiguration);
    }

    public <T> T createService(ServiceDefinition<T> serviceDefinition) {
        logger.entry(serviceDefinition);
        if (serviceDefinition == null)
            return logger.exit(null);
        T serviceInstance;
        if (serviceDefinition.serviceInstance != null && serviceDefinition.serviceClass != null
                && !serviceDefinition.serviceClass.isAssignableFrom(serviceDefinition.serviceInstance.getClass())) {
            throw logger.throwing(new ValidationException("serviceInstance", ValidationException.VALIDATION_ERROR_CODE.INVALID_TYPE,
                    "Service instance of class " + serviceDefinition.serviceInstance.getClass() + " can not be assigned from class "
                            + serviceDefinition.serviceClass + " which implies that service definition " + serviceDefinition + " is incorrect."));
        }
        Class<? extends T> serviceClass = serviceDefinition.serviceClass;
        if (serviceDefinition.serviceClass == null && serviceDefinition.serviceInstance != null) {
            logger.trace("Identifying class for service definition {} using instance {}", serviceDefinition, serviceDefinition.serviceInstance);
            serviceClass = (Class<T>) serviceDefinition.serviceInstance.getClass();
        }
        logger.trace("Applicable service class {}", serviceClass);
        if (serviceClass == null)
            throw logger.throwing(new ValidationException("service-class", ValidationException.VALIDATION_ERROR_CODE.REQUIRED, "The service class could not be resolved using service definition "
                    + serviceDefinition + " with class " + serviceDefinition.serviceClass + " and instance " + serviceDefinition.serviceInstance));
        serviceInstance = serviceDefinition.serviceInstance;
        try {
            enable();
            if (serviceInstance == null) {
                try {
                    serviceInstance = serviceClass.newInstance();
                } catch (Exception exception) {
                    throw logger.throwing(new ValidationException("Failed to create a new instance of class " + serviceClass
                            + " as part of service initialization using " + serviceDefinition, exception));
                }
            }
            logger.trace("Service instance {}", serviceInstance);
            if (serviceInstance == null)
                throw logger.throwing(new ValidationException("service-instance", ValidationException.VALIDATION_ERROR_CODE.REQUIRED,
                        "No service instance could be either located or created using service definition "
                                + serviceDefinition + " with class " + serviceDefinition.serviceClass + " and instance " + serviceDefinition.serviceInstance));
            Configuration applicableConfiguration = extractConfigurationForService(serviceInstance, serviceDefinition);
            logger.trace("Applicable configuration identified for service as {}", applicableConfiguration);
            LifecycleManager.createService(serviceInstance, applicableConfiguration);
            logger.trace("Initialized service.");
        } catch (Exception exception) {
            logger.catching(exception);
            LifecycleManager.destroyService(serviceInstance, true);
            serviceInstance = null;
            throw logger.throwing(new ServiceException(ServiceException.ServiceErrorCode.initialization_error, "Failed to initialize service " + serviceDefinition + ".", exception));
        } finally {
            disable();
        }
        return logger.exit(serviceInstance);
    }

    public <T> T registerService(ServiceDefinition<T> serviceDefinition) {
        logger.entry(serviceDefinition);
        T serviceInstance = createService(serviceDefinition);
        if (serviceInstance != null) {
            Class serviceClass = serviceDefinition.serviceClass;
            if (serviceClass == null)
                serviceClass = serviceInstance.getClass();
            boolean registerServiceWithClass = canClassBeRegistered(serviceClass, serviceDefinition.singletonStrategy, serviceDefinition);
            boolean registerServiceUsingName;
            boolean registerServiceUsingNameAndVersion;
            ServiceFactory.ServiceDetail serviceDetail = serviceDefinition.serviceDetail;
            logger.trace("Service detail associated with service definition {}", serviceDetail);
            if (serviceDetail == null && serviceInstance instanceof ServiceFactory) {
                serviceDetail = ((ServiceFactory) serviceInstance).getServiceDetail();
                logger.trace("Service detail using factory information is {}", serviceDetail);
            }
            if (serviceDetail != null) {
                registerServiceUsingName = canNameBeRegistered(serviceDetail.getName(), serviceDefinition.singletonStrategy, serviceDefinition);
                registerServiceUsingNameAndVersion = canNameAndVersionBeRegistered(serviceDetail.getName(), serviceDetail.getVersion(), serviceDefinition.singletonStrategy, serviceDefinition);
            } else {
                logger.debug("No registration will be performed using name (and version) for service detail since no service detail was identified.");
                registerServiceUsingName = false;
                registerServiceUsingNameAndVersion = false;
            }
            if (registerServiceWithClass) {
                logger.debug("Registering service {} with class {}.", serviceInstance, serviceClass);
                Object replacedServiceInstance = this.classServices.put(serviceClass, serviceInstance);
                if (replacedServiceInstance != null)
                    logger.debug("Replaced existing instance {} associated with class {}", replacedServiceInstance, serviceClass);
            }
            if (serviceDetail != null && registerServiceUsingName) {
                logger.debug("Registering service {} with name {}.", serviceInstance, serviceDetail.getName());
                Object replacedServiceInstance = nameServices.put(serviceDetail.getName(), serviceInstance);
                if (replacedServiceInstance != null)
                    logger.debug("Replaced existing instance {} associated with name {}", replacedServiceInstance, serviceDetail.getName());
            }
            if (registerServiceUsingNameAndVersion) {
                logger.debug("Registering service {} with name {} and version {}.", serviceInstance, serviceDetail.getName(), serviceDetail.getVersion());
                Object replacedServiceInstance = serviceDetailBasedServices.put(serviceDetail, serviceInstance);
                if (replacedServiceInstance != null)
                    logger.debug("Replaced existing instance {} associated with name {} and version {}", replacedServiceInstance, serviceDetail.getName(), serviceDetail.getVersion());
            }
            logger.debug("Registering service {}", serviceInstance);
            services.add(serviceInstance);
        } else {
            logger.debug("Service could not be initialized correctly.");
        }
        return logger.exit(serviceInstance);
    }

    public <A, T extends ServiceFactory<A>> T registerServiceFactory(T serviceFactory) {
        logger.entry(serviceFactory);
        if (serviceFactory == null)
            logger.throwing(new ValidationException("Can not register null service factory instance"));
        return logger.exit(this.<A, T>registerServiceFactory(serviceFactory.getClass().getSimpleName(), (Class<T>) serviceFactory.getClass(), serviceFactory));
    }

    public <A, T extends ServiceFactory<A>> T registerServiceFactory(final Class<T> serviceFactoryClass) {
        logger.entry(serviceFactoryClass);
        if (serviceFactoryClass == null)
            throw logger.throwing(new ValidationException("Can not register null service factory class."));
        return logger.exit(this.<A, T>registerServiceFactory(serviceFactoryClass.getSimpleName(), serviceFactoryClass, null));
    }

    public <A, T extends ServiceFactory<A>> T registerServiceFactory(final String name, final Class<? extends T> serviceFactoryClass, final T serviceInstanceObject) {
        logger.entry(name, serviceFactoryClass, serviceInstanceObject);
        T serviceInstance = registerService(new ServiceDefinition<T>(name) {{
            this.serviceClass = serviceFactoryClass;
            this.serviceInstance = serviceInstanceObject;
            this.singletonStrategy = null;
            this.serviceDetail = null;
            this.serviceConfiguration = null;
        }});
        if (serviceInstance != null) {
            Set<? extends Feature> features = serviceInstance.getSupportedFeatures();
            Class<? extends A> serviceClass = serviceInstance.getServiceClass();
            logger.trace("Service Class {} for service factory", serviceClass);
            if (features != null) {
                for (Feature feature : features) {
                    if (feature != null && feature.getFeatureClass() != null && serviceClass != null && feature.getFeatureClass().isAssignableFrom(serviceClass)) {
                        logger.debug("Registering feature specific implementation {} for class {}", serviceInstance, feature);
                        ServiceFactory replacedServiceFactory = serviceFactories.put(feature, serviceInstance);
                        if (replacedServiceFactory != null)
                            logger.debug("Replaced existing service factory {} while registering new service.", replacedServiceFactory);
                    } else {
                        logger.trace("Invalid feature {} and service class {} were encountered. Skipping it", feature, serviceClass);
                    }
                }
            } else {
                logger.trace("No feature is associated with service factory {}.", serviceInstance);
            }
        } else {
            logger.trace("Service is not an instance of service factory. No additional class registration is needed.");
        }
        return logger.exit(serviceInstance);
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    public Object getService(Configuration serviceDetails) {
        logger.entry(serviceDetails);
        if (serviceDetails != null) {
            String typeObject = serviceDetails.getProperty("type", String.class);
            if (!Utils.isEmpty(typeObject)) {
                SINGLETON_LEVEL type = SINGLETON_LEVEL.valueOf(typeObject);
                switch (type) {
                    case CLASS: {
                        String classObject = serviceDetails.getProperty("class", String.class);
                        if (classObject instanceof String) {
                            return logger.exit(getService(SINGLETON_LEVEL.CLASS, (String) classObject));
                        } else {
                            logger.debug("Failed to read class from {} as expected for a class lookup.", serviceDetails);
                        }
                        break;
                    }
                    case NAME: {
                        String nameObject = serviceDetails.getProperty("name", String.class);
                        if (!Utils.isEmpty(nameObject)) {
                            return logger.exit(getService(SINGLETON_LEVEL.NAME, nameObject));
                        } else {
                            logger.debug("Failed to read name from {} as expected for a name lookup.", serviceDetails);
                        }
                        break;
                    }
                    case VERSION:
                        String nameObject = serviceDetails.getProperty("name", String.class);
                        String versionObject = serviceDetails.getProperty("version", String.class);
                        if (!Utils.isEmpty(nameObject) && !Utils.isEmpty(versionObject)) {
                            return logger.exit(getService(SINGLETON_LEVEL.VERSION, nameObject, versionObject));
                        } else {
                            logger.debug("Failed to read name and version from {} as expected for a name/version lookup.", serviceDetails);
                        }
                        break;
                    default:
                        logger.debug("Request for service of type {} ({}) is not supported.", type, typeObject);
                        break;
                }
            } else {
                logger.debug("Type  {} of rest-api service read from {} is not supported.", typeObject, serviceDetails);
            }
        } else {
            logger.debug("Identified null service definition during registration. Skipping.");
        }
        return logger.exit(null);
    }

    public Object getService(SINGLETON_LEVEL type, String... parameters) {
        logger.entry(type, parameters);
        Object service = null;
        switch (type) {
            case CLASS: {
                if (parameters != null && parameters.length == 1) {
                    Class<?> serviceClass = Utils.loadClass(parameters[0]);
                    if (serviceClass != null)
                        service = getService(serviceClass);
                    else
                        logger.debug("Expecting a valid class (found {}) as input parameter.", serviceClass);
                } else {
                    logger.debug("Expecting a parameter with class name for service requested using {}", type);
                }
                break;
            }
            case NAME: {
                if (parameters != null && parameters.length == 1) {
                    if (!Utils.isEmpty(parameters[0]))
                        service = getService(parameters[0], Object.class);
                    else
                        logger.debug("Expecting a valid name (found {}) as input parameter.", parameters[0]);
                } else {
                    logger.debug("Expecting a parameter with name since {} type of service is being requested.", type);
                }
                break;
            }
            case VERSION: {
                if (parameters != null && parameters.length == 2) {
                    if (!Utils.isEmpty(parameters[0]) && !Utils.isEmpty(parameters[1]))
                        service = getService(parameters[0], parameters[1], Object.class);
                    else
                        logger.debug("Expecting a valid name (found {}) and version (found {}) as input parameter.", parameters[0], parameters[1]);
                } else {
                    logger.debug("Expecting a parameter with name and version since {} type of service is being requested.", type);
                }
                break;
            }
        }
        return logger.exit(service);
    }

    public <T> T getService(Class<T> serviceClass) {
        logger.entry(serviceClass);
        if (serviceClass == null)
            throw logger.throwing(XLogger.Level.DEBUG, new ValidationException("Can not return service corresponding to class null"));
        return logger.exit(serviceClass.cast(classServices.get(serviceClass)));
    }

    public <T> T getService(String name) {
        logger.entry(name);
        return logger.exit((T) getService(name, Object.class));
    }

    public <T> T getService(String name, Class<T> serviceClass) {
        logger.entry(name);
        if (Utils.isEmpty(name))
            throw logger.throwing(XLogger.Level.DEBUG, new ValidationException("Can not return service corresponding to name " + name));
        if (serviceClass == null)
            throw logger.throwing(XLogger.Level.DEBUG, new ValidationException("Can not return service if class is not specified."));
        return logger.exit(serviceClass.cast(nameServices.get(name)));
    }

    public <T> T getService(String name, String version, Class<T> serviceClass) {
        logger.entry(name, version);
        if (Utils.isEmpty(name))
            throw logger.throwing(XLogger.Level.DEBUG, new ValidationException("Can not return service corresponding to name " + name));
        if (Utils.isEmpty(version))
            throw logger.throwing(XLogger.Level.DEBUG, new ValidationException("Can not return service corresponding to version " + version));
        if (serviceClass == null)
            throw logger.throwing(XLogger.Level.DEBUG, new ValidationException("Can not return service if class is not specified."));
        return logger.exit(serviceClass.cast(serviceDetailBasedServices.get(new ServiceFactory.ServiceDetail(name, version))));
    }

    public <A, T extends ServiceFactory<A>> T getServiceFactory(Feature<A> feature, Class<T> serviceClass) {
        logger.entry(feature);
        if (feature == null)
            throw logger.throwing(new ValidationException("Can not return service factory for null feature."));
        Class<A> featureClass = feature.getFeatureClass();
        if (featureClass == null)
            throw logger.throwing(new ValidationException("Feature " + feature + " does not have any associated class."));
        ServiceFactory serviceFactory = serviceFactories.get(feature);
        if (serviceFactory != null && serviceFactory.getServiceClass() != null) {
            if (feature.getFeatureClass().isAssignableFrom(serviceFactory.getServiceClass())) {
                if (serviceClass != null && serviceClass.isAssignableFrom(serviceFactory.getClass())) {
                    return logger.exit(serviceClass.cast(serviceFactory));
                } else {
                    logger.debug("Service class {} does not seem to be not assignable from {}", serviceClass, serviceFactory.getClass());
                }
            } else {
                logger.debug("Feature class {} is not assignable from Service factory's service class {}", feature.getFeatureClass(), serviceFactory.getServiceClass());
            }
        } else {
            logger.debug("Service factory associated with {} is null or associated service class {} is null", serviceFactory, (serviceFactory == null) ? "null" : serviceFactory.getServiceClass());
        }
        return logger.exit(null);
    }

    @Override
    public void destroy() {
        logger.entry();
        initialized = false;
        ServiceManagerContext serviceManagerContext = new ServiceManagerContext();
        serviceManagerContext.serviceManager = this;
        serviceManagerContext.enable();
        for (int counter = services.size() - 1; counter >= 0; counter--) {
            LifecycleManager.destroyService(services.get(counter), true);
        }
        serviceManagerContext.disable();
        this.services.clear();
        this.nameServices.clear();
        this.classServices.clear();
        this.serviceDetailBasedServices.clear();
        this.serviceFactories.clear();
        if (configurationService != null) {
            try {
                configurationService.destroy();
            } catch (Exception exception) {
                logger.catching(exception);
            }
            configurationService = null;
        }
        logger.exit();
    }


    public enum SINGLETON_LEVEL {CLASS, NAME, VERSION}

    public enum SINGLETON_STRATEGY_ERROR_HANDLING {IGNORE, ERROR, REPLACE}

    public static class ServiceDefinition<T> {

        public final String serviceIdentifier;
        public Class<? extends T> serviceClass;
        public T serviceInstance;
        public Configuration serviceConfiguration;
        public ServiceFactory.ServiceDetail serviceDetail;
        public SINGLETON_APPROACH singletonStrategy;
        // Removed support for initialization script.
        // public String initializationScript;
        public List dependencies;

        public ServiceDefinition(String serviceIdentifier) {
            this.serviceIdentifier = serviceIdentifier;
        }

        @Override
        public String toString() {
            return "Service " + serviceIdentifier + " (class: " + serviceClass + ")";
        }
    }

    public static class SINGLETON_APPROACH {
        public final SINGLETON_LEVEL singletonLevel;
        public final SINGLETON_STRATEGY_ERROR_HANDLING singletonStrategyErrorHandling;

        public SINGLETON_APPROACH(SINGLETON_LEVEL level, SINGLETON_STRATEGY_ERROR_HANDLING errorHandling) {
            this.singletonLevel = level;
            this.singletonStrategyErrorHandling = errorHandling;
        }
    }

    private static class ServiceManagerContext extends Context {

        private static final XLogger logger = XLoggerFactory.getXLogger(ServiceManagerContext.class);
        private ServiceManager serviceManager;
        private Map<String, Object> requestContext = new HashMap<>();
        private Map<String, Object> unmodifiableContext = Collections.unmodifiableMap(requestContext);
        private boolean debug;

        @Override
        public ServiceManager getServiceManager() {
            return logger.exit(serviceManager);
        }

        @Override
        public Map<String, Object> getRequestContext() {
            return logger.exit(unmodifiableContext);
        }

        @Override
        public boolean isDebugEnabled() {
            return debug;
        }

        public void enable() {
            logger.entry();
            Context.threadLocal.set(this);
            logger.exit();
        }

        public void disable() {
            logger.entry();
            Context.threadLocal.remove();
            logger.exit();
        }

    }

    private static class ServiceDependencyNode {
        public final String serviceName;
        private final Set<ServiceDependencyNode> dependsOn = new HashSet<>();
        private final Set<String> dependsOnServiceNames = new HashSet<>();
        private final Set<ServiceDependencyNode> dependedBy = new HashSet<>();
        private final Set<String> dependedByServiceNames = new HashSet<>();

        ServiceDependencyNode(String serviceName) {
            this.serviceName = serviceName;
        }

        public ServiceDependencyNode addDependsOn(ServiceDependencyNode dependencyNode) {
            dependsOn.add(dependencyNode);
            dependsOnServiceNames.add(dependencyNode.serviceName);
            return this;
        }

        public ServiceDependencyNode addDependedBy(ServiceDependencyNode dependencyNode) {
            dependedBy.add(dependencyNode);
            dependedByServiceNames.add(dependencyNode.serviceName);
            return this;
        }

        @Override
        public boolean equals(Object object) {
            if (object == this)
                return true;
            if (object instanceof ServiceDependencyNode) {
                if (((ServiceDependencyNode) object).serviceName.equals(serviceName))
                    return true;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return serviceName.hashCode();
        }

        @Override
        public String toString() {
            return "ServiceDependencyNode : {serviceName : " + serviceName + ", dependedBy: " + dependedByServiceNames + ", dependedOn: " + dependsOnServiceNames + " }";
        }
    }
}
