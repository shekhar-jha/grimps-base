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
 * Identifies whether a service can be configured by passing configuration. Applicable configuration will be injected
 * into the implementing class. This interface if implemented by services will allow service manager to configure the
 * service during initialization process.
 * In case the initialization process can not locate any associated configuration, this method may not be called on the
 * service.
 */
public interface Configurable {

    /**
     * Set the applicable configuration.
     *
     * @param configuration The applicable configuration if available, null otherwise.
     */
    void configure(Configuration configuration);

}
