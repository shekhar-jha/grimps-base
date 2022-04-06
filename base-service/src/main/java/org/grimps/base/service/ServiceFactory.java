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

import org.grimps.base.config.Configuration;
import org.grimps.base.utils.Utils;

import java.util.Set;

/**
 * Interface implemented by factory that manages services.
 *
 * @param <T> Data type of service being managed by the service factory.
 */
public interface ServiceFactory<T> {

    /**
     * Returns the details of service being managed by the service factory
     *
     * @return Details of service being managed.
     */
    ServiceDetail getServiceDetail();

    /**
     * Features supported by the service
     *
     * @return Set of features supported. If no feature is supported, empty Set must be returned.
     */
    Set<? extends Feature> getSupportedFeatures();

    /**
     * Whether the service managed by the service factory compatible with given version
     *
     * @param version The version that service is compatible with
     * @return true if provided version is compatible, false otherwise. If version is null or empty, false must be returned.
     */
    boolean isCompatible(String version);

    /**
     * Return the class of service being managed by this service factory. Please note that this function should return value immediately after
     * creation of an object and before lifecycle and configuration events, if applicable, are executed.
     * The returned value should never be null.
     *
     * @return Class that this service factory manages. This should never be null.
     */
    Class<T> getServiceClass();

    /**
     * Create a new instance of service using the given configuration.
     *
     * @param configuration Configuration to use for creating the new service.
     * @return New instance of the service managed by the factory.
     */
    <A extends T> A create(Configuration configuration);

    /**
     * Destroy the service passed and release any associated resources. In case service factory can not handle the service
     * passed, no exception may be thrown.
     *
     * @param service The service created earlier by factory that needs to be deposed.
     */
    <A extends T> void dispose(A service);

    /**
     * The name and version details about the service.
     */
    class ServiceDetail {

        public static final String ATTR_NAME = "name";
        public static final String ATTR_VERSION = "version";

        private String detail = "notavailable:1.0";
        private String name;
        private String version;

        public ServiceDetail(String name, String version) {
            this.detail = name + ":" + version;
            this.name = name;
            this.version = version;
        }

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }

        @Override
        public String toString() {
            return detail;
        }

        @Override
        public int hashCode() {
            return detail.hashCode();
        }

        @Override
        public boolean equals(Object object) {
            if (object instanceof ServiceDetail) {
                return Utils.equals(name, ((ServiceDetail) object).name) && Utils.equals(version, ((ServiceDetail) object).version);
            } else {
                return false;
            }
        }

    }

}
