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

import org.grimps.base.service.Feature;

import java.util.Map;

/**
 * This interface must be implemented by plugins that can be loaded by Configuration Service to build the
 * configuration during startup.
 * <p><b>LifeCycle</b></p>
 * Please note that due to the stage at which these plugins are initialized, very limited services or configuration may be available
 * (depending on configuration plugin initialization sequence). The plugin should be manage configuration from pre-defined locations
 * like file at pre-defined locations, database using pre-defined connection pools (e.g. JEE Datasource).
 * <p><b>Plugin Features</b></p>
 * The plugin must support one of the following feature
 * <ol>
 * <li>{@link #CONFIGURATION_READ_WRITE Read-write} configuration managers provide configuration from it's repository and
 * allow updates to the same. This may include configuration manager that store configuration to database or file that can be easily updated.</li>
 * <li>{@link #CONFIGURATION_READ_ONLY Read Only} configuration managers provide configuration from it's repository in but do not allow saving. This may include configuration manager that
 * read configuration from configuration files embedded in jar files.</li>
 * <li>{@link #CONFIGURATION_AUDITOR Auditor} configuration managers allow saving changes to the configuration without changing the original configuration. These configuration managers
 * may be used by other configuration to save and retrieve configuration changes without changing configuration store.</li>
 * </ol>
 * <p><b>Configuration Format</b></p>
 * The configuration may be managed in one of the following formats.
 * <ol>
 * <li>Standard format - This defines the configuration as a one or more combination of service and associated
 * configuration (i.e. a map). The service itself is a map of name-value pairs. The values can be
 * <ol>
 * <li>null</li>
 * <li>String</li>
 * <li>Number</li>
 * <li>Boolean</li>
 * <li>List/Array of other values</li>
 * <li>Object/Map which can be extracted as configuration using {@link org.grimps.base.config.Configuration#subset(String) subset}</li>
 * </ol>
 * </li>
 * <li> Update format - This defines a way to update a specific node of configuration tree.
 * The configuration must contain name of the service to be updated as key and a List of changes.
 * Each of the change, must be a Map containing the following entries.<br/>
 * <ol>
 * <li><b>property</b> - Defines the name of the property to be updated. In order to update the "property2"
 * in following example
 * <pre>
 *  {
 *      "property1" : {
 *          "property2" :"value2"
 *      },
 *      "property3" : "value3"
 * }
 * </pre>
 * the property name can be specified as
 * <ul>
 * <li>["property1", "property2"] - List of property names in specific sequence to reference specified property.</li>
 * <li>"property1.property2"</li>
 * </ul>
 * </li>
 * <li><b>value</b> - The new value that should replace existing value. In case the existing value needs to be removed,
 * null must be set.
 * </li>
 * </li>
 * </ol>
 *
 * @see ConfigurationService
 */
public interface ConfigurationManagerPlugin {

    /**
     * This feature defines whether Configuration Manager Plugin has capability to save the given configuration. This
     * allows caller to ensure that it passes the complete configuration (including updates) for saving.
     */
    Feature<ConfigurationManagerPlugin> CONFIGURATION_SAVE = new Feature<>("configuration_save", ConfigurationManagerPlugin.class);
    /**
     * This feature defines whether implementing Configuration Manager Plugin has capability to save updates. This allows caller to
     * pass just the updates instead of complete configuration.
     */
    Feature<ConfigurationManagerPlugin> CONFIGURATION_UPDATE = new Feature<>("configuration_update", ConfigurationManagerPlugin.class);

    /**
     * Returns the changes and addition to application's configuration in the supported format.
     *
     * @return The configuration changes/updates to be made if available, null in case the configuration manager can not
     * provide configuration.
     */
    Map<String, Object> getConfiguration();

    /**
     * Returns whether a given feature is supported by the plugin.
     *
     * @param feature Feature supported.
     * @return true if feature is supported, false otherwise
     * @see #CONFIGURATION_SAVE
     * @see #CONFIGURATION_UPDATE
     */
    boolean supports(Feature<ConfigurationManagerPlugin> feature);

    /**
     * Saves the given configuration. This configuration should be complete configuration and should not be limited to
     * updates.
     *
     * @param configuration Configuration to be saved.
     */
    void save(Map<String, Object> configuration);

    /**
     * Saves the configuration updates provided. This configuration should be updates to the configuration returned earlier.
     *
     * @param configuration Configuration to be saved.
     */
    void update(Map<String, Object> configuration);

}
