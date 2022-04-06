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

package org.grimps.service.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.grimps.service.config.internal.MapConfiguration;
import org.grimps.base.InternalErrorException;
import org.grimps.base.NotFoundException;
import org.grimps.base.ValidationException;
import org.grimps.base.config.Configuration;
import org.grimps.base.config.ConfigurationException;
import org.grimps.base.config.ManagedConfiguration;
import org.grimps.base.service.Lifecycle;
import org.grimps.base.service.LifecycleManager;
import org.grimps.base.service.ServiceException;
import org.grimps.base.utils.Utils;
import org.grimps.service.config.plugins.ReadOnlyFileConfigurationManagerPlugin;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import java.io.InputStream;
import java.net.URL;
import java.util.*;

/**
 * Configuration Service manages the configuration for the implementation by providing a standard interface that can be
 * used by other services to retrieve and manage configuration across one or more repositories.
 * The system is bootstrapped using "config-service-config.json" configuration file in case no configuration is specified.
 *
 */
public class ConfigurationService implements Lifecycle {

    public static final String CONF_SERVICE_NAME = "ConfigurationService";
    /**
     * Location of the file that contains bootstrap configuration
     */
    public static final String BOOTSTRAP_CONFIG_LOCATION = "config-service-config.json";
    public static final String BOOTSTRAP_CONFIG_PATH_ENV_VARIABLE = "BOOTSTRAP_CONFIG_PATH";
    private static final XLogger logger = XLoggerFactory.getXLogger(ConfigurationService.class);
    private URL bootstrapConfigurationLocation;
    private boolean isInitialized;
    private Map<String, Configuration> configurations = new HashMap<>();

    public static Map<String, Object> readConfigurationData(URL configURL) {
        logger.entry(configURL);
        ObjectMapper jsonParser = new ObjectMapper();
        jsonParser.enable(JsonParser.Feature.ALLOW_COMMENTS);
        jsonParser.enable(JsonParser.Feature.ALLOW_YAML_COMMENTS);
        jsonParser.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        jsonParser.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);
        InputStream bootstrapConfigInputStream = null;
        Map<String, Object> readConfig;
        try {
            logger.debug("Trying to open an input stream from url {}", configURL);
            bootstrapConfigInputStream = configURL.openStream();
            logger.debug("Trying to read values from {} using parser {} ", bootstrapConfigInputStream, jsonParser);
            readConfig = jsonParser.readValue(bootstrapConfigInputStream, Map.class);
            logger.debug("Read configuration {}", readConfig);
        } catch (Exception exception) {
            logger.catching(exception);
            throw logger.throwing(new InternalErrorException("Failed to load bootstrap configuration from " + configURL, exception));
        } finally {
            if (bootstrapConfigInputStream != null) {
                try {
                    logger.debug("Trying to close input stream {}", bootstrapConfigInputStream);
                    bootstrapConfigInputStream.close();
                    logger.debug("Closed input stream");
                } catch (Exception exception1) {
                    logger.warn("Failed to close input stream " + bootstrapConfigInputStream
                            + " of bootstrap config file " + configURL, exception1);
                }
            }
        }
        if (readConfig == null)
            throw logger.throwing(new NullPointerException("The JSON parsing of bootstrap configuration " + configURL + " returned null"));
        return logger.exit(readConfig);
    }

    public void configure(URL bootstrapConfigurationLocation) {
        logger.entry(bootstrapConfigurationLocation);
        if (isInitialized)
            throw logger.throwing(new ServiceException(ServiceException.ServiceErrorCode.already_initialized, "The configuration service is already initialized. Please destroy the service before re-configuring service."));
        this.bootstrapConfigurationLocation = bootstrapConfigurationLocation;
        logger.exit();
    }

    /**
     * Initializes the service by loading the bootstrap configuration.
     */
    @Override
    public void initialize() {
        logger.entry();
        initializeBootstrap();
        initializeConfigurationManagerPlugins();
        initializeKeywordReplacementPlugins();
        isInitialized = true;
        logger.exit();
    }

    protected void initializeBootstrap() {
        logger.entry();
        URL applicableBootstrapConfigURL = this.bootstrapConfigurationLocation;
        if (applicableBootstrapConfigURL == null) {
            String bootStrapEnvPath = System.getProperty(BOOTSTRAP_CONFIG_PATH_ENV_VARIABLE);
            if (Utils.isEmpty(bootStrapEnvPath))
                bootStrapEnvPath = System.getenv(BOOTSTRAP_CONFIG_PATH_ENV_VARIABLE);
            String bootStrapConfigLocation = Utils.isEmpty(bootStrapEnvPath) ? BOOTSTRAP_CONFIG_LOCATION : bootStrapEnvPath;
            logger.debug("Trying to load bootstrap config file {} as resource.", bootStrapConfigLocation);
            //TODO: How can we restrict loading to the particular jar file that contains Configuration Service class
            URL bootstrapConfigURL = Utils.loadResource(bootStrapConfigLocation);
            logger.debug("Retrieved resource URL {} ", bootstrapConfigURL);
            if (bootstrapConfigURL == null)
                throw logger.throwing(new NotFoundException("Failed to locate the bootstrap configuration file expected at " + bootStrapConfigLocation));
            try {
                applicableBootstrapConfigURL = bootstrapConfigURL;
            } catch (Exception exception) {
                logger.catching(exception);
                throw logger.throwing(new InternalErrorException("Failed to read configuration data from bootstrap URL " + bootstrapConfigURL, exception));
            }
        }
        ReadOnlyFileConfigurationManagerPlugin bootStrapConfigurationManager = LifecycleManager.createService(new ReadOnlyFileConfigurationManagerPlugin(applicableBootstrapConfigURL), null);
        Map<String, Object> readConfig = bootStrapConfigurationManager.getConfiguration();
        if (readConfig != null) {
            Map<String, Configuration> serviceDetails = new HashMap<>();
            for (String serviceName : readConfig.keySet()) {
                logger.debug("Started processing service {}", serviceName);
                Configuration configuration;
                Object serviceConfigObject = readConfig.get(serviceName);
                logger.debug("Started processing configuration {}", serviceConfigObject);
                if (serviceConfigObject instanceof Map) {
                    Map<String, Object> serviceConfiguration = (Map<String, Object>) serviceConfigObject;
                    logger.debug("Processing the configuration {}", serviceConfiguration);
                    configuration = new MapConfiguration(serviceConfiguration,bootStrapConfigurationManager);
                } else {
                    throw logger.throwing(new InternalErrorException("Configuration for service "
                            + serviceName + " was expected to be Map. Found "
                            + (serviceConfigObject == null ? "null" : serviceConfigObject.getClass())));
                }
                logger.debug("Adding configuration for service {}", serviceName);
                serviceDetails.put(serviceName, configuration);
                logger.debug("Completed processing service {}", serviceName);
            }
            this.configurations.putAll(serviceDetails);
        } else {
            throw logger.throwing(new InternalErrorException("Failed to read configuration data from bootstrap URL " + applicableBootstrapConfigURL));
        }
        logger.exit();
    }

    protected void initializeConfigurationManagerPlugins() {
        logger.entry();
        Configuration configurationServiceConfiguration = configurations.get(CONF_SERVICE_NAME);
        List<ConfigurationManagerPlugin> configurationUpdatePlugins = LifecycleManager.loadServices(ConfigurationManagerPlugin.class, configurationServiceConfiguration);
        if (configurationUpdatePlugins != null) {
            logger.trace("Processing configuration update plugins");
            for (ConfigurationManagerPlugin configurationUpdatePlugin : configurationUpdatePlugins) {
                if (configurationUpdatePlugin != null) {
                    logger.trace("Processing configuration update plugin {}", configurationUpdatePlugin);
                    Map<String, Object> updateConfigurationData = null;
                    try {
                        updateConfigurationData = configurationUpdatePlugin.getConfiguration();
                    } catch (Exception exception) {
                        logger.warn("Failed to retrieve the configuration updates from update plugin " + configurationUpdatePlugin, exception);
                    }
                    logger.trace("Loaded configuration data as {}", updateConfigurationData);
                    if (updateConfigurationData != null) {
                        for (Map.Entry<String, Object> mapEntry : updateConfigurationData.entrySet()) {
                            Object value = mapEntry.getValue();
                            if (value instanceof List) {
                                List<?> details = (List) value;
                                for (Object detail : details) {
                                    if (detail instanceof Map) {
                                        setConfiguration(mapEntry.getKey(), (Map) detail, configurationUpdatePlugin);
                                    } else if (detail == null) {
                                        setConfiguration(mapEntry.getKey(), (Map) null, configurationUpdatePlugin);
                                    } else {
                                        logger.debug("Ignoring entry {} associated with service {} while processing result of {}", detail, mapEntry.getKey(), configurationUpdatePlugin);
                                    }
                                }
                            } else if (value instanceof Map) {
                                setConfiguration(mapEntry.getKey(), (Map) value, configurationUpdatePlugin);
                            } else if (value == null) {
                                setConfiguration(mapEntry.getKey(), (Map) null, configurationUpdatePlugin);
                            } else {
                                logger.debug("Ignoring entry {} while processing result of {}", mapEntry, configurationUpdatePlugin);
                            }
                        }
                    } else {
                        logger.debug("No configuration update detail was provided by plugin {}", configurationUpdatePlugin);
                    }
                } else {
                    logger.debug("Skipping item since it is null in list of update plugins {}.", configurationUpdatePlugins);
                }
            }
        } else {
            logger.debug("No plugins located to update configurations.");
        }
        logger.exit();
    }

    protected void initializeKeywordReplacementPlugins() {
        logger.entry();
        Configuration configurationServiceConfiguration = configurations.get(CONF_SERVICE_NAME);
        List<KeywordReplacementPlugin> keywordReplacementPlugins = LifecycleManager.loadServices(KeywordReplacementPlugin.class, configurationServiceConfiguration);
        for (Configuration configuration : configurations.values()) {
            logger.trace("Processing configuration {} for keywords", configuration);
            if (configuration instanceof MapConfiguration) {
                processForKeywords(keywordReplacementPlugins, ((MapConfiguration) configuration).configuration);
            } else {
                logger.debug("Skipping key word processing for configuration {} since it is not MapConfiguration", configuration);
            }
            logger.trace("Processed configuration");
        }
        logger.exit();
    }

    private Object processForKeywords(List<KeywordReplacementPlugin> keywordReplacementPlugins, Object value) {
        if (value instanceof String) {
            return processForKeywords(keywordReplacementPlugins, (String) value);
        } else if (value instanceof List) {
            List<Object> valueAsList = (List) value;
            for (int index = 0; index < valueAsList.size(); index++) {
                Object item = valueAsList.get(index);
                Object transformedItem = processForKeywords(keywordReplacementPlugins, item);
                valueAsList.set(index, transformedItem);
            }
            return valueAsList;
        } else if (value instanceof Map) {
            Map<String, Object> configData = (Map) value;
            for (String key : configData.keySet()) {
                Object mapValue = configData.get(key);
                Object transformedValue = processForKeywords(keywordReplacementPlugins, mapValue);
                if (transformedValue != mapValue)
                    configData.put(key, transformedValue);
            }
            return configData;
        }
        return value;
    }


    private Object processForKeywords(List<KeywordReplacementPlugin> keywordReplacementPlugins, String inputValue) {
        if (keywordReplacementPlugins != null) {
            for (KeywordReplacementPlugin keywordReplacementPlugin : keywordReplacementPlugins) {
                if (keywordReplacementPlugin != null) {
                    if (keywordReplacementPlugin.supported(inputValue)) {
                        return keywordReplacementPlugin.replace(inputValue);
                    }
                }
            }
        }
        String keywordBase = "{$";
        String configurationPropertyReferencePrefix = "{$CONFIGREF:";
        String configurationPropertyReferenceDelimiter = "','";
        String systemPropertyKeywordSuffix = "$}";
        if (inputValue.startsWith(keywordBase)) {
            if (inputValue.toUpperCase().startsWith(configurationPropertyReferencePrefix)) {
                String configurationPropertyReference = Utils.extractValue(inputValue, configurationPropertyReferencePrefix, systemPropertyKeywordSuffix);
                logger.trace("Located attribute details {}", configurationPropertyReference);
                if (configurationPropertyReference != null) {
                    String[] configurationPropertyReferenceItems = configurationPropertyReference.split(configurationPropertyReferenceDelimiter);
                    logger.trace("Result of configuration reference split using delimiter {}", (Object) configurationPropertyReferenceItems);
                    if (configurationPropertyReferenceItems.length >= 2) {
                        List<String> propertyName = Arrays.asList(Arrays.copyOfRange(configurationPropertyReferenceItems, 1, configurationPropertyReferenceItems.length));
                        Object configurationValue = ConfigurationUtils.getValue(getConfiguration(configurationPropertyReferenceItems[0]), propertyName);
                        return logger.exit(configurationValue);
                    } else {
                        logger.debug("Even though {} and {} were found (implying a configuration property reference), failed to parse configuration property {} into more than 2 items with name and property item.", configurationPropertyReferencePrefix, systemPropertyKeywordSuffix, configurationPropertyReference);
                    }
                }
            } else {
                logger.trace("Even though the keyword '{$' was encountered in {}, it did not match any supported expansion keyword. Assuming non-keyword value.", inputValue);
            }
        }
        return inputValue;
    }

    /**
     * Returns the configuration for given service if available, null otherwise.
     *
     * @param serviceName Name of service
     * @return If any Configuration is associated then it is returned, null otherwise.
     * @throws ValidationException in case service name is not provided.
     */
    public Configuration getConfiguration(String serviceName) {
        logger.entry(serviceName);
        if (Utils.isEmpty(serviceName))
            throw logger.throwing(new ValidationException("No service name was provided."));
        if (configurations.containsKey(serviceName)) {
            return logger.exit(configurations.get(serviceName));
        }
        return logger.exit(null);
    }

    /**
     * Utility method to return configuration from specific location in property hierarchy of a service.
     *
     * @param serviceName  Service from which configuration needs to be extracted.
     * @param propertyPath Property hierarchy that should be used to extract configuration.
     * @return Configuration if the corresponding property can be resolved to applicable configuration.
     * @see #setConfiguration(String, Map, ConfigurationManagerPlugin) for additional details about property hierarchy.
     * @see #getConfiguration(String, List) for details
     */
    public Configuration getConfiguration(String serviceName, String... propertyPath) {
        logger.entry(serviceName, propertyPath);
        List<String> configurationPathAsList = null;
        if (propertyPath != null && propertyPath.length > 0)
            configurationPathAsList = Arrays.asList(propertyPath);
        return logger.exit(getConfiguration(serviceName, configurationPathAsList));
    }

    /**
     * Utility method to return configuration from specific location in property hierarchy of a service.
     *
     * @param serviceName  Service from which configuration needs to be extracted.
     * @param propertyPath Property hierarchy that should be used to extract configuration.
     * @return Configuration if the corresponding property can be resolved to applicable configuration.
     * @throws ConfigurationException (Error code MissingConfiguration) in case service name does not have associated
     *                                configuration or property path could not be resolved to a configuration.
     * @see #setConfiguration(String, Map, ConfigurationManagerPlugin) for additional details about property hierarchy.
     * @see #getConfiguration(String, List) for details
     */
    public Configuration getConfiguration(String serviceName, List<String> propertyPath) {
        logger.entry(serviceName, propertyPath);
        Configuration configuration = getConfiguration(serviceName);
        if (configuration == null)
            throw logger.throwing(new ConfigurationException(ConfigurationException.ErrorCodes.MissingConfiguration, "No configuration was located for service " + serviceName));
        ApplicableConfigurationDetails applicableConfigurationDetails = new ApplicableConfigurationDetails(configuration, null);
        if (propertyPath != null) {
            applicableConfigurationDetails = getApplicableConfigurationDetails(configuration, propertyPath);
        }
        logger.debug("Applicable configuration {} & property {}", applicableConfigurationDetails.applicableConfiguration, applicableConfigurationDetails.applicableProperty);
        if (applicableConfigurationDetails.applicableConfiguration == null)
            throw logger.throwing(new ConfigurationException(ConfigurationException.ErrorCodes.MissingConfiguration, "No configuration " + (propertyPath != null ? "for property " + propertyPath : ""
                    + " was located for service " + serviceName)));
        if (applicableConfigurationDetails.applicableProperty == null) {
            return logger.exit(applicableConfigurationDetails.applicableConfiguration);
        } else {
            return logger.exit(applicableConfigurationDetails.applicableConfiguration.subset(applicableConfigurationDetails.applicableProperty));
        }
    }

    protected static ApplicableConfigurationDetails getApplicableConfigurationDetails(Configuration configuration, List<String> property) {
        logger.entry(configuration, property);
        Configuration applicableConfiguration;
        String applicablePropertyName;
        if (property == null || property.isEmpty()) {
            logger.debug("No property was found, applicable configuration that for given type & service name");
            applicableConfiguration = configuration;
            applicablePropertyName = null;
        } else {
            //TODO: Evaluate option for using ExpressionLanguage.
            int secondLastItem = property.size() - 2;
            logger.trace("Identified second last item location {}", secondLastItem);
            if (secondLastItem < 0) {
                logger.debug("Since parent property set is not there, assume applicable configuration to given type & service");
                applicableConfiguration = configuration;
                logger.debug("Applicable property same as that provided i.e. {}", property);
                applicablePropertyName = property.get(0);
            } else {
                applicableConfiguration = configuration;
                logger.trace("Trying to find the subset configuration applicable");
                for (int counter = 0; counter <= secondLastItem; counter++) {
                    String propertyName = property.get(counter);
                    applicableConfiguration = applicableConfiguration.subset(propertyName);
                    logger.trace("Located configuration for property {} at {} as {}", propertyName, counter, applicableConfiguration);
                    if (applicableConfiguration == null) {
                        logger.trace("Failed to locate configuration for property {}. Skipping remaining parts of property expression", propertyName);
                        break;
                    }
                }
                if (applicableConfiguration != null) {
                    logger.debug("Setting applicable property name to the last item in the split property array {}", property.size() - 1);
                    applicablePropertyName = property.get(property.size() - 1);
                } else {
                    applicablePropertyName = null;
                }
            }
        }
        return logger.exit(new ApplicableConfigurationDetails(applicableConfiguration, applicablePropertyName));
    }

    /**
     * Creates a {@link Configuration} object from the given map.
     *
     * @param configuration Map containing configuration
     * @return Instance of configuration created using given Map.
     */
    public Configuration getConfiguration(Map<String, Object> configuration) {
        logger.entry(configuration);
        return logger.exit(new MapConfiguration(configuration, null));
    }

    /**
     * Creates a configuration from the given configurations.
     *
     * @param primaryConfiguration The first configuration to look up for properties.
     * @param backupConfiguration  If not found second configuration to use.
     * @return A composite configuration.
     * @throws ServiceException In case service is not available
     */
    public Configuration getCompositeConfiguration(Configuration primaryConfiguration, Configuration backupConfiguration) {
        logger.entry(primaryConfiguration, backupConfiguration);
        return logger.exit(new CompositeConfiguration(primaryConfiguration, backupConfiguration));
    }

    /**
     * Updates the given property with the new value. In order to update the property within a map, the child property
     * can be represented by a list of String or by a name separated by "."
     * In order to update the "property2" in following example
     * <pre>
     *     {
     *         "property1" : {
     *             "property2" :"value2"
     *         },
     *         "property3" : "value3"
     *     }
     * </pre>
     * the property name can be specified as
     * <ol>
     * <li>["property1", "property2"]</li>
     * <li>"property1.property2"</li>
     * </ol>
     *
     * @param serviceName Name of service being updated
     * @param details     Map containing property name (key "property") and new value (key "value")
     * @throws ValidationException    Exception is raised if
     *                                <ol>
     *                                <li>service name is incorrect</li>
     *                                <li>property details have not been provided</li>
     *                                <li>Given details is supposed to replace service configuration (because no configuration is currently associated)
     *                                and it contains "property" and "value"</li>
     *                                <li>Id details does not contain "property" and "value" with details about replacement</li>
     *                                <li>If "property" is a string and it can not be split using "." as delimiter</li>
     *                                <li>"property" is not string or List of string</li>
     *                                </ol>In case type or service name is incorrect or property details have not been provided.
     *                                In addition to that if map does not contain property and value or if configuration is
     *                                supposed to replace existing configuration and contains these attributes, this exception is
     * @throws ConfigurationException In case the given property can not resolve to a configuration or configuration can not be updated.
     */
    protected void setConfiguration(String serviceName, Map details, ConfigurationManagerPlugin configurationSource) {
        logger.entry(serviceName, details);
        if (Utils.isEmpty(serviceName))
            throw logger.throwing(new ValidationException("No service name was provided."));
        if (details == null) {
            logger.debug("Removing configuration for service {}", serviceName);
            configurations.remove(serviceName);
        } else {
            Configuration configuration = getConfiguration(serviceName);
            if (configuration == null) {
                //TODO: how to handle scenario where configuration contains property & value.
                if (details.containsKey("property") && details.containsKey("value"))
                    throw logger.throwing(new ValidationException("The requested " + serviceName + " has no associated configuration. The provided configuration (contains property & value) assumes a existing configuration to be present."));
                Configuration configurationObject = new MapConfiguration(details, configurationSource);
                this.configurations.put(serviceName, configurationObject);
            } else {
                if (!details.containsKey("property") || !details.containsKey("value")) {
                    logger.trace("Replacing existing configuration {} with composite configuration created using new configuration {} as primary and original configuration as backup", configuration, details);
                    this.configurations.put(serviceName, new CompositeConfiguration(new MapConfiguration(details, configurationSource), configuration));
                } else {
                    Object propertyObject = details.get("property");
                    List<String> propertyAccessList;
                    if (propertyObject instanceof String) {
                        String[] propertySubset = ((String) propertyObject).split("\\.");
                        logger.trace("Split result {}", (Object) propertySubset);
                        propertyAccessList = Arrays.asList(propertySubset);
                    } else if (propertyObject instanceof List) {
                        propertyAccessList = (List) propertyObject;
                    } else {
                        throw logger.throwing(new ValidationException("The property name " + propertyObject + " is not in a supported format (only string and list are currently supported)."));
                    }
                    ApplicableConfigurationDetails applicableConfigurationDetails = getApplicableConfigurationDetails(configuration, propertyAccessList);
                    Configuration applicableConfiguration = applicableConfigurationDetails.applicableConfiguration;
                    String applicablePropertyName = applicableConfigurationDetails.applicableProperty;
                    if (applicableConfiguration != null) {
                        if (applicableConfiguration instanceof ManagedConfiguration) {
                            ((ManagedConfiguration) applicableConfiguration).setProperty(applicablePropertyName, details.get("value"));
                        } else {
                            throw new ConfigurationException(ConfigurationException.ErrorCodes.ReadOnly, "The applicable configuration is not editable.");
                        }
                    } else {
                        throw logger.throwing(new ConfigurationException(ConfigurationException.ErrorCodes.MissingConfiguration, "No configuration for property " + propertyObject
                                + " was located for service " + serviceName));
                    }
                }
            }
        }
    }

    @Override
    public boolean isInitialized() {
        return isInitialized;
    }

    @Override
    public void destroy() {
        logger.entry();
        isInitialized = false;
        configurations.clear();
        logger.exit();
    }

    /**
     * Utility class to store applicable configuration details.
     */
    protected static class ApplicableConfigurationDetails {
        public final Configuration applicableConfiguration;
        public final String applicableProperty;

        public ApplicableConfigurationDetails(Configuration applicableConfiguration, String applicableProperty) {
            this.applicableConfiguration = applicableConfiguration;
            this.applicableProperty = applicableProperty;
        }

    }

    private static class CompositeConfiguration implements Configuration, ManagedConfiguration {

        private static final XLogger logger = XLoggerFactory.getXLogger(CompositeConfiguration.class);
        private Set<String> propertyNames = new HashSet<String>();
        private Configuration configuration;
        private Configuration backupConfiguration;

        public CompositeConfiguration(Configuration primaryConfiguration, Configuration backupConfiguration) {
            logger.entry(primaryConfiguration, backupConfiguration);
            this.configuration = primaryConfiguration;
            if (primaryConfiguration != null)
                this.propertyNames.addAll(configuration.getPropertyNames());
            this.backupConfiguration = backupConfiguration;
            if (backupConfiguration != null)
                this.propertyNames.addAll(backupConfiguration.getPropertyNames());
            logger.exit();
        }

        public synchronized void setPrimaryConfiguration(Configuration configuration) {
            Set<String> commonPropertyNames = getCombinedPropertyNames(configuration, backupConfiguration);
            this.configuration = configuration;
            this.propertyNames = commonPropertyNames;
        }

        private Set<String> getCombinedPropertyNames(Configuration primaryConfiguration, Configuration secondaryConfiguration) {
            Set<String> propertyNames = primaryConfiguration != null ? primaryConfiguration.getPropertyNames() : new HashSet<String>();
            Set<String> secondaryPropertyNames = secondaryConfiguration != null ? secondaryConfiguration.getPropertyNames() : new HashSet<String>();
            Set<String> allPropertyNames = new HashSet<String>();
            allPropertyNames.addAll(propertyNames);
            allPropertyNames.addAll(secondaryPropertyNames);
            return allPropertyNames;
        }

        public synchronized void setBackupConfiguration(Configuration configuration) {
            Set<String> commonPropertyNames = getCombinedPropertyNames(this.configuration, configuration);
            this.backupConfiguration = configuration;
            this.propertyNames = commonPropertyNames;
        }

        @Override
        public boolean containsProperty(String name) {
            return propertyNames.contains(name);
        }

        @Override
        public Set<String> getPropertyNames() {
            return logger.exit(Collections.unmodifiableSet(propertyNames));
        }

        @Override
        public <T> T getProperty(String propertyName) {
            logger.entry(propertyName);
            if (configuration != null && configuration.containsProperty(propertyName)) {
                return (T) logger.exit(configuration.getProperty(propertyName));
            }
            if (backupConfiguration != null) {
                return (T) logger.exit(backupConfiguration.getProperty(propertyName));
            }
            return logger.exit(null);
        }

        @Override
        public <T> T getProperty(String propertyName, Class<T> valueClass) {
            return logger.exit(Utils.castOrReturnNull(getProperty(propertyName), valueClass));
        }

        @Override
        public <T> void setProperty(String propertyName, T newValue) {
            boolean valueWasSet = false;
            if (this.configuration != null && this.configuration instanceof ManagedConfiguration) {
                ((ManagedConfiguration) configuration).setProperty(propertyName, newValue);
                valueWasSet = true;
            } else if (backupConfiguration != null && backupConfiguration instanceof ManagedConfiguration) {
                ((ManagedConfiguration) backupConfiguration).setProperty(propertyName, newValue);
                valueWasSet = true;
            } else if (backupConfiguration instanceof CompositeConfiguration) {
                //TODO: Review why primary configuration is not being looked at and possibly updated.
                CompositeConfiguration currentConfigurationBeingProcessed = (CompositeConfiguration) backupConfiguration;
                while (currentConfigurationBeingProcessed.backupConfiguration != null) {
                    if (currentConfigurationBeingProcessed.backupConfiguration instanceof ManagedConfiguration) {
                        ((ManagedConfiguration) currentConfigurationBeingProcessed.backupConfiguration).setProperty(propertyName, newValue);
                        valueWasSet = true;
                        break;
                    }
                    if (currentConfigurationBeingProcessed.backupConfiguration instanceof CompositeConfiguration) {
                        currentConfigurationBeingProcessed = ((CompositeConfiguration) currentConfigurationBeingProcessed.backupConfiguration);
                    } else {
                        break;
                    }
                }
            }
            if (!valueWasSet) {
                throw new ValidationException("The new property " + propertyName + " can not be set because the configuration is non-editable.");
            }
            if (this.propertyNames != null) {
                propertyNames.add(propertyName);
            }
        }

        @Override
        public void refresh() {
            this.propertyNames = getCombinedPropertyNames(this.configuration, configuration);
        }

        @Override
        public <T> T getProperty(String propertyName, T defaultValue) {
            logger.entry(propertyName, defaultValue);
            T value = getProperty(propertyName);
            if (value == null)
                return logger.exit(defaultValue);
            return logger.exit(value);
        }

        @Override
        public <T> T getProperty(String propertyName, T defaultValue, Class<T> valueClass) {
            return logger.exit(Utils.castOrReturnNull(getProperty(propertyName, defaultValue), valueClass));
        }

        @Override
        public Configuration subset(String prefix) {
            logger.entry(prefix);
            Configuration backupConfigurationForPrefix = null;
            if (backupConfiguration != null) {
                backupConfigurationForPrefix = backupConfiguration.subset(prefix);
            }
            if (configuration != null) {
                Configuration primarySubsetConfiguration = configuration.subset(prefix);
                if (primarySubsetConfiguration != null) {
                    return logger.exit(new CompositeConfiguration(primarySubsetConfiguration, backupConfigurationForPrefix));
                } else
                    return logger.exit(backupConfigurationForPrefix);
            } else {
                return logger.exit(backupConfigurationForPrefix);
            }
        }
    }

}
