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

/**
 * The base class for all the exceptions. The basic runtime exception is enhanced with a concept of error code.
 */
public class BaseException extends RuntimeException {

    public static final String UNKNOWN_ERROR_CODE = "unknown_error";
    public static final String LINE_DELIMITER = System.lineSeparator();
    private String errorCode;

    public BaseException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode != null ? errorCode.getErrorCode() : UNKNOWN_ERROR_CODE;
    }

    public BaseException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode != null ? errorCode.getErrorCode() : UNKNOWN_ERROR_CODE;
    }

    public String getErrorCode() {
        return this.errorCode;
    }

    /**
     * Generic error codes.
     */
    public enum ERROR_CODES implements ErrorCode {
        SUCCESS("success"), UNKNOWN_ERROR(UNKNOWN_ERROR_CODE);

        private String errorCode;

        ERROR_CODES(String errorCode) {
            this.errorCode = errorCode;
        }

        @Override
        public String getErrorCode() {
            return errorCode;
        }
    }

    /**
     * Base interface that all ErroCodes must implement.
     */
    public interface ErrorCode {
        String getErrorCode();
    }

}
