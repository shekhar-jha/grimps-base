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

import org.grimps.base.config.ConfigurationException;
import org.grimps.base.service.Feature;
import org.grimps.service.config.ConfigurationManagerPlugin;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import java.util.Map;

/**
 * Base class that can be extended by Configuration Manager plugin implementation that do not support
 * persisting configuration changes.
 */
public abstract class ConfigurationManagerPluginReadOnly implements ConfigurationManagerPlugin {

    private static final XLogger logger = XLoggerFactory.getXLogger(ConfigurationManagerPluginReadOnly.class);

    /**
     * Return false for all the features.
     *
     * @param feature Feature supported.
     * @return false
     */
    @Override
    public boolean supports(Feature<ConfigurationManagerPlugin> feature) {
        logger.entry(feature);
        return logger.exit(false);
    }

    /**
     * Throw error if invoked.
     *
     * @param configuration Configuration to be saved.
     * @throws ConfigurationException - with error code {@link ConfigurationException.ErrorCodes#NotSupported}
     */
    @Override
    public void save(Map<String, Object> configuration) {
        logger.entry(configuration);
        throw logger.throwing(new ConfigurationException(ConfigurationException.ErrorCodes.NotSupported, "The read only configuration manager " + this + " can not save configuration."));
    }

    /**
     * Throw error if invoked.
     *
     * @param configuration Configuration to be saved.
     * @throws ConfigurationException - with error code {@link ConfigurationException.ErrorCodes#NotSupported}
     */
    @Override
    public void update(Map<String, Object> configuration) {
        logger.entry(configuration);
        throw logger.throwing(new ConfigurationException(ConfigurationException.ErrorCodes.NotSupported, "The read only configuration manager " + this + " can not save updated configuration."));
    }
}
