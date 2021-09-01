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
package org.eclipse.jifa.master;

public interface Constant extends org.eclipse.jifa.common.Constant {
    /**
     * TOP CONFIG KEY
     */
    String HTTP_VERTICLE_CONFIG_KEY = "http-verticle-config";
    String WORKER_CONFIG_KEY = "worker-config";

    /**
     * Database
     */
    String DB_KEYWORD = "database";
    String DB_USERNAME = "username";
    String DB_PASSWORD = "password";
    String DB_URL = "url";
    String DB_DRIVER_CLASS_NAME = "com.mysql.jdbc.Driver";

    /**
     * User
     */
    String USER_ID_KEY = "id";
    String USER_NAME_KEY = "name";
    String USER_IS_ADMIN_KEY = "isAdmin";
    String USER_INFO_KEY = "userInfo";

    /**
     * JWT
     */
    String JWT_SUBJECT = "Grace Master";
    String JWT_ISSUER = "Grace Master";
    String JWT_ALGORITHM_HS256 = "HS256";
    String JWT_ALGORITHM_HS256_PUBLIC_KEY = "Grace Master Public Key";
    int JWT_EXPIRES_IN_MINUTES = 7 * 24 * 60; // seven days

    /**
     * BASE URL
     */
    String BASE = "/jifa-api";
    String AUTH = "/auth";
    String USER_INFO = "/userInfo";

    /**
     * FILE URL
     */
    String FILES = "/files";
    String FILE = "/file";
    String PUBLIC_KEY = "/publicKey";

    String FILE_SET_SHARED = "/file/setShared";
    String FILE_UNSET_SHARED = "/file/unsetShared";
    String FILE_DELETE = "/file/delete";
    String FILE_SYNC = "/file/sync";
    String TRANSFER_BY_URL = "/file/transferByURL";
    String TRANSFER_BY_SCP = "/file/transferBySCP";
    String TRANSFER_BY_OSS = "/file/transferByOSS";
    String TRANSFER_PROGRESS = "/file/transferProgress";

    String FILE_UPDATE_DISPLAY_NAME = "/file/updateDisplayName";

    String UPLOAD_TO_OSS = "/file/uploadToOSS";
    String UPLOAD_TO_OSS_PROGRESS = "/file/uploadToOSSProgress";

    /**
     * JOB URL
     */
    String PENDING_JOBS = "/pendingJobs";

    /**
     * HEAP DUMP URL
     */
    String HEAP_DUMP_RELEASE = "/heap-dump/:file/release";
    String HEAP_DUMP_COMMON = "/heap-dump/:file/*";

    /**
     * USER WORKER URL
     */
    String ADD_USER_WORKER = "/userWorker/add";
    String DELETE_USER_WORKER = "/userWorker/delete";
    String CHECK_USER_WORKER = "/userWorker/check";
    String UPDATE_USER_WORKER_USER_IDS = "/userWorker/updateUserIds";
    String ENABLE_USER_WORKER = "/userWorker/enable";
    String DISABLE_USER_WORKER = "/userWorker/disable";
    String UPDATE_USER_WORKER_USER_IDS_AND_STATE = "/userWorker/updateUserIdsAndState";
    String QUERY_ALL_USER_WORKERS = "/userWorkers";

    /**
     * WORKER URL
     */
    String START_WORKER = "/worker/startWorker";
    String STOP_WORKER = "/worker/stopWorker";
    String START_WORKER_DONE = "/worker/startWorkerDone";

    /**
     * ADMIN URL
     */
    String ADD_ADMIN = "/admin/add";
    String QUERY_ALL_ADMIN = "/admins";

    /**
     * MISC
     */
    String USERNAME = "username";
    String PASSWORD = "password";
    String PORT = "port";
    String PING = "/system/ping";

    /**
     * DEV
     */
    String LOCAL_HOST = "localhost";

    /**
     * K8S CLOUD CONFIG
     */
    String K8S_KEYWORD = "k8s-config";
    String K8S_NAMESPACE = "namespace";
    String K8S_WORKER_IMAGE = "worker-image";
    String K8S_MINIMAL_MEM_REQ = "minimal-mem-req";

    static String uri(String suffix) {
        return BASE + suffix;
    }
}
