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

import org.grimps.base.ValidationException;
import org.grimps.base.config.Configuration;
import org.grimps.base.config.ConfigurationException;
import org.grimps.service.config.internal.MapConfiguration;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class provides utility functions to work with configuration and configuration service.
 */
public class ConfigurationUtils {

    private static final XLogger logger = XLoggerFactory.getXLogger(ConfigurationUtils.class);

    public static Configuration verifyConfiguration(Configuration configuration, String... configurationLocation) {
        logger.entry(configuration, configurationLocation);
        if (configuration != null)
            return configuration;
        StringBuilder configurationPath = new StringBuilder();
        if (configurationLocation != null) {
            for (String configurationProperty : configurationLocation) {
                configurationPath.append(configurationProperty).append(".");
            }
        }
        throw logger.throwing(new ConfigurationException(ConfigurationException.ErrorCodes.MissingConfiguration,
                "The configuration could not be located. Please ensure that configuration is specified at " + configurationPath));
    }

    /**
     * Returns the configuration value from given location.
     *
     * @param configuration     Configuration from which value needs to be extracted.
     * @param configurationPath Property hierarchy to use to resolve value
     * @return Value if available, null otherwise.
     * @see #getValue(Configuration, List) for specific details about exceptions
     * @see ConfigurationService#setConfiguration(String, Map, ConfigurationManagerPlugin) for details about property hierarchy
     */
    public static Object getValue(Configuration configuration, String... configurationPath) {
        logger.entry(configuration, configurationPath);
        List<String> configurationPathAsList = null;
        if (configurationPath != null && configurationPath.length > 0)
            configurationPathAsList = Arrays.asList(configurationPath);
        return logger.exit(getValue(configuration, configurationPathAsList));
    }

    /**
     * Returns the configuration value from given location. If configuration path is null or can not be fully resolved,
     * it returns the configuration that was resolved to as map.
     *
     * @param configuration     Configuration from which value needs to be extracted.
     * @param configurationPath Property hierarchy to use to resolve value
     * @return Value if available, null otherwise.
     * @throws ValidationException if no configuration is provided.
     * @see #getValue(Configuration, List) for specific details about exceptions
     * @see ConfigurationService#setConfiguration(String, Map, ConfigurationManagerPlugin) for details about property hierarchy
     */
    public static Object getValue(Configuration configuration, List<String> configurationPath) {
        logger.entry(configuration, configurationPath);
        if (configuration == null)
            throw logger.throwing(new ValidationException("No configuration was provided"));
        ConfigurationService.ApplicableConfigurationDetails result = ConfigurationService.getApplicableConfigurationDetails(configuration, configurationPath);
        if (result != null && result.applicableConfiguration != null) {
            if (result.applicableProperty != null)
                return logger.exit(result.applicableConfiguration.getProperty(result.applicableProperty));
            else if (result.applicableConfiguration instanceof MapConfiguration)
                return logger.exit(((MapConfiguration) result.applicableConfiguration).getConfiguration());
            else
                return logger.exit(null);
        } else
            return logger.exit(null);
    }

    /**
     * Updates the given property
     *
     * @param configurationService Instance of configuration service to update.
     * @param serviceName          Name of service being updated
     * @param property             Property Name (in dot format)
     * @param newData              New value
     * @return true if value was set successfully, false otherwise.
     * @see ConfigurationService#setConfiguration(String, Map, ConfigurationManagerPlugin)
     */
    public boolean setConfiguration(ConfigurationService configurationService, String serviceName, final String property, final Object newData) {
        logger.entry(configurationService, serviceName, property, newData);
        if (configurationService != null) {
            configurationService.setConfiguration(serviceName, new HashMap<String, Object>() {{
                put("property", property);
                put("value", newData);
            }}, null);
            return logger.exit(true);
        } else {
            logger.debug("Ignoring the call to set configuration since no configuration service was provided.");
            return logger.exit(false);
        }
    }
}
