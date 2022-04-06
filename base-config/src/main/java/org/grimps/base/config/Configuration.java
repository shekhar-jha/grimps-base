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

package org.grimps.base.config;

import java.util.Set;

/**
 * Configuration details for a particular service or class. The configuration is defined as one or more name and value pairs.
 * The property name is expected to be alpha-numeric but the implementations are free to support other character set. The values
 * may be any data type supported by underlying configuration implementation including another configuration.
 */
public interface Configuration {

    /**
     * Validate whether this configurations the given name as property. All the configuration implementations must support
     * the basic identification of alpha-numeric string as property name. Other interpretation of property name (e.g. XPATH,
     * Expression language, etc) may be supported by the configuration.
     *
     * @param name The name of the property that needs to be validated. It is typically alpha-numeric for maximum
     *             compatibility. Other formats for the name may be supported by various configuration implementation.
     * @return true if the configuration contains the requested property, false otherwise.
     */
    boolean containsProperty(String name);


    /**
     * Returns a list of properties supported by configuration
     *
     * @return Unique names of the property that can be resolved by configuration object.
     */
    Set<String> getPropertyNames();

    /**
     * Returns the property value if available.
     *
     * @param propertyName Name of the property that needs to be resolved to a value.
     * @param <T>          Data type of the value being retrieved.
     * @return Value of the property if available, null otherwise.
     */
    <T> T getProperty(String propertyName);

    /**
     * Returns the property value if available.
     *
     * @param propertyName Name of the property that needs to be resolved to a value.
     * @param <T>          Data type of the value being retrieved.
     * @param valueClass   Data type of the value being retrieved.
     * @return Value of the property if available, null otherwise.
     */
    <T> T getProperty(String propertyName, Class<T> valueClass);

    /**
     * Returns the property value if available, otherwise return {@code defaultValue}.
     *
     * @param propertyName Name of the property that needs to be resolved to a value.
     * @param defaultValue Value to be returned in case property is not set.
     * @param <T>          Data type of the value being retrieved.
     * @return Value of the property if available, {@code defaultValue} otherwise.
     */
    <T> T getProperty(String propertyName, T defaultValue);

    /**
     * Returns the property value if available, otherwise return {@code defaultValue}.
     *
     * @param propertyName Name of the property that needs to be resolved to a value.
     * @param defaultValue Value to be returned in case property is not set.
     * @param valueClass   Data type of the value being retrieved.
     * @param <T>          Data type of the value being retrieved.
     * @return Value of the property if available, {@code defaultValue} otherwise.
     */
    <T> T getProperty(String propertyName, T defaultValue, Class<T> valueClass);

    /**
     * Returns the value of given prefix as configuration. This method allows the implementation to interpret the value
     * and convert that into Configuration. For example a Map value for a property may be interpreted as standard name-value
     * configuration and the implementation may return it a configuration.
     * In case the given prefix contains a value but that can not be parsed as configuration, null must be returned. The
     * caller may use other methods to parse and validate the property value.
     *
     * @param prefix Name of the property that should be parsed as configuration
     * @return Configuration if prefix value can be converted in to configuration, null otherwise.
     */
    Configuration subset(String prefix);
}
