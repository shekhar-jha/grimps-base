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
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines the search query. A search query consists of
 * <ol>
 * <li>Search Query ID - This allows caller to use pre-defined search criteria with parameter values passed for substitution.</li>
 * <li>Search Criteria - This allows caller to specify filter criteria to be used as part of search query.</li>
 * <li>Search Parameters - A map of name and values that can be used to customize search criteria</li>
 * <li>Attributes - A list of attributes that must be returned in result</li>
 * <li>Excluded Attributes - A list of attributes that must be excluded from result set. It is recommended that
 * if same attribute is identified in both attribute list and excluded attribute list, the attribute should not be part of
 * result. In case an attribute is specified in excluded list but not defined in attribute list, no error must be raised.</li>
 * <li>Sort Criteria - A list of criteria by which result set must be sorted</li>
 * <li>Page Tracker - Detail about the page of search result being requested.</li>
 * <p>
 * </ol>
 * Please note that the service implementations may support limited features of Search Query.
 */
public class SearchQuery extends GenericObject {

    public static ATTRIBUTE<String> SEARCH_QUERY_ID = new ATTRIBUTE<>("searchQueryId", String.class);
    public static ATTRIBUTE<Map<String, Object>> SEARCH_QUERY_PARAMETERS = new ATTRIBUTE("searchParameters", Map.class);
    public static ATTRIBUTE<List<String>> SEARCH_QUERY_ATTRIBUTES_TO_RETURN = new ATTRIBUTE("attributes", List.class);
    public static ATTRIBUTE<List<String>> SEARCH_QUERY_EXCLUDED_ATTRIBUTES = new ATTRIBUTE("excludedAttributes", List.class);
    public static ATTRIBUTE<SearchCriteria> SEARCH_CRITERIA = new ATTRIBUTE<>("searchCriteria", SearchCriteria.class);
    public static ATTRIBUTE<List<SortCriteria>> SEARCH_SORT_ORDERS = new ATTRIBUTE("sortCriteria", List.class);
    public static ATTRIBUTE<PageTracker> SEARCH_RESULT_PAGE = new ATTRIBUTE<>("pageTracker", PageTracker.class);
    private static XLogger logger = XLoggerFactory.getXLogger(SearchQuery.class);

    public SearchQuery() {
        super();
    }

    public SearchQuery(String id, Map<String, Object> data) {
        super();
        setId(id);
        //TODO: This implementation has been changed compared to previous implementation. Need to understand impact.
        setSearchParameters(data);
    }

    public SearchQuery(Map<String, Object> data) {
        super(data);
    }

    public String getId() {
        return retrieveValue(SEARCH_QUERY_ID);
    }

    public SearchQuery setId(String id) {
        assignValue(SEARCH_QUERY_ID, id);
        return this;
    }

    public List<String> getAttributes() {
        return retrieveValue(SEARCH_QUERY_ATTRIBUTES_TO_RETURN);
    }

    public SearchQuery setAttributes(List<String> returnAttributes) {
        assignValue(SEARCH_QUERY_ATTRIBUTES_TO_RETURN, returnAttributes);
        return this;
    }

    public List<String> getExcludedAttributes() {
        return retrieveValue(SEARCH_QUERY_EXCLUDED_ATTRIBUTES);
    }

    public SearchQuery setExcludedAttributes(List<String> excludedAttributes) {
        assignValue(SEARCH_QUERY_EXCLUDED_ATTRIBUTES, excludedAttributes);
        return this;
    }

    public SearchCriteria getSearchCriteria() {
        return retrieveValue(SEARCH_CRITERIA);
    }

    public SearchQuery setSearchCriteria(SearchCriteria searchCriteria) {
        assignValue(SEARCH_CRITERIA, searchCriteria);
        return this;
    }

    public List<SortCriteria> getSortCriteria() {
        return retrieveValue(SEARCH_SORT_ORDERS);
    }

    public SearchQuery setSortCriteria(List<SortCriteria> sortCriteria) {
        assignValue(SEARCH_SORT_ORDERS, sortCriteria);
        return this;
    }

    public SearchQuery addSortCriteria(SortCriteria sortCriteria) {
        List<SortCriteria> currentSortCriteria = getSortCriteria();
        if (currentSortCriteria == null) {
            currentSortCriteria = new ArrayList<>();
            currentSortCriteria.add(sortCriteria);
            setSortCriteria(currentSortCriteria);
        } else {
            currentSortCriteria.add(sortCriteria);
        }
        return this;
    }

    public PageTracker getPageTracker() {
        return retrieveValue(SEARCH_RESULT_PAGE);
    }

    public SearchQuery setPageTracker(PageTracker pageTracker) {
        assignValue(SEARCH_RESULT_PAGE, pageTracker);
        return this;
    }

    public Map<String, Object> getSearchParameters() {
        return retrieveValue(SEARCH_QUERY_PARAMETERS);
    }

    public SearchQuery setSearchParameters(Map<String, Object> searchParameters) {
        assignValue(SEARCH_QUERY_PARAMETERS, searchParameters);
        return this;
    }

    public SearchQuery addSearchParameters(String parameterName, Object parameterValue) {
        Map<String, Object> searchParameters = getSearchParameters();
        if (searchParameters == null) {
            searchParameters = new HashMap<>();
            searchParameters.put(parameterName, parameterValue);
            assignValue(SEARCH_QUERY_PARAMETERS, searchParameters);
        } else {
            searchParameters.put(parameterName, parameterValue);
        }
        return this;
    }

    public List<ValidationException.ValidationError> validate() {
        logger.entry();
        List<ValidationException.ValidationError> errors = new ArrayList<>();
        if (getPageTracker() != null)
            errors.addAll(getPageTracker().validate());
        return logger.exit(errors);
    }

}
