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

package org.grimps.service.manager;

import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import java.io.Serializable;
import java.util.Map;

public abstract class Context implements Serializable {

    private static transient final XLogger logger = XLoggerFactory.getXLogger(Context.class);
    protected static transient ThreadLocal<Context> threadLocal = new ThreadLocal<>();

    public static Context getContext() {
        logger.entry();
        Context context = threadLocal.get();
        return logger.exit(context);
    }

    public abstract ServiceManager getServiceManager();

    public abstract Map<String, Object> getRequestContext();

    public abstract boolean isDebugEnabled();

    public enum ID {SESSION_ID, USER_ID, REQUEST_ID, LOCALE, REQUEST_URL, PASSWORD}
}
