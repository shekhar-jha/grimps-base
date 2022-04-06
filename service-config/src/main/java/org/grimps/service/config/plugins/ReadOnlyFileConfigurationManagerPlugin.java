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

package org.grimps.service.config.plugins;

import org.grimps.base.config.Configurable;
import org.grimps.base.config.Configuration;
import org.grimps.base.service.LifecycleManager;
import org.grimps.base.utils.Utils;
import org.grimps.service.config.ConfigurationService;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Configuration Manager plugin that provides configuration which can not be updated. These are typically files that
 * are packaged as part of jar files or as configuration files on file system that are expected to be read-only.
 */
public class ReadOnlyFileConfigurationManagerPlugin extends ConfigurationManagerPluginReadOnly implements Configurable {

    public static final String CONFIG_FILE_NAME = "service-org.grimps.service.config.json";
    private static final XLogger logger = XLoggerFactory.getXLogger(ReadOnlyFileConfigurationManagerPlugin.class);
    private final URL configurationLoadedFrom;
    private Configuration configuration;

    public ReadOnlyFileConfigurationManagerPlugin() {
        logger.entry();
        configurationLoadedFrom = null;
        logger.exit();
    }

    public ReadOnlyFileConfigurationManagerPlugin(URL fileLocation) {
        logger.entry(fileLocation);
        this.configurationLoadedFrom = fileLocation;
        logger.exit();
    }

    @Override
    public void configure(Configuration configuration) {
        logger.entry(configuration);
        this.configuration = configuration;
        logger.exit();
    }

    protected List<URL> locateConfigurationFiles(List<FileLocationPlugin> fileLocationPlugins) {
        logger.entry(fileLocationPlugins);
        List<URL> readFromLocations = new ArrayList<>();
        if (fileLocationPlugins != null && !fileLocationPlugins.isEmpty()) {
            for (FileLocationPlugin fileLocationPlugin : fileLocationPlugins) {
                logger.trace("Processing file location plugin {}", fileLocationPlugin);
                if (fileLocationPlugin != null) {
                    try {
                        List<URL> fileLocations = fileLocationPlugin.getFileLocations();
                        logger.trace("Retrieved file locations as {}", fileLocations);
                        if (fileLocations != null) {
                            readFromLocations.addAll(fileLocations);
                        }
                    } catch (Exception exception) {
                        logger.debug("Failed to retrieve file locations and adding it to list for file location plugin " + fileLocationPlugin, exception);
                    }
                }
                logger.trace("Processed file location plugin.");
            }
        } else {
            logger.debug("No file location plugins available to process.");
        }
        return logger.exit(readFromLocations);
    }

    protected List<FileLocationPlugin> locateFileLocationPlugins() {
        logger.entry();
        List<FileLocationPlugin> fileLocationPlugins = LifecycleManager.loadServices(FileLocationPlugin.class, configuration);
        return logger.exit(fileLocationPlugins);
    }

    protected List<URL> processConfigurationFileLocationList(List<URL> configurationFileLocation) {
        logger.entry(configurationFileLocation);
        logger.debug("This method does not processing on the list.");
        return logger.exit(configurationFileLocation);
    }

    @Override
    public Map<String, Object> getConfiguration() {
        List<URL> readFromLocations = new ArrayList<>();
        if (configurationLoadedFrom != null) {
            logger.debug("Configuration to read from has been explicitly specified as {}", configurationLoadedFrom);
            readFromLocations.add(configurationLoadedFrom);
        } else {
            logger.debug("Trying to locate all configuration files that should be loaded.");
            List<FileLocationPlugin> fileLocationPlugins = locateFileLocationPlugins();
            logger.trace("Located file location plugins as {}", fileLocationPlugins);
            readFromLocations.addAll(locateConfigurationFiles(fileLocationPlugins));
        }
        logger.debug("Configuration file locations to process are {}", readFromLocations);
        if (!readFromLocations.isEmpty()) {
            logger.trace("Processing configuration locations {}", readFromLocations);
            Map<String, Object> accumulatedConfiguration = null;
            for (URL readFromLocation : readFromLocations) {
                logger.trace("Processing configuration location {}", readFromLocation);
                accumulatedConfiguration = mergeConfiguration(readFromLocation, accumulatedConfiguration, readConfiguration(readFromLocation));
                logger.trace("Processed configuration location.");
            }
            logger.debug("Processed configuration locations");
            return logger.exit(accumulatedConfiguration);
        } else {
            logger.debug("No file location could be located for reading purpose.");
            return logger.exit(null);
        }
    }

    protected Map<String, Object> readConfiguration(URL configurationLocation) {
        logger.entry(configurationLocation);
        try {
            if (configurationLocation != null) {
                Map<String, Object> configuration = ConfigurationService.readConfigurationData(configurationLocation);
                return logger.exit(configuration);
            }
        } catch (Exception exception) {
            logger.debug("Failed to read configuration from URL " + configurationLocation, exception);
        }
        return logger.exit(null);
    }

    protected Map<String, Object> mergeConfiguration(URL configurationLocation, Map<String, Object> configuration, Map<String, Object> newlyReadConfiguration) {
        logger.entry(configurationLocation, configuration, newlyReadConfiguration);
        if (newlyReadConfiguration != null) {
            if (configuration == null) {
                logger.debug("No existing configuration has been passed, returning the newly read configuration as it is");
                return logger.exit(newlyReadConfiguration);
            } else {
                logger.debug("Merging the configuration");
                return logger.exit(Utils.deepMerge(configuration, newlyReadConfiguration));
            }
        } else {
            logger.debug("No new configuration was read while processing {}", configurationLocation);
            return logger.exit(configuration);
        }
    }

}
