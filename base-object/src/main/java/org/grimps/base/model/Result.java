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

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * This class is used to represent a list of values with ability to store additional details like total count,
 * list of column names and pagination information.
 *
 * @param <T> Type of Object being returned in result.
 */
public class Result<T> {

    private int totalCount = -1;
    private Set<String> attributeNames;
    private List<T> data;
    private PageTracker paginationInfo;

    public Result(List<T> data) {
        this.data = data;
    }

    public Result(Result result, List<T> data) {
        this.totalCount = result.totalCount;
        this.attributeNames = result.attributeNames;
        this.data = data;
        this.paginationInfo = result.paginationInfo;
    }

    public Result(Set<String> columns, List<T> data) {
        this.data = Collections.unmodifiableList(data);
        this.attributeNames = Collections.unmodifiableSet(columns);
    }

    /**
     * Returns the total number of records associated with this result set.
     *
     * @return Total number of records if set, -1 otherwise.
     */
    public int getTotalCount() {
        return totalCount;
    }

    /**
     * Set the total number of records associated with this result set.
     *
     * @param count Total number of records.
     */
    public Result setTotalCount(int count) {
        this.totalCount = count;
        return this;
    }

    /**
     * Returns a set of attribute names associated with object being returned.
     *
     * @return Set of names if set, null otherwise.
     */
    public Set<String> getAttributeNames() {
        return attributeNames;
    }

    /**
     * Returns the data set associated with this result.
     *
     * @return Data if set null otherwise.
     */
    public List<T> getData() {
        return data;
    }

    /**
     * Returns pagination info associated with result.
     *
     * @return Pagination info if set, null otherwise
     */
    public PageTracker getPaginationInfo() {
        return paginationInfo;
    }

    /**
     * Set pagination info associated with result
     *
     * @param paginationInfo Pagination information for result
     */
    public Result setPaginationInfo(PageTracker paginationInfo) {
        this.paginationInfo = paginationInfo;
        return this;
    }
}
