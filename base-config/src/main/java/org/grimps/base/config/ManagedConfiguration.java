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

/**
 * Interface implemented by configuration that can be managed. It defines the ability to
 * <ol>
 * <li>Update property</li>
 * <li>Refresh the configuration - that can trigger reload of the configuration from the underlying source.</li>
 * </ol>
 */
public interface ManagedConfiguration extends Configuration {

    <T> void setProperty(String propertyName, T newValue);

    /**
     * Refresh the configuration. Typically may involve reloading the original configuration from underlying source.
     * This is an optional functionality that may not be implemented by underlying configuration implementation.
     */
    void refresh();

    /**
     * Identifies whether the configuration changes are persistent. If
     * configuration is persistent then {@link #save()} can be called safely.
     *
     * @return true if configuration can persist the changes made, false otherwise.
     */
    boolean isPersistent();

    /**
     * Save the changes made to configuration.
     * This is an optional functionality that may not be implemented by underlying configuration implementation. If
     * not implemented, should throw an error.
     *
     * @throws ConfigurationException In case of any error while saving or if feature is not supported.
     * @see ConfigurationException.ErrorCodes#NotSupported
     * @see ConfigurationException.ErrorCodes#OperationFailed
     */
    void save() throws ConfigurationException;

}
