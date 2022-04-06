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

package org.grimps.base.utils;

import org.grimps.base.config.Configuration;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

/**
 * Common Utility methods to handle typical use-cases.
 */
public class ConfigUtils {

    private static final XLogger logger = XLoggerFactory.getXLogger(ConfigUtils.class);

    /**
     * Returns the value of a property by looking it up from given configuration set in specified sequence.
     *
     * @param propertyName          Name of the property that needs to be looked up
     * @param configurationSequence Sequence of configuration from which property would be resolved.
     * @param <T>                   Type of value that must be returned.
     * @return Value of property.
     * @see #readProperty(String, Object, Configuration...)
     */
    public static <T> T readProperty(String propertyName, Configuration... configurationSequence) {
        logger.entry(propertyName, configurationSequence);
        return logger.exit((T) readProperty(propertyName, null, configurationSequence));
    }

    /**
     * Returns the value of a property by looking it up from given configuration set in specified sequence. If property
     * could not be located, {@code defaultValue} is returned.
     *
     * @param propertyName          Name of the property that needs to be looked up
     * @param defaultValue          Value to be returned in case none of the configuration contain given property name.
     * @param configurationSequence Sequence of configuration from which property would be resolved.
     * @param <T>                   Type of value that must be returned.
     * @return Value of property.
     */
    public static <T> T readProperty(String propertyName, T defaultValue, Configuration... configurationSequence) {
        logger.entry(propertyName, configurationSequence);
        if (propertyName == null)
            return logger.exit(defaultValue);
        if (configurationSequence == null || configurationSequence.length == 0)
            return logger.exit(defaultValue);
        T applicableResult = defaultValue;
        for (Configuration configuration : configurationSequence) {
            if (configuration != null && configuration.containsProperty(propertyName)) {
                logger.trace("Located property {} in configuration {}", propertyName, configuration);
                applicableResult = configuration.getProperty(propertyName);
                logger.trace("Value {}", applicableResult);
                break;
            }
        }
        return logger.exit(applicableResult);
    }

}
