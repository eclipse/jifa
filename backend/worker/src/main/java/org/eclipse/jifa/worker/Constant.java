/********************************************************************************
 * Copyright (c) 2020, 2021 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.jifa.worker;

public interface Constant extends org.eclipse.jifa.common.Constant {

    interface Misc {
        String VERTX_CONFIG_PROP = "jifa.vertx.config";
        String WORKER_CONFIG_PROP = "jifa.worker.config";
        String DEFAULT_VERTX_CONFIG_FILE = "vertx-config.json";
        String DEFAULT_WORKER_CONFIG_FILE = "worker-config.json";
        String DEFAULT_HOST = "0.0.0.0";
        String WEB_ROOT_KEY = "jifa.webroot";
    }

    interface Heap {
        String TOTAL_SIZE_KEY = "totalSize";
        String SHALLOW_HEAP_KEY = "shallowHeap";
        String RETAINED_HEAP_KEY = "retainedHeap";
    }

    interface File {
        String INFO_FILE_SUFFIX = "-info.json";
    }

    interface ConfigKey {
        String BASIC_AUTH = "basicAuth";
        String ENABLED = "enabled";
        String WORKSPACE = "workspace";
        String API_PREFIX = "api.prefix";
        String SERVER_HOST_KEY = "server.host";
        String SERVER_PORT_KEY = "server.port";
        String USERNAME = "username";
        String PASSWORD = "password";
        String HOOKS_NAME_KEY = "hooks.className";
        String SERVER_UPLOAD_DIR_KEY = "server.uploadDir";
    }

    interface CacheConfig {
        String CACHE_CONFIG = "cacheConfig";
        String EXPIRE_AFTER_ACCESS = "expireAfterAccess";
        String EXPIRE_AFTER_ACCESS_TIME_UNIT = "expireAfterAccessTimeUnit";
    }
}
