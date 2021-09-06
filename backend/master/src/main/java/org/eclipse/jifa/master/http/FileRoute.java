/********************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
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
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jifa.common.ErrorCode;
import org.eclipse.jifa.common.JifaException;
import org.eclipse.jifa.common.enums.FileTransferState;
import org.eclipse.jifa.common.enums.FileType;
import org.eclipse.jifa.common.enums.ProgressState;
import org.eclipse.jifa.common.request.MakeHttpResponse;
import org.eclipse.jifa.common.util.FileUtil;
import org.eclipse.jifa.common.vo.FileInfo;
import org.eclipse.jifa.common.vo.PageView;
import org.eclipse.jifa.common.vo.TransferProgress;
import org.eclipse.jifa.common.vo.TransferringFile;
import org.eclipse.jifa.master.Constant;
import org.eclipse.jifa.master.entity.ExtendedFileInfo;
import org.eclipse.jifa.master.entity.FileRecord;
import org.eclipse.jifa.master.entity.Job;
import org.eclipse.jifa.master.entity.User;
import org.eclipse.jifa.master.entity.enums.Deleter;
import org.eclipse.jifa.master.entity.enums.JobType;
import org.eclipse.jifa.master.entity.enums.TransferWay;
import org.eclipse.jifa.master.service.FileService;
import org.eclipse.jifa.master.service.JobService;
import org.eclipse.jifa.master.service.ServiceCenter;
import org.eclipse.jifa.master.support.$;
import org.eclipse.jifa.master.support.WorkerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.eclipse.jifa.common.util.Assertion.ASSERT;

class FileRoute extends BaseRoute implements Constant {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileRoute.class.getName());

    private static String PUB_KEY = Constant.EMPTY_STRING;

    static {
        String path = System.getProperty("user.home") + java.io.File.separator + ".ssh" + java.io.File.separator +
                "jifa-ssh-key.pub";
        java.io.File file = new java.io.File(path);
        if (file.exists()) {
            PUB_KEY = FileUtil.content(file);
        } else {
            LOGGER.warn("SSH public key file {} doesn't exist", file.getAbsolutePath());
        }
    }

    private FileService fileService;

    private JobService jobService;

    private static ExtendedFileInfo buildFileInfo(FileRecord file) {
        ExtendedFileInfo info = new ExtendedFileInfo();
        info.setOriginalName(file.getOriginalName());
        info.setDisplayName(
                StringUtils.isBlank(file.getDisplayName()) ? file.getOriginalName() : file.getDisplayName());
        info.setName(file.getName());
        info.setType(file.getType());
        info.setSize(file.getSize());
        info.setTransferState(file.getTransferState());
        info.setShared(file.isShared());
        info.setDownloadable(false);
        info.setCreationTime(file.getCreationTime());
        info.setUserId(file.getUserId());
        return info;
    }

    void init(Vertx vertx, JsonObject config, Router apiRouter) {
        fileService = ServiceCenter.lookup(FileService.class);
        jobService = ServiceCenter.lookup(JobService.class);

        apiRouter.get().path(FILES).handler(ctx -> runHttpHandlerAsync(ctx, FileRoute.this::files));
        apiRouter.get().path(FILE).handler(ctx -> runHttpHandlerAsync(ctx, FileRoute.this::file));
        apiRouter.post().path(FILE_DELETE).handler(ctx -> runHttpHandlerAsync(ctx, FileRoute.this::delete));
        apiRouter.post().path(TRANSFER_BY_URL).handler(context -> runHttpHandlerAsync(context, context1 -> transfer(context1, TransferWay.URL)));
        apiRouter.post().path(TRANSFER_BY_SCP).handler(context -> runHttpHandlerAsync(context, context1 -> transfer(context1, TransferWay.SCP)));
        apiRouter.post().path(TRANSFER_BY_OSS).handler(context -> runHttpHandlerAsync(context, context1 -> transfer(context1, TransferWay.OSS)));

        apiRouter.get().path(TRANSFER_PROGRESS).handler(ctx -> runHttpHandlerAsync(ctx, FileRoute.this::fileTransportProgress));
        apiRouter.get().path(PUBLIC_KEY).handler(this::publicKey);

        apiRouter.post().path(FILE_SET_SHARED).handler(ctx -> runHttpHandlerAsync(ctx, FileRoute.this::setShared));
        apiRouter.post().path(FILE_UNSET_SHARED).handler(ctx -> runHttpHandlerAsync(ctx, FileRoute.this::unsetShared));

        apiRouter.post().path(FILE_UPDATE_DISPLAY_NAME).handler(ctx -> runHttpHandlerAsync(ctx, FileRoute.this::updateDisplayName));

        apiRouter.post().path(UPLOAD_TO_OSS).handler(ctx -> runHttpHandlerAsync(ctx, FileRoute.this::uploadToOSS));
        apiRouter.get().path(UPLOAD_TO_OSS_PROGRESS).handler(ctx -> runHttpHandlerAsync(ctx, FileRoute.this::uploadToOSSProgress));
    }

    @AsyncHttpHandler
    private void files(RoutingContext context) {
        String userId = context.<User>get(USER_INFO_KEY).getId();
        int page = Integer.parseInt(context.request().getParam(PAGE));
        int pageSize = Integer.parseInt(context.request().getParam(PAGE_SIZE));
        FileType type = FileType.valueOf(context.request().getParam(FILE_TYPE));
        String expected = context.request().getParam("expectedFilename");
        int count = fileService.count(userId, type, expected);

        List<FileRecord> fileRecords = fileService.files(userId, type, expected, page, pageSize);
        PageView<FileInfo> pv = new PageView<>();
        pv.setTotalSize(count);
        pv.setPage(page);
        pv.setPageSize(pageSize);
        pv.setData(fileRecords.stream().map(FileRoute::buildFileInfo).collect(Collectors.toList()));
        MakeHttpResponse.ok(context, fileRecords);
    }

    @AsyncHttpHandler
    private void file(RoutingContext context) {
        User user = context.get(USER_INFO_KEY);
        String name = context.request().getParam("name");
        FileRecord file = fileService.file(name);
        assertFileAvailable(file);
        checkPermission(user, file);
        ExtendedFileInfo info = buildFileInfo(file);
        MakeHttpResponse.ok(context, info);
    }

    @AsyncHttpHandler
    private void delete(RoutingContext context) {
        User user = context.get(USER_INFO_KEY);
        String name = context.request().getParam("name");

        FileRecord file = fileService.file(name);
        assertFileAvailable(file);
        checkDeletePermission(user, file);
        ASSERT.isTrue(file.getTransferState().isFinal());

        fileService.deleteFile(name,
                file.getUserId().equals(user.getId()) ?
                        Deleter.USER : Deleter.ADMIN);
        MakeHttpResponse.ok(context);
    }

    @AsyncHttpHandler
    private void transfer(RoutingContext context, TransferWay way) {
        String userId = context.<User>get(USER_INFO_KEY).getId();
        HttpServerRequest request = context.request();
        String[] paths = way.getPathKeys();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paths.length; i++) {
            sb.append(request.getParam(paths[i]));
            if (i != paths.length - 1) {
                sb.append("_");

            }
        }
        String origin = extractOriginalName(sb.toString());
        FileType type = FileType.valueOf(context.request().getParam("type"));

        String name = buildFileName(userId, origin);
        request.params().add("fileName", name);

        fileService.transfer(userId, type, origin, name, way, convert(request.params()));

        MakeHttpResponse.ok(context, new TransferringFile(name));
    }

    @AsyncHttpHandler
    private void fileTransportProgress(RoutingContext context) {
        String name = context.request().getParam("name");
        User user = context.get(USER_INFO_KEY);

        Job job = jobService.findActive(JobType.FILE_TRANSFER, name);
        if (job.notFound()) {
            FileRecord file = fileService.file(name);

            assertFileAvailable(file);
            checkPermission(user, file);
            MakeHttpResponse.ok(context, toProgress(file));
        } else {
            checkPermission(user, job);
            Future<HttpResponse<Buffer>> future = $.asyncVoid(WorkerClient::send, context.request(), job.getHostIP());
            HttpResponse<Buffer> resp = $.await(future);
            TransferProgress progress = JSON.parseObject(resp.bodyAsString(), TransferProgress.class);
            ProgressState progressState = progress.getState();
            if (progressState.isFinal()) {
                LOGGER.info("File transfer {} done, state is {}", name, progress.getState());
                FileTransferState transferState = FileTransferState.fromProgressState(progressState);
                fileService.transferDone(name, transferState, progress.getTotalSize());
                MakeHttpResponse.ok(context, progress);
            } else {
                MakeHttpResponse.ok(context, progress);
            }
        }
    }

    @AsyncHttpHandler
    private void uploadToOSS(RoutingContext context) {
        String name = context.request().getParam("srcName");
        ASSERT.isTrue(StringUtils.isNotBlank(name), ErrorCode.ILLEGAL_ARGUMENT, "srcName mustn't be empty");
        User user = context.get(USER_INFO_KEY);
        FileRecord file = fileService.file(name);
        assertFileAvailable(file);
        checkPermission(user, file);
        ASSERT.isTrue(file.transferred(), ErrorCode.NOT_TRANSFERRED);
        context.request().params().add("type", file.getType().name());
        Future<HttpResponse<Buffer>> future = $.asyncVoid(WorkerClient::send, context.request(), file.getHostIP());
        HttpResponse<Buffer> resp = $.await(future);
        MakeHttpResponse.ok(context, resp.statusCode(), resp.bodyAsString());
    }

    @AsyncHttpHandler
    private void uploadToOSSProgress(RoutingContext context) {
        String name = context.request().getParam("name");
        ASSERT.isTrue(StringUtils.isNotBlank(name), ErrorCode.ILLEGAL_ARGUMENT, "name mustn't be empty");
        User user = context.get(USER_INFO_KEY);
        FileRecord file = fileService.file(name);
        assertFileAvailable(file);
        checkPermission(user, file);
        ASSERT.isTrue(file.transferred(), ErrorCode.NOT_TRANSFERRED);
        Future<HttpResponse<Buffer>> future = $.asyncVoid(WorkerClient::send, context.request(), file.getHostIP());
        HttpResponse<Buffer> resp = $.await(future);
        MakeHttpResponse.ok(context, resp.statusCode(), resp.bodyAsString());
    }

    @AsyncHttpHandler
    private void setShared(RoutingContext context) {
        String name = context.request().getParam("name");
        User user = context.get(USER_INFO_KEY);
        FileRecord file = fileService.file(name);
        ASSERT.isTrue(file.found(), ErrorCode.FILE_DOES_NOT_EXIST);
        checkPermission(user, file);
        fileService.setShared(name);
        MakeHttpResponse.ok(context);
    }

    @AsyncHttpHandler
    private void updateDisplayName(RoutingContext context) {
        String name = context.request().getParam("name");
        String displayName = context.request().getParam("displayName");
        User user = context.get(USER_INFO_KEY);
        FileRecord file = fileService.file(name);
        ASSERT.isTrue(file.found(), ErrorCode.FILE_DOES_NOT_EXIST);
        checkPermission(user, file);
        fileService.updateDisplayName(name, displayName);
        MakeHttpResponse.ok(context);
    }

    private void publicKey(RoutingContext context) {
        MakeHttpResponse.ok(context, PUB_KEY);
    }

    @AsyncHttpHandler
    private void unsetShared(RoutingContext context) {
        MakeHttpResponse.fail(context, new JifaException(ErrorCode.UNSUPPORTED_OPERATION));
    }

    private String extractOriginalName(String path) {
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

    private Map<String, String> convert(MultiMap src) {
        Map<String, String> target = new HashMap<>();
        for (Map.Entry<String, String> entry : src) {
            target.put(entry.getKey(), entry.getValue());
        }
        return target;
    }

    private TransferProgress toProgress(FileRecord file) {
        FileTransferState transferState = file.getTransferState();
        ASSERT.isTrue(transferState.isFinal(), ErrorCode.SANITY_CHECK);
        TransferProgress progress = new TransferProgress();
        progress.setState(transferState.toProgressState());
        progress.setTotalSize(file.getSize());
        if (transferState == FileTransferState.SUCCESS) {
            progress.setPercent(1.0);
            progress.setTransferredSize(file.getSize());
        }
        return progress;
    }
}
