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

public class SecurityException extends BaseException {
    public SecurityException(SECURITY_ERROR_CODE error_code, String message) {
        super(error_code, message);
    }

    public SecurityException(SECURITY_ERROR_CODE error_code, String message, Throwable exception) {
        super(error_code, message, exception);
    }

    public static class SECURITY_ERROR_CODE implements ErrorCode {

        public static final SECURITY_ERROR_CODE ERROR_CODE = new SECURITY_ERROR_CODE("security_error");
        public static final SECURITY_ERROR_CODE AUTHENTICATION_REQUIRED = new SECURITY_ERROR_CODE("authentication_required");
        public static final SECURITY_ERROR_CODE NOT_AUTHORIZED = new SECURITY_ERROR_CODE("not_authorized");

        private final String errorCode;

        SECURITY_ERROR_CODE(String code) {
            this.errorCode = code;
        }

        public String toString() {
            return errorCode;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }

}
