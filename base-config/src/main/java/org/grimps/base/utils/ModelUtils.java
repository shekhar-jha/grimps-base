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

package org.grimps.base.utils;

import org.grimps.base.config.Configuration;
import org.grimps.base.model.PageTracker;
import org.grimps.base.model.SearchQuery;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

public class ModelUtils {
    private static final XLogger logger = XLoggerFactory.getXLogger(ModelUtils.class);

    public static PageTracker.PageIndexBased extractPageTracker(Configuration searchQueryConfiguration, SearchQuery searchQuery, int SEARCH_START_ROW, int defaultSearchResultCount) {
        logger.entry(searchQueryConfiguration, searchQuery, SEARCH_START_ROW, defaultSearchResultCount);
        PageTracker.PageIndexBased pageTracker = new PageTracker.PageIndexBased();
        int defaultCount;
        if (searchQueryConfiguration != null) {
            if (searchQueryConfiguration.containsProperty("default-search-result-count")) {
                defaultCount = searchQueryConfiguration.getProperty("default-search-result-count");
            } else {
                defaultCount = defaultSearchResultCount;
            }
            if (searchQueryConfiguration.containsProperty("disable-pagination") && (Boolean) searchQueryConfiguration.getProperty("disable-pagination")) {
                return logger.exit(null);
            }
        } else {
            defaultCount = defaultSearchResultCount;
        }
        if (searchQuery != null && searchQuery.getPageTracker() != null && searchQuery.getPageTracker() instanceof PageTracker.PageIndexBased) {
            PageTracker.PageIndexBased extractedPageTracker = (PageTracker.PageIndexBased) searchQuery.getPageTracker();
            if (extractedPageTracker.getStartIndex() < SEARCH_START_ROW)
                pageTracker.setStartIndex(SEARCH_START_ROW);
            else
                pageTracker.setStartIndex(extractedPageTracker.getStartIndex());
            if (extractedPageTracker.getCount() == -1) {
                pageTracker.setCount(defaultCount);
            } else
                pageTracker.setCount(extractedPageTracker.getCount());
        } else {
            pageTracker.setStartIndex(SEARCH_START_ROW);
            pageTracker.setCount(defaultCount);
        }
        return logger.exit(pageTracker);
    }
}
