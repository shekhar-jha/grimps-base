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

/**
 * Defines the criteria that is used to build search filter.
 */
public class SearchCriteria {

    private Object argument1;
    private OPERATOR operation;
    private Object argument2;
    private CRITERIA_TYPE type;

    /**
     * Defines search criteria for performing unary operation (like {@link OPERATOR#PRESENT} on attribute.
     *
     * @param attribute Name of the attribute on which operator must be applied.
     * @param operator  Operator to be applied
     */
    public SearchCriteria(String attribute, OPERATOR operator) {
        this.type = CRITERIA_TYPE.ATTRIBUTE_OPERATORS;
        this.argument1 = attribute;
        this.operation = operator;
    }

    public SearchCriteria(String attribute, String operator) {
        this(attribute, OPERATOR.valueOf(operator));
    }

    /**
     * Defines typical attribute / operator/ value search filter.
     *
     * @param attribute Name of attribute
     * @param operator  Operator to be applied
     * @param value     Value to compare
     */
    public SearchCriteria(String attribute, OPERATOR operator, Object value) {
        this.type = CRITERIA_TYPE.ATTRIBUTE_OPERATORS;
        this.argument1 = attribute;
        this.operation = operator;
        this.argument2 = value;
    }

    public SearchCriteria(String attribute, String operator, Object value) {
        this(attribute, OPERATOR.valueOf(operator), value);
    }

    /**
     * Defines a unary operation (like {@link OPERATOR#NOT}) on search criteria
     *
     * @param searchCriteria Search criteria to operate on
     * @param operator       Operator
     */
    public SearchCriteria(SearchCriteria searchCriteria, OPERATOR operator) {
        this.type = CRITERIA_TYPE.LOGICAL_OPERATORS;
        this.argument1 = searchCriteria;
        this.operation = operator;
    }

    public SearchCriteria(SearchCriteria searchCriteria, String operator) {
        this(searchCriteria, OPERATOR.valueOf(operator));
    }

    /**
     * Defines a way to combine multiple search criteria using an operator
     *
     * @param searchCriteria
     * @param operator
     * @param searchCriteria2
     */
    public SearchCriteria(SearchCriteria searchCriteria, OPERATOR operator, SearchCriteria searchCriteria2) {
        this.type = CRITERIA_TYPE.LOGICAL_OPERATORS;
        this.argument1 = searchCriteria;
        this.operation = operator;
        this.argument2 = searchCriteria2;
    }

    public SearchCriteria(SearchCriteria searchCriteria, String operator, SearchCriteria searchCriteria2) {
        this(searchCriteria, OPERATOR.valueOf(operator), searchCriteria2);
    }

    public SearchCriteria and(SearchCriteria searchCriteria) {
        return new SearchCriteria(this, OPERATOR.AND, searchCriteria);
    }

    public SearchCriteria and(String attribute, OPERATOR operation, Object value) {
        return new SearchCriteria(this, OPERATOR.AND, new SearchCriteria(attribute, operation, value));
    }

    public SearchCriteria and(String attribute, String operation, Object value) {
        return new SearchCriteria(this, OPERATOR.AND, new SearchCriteria(attribute, OPERATOR.valueOf(operation), value));
    }

    public SearchCriteria or(SearchCriteria searchCriteria) {
        return new SearchCriteria(this, OPERATOR.OR, searchCriteria);
    }

    public SearchCriteria or(String attribute, OPERATOR operation, Object value) {
        return new SearchCriteria(this, OPERATOR.OR, new SearchCriteria(attribute, operation, value));
    }

    public SearchCriteria or(String attribute, String operation, Object value) {
        return new SearchCriteria(this, OPERATOR.OR, new SearchCriteria(attribute, OPERATOR.valueOf(operation), value));
    }

    public SearchCriteria not() {
        return new SearchCriteria(this, OPERATOR.NOT);
    }

    /**
     * Returns whether the search criteria is valid.
     *
     * @return true if search criteria is valid, false otherwise
     */
    public boolean valid() {
        switch (type) {
            case LOGICAL_OPERATORS:
                if ((!(argument1 instanceof SearchCriteria)) || (!(argument2 instanceof SearchCriteria)))
                    return false;
                switch (operation) {
                    case AND:
                    case OR:
                        return argument1 != null && argument2 != null;
                    case NOT:
                        return argument1 != null && argument2 == null;
                    default:
                        return false;
                }
            case ATTRIBUTE_OPERATORS:
                if (argument1 == null)
                    return false;
                return !(operation != OPERATOR.PRESENT && argument2 == null);
        }
        return false;
    }

    public Object getArgument1() {
        return argument1;
    }

    public Object getArgument2() {
        return argument2;
    }

    public OPERATOR getOperation() {
        return operation;
    }

    public enum OPERATOR {
        EQUAL, NOT_EQUAL, CONTAINS, STARTS_WITH,
        ENDS_WITH, PRESENT, GREATER_THAN, GREATHER_THAN_OR_EQUALS, LESS_THAN, LESS_THAN_OR_EQUALS,
        AND, OR, NOT
    }

    private enum CRITERIA_TYPE {ATTRIBUTE_OPERATORS, LOGICAL_OPERATORS, GROUPING_OPERATORS}

}
