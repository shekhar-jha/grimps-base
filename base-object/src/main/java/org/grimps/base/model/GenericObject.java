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

package org.grimps.base.model;

import org.grimps.base.utils.Utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Generic Object is the base class for any data model being built on grimps framework. This base object defines a
 * simple implementation to have common attribute definition and storage framework which can be relied upon by various
 * services that can consume this object. All the service implementations within the grimps framework are expected to be
 * able to consume GenericObject.
 */
public class GenericObject {

    /**
     * Underlying construct to store object's data. This should not be accessed directly by most of underlying
     * implementation. The accessor methods have been provided to handle to operate on this attribute.
     *
     * @see #assignValue(ATTRIBUTE, Object)
     * @see #retrieveValue(String)
     */
    protected Map attributes;

    public GenericObject() {
        attributes = new HashMap<>();
    }

    public GenericObject(Map attributes) {
        this.attributes = ((attributes == null) ? new HashMap() : new HashMap(attributes));
    }

    /**
     * Returns the data associated with the object. Even though exposed as public method, most of the implementations
     * should not have any specific need to use this method.
     *
     * @return Map of attribute key and data associated with this object.
     */
    public Map attributes() {
        return attributes;
    }

    protected <T> void assignValue(ATTRIBUTE<T> attributeName, T value) {
        this.attributes.put(attributeName, value);
    }

    protected <T> T retrieveValue(ATTRIBUTE<T> attributeName) {
        if (attributeName == null || attributeName.attributeType == null)
            return null;
        return attributeName.attributeType.cast(attributes.get(attributeName));
    }

    protected <T> boolean contains(ATTRIBUTE<T> attributeName) {
        return attributes.containsKey(attributeName);
    }

    /**
     * Returns value of attribute matching given attribute name.
     *
     * @param attributeName Name of the attribute to be retrieved.
     * @return value of attribute with matching name.
     */
    public Object retrieveValue(String attributeName) {
        if (attributeName == null)
            return null;
        return attributes.get(new ATTRIBUTE(attributeName, Object.class));
    }

    /**
     * Class that defines an attribute of data model class.
     *
     * @param <T> Data Type of the attribute.
     */
    public static class ATTRIBUTE<T> {
        public final Class<T> attributeType;
        public final String name;
        private final String representation;

        public ATTRIBUTE(String name, Class<T> attributeType) {
            this.name = name;
            this.attributeType = attributeType;
            representation = name + " (" + attributeType + ")";
        }

        @Override
        public boolean equals(Object input) {
            if (input instanceof ATTRIBUTE)
                return Utils.equals(((ATTRIBUTE) input).name, name);
            if (input instanceof String)
                return Utils.equals(input, name);
            return false;
        }

        @Override
        public int hashCode() {
            return (name == null) ? 0 : name.hashCode();
        }

        public String toString() {
            return representation;
        }
    }

    /**
     * A base class that can be used to define reference to another object.
     */
    public static class Reference extends GenericObject {
        public static final ATTRIBUTE<String> VALUE = new ATTRIBUTE<>("value", String.class);
        public static final ATTRIBUTE<String> DISPLAY = new ATTRIBUTE<>("display", String.class);

        public Reference(String value, String display) {
            assignValue(VALUE, value);
            assignValue(DISPLAY, display);
        }

        public String getValue() {
            return retrieveValue(VALUE);
        }

        public String getDisplay() {
            return retrieveValue(DISPLAY);
        }

    }
}
