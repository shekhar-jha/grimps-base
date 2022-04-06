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
import org.grimps.base.utils.Utils;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import java.util.*;

/**
 * Feature is a functionality provided by the service. This allows services to distinguish mandatory and optional capabilities.
 * If a service supports concept of feature, it should implement {@link FeaturedService}.
 * <p>
 * The feature has a name and associated Feature class ({@link Feature#getFeatureClass()}. A combination of the two can
 * be used to define feature at following levels
 * <ol>
 * <li><b>Application</b> - If no feature class is specified (or {@link #Feature(String, Class, Boolean) no name is specified})
 * the feature is expected to be provided by non-specified service.
 * </li>
 * <li><b>Service Definition/Interface Class</b> - If service Class is specified as feature class, the feature is expected to be provided by
 * service implementation.</li>
 * <li><b>Service Implementation Class</b> - If service implementation is specified as feature class, the feature is
 * specific to service implementation. </li>
 * </ol>
 *
 * @param <T> Data type of class that implements the feature. In case there is no associated feature class, use Object
 * @see FeaturedService
 */
public class Feature<T> {

    private static final XLogger logger = XLoggerFactory.getXLogger(Feature.class);
    private static final Map<String, Feature> featureMap = new HashMap<>();

    private final Class<T> featureClass;
    private final String name;
    private final String identifier;

    /**
     * Define a new feature.
     *
     * @see #Feature(String, Class, Boolean)
     */
    public Feature() {
        this((String) null);
    }

    /**
     * Define a new feature with specific name.
     *
     * @param name Name of new feature
     * @see #Feature(String, Class, Boolean)
     */
    public Feature(String name) {
        this(name, null);
    }

    /**
     * Define a new feature for the given class.
     *
     * @param featureClass Class associated with feature.
     * @see #Feature(String, Class, Boolean)
     */
    public Feature(Class<T> featureClass) {
        this(null, featureClass);
    }

    /**
     * Define a new feature for given name and class
     *
     * @param name         Name of the feature
     * @param featureClass Class associated with feature.
     * @see #Feature(String, Class, Boolean)
     */
    public Feature(String name, Class<T> featureClass) {
        this(name, featureClass, null);
    }

    /**
     * Define a new feature for given name and class and, if requested, register it.
     *
     * @param name         Name of feature. If null it defaults to Name of class
     * @param featureClass Class that this feature is associated to.
     * @param register     Whether the feature should be registered or not.
     *                     If specified as null, feature is registered.
     * @see #register(Feature)
     */
    public Feature(String name, Class<T> featureClass, Boolean register) {
        if (name == null)
            this.name = this.getClass().getName();
        else
            this.name = name;
        this.featureClass = featureClass;
        this.identifier = generateIdentifier(this.name, this.featureClass);
        if (register == null || register)
            register(this);
    }

    private static String generateIdentifier(String name, Class featureClass) {
        return name + "[ " + (featureClass == null ? "" : featureClass.getName()) + " ]";
    }

    /**
     * Registers the given feature and does not ignore error.
     *
     * @param feature feature to be registered.
     * @throws ValidationException In case of any error.
     * @see #register(Feature, boolean)
     */
    public static void register(Feature feature) throws ValidationException {
        register(feature, false);
    }

    /**
     * Register an given feature. Depending on ignoreError flag, it will throw appropriate exception.
     *
     * @param feature     Feature to register.
     * @param ignoreError If true does not throw exception in case of any validation errors.
     * @throws ValidationException Error codes
     *                             <ol><li>{@link ValidationException.VALIDATION_ERROR_CODE#REQUIRED} if feature is not passed.</li>
     *                             <li>{@link ValidationException.VALIDATION_ERROR_CODE#NOT_UNIQUE} if feature is already registered.</li>
     *                             </ol>
     */
    public static void register(Feature feature, boolean ignoreError) throws ValidationException {
        if (feature == null)
            if (ignoreError)
                return;
            else
                throw new ValidationException("Feature", ValidationException.VALIDATION_ERROR_CODE.REQUIRED, "No feature was provided for registration.");
        Feature existingFeature;
        if (featureMap.containsKey(feature.identifier) && (existingFeature = featureMap.get(feature.identifier)) != null
                && !existingFeature.equals(feature)) {
            String errorMessage = "An existing feature with matching name "
                    + existingFeature.name + " for class " + existingFeature.featureClass + " has already been registered. Can not register " + feature;
            if (!ignoreError) {
                throw new ValidationException("Feature", ValidationException.VALIDATION_ERROR_CODE.NOT_UNIQUE, errorMessage);
            } else {
                logger.warn(errorMessage);
                return;
            }
        }
        logger.debug("Registering Feature {} ({}", feature);
        featureMap.put(feature.identifier, feature);
    }

    /**
     * Returns a list of features associated with given name and class. Please note that this feature
     * with given name registered at application, service definition (using interface implemented by class)
     * and service instance level
     *
     * @param name           Name of the class
     * @param classToAnalyse Class for which analysis must be performed.
     * @return A list of features that is associated with given class.
     * @throws ValidationException with error code {@link ValidationException.VALIDATION_ERROR_CODE#REQUIRED} if name is not provided.
     */
    public static List<Feature> getFeatures(String name, Class classToAnalyse) {
        logger.entry(name, classToAnalyse);
        if (Utils.isEmpty(name))
            throw new ValidationException("Name", ValidationException.VALIDATION_ERROR_CODE.REQUIRED, "Must provide name of the feature to retrieve associated feature list.");
        List<Feature> supportedFeatures = new ArrayList<>();
        Feature feature = featureMap.get(generateIdentifier(name, null));
        if (feature != null)
            supportedFeatures.add(feature);
        if (classToAnalyse != null) {
            Set<Class<?>> associatedClasses = Utils.getAssociatedClasses(classToAnalyse);
            for (Class<?> associatedClass : associatedClasses) {
                String identifier = generateIdentifier(name, associatedClass);
                if (featureMap.containsKey(identifier)) {
                    Feature classFeature = featureMap.get(identifier);
                    supportedFeatures.add(classFeature);
                }
            }
        }
        return logger.exit(supportedFeatures);
    }

    /**
     * Removes the given feature from feature registration.
     *
     * @param feature Feature to be un-registered.
     * @return true if removed a registered feature false otherwise
     */
    public static boolean unregister(Feature feature) {
        if (feature != null && featureMap.containsKey(feature.identifier)) {
            featureMap.remove(feature.identifier);
            return true;
        }
        return false;
    }

    /**
     * Name of the feature.
     *
     * @return Name of feature
     */
    public String getName() {
        return name;
    }

    /**
     * Class associated with this feature.
     *
     * @return Feature class
     */
    public Class<T> getFeatureClass() {
        return featureClass;
    }

    @Override
    public String toString() {
        return identifier;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof Feature) {
            Feature other = (Feature) object;
            if (this.name.equalsIgnoreCase(other.name) &&
                    (this.featureClass == other.featureClass ||
                            (this.featureClass != null && this.featureClass.equals(other.featureClass)))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return identifier.hashCode();
    }
}
