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

package org.grimps.base.service;

import org.grimps.base.BaseException;

/**
 * Exception to indicate service management related errors.
 */
public class ServiceException extends BaseException {


    public ServiceException(ServiceErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public ServiceException(ServiceErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public enum ServiceErrorCode implements BaseException.ErrorCode {
        /**
         * Service is not available.
         */
        not_available("service_not_available"),
        /**
         * Service/Feature has not been implemented.
         */
        not_implemented("service_not_implemented"),
        /**
         * Service has not been initialized successfully.
         */
        already_initialized("already_initialized"),
        /**
         * Service has not been initialized successfully.
         */
        no_init("not_initialized"),
        /**
         * Service initialization failed
         */
        initialization_error("init_failed");

        private final String errorCode;


        ServiceErrorCode(String errorCode) {
            this.errorCode = errorCode;
        }

        public String getErrorCode() {
            return errorCode;
        }

    }

}

