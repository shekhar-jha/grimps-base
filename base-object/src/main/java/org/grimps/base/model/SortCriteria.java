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
import org.grimps.base.utils.Utils;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

/**
 * Definition of Sort criteria containing attribute and order of sort.
 */
public class SortCriteria {

    private static final XLogger logger = XLoggerFactory.getXLogger(SortCriteria.class);

    private String sortBy;
    private SORT_ORDER sort_order;
    private int hashCode;

    public SortCriteria() {
    }

    public SortCriteria(String sortBy, SORT_ORDER sort_order) {
        setSortBy(sortBy);
        setSortOrderValue(sort_order);
    }

    public SORT_ORDER getSortOrder() {
        return sort_order;
    }

    public SortCriteria setSortOrder(String sortOrder) {
        if (sortOrder != null) {
            if ((sortOrder.equalsIgnoreCase(SORT_ORDER.ascending.name()) || sortOrder.equalsIgnoreCase(SORT_ORDER.descending.name())))
                setSortOrderValue(Utils.getEnumValueOf(SORT_ORDER.class, sortOrder));
            else
                throw logger.throwing(new ValidationException("sortOrder", ValidationException.VALIDATION_ERROR_CODE.INVALID_VALUE
                        , "sortOrder " + sortOrder + " is not supported. Supported values " + SORT_ORDER.ascending.name() + ", " + SORT_ORDER.descending.name()));
        }
        return this;
    }

    public SortCriteria setSortOrderValue(SORT_ORDER sortOrder) {
        this.sort_order = sortOrder;
        computeHashCode();
        return this;
    }

    public String getSortBy() {
        return sortBy;
    }

    public SortCriteria setSortBy(String sortBy) {
        this.sortBy = sortBy;
        computeHashCode();
        return this;
    }

    private void computeHashCode() {
        hashCode = ("" + getSortBy() + getSortOrder()).hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof SortCriteria)) return false;
        SortCriteria sortCriteria = (SortCriteria) object;
        return sortCriteria.getSortOrder() == sortCriteria.getSortOrder() &&
                sortCriteria.getSortBy() != null &&
                sortCriteria.getSortBy().equalsIgnoreCase(getSortBy());
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    public enum SORT_ORDER {ascending, descending}
}
