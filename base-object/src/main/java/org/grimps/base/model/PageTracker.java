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

import org.grimps.base.ValidationException;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines the interface that defines the details associated with a page of result. This is used to track details about
 * particular page of result set. The framework provides out of box implementation to handle two types of standard
 * pagination approach used
 * <ol>
 * <li>{@link SearchCookieBased Search Cookie} - This approach uses a obfuscated handle or cursor generated and provided by server
 * to track current page of a result set being managed. API caller can pass this cookie information to server to request next
 * or previous page of result set.</li>
 * <li>{@link PageIndexBased Page Number} - This approach allows end user to request a specific page number of the result set.</li>
 * </ol>
 *
 * @see Result
 */
public interface PageTracker {

    /**
     * Ensures that the given page tracker is valid.
     *
     * @return Empty List if no validation error otherwise a list of errors.
     * @see ValidationException.ValidationError
     */
    List<ValidationException.ValidationError> validate();

    /**
     * This page trackers can be used by result set provided to support cursor based pagination.
     */
    class SearchCookieBased extends GenericObject implements PageTracker {

        public static ATTRIBUTE<String> SEARCH_COOKIE = new ATTRIBUTE<>("searchCookie", String.class);
        public static ATTRIBUTE<Integer> SEARCH_COUNT = new ATTRIBUTE<>("count", Integer.class);

        public SearchCookieBased() {
            super();
        }

        public SearchCookieBased(String searchCookie, int count) {
            super();
            setSearchCookie(searchCookie);
            setCount(count);
        }

        /**
         * Returns the cursor value as String. Please encode the string (preferably Base64 URL) to ensure that it can be
         * transmitted.
         *
         * @return Cursor value if set, null otherwise.
         */
        public String getSearchCookie() {
            return retrieveValue(SEARCH_COOKIE);
        }

        /**
         * Sets the cursor value.
         *
         * @param searchCookie Cursor value
         */
        public SearchCookieBased setSearchCookie(String searchCookie) {
            assignValue(SEARCH_COOKIE, searchCookie);
            return this;
        }

        /**
         * Return the number of items to retrieve.
         *
         * @return Number of items if set, -1 otherwise.
         */
        public int getCount() {
            if (!contains(SEARCH_COUNT))
                return -1;
            return retrieveValue(SEARCH_COUNT);
        }

        /**
         * Set the count of items in page.
         *
         * @param count Number of items to retrieve
         */
        public SearchCookieBased setCount(int count) {
            assignValue(SEARCH_COUNT, count);
            return this;
        }

        /**
         * Implementation of cursor validation.
         *
         * @return List of errors associated with page tracker. Empty list if no error was identified.
         */
        public List<ValidationException.ValidationError> validate() {
            List<ValidationException.ValidationError> validationErrors = new ArrayList<>();
            return validationErrors;
        }

    }

    /**
     * An implementation of page number based pagination approach that tracks pagination state using start index and count
     * of items in a page. The start index defines the row number from which applicable page starts from and count defines
     * number of items to be/being retrieved in the current page.
     */
    class PageIndexBased extends GenericObject implements PageTracker {
        public static GenericObject.ATTRIBUTE<Integer> SEARCH_START_INDEX = new GenericObject.ATTRIBUTE<>("startIndex", Integer.class);
        public static GenericObject.ATTRIBUTE<Integer> SEARCH_COUNT = new GenericObject.ATTRIBUTE<>("count", Integer.class);

        public PageIndexBased() {
            super();
        }

        public PageIndexBased(int startIndex, int count) {
            super();
            setStartIndex(startIndex);
            setCount(count);
        }

        /**
         * Return the start index of page.
         *
         * @return If start index is set, return the value otherwise return -1.
         */
        public int getStartIndex() {
            if (!contains(SEARCH_START_INDEX))
                return -1;
            return retrieveValue(SEARCH_START_INDEX);
        }

        public PageIndexBased setStartIndex(int startIndex) {
            assignValue(SEARCH_START_INDEX, startIndex);
            return this;
        }

        /**
         * Return the number of items in the page.
         *
         * @return If set returns the set value, -1 otherwise.
         */
        public int getCount() {
            if (!contains(SEARCH_COUNT))
                return -1;
            return retrieveValue(SEARCH_COUNT);
        }

        public PageIndexBased setCount(int count) {
            assignValue(SEARCH_COUNT, count);
            return this;
        }

        /**
         * Ensures that the page index tracker fulfills the following criteria
         * <ol>
         * <li>Start Index - Ensure that start index is greater than 1</li>
         * <li>Count - Ensure that count is greater than 0</li>
         * </ol>
         *
         * @return list of validation errors based on validation identified above.
         */
        public List<ValidationException.ValidationError> validate() {
            List<ValidationException.ValidationError> validationErrors = new ArrayList<>();
            if (getStartIndex() < 1)
                validationErrors.add(new ValidationException.ValidationError(SEARCH_START_INDEX.name, ValidationException.VALIDATION_ERROR_CODE.INVALID_VALUE, "Starting index of requested result can not be less than 1."));
            if (getCount() < 0)
                validationErrors.add(new ValidationException.ValidationError(SEARCH_COUNT.name, ValidationException.VALIDATION_ERROR_CODE.INVALID_VALUE, "The number of records in requested result can not be less than 0."));
            return validationErrors;
        }

    }
}
