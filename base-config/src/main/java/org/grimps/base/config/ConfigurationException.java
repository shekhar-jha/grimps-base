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

package org.grimps.base.config;

import org.grimps.base.BaseException;

/**
 * Exception triggered in case of an error will reading or setting configuration.
 */
public class ConfigurationException extends BaseException {

    public ConfigurationException(ErrorCodes errorCode, String message) {
        super(errorCode, message);
    }

    public ConfigurationException(ErrorCodes errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    /**
     * Configuration related error codes.
     */
    public static class ErrorCodes implements ErrorCode {

        /**
         * In case the configuration is not available
         */
        public static final ErrorCodes NotConfigured = new ErrorCodes("not_configured");
        /**
         * In case a required configuration property is missing
         */
        public static final ErrorCodes MissingConfiguration = new ErrorCodes("required_configuration_missing");
        /**
         * In case the configuration being set is not editable.
         */
        public static final ErrorCodes ReadOnly = new ErrorCodes("read_only_configuration");

        /**
         * In case an optional feature is not supported.
         *
         * @see ManagedConfiguration#refresh() Refresh Feature.
         */
        public static final ErrorCodes NotSupported = new ErrorCodes("not_supported");

        /**
         * In case an operation fails.
         *
         * @see ManagedConfiguration#refresh() Refresh Feature.
         */
        public static final ErrorCodes OperationFailed = new ErrorCodes("operation_failed");

        /**
         * In case the configuration is invalid.
         *
         */
        public static final ErrorCodes Invalid = new ErrorCodes("invalid");

        String errorCode;

        ErrorCodes(String errorCode) {
            this.errorCode = errorCode;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }

}
