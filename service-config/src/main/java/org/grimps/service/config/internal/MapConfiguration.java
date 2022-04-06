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

package org.grimps.service.config.internal;

import org.grimps.base.config.Configuration;
import org.grimps.base.config.ConfigurationException;
import org.grimps.base.config.ManagedConfiguration;
import org.grimps.base.utils.Utils;
import org.grimps.service.config.ConfigurationManagerPlugin;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import java.util.*;

public class MapConfiguration implements Configuration, ManagedConfiguration {

    private static final XLogger logger = XLoggerFactory.getXLogger(MapConfiguration.class);
    private Map<String, Object> configuration;
    private Map<String, Object> updates = new HashMap<>();
    private boolean isSubsetConfiguration;
    private ConfigurationManagerPlugin configurationManagerPlugin;
    private List<String> parentAttributeList;
    private MapConfiguration originalConfiguration;

    public MapConfiguration(Map<String, Object> configuration, ConfigurationManagerPlugin configurationManagerPlugin) {
        logger.entry(configuration, configurationManagerPlugin);
        this.configuration = configuration;
        this.configurationManagerPlugin = configurationManagerPlugin;
        isSubsetConfiguration = false;
        logger.exit();
    }

    public MapConfiguration(Map<String, Object> configuration, List<String> subsetAttributeHierarchy, MapConfiguration originalConfiguration) {
        logger.entry(configuration, subsetAttributeHierarchy, originalConfiguration);
        this.configuration = configuration;
        this.parentAttributeList = subsetAttributeHierarchy;
        this.originalConfiguration = originalConfiguration;
        isSubsetConfiguration = true;
        logger.exit();
    }

    @Override
    public <T> void setProperty(String propertyName, T newValue) {
        logger.entry(propertyName, newValue);
        if (configuration != null) {
            logger.trace("Setting property {} to the configuration", propertyName);
            configuration.put(propertyName, newValue);
            validate(true);
            List<String> propertyLocation = new ArrayList<>();
            Map<String, Object> updateMapToUpdate;
            if (isSubsetConfiguration) {
                propertyLocation.addAll(this.parentAttributeList);
                propertyLocation.add(propertyName);
                updateMapToUpdate = originalConfiguration.updates;
                logger.trace("Adding update to original configuration's {} update {}", originalConfiguration, originalConfiguration.updates);
            } else {
                propertyLocation.add(propertyName);
                updateMapToUpdate = updates;
                logger.trace("Adding update to this configuration's update");
            }
            logger.trace("Adding property 'property' as {}", propertyLocation);
            updateMapToUpdate.put("property", propertyLocation);
            logger.trace("Adding property 'value' with given value.");
            updateMapToUpdate.put("value", newValue);
        } else {
            logger.debug("Skipping property setting since map configuration does not have any associated configuration map.");
        }
        logger.exit();
    }

    @Override
    public void refresh() {
        logger.entry();
        logger.exit();
    }

    @Override
    public boolean isPersistent() {
        logger.entry();
        if (configurationManagerPlugin != null &&
                (configurationManagerPlugin.supports(ConfigurationManagerPlugin.CONFIGURATION_UPDATE) ||
                        configurationManagerPlugin.supports(ConfigurationManagerPlugin.CONFIGURATION_SAVE)))
            return logger.exit(true);
        else
            return logger.exit(false);
    }

    @Override
    public void save() {
        logger.entry();
        validate(true);
        if (isSubsetConfiguration) {
            logger.debug("Saving original configuration {}", originalConfiguration);
            originalConfiguration.save();
        } else {
            if (configurationManagerPlugin != null) {
                if (configurationManagerPlugin.supports(ConfigurationManagerPlugin.CONFIGURATION_SAVE)) {
                    logger.debug("Saving configuration.");
                    logger.trace("Configuration {}", configuration);
                    configurationManagerPlugin.save(this.configuration);
                } else if (configurationManagerPlugin.supports(ConfigurationManagerPlugin.CONFIGURATION_UPDATE)) {
                    logger.debug("Updating configuration.");
                    logger.trace("Configuration {}", updates);
                    configurationManagerPlugin.update(this.updates);
                } else {
                    throw logger.throwing(new ConfigurationException(ConfigurationException.ErrorCodes.NotSupported,
                            "Configuration Manager " + configurationManagerPlugin + " associated with this configuration does not support saving or updating configuration changes. Ignoring the updates."));
                }
            } else {
                throw logger.throwing(new ConfigurationException(ConfigurationException.ErrorCodes.NotSupported, "Since no Configuration Manager is associated with this configuration, can not save or update configuration."));
            }
        }
        logger.exit();
    }

    public boolean validate(boolean throwException) {
        logger.entry(throwException);
        boolean isValid = true;
        if (configuration == null) {
            if (throwException)
                throw logger.throwing(new ConfigurationException(ConfigurationException.ErrorCodes.Invalid, "The configuration object " + this + " was not created with any configuration it is supposed to manage."));
            logger.warn("The configuration {} does not have any associated configuration it is supposed to manage.", this);
            isValid = false;
        }
        if (isSubsetConfiguration) {
            if (this.originalConfiguration == null) {
                if (throwException)
                    throw logger.throwing(new ConfigurationException(ConfigurationException.ErrorCodes.Invalid, "The configuration object  " + this + " is marked as configuration created as subset but does not have any associated original configuration object."));
                logger.warn("The configuration object {} is marked as configuration created as subset but does not have any associated original configuration object.", this);
                isValid = false;
            }
            if (this.parentAttributeList == null || parentAttributeList.size() == 0) {
                if (throwException)
                    throw logger.throwing(new ConfigurationException(ConfigurationException.ErrorCodes.Invalid, "The configuration object " + this + " is marked as configuration created as subset but does not have parent attribute list as expected."));
                logger.warn("The configuration object {} is marked as configuration created as subset but does not have parent attribute list as expected.", this);
                isValid = false;
            }
        } else {
            logger.debug("The Configuration Manager Plugin {} is an optional attribute for configuration. So, this is marked as valid.", configurationManagerPlugin);
        }
        return logger.exit(isValid);
    }

    @Override
    public boolean containsProperty(String name) {
        logger.entry(name);
        return logger.exit(configuration != null && configuration.containsKey(name));
    }

    @Override
    public Set<String> getPropertyNames() {
        logger.entry();
        return logger.exit(configuration == null ? new HashSet<String>() : Collections.unmodifiableSet(configuration.keySet()));
    }

    @Override
    public <T> T getProperty(String propertyName) {
        logger.entry(propertyName);
        return (T) logger.exit(configuration != null ? configuration.get(propertyName) : null);
    }

    @Override
    public <T> T getProperty(String propertyName, Class<T> valueClass) {
        logger.entry(propertyName, valueClass);
        return logger.exit(Utils.castOrReturnNull(getProperty(propertyName), valueClass));
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
        logger.entry(propertyName, defaultValue, valueClass);
        return logger.exit(Utils.castOrReturnNull(getProperty(propertyName, defaultValue), valueClass));
    }

    @Override
    public Configuration subset(String prefix) {
        Object subsetValue;
        if (configuration != null && configuration.containsKey(prefix)
                && (subsetValue = configuration.get(prefix)) != null && subsetValue instanceof Map) {
            List<String> parentAttributes = new ArrayList<>();
            if (parentAttributeList != null) {
                parentAttributes.addAll(parentAttributeList);
            }
            parentAttributes.add(prefix);
            return new MapConfiguration((Map<String, Object>) subsetValue, parentAttributes, originalConfiguration == null ? this : originalConfiguration);
        }
        return null;
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }
}
