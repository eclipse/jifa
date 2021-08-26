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
package org.eclipse.jifa.master.http;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Strings;
import org.eclipse.jifa.common.aux.ErrorCode;
import org.eclipse.jifa.master.Constant;
import org.eclipse.jifa.master.entity.File;
import org.eclipse.jifa.master.entity.Job;
import org.eclipse.jifa.master.entity.UserWorker;
import org.eclipse.jifa.master.entity.enums.JobState;
import org.eclipse.jifa.master.model.User;
import org.eclipse.jifa.master.vo.PendingJob;

import static org.eclipse.jifa.common.util.Assertion.ASSERT;

class BaseRoute implements Constant {

    static final String SEP = "-";

    void assertFileAvailable(File file) {
        ASSERT.isTrue(file.found(), ErrorCode.FILE_DOES_NOT_EXIST);
        ASSERT.isTrue(!file.isDeleted(), ErrorCode.FILE_HAS_BEEN_DELETED);
    }

    void assertJobExist(Job job) {
        ASSERT.isTrue(job.found(), ErrorCode.JOB_DOES_NOT_EXIST);
    }

    void checkPermission(User user, File file) {
        ASSERT.isTrue(file.isShared() || file.getUserId().equals(user.getId()) || user.isAdmin(),
                      ErrorCode.FORBIDDEN);
    }

    void checkDeletePermission(User user, File file) {
        ASSERT.isTrue(file.getUserId().equals(user.getId()) || user.isAdmin(), ErrorCode.FORBIDDEN);
    }

    void checkPermission(User user, Job job) {
        ASSERT.isTrue(job.getUserId().equals(user.getId()) || user.isAdmin(), ErrorCode.FORBIDDEN);
    }

    void checkPermission(User user, UserWorker userWorker) {

        ASSERT.isTrue(userWorker.getUserIds().contains(user.getId()) || user.isAdmin(), ErrorCode.FORBIDDEN);
    }

    void assertJobInProgress(Job job) {
        ASSERT.isTrue(job.getState() != JobState.PENDING, ErrorCode.PENDING_JOB,
                      () -> JSON.toJSONString(new PendingJob(job)))
              .isTrue(job.getState() == JobState.IN_PROGRESS, ErrorCode.SANITY_CHECK);
    }

    String buildFileName(String userId, String originalName) {
        ASSERT.isTrue(!Strings.isNullOrEmpty(userId), ErrorCode.ILLEGAL_ARGUMENT);
        ASSERT.isTrue(!Strings.isNullOrEmpty(originalName), ErrorCode.ILLEGAL_ARGUMENT);
        return userId + SEP + System.currentTimeMillis() + SEP + originalName;
    }

    String extractOriginalName(String path) {
        String name = path.substring(path.lastIndexOf(java.io.File.separatorChar) + 1);

        if (name.contains("?")) {
            name = name.substring(0, name.indexOf("?"));
        }

        name = name.replaceAll("[%\\\\& ]", "_");

        if (name.length() == 0) {
            name = System.currentTimeMillis() + "";
        }

        return name;
    }
}
