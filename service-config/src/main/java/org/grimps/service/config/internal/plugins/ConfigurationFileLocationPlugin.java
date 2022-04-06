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

package org.grimps.service.config.internal.plugins;

import org.grimps.base.config.Configurable;
import org.grimps.base.config.Configuration;
import org.grimps.base.utils.Utils;
import org.grimps.service.config.plugins.FileLocationPlugin;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ConfigurationFileLocationPlugin implements Configurable, FileLocationPlugin {

    public static final String CONFIG_FILE_LOCATION_PARAMETER_NAME = "config-location";
    private static final XLogger logger = XLoggerFactory.getXLogger(ConfigurationFileLocationPlugin.class);
    private Configuration configuration;

    @Override
    public void configure(Configuration configuration) {
        logger.entry(configuration);
        logger.exit();
    }

    @Override
    public List<URL> getFileLocations() {
        logger.entry();
        List<URL> readFromLocations = new ArrayList<>();
        if (configuration != null) {
            String configurationLocation = configuration.getProperty(CONFIG_FILE_LOCATION_PARAMETER_NAME, String.class);
            if (!Utils.isEmpty(configurationLocation)) {
                readFromLocations.addAll(Utils.loadResources(configurationLocation));
            } else {
                logger.debug("No configuration resource location is specified using parameter {}", CONFIG_FILE_LOCATION_PARAMETER_NAME);
            }
        }
        return logger.exit(readFromLocations);
    }
}
