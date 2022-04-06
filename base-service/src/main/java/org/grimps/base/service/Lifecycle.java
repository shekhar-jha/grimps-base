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

package org.grimps.base.service;

/**
 * Defines the contract for service lifecycle.
 *
 * @see LifecycleManager
 */
public interface Lifecycle {

    /**
     * Initialize the service
     */
    void initialize();

    /**
     * Returns whether the service is initialized.
     *
     * @return true if service was initialized, false otherwise.
     */
    boolean isInitialized();

    /**
     * Destroy the service and release all allocated resources.
     */
    void destroy();
}
