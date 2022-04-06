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

import org.grimps.service.config.internal.plugins.ConfigurationFileLocationPlugin;
import org.grimps.service.config.internal.plugins.SystemPropertyFileLocation;

import java.net.URL;
import java.util.List;

/**
 * This interface defines the methods that a file location plugin must implement. The file location plugin implementation
 * is used by the system to identify the various locations from which the configuration details must be read.
 * The file must be in standard JSON format to be integrated in to the configuration.
 *
 * @see ReadOnlyFileConfigurationManagerPlugin
 * @see ConfigurationFileLocationPlugin
 * @see SystemPropertyFileLocation
 */
public interface FileLocationPlugin {

    /**
     * Returns a list of URLs representing the configuration file to be read.
     *
     * @return A list of URL to be read. Empty list or null in case implementation can not provide any valid location.
     */
    List<URL> getFileLocations();

}
