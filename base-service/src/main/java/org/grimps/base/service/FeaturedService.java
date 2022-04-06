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

import org.grimps.base.ValidationException;

import java.util.Set;

/**
 * Interface implemented by services that support features.
 *
 * @see Feature
 */
public interface FeaturedService extends Service {

    /**
     * Return list of features supported by the service.
     *
     * @return List of features supported. In case no feature is supported, empty set must be returned.
     */
    Set<Feature> getSupportedFeatures();

    /**
     * Whether the service supports the given feature.
     *
     * @param feature feature to be supported.
     * @return true if feature is supported otherwise false.
     * @throws org.grimps.base.ValidationException In case feature is invalid.
     */
    boolean supported(Feature feature) throws ValidationException;
}
