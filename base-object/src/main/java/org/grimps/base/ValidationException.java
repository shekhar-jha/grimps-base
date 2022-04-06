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

package org.grimps.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Exception to indicate that validation failed. This exception has additional ability to capture multiple validation
 * errors in single exception to allow for propagation of multiple validation errors (e.g. form validation) in single
 * exception
 */
public class ValidationException extends BaseException {


    private List<ValidationError> errors = new ArrayList<ValidationError>();
    private List<ValidationError> externalList = Collections.unmodifiableList(errors);

    public ValidationException(String message) {
        super(VALIDATION_ERROR_CODE.ERROR_CODE, message);
    }

    public ValidationException(String message, Throwable exception) {
        super(VALIDATION_ERROR_CODE.ERROR_CODE, message, exception);
    }

    public ValidationException(VALIDATION_ERROR_CODE error_code, String message) {
        super(error_code, message);
    }

    public ValidationException(VALIDATION_ERROR_CODE error_code, String message, Throwable exception) {
        super(error_code, message, exception);
    }

    public ValidationException(String fieldName, VALIDATION_ERROR_CODE error_code, String message) {
        super(error_code, message);
        addValidationError(fieldName, error_code, message);
    }

    public ValidationException(String fieldName, VALIDATION_ERROR_CODE error_code, String message, Throwable exception) {
        super(error_code, message, exception);
        addValidationError(fieldName, error_code, message, exception);
    }

    public ValidationException addValidationError(String fieldName, VALIDATION_ERROR_CODE error_code, String message) {
        addValidationError(fieldName, error_code, message, null);
        return this;
    }

    public ValidationException addValidationError(String fieldName, VALIDATION_ERROR_CODE error_code, String message, Throwable exception) {
        errors.add(new ValidationError(fieldName, error_code, message, exception));
        return this;
    }

    public ValidationException addValidationErrors(Collection<ValidationError> validationErrorCollection) {
        errors.addAll(validationErrorCollection);
        return this;
    }

    public ValidationException addValidationError(ValidationException exception) {
        addValidationErrors(exception.errors);
        return this;
    }

    public List<ValidationError> getErrors() {
        return externalList;
    }

    @Override
    public String toString() {
        if (errors.size() == 0) {
            return super.toString();
        } else {
            StringBuilder msg = new StringBuilder();
            msg.append(super.toString());
            if (!errors.isEmpty()) {
                msg.append(LINE_DELIMITER);
                msg.append("Errors : ");
                msg.append(LINE_DELIMITER);
                for (int i = 0; i < errors.size(); ++i) {
                    ValidationException.ValidationError error = errors.get(i);
                    msg.append(i + 1);
                    msg.append(". [");
                    msg.append(error.field_name);
                    msg.append("] ");
                    msg.append(error.error_code);
                    msg.append(" : ");
                    msg.append(error.message);
                    if (i + 1 < errors.size()) {
                        msg.append(LINE_DELIMITER);
                    }
                }
            }
            return msg.toString();
        }
    }

    public static class VALIDATION_ERROR_CODE implements ErrorCode {

        public static final VALIDATION_ERROR_CODE ERROR_CODE = new VALIDATION_ERROR_CODE("validation_failed");
        public static final VALIDATION_ERROR_CODE REQUIRED = new VALIDATION_ERROR_CODE("required");
        public static final VALIDATION_ERROR_CODE INVALID_LENGTH = new VALIDATION_ERROR_CODE("invalid_length");
        public static final VALIDATION_ERROR_CODE INVALID_VALUE = new VALIDATION_ERROR_CODE("value_not_allowed");
        public static final VALIDATION_ERROR_CODE NOT_UNIQUE = new VALIDATION_ERROR_CODE("unique");
        public static final VALIDATION_ERROR_CODE INVALID_TYPE = new VALIDATION_ERROR_CODE("invalid_data_type");

        private final String errorCode;

        VALIDATION_ERROR_CODE(String code) {
            this.errorCode = code;
        }

        public String toString() {
            return errorCode;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }


    /**
     * Validation results.
     */
    public static class ValidationError {
        public final String field_name;
        public final String error_code;
        public final String message;
        public final Throwable cause;

        public ValidationError(String fieldName, VALIDATION_ERROR_CODE errorCode, String message) {
            this.field_name = fieldName;
            this.error_code = errorCode != null ? errorCode.getErrorCode() : UNKNOWN_ERROR_CODE;
            this.message = message;
            this.cause = null;
        }

        public ValidationError(String fieldName, VALIDATION_ERROR_CODE errorCode, String message, Throwable cause) {
            this.field_name = fieldName;
            this.error_code = errorCode != null ? errorCode.getErrorCode() : UNKNOWN_ERROR_CODE;
            this.message = message;
            this.cause = cause;
        }

        @Override
        public String toString() {
            return "ValidationError (field: " + field_name + ", error code: " + error_code + ", message: " + message + ")";
        }
    }
}
