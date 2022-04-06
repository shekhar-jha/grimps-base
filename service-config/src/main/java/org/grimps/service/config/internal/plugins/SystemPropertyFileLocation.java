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

import org.grimps.base.utils.Utils;
import org.grimps.service.config.plugins.FileLocationPlugin;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * This implementation returns the location of configuration files based on environment variable and system property
 * variable {@link #CONFIG_FILE_LOCATION_PARAMETER_NAME} specified during application startup.
 */
public class SystemPropertyFileLocation implements FileLocationPlugin {

    public static final String CONFIG_FILE_LOCATION_PARAMETER_NAME = "CONFIG_LOCATION";
    private static final XLogger logger = XLoggerFactory.getXLogger(SystemPropertyFileLocation.class);

    @Override
    public List<URL> getFileLocations() {
        logger.entry();
        List<URL> readFromLocations = new ArrayList<>();
        String fileLocationNameParameterValue = System.getProperty(CONFIG_FILE_LOCATION_PARAMETER_NAME);
        logger.trace("System property {}={}", CONFIG_FILE_LOCATION_PARAMETER_NAME, fileLocationNameParameterValue);
        if (!Utils.isEmpty(fileLocationNameParameterValue)) {
            readFromLocations.addAll(Utils.loadResources(fileLocationNameParameterValue));
        }
        fileLocationNameParameterValue = System.getenv(CONFIG_FILE_LOCATION_PARAMETER_NAME);
        logger.trace("System Environment variable {}={}", CONFIG_FILE_LOCATION_PARAMETER_NAME, fileLocationNameParameterValue);
        if (!Utils.isEmpty(fileLocationNameParameterValue)) {
            readFromLocations.addAll(Utils.loadResources(fileLocationNameParameterValue));
        }
        return logger.exit(readFromLocations);
    }
}
