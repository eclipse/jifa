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
package org.eclipse.jifa.master.support;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.ext.web.client.HttpResponse;
import org.eclipse.jifa.common.ErrorCode;
import org.eclipse.jifa.common.enums.FileTransferState;
import org.eclipse.jifa.common.enums.ProgressState;
import org.eclipse.jifa.common.vo.TransferProgress;
import org.eclipse.jifa.master.Constant;
import org.eclipse.jifa.master.entity.FileRecord;
import org.eclipse.jifa.master.entity.Job;
import org.eclipse.jifa.master.entity.Master;
import org.eclipse.jifa.master.entity.Worker;
import org.eclipse.jifa.master.entity.enums.Deleter;
import org.eclipse.jifa.master.entity.enums.JobState;
import org.eclipse.jifa.master.entity.enums.JobType;
import org.eclipse.jifa.master.service.orm.*;
import org.eclipse.jifa.master.service.sql.*;
import org.eclipse.jifa.master.task.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.eclipse.jifa.master.Constant.LOCAL_HOST;
import static org.eclipse.jifa.master.Constant.uri;
import static org.eclipse.jifa.master.service.ServiceAssertion.SERVICE_ASSERT;
import static org.eclipse.jifa.master.service.orm.SQLHelper.makeSqlArgument;
import static org.eclipse.jifa.master.service.sql.JobSQL.*;
import static org.eclipse.jifa.master.support.WorkerClient.get;
import static org.eclipse.jifa.master.support.WorkerClient.post;

public class Pivot {

    private static final Logger LOGGER = LoggerFactory.getLogger(Pivot.class);

    private static final String JOB_LOCK_KEY = "MASTER-JOB-LOCK";

    private static final String PENDING_JOB_MAX_COUNT_KEY = "JOB-PENDING-MAX-COUNT";

    private static final String NOTIFY_WORKER_ACTION_DELAY_KEY = "JOB-NOTIFY-WORKER-ACTION-DELAY";

    private static final String TRIGGER_SCHEDULING_ACTION_DELAY = "JOB-TRIGGER-SCHEDULING-ACTION-DELAY";

    private static Pivot SINGLETON;

    private Master currentMaster;

    private JDBCClient dbClient;

    private Vertx vertx;

    private int pendingJobMaxCount;

    private long notifyWorkerActionDelay;

    private long triggerSchedulingActionDelay;

    private SchedulingTask schedulingTask;

    private Pivot() {
    }

    public static synchronized Pivot instance(Vertx vertx, JDBCClient dbClient) {
        if (SINGLETON != null) {
            return SINGLETON;
        }

        Pivot jm = new Pivot();
        jm.dbClient = dbClient;
        jm.vertx = vertx;

        try {

            jm.pendingJobMaxCount = ConfigHelper.getInt(jm.config(PENDING_JOB_MAX_COUNT_KEY));

            jm.notifyWorkerActionDelay = ConfigHelper.getLong(jm.config(NOTIFY_WORKER_ACTION_DELAY_KEY));

            jm.triggerSchedulingActionDelay = ConfigHelper.getLong(jm.config(TRIGGER_SCHEDULING_ACTION_DELAY));

            String ip =
                    org.eclipse.jifa.master.Master.DEV_MODE ? LOCAL_HOST : InetAddress.getLocalHost().getHostAddress();
            LOGGER.info("Current Master Host IP is {}", ip);


            jm.currentMaster = jm.ensureSqlConnection(conn -> {
                return jm.selectMaster(conn, ip);
            });

            if (jm.currentMaster.isLeader()) {
                new DiskCleaningTask(jm, vertx);
                new RetiringTask(jm, vertx);
                jm.schedulingTask = new SchedulingTask(jm, vertx);
                new TransferJobResultFillingTask(jm, vertx);
                new DiskUsageUpdatingTask(jm, vertx);
                new FileSyncTask(jm, vertx);
            }
        } catch (Throwable t) {
            LOGGER.error("Init job service error", t);
            System.exit(-1);
        }

        SINGLETON = jm;
        return jm;
    }

    private static Job buildPendingJob(String userId, String hostIP, JobType jobType, String target,
                                       String attachment, long estimatedLoad, boolean immediate) {
        Job job = new Job();
        job.setUserId(userId);
        job.setType(jobType);
        job.setTarget(target);
        job.setState(JobState.PENDING);
        job.setHostIP(hostIP);
        job.setAttachment(attachment);
        job.setEstimatedLoad(estimatedLoad);
        job.setImmediate(immediate);
        return job;
    }

    public JDBCClient dbClient() {
        return dbClient;
    }

    public void ensureSqlConnection(Consumer<SQLConnection> func) {
        ensureSqlConnection(conn -> {
            func.accept(conn);
            return null;
        });
    }

    public <T> T ensureSqlConnection(Function<SQLConnection, T> func) {
        SQLConnection conn = null;
        try {
            Future<SQLConnection> future = $.async(dbClient::getConnection);
            conn = $.await(future);
            return func.apply(conn);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void ensureTransaction(Consumer<SQLConnection> func, Consumer<Throwable> errHandler) {
        ensureTransaction(conn -> {
            func.accept(conn);
            return null;
        }, errHandler);
    }

    public <R> R ensureTransaction(Function<SQLConnection, R> func, Consumer<Throwable> errHandler) {
        return ensureSqlConnection(conn -> {
            try {
                Future<Void> future = $.async(conn::setAutoCommit, false);
                $.await(future);
            } catch (Throwable e) {
                LOGGER.error("Failed to disable auto commit, tx execution failure", e);
                return null;
            }
            // execute user defined function and try to commit
            R result;
            try {
                result = func.apply(conn);
                Future<Void> future = $.async(conn::commit);
                $.await(future);
                return result;
            } catch (Throwable e) {
                // failed to commit, try to rollback
                LOGGER.error("Can not commit, try to rollback...", e);
                try {
                    Future<Void> future = $.async(conn::rollback);
                    $.await(future);
                } catch (Throwable e1) {
                    LOGGER.error("Failed to rollback changes, database may corrupt", e1);
                } finally {
                    // then use error handler
                    try {
                        errHandler.accept(e);
                    } catch (Throwable e1) {
                        LOGGER.error("errHandler has yet another error", e1);
                    }
                }
                return null;
            } finally {
                // restore auto commit
                try {
                    Future<Void> future = $.async(conn::setAutoCommit, true);
                    $.await(future);
                } catch (Throwable e) {
                    LOGGER.error("Failed to enable auto commit", e);
                }
            }
        });
    }


    public Job allocate(String userId, String hostIP, JobType jobType, String target,
                        String attachment, long estimatedLoad, boolean immediate) {

        Job job = buildPendingJob(userId, hostIP, jobType, target, attachment, estimatedLoad, immediate);

        return doAllocate(job);
    }

    private Job doAllocate(Job job) {
        ensureTransaction(conn -> {
            globalLock(conn);
            int pendingCount = checkPendingCont(conn, job);
            doAllocate(conn, job, pendingCount);
            postInProgressJob(job);
        }, err -> {
        });

        return job;
    }

    private Job doAllocate(SQLConnection conn, Job job, int pendingCount) {
        job.setState(JobState.PENDING);

        if (pendingCount > 0) {
            Future<UpdateResult> future = $.async(conn::updateWithParams, INSERT_ACTIVE, buildActiveParams(job));
            $.await(future);
            return job;
        }

        setFileUsed(conn, job);
        Worker worker = decideWorker(conn, job);
        String selectedHostIP = worker.getHostIP();
        long loadSum = worker.getCurrentLoad() + job.getEstimatedLoad();
        if (loadSum <= worker.getMaxLoad()) {
            // new in progress job
            job.setHostIP(selectedHostIP);
            job.setState(JobState.IN_PROGRESS);

            insertActiveJob(conn, job);
            updateWorkerLoad(conn, selectedHostIP, loadSum);
            return job;
        }
        SERVICE_ASSERT.isTrue(!job.isImmediate(), ErrorCode.IMMEDIATE_JOB);
        insertActiveJob(conn, job);
        return job;
    }

    private Map<String, String> buildQueryFileTransferProgressParams(FileRecord file) {
        Map<String, String> map = new HashMap<>();
        map.put("name", file.getName());
        map.put("type", file.getType().name());
        return map;
    }

    private void processTransferProgressResult(String name, HttpResponse<Buffer> resp) {
        if (resp.statusCode() != Constant.HTTP_GET_OK_STATUS_CODE) {
            LOGGER.warn("Query file transfer progress error : {}", resp.bodyAsString());
            return;
        }

        TransferProgress progress = JSON.parseObject(resp.bodyAsString(), TransferProgress.class);
        ProgressState state = progress.getState();
        if (state.isFinal()) {
            transferDone(name, FileTransferState.fromProgressState(state), progress.getTotalSize());
        }
    }

    public void processTimeoutTransferJob(Job job) {
        ensureSqlConnection(conn -> {
            try {
                FileRecord fileRecord = selectFileOrNotFount(conn, job.getTarget());
                if (fileRecord.found()) {
                    Future<HttpResponse<Buffer>> future1 = $.async(new ForthFunction<String, String, Map<String, String>, Handler<AsyncResult<HttpResponse<Buffer>>>, Object>() {
                        @Override
                        public Object apply(String s, String s2, Map<String, String> map, Handler<AsyncResult<HttpResponse<Buffer>>> handler) {
                            get(s, s2, map, handler);
                            return null;
                        }
                    }, job.getHostIP(), uri(Constant.TRANSFER_PROGRESS), buildQueryFileTransferProgressParams(fileRecord));
                    HttpResponse<Buffer> resp = $.await(future1);
                    processTransferProgressResult(job.getTarget(), resp);
                } else {
                    finish(job);
                }
            } catch (Throwable e) {
                LOGGER.warn("Process time out transfer job {} error", job.getTarget(), e);
            }
        });
    }

    public void transferDone(String name, FileTransferState transferState, long size) {
        ensureSqlConnection(conn -> {
            Job job = null;
            try {
                job = selectActiveJob(conn, JobType.FILE_TRANSFER, name);
                Job finalJob = job;
                finish(job, conn1 -> {
                    updateFileTransferResult(conn1, finalJob, name, transferState, size);
                });
            } catch (Throwable e) {
                if (job != null) {
                    LOGGER.warn("Process time out transfer job {} error", job.getTarget(), e);
                } else {
                    LOGGER.warn("Process time out job error", e);
                }
            }
        });
    }

    private HashMap<String, String> buildParams(FileRecord... files) {
        JSONArray ja = new JSONArray();
        for (FileRecord file : files) {
            JSONObject jo = new JSONObject();
            jo.put("name", file.getName());
            jo.put("type", file.getType().name());
            ja.add(jo);
        }
        return new HashMap<>(1) {{
            put("files", ja.toJSONString());
        }};
    }

    private void batchDeleteFiles(SQLConnection conn, Deleter deleter,
                                  HashMap<String, List<FileRecord>> groupedFiles) {
        groupedFiles.forEach((hostIp, files) -> {
            for (FileRecord file : files) {
                List<Job> jobs = selectActiveJob(conn, file.getName());
                SERVICE_ASSERT
                        .isTrue(jobs.size() == 0, ErrorCode.FILE_IS_IN_USED);
                deleteFileRecords(conn, deleter, files.toArray(new FileRecord[0]));
                Future<HttpResponse<Buffer>> future = $.async(new ForthFunction<String, String, HashMap<String, String>, Handler<AsyncResult<HttpResponse<Buffer>>>, Object>() {
                                                                  @Override
                                                                  public Object apply(String s, String s2, HashMap<String, String> map, Handler<AsyncResult<HttpResponse<Buffer>>> handler) {
                                                                      post(s, s2, map, handler);
                                                                      return null;
                                                                  }
                                                              }, hostIp, uri(Constant.FILE_DELETE),
                        buildParams(files.toArray(new FileRecord[0])));
                HttpResponse<Buffer> resp = $.await(future);
                SERVICE_ASSERT
                        .isTrue(resp.statusCode() == Constant.HTTP_POST_CREATED_STATUS,
                                resp.bodyAsString());
            }
        });
    }

    public void deleteFile(Deleter deleter, String... fileNames) {
        ensureSqlConnection(conn -> {
            Future<SQLConnection> future = $.async(dbClient::getConnection);
            conn = $.await(future);
            HashMap<String, List<FileRecord>> grouped = new HashMap<>();
            for (String fileName : fileNames) {
                FileRecord fileRecord = selectFile(conn, fileName);
                if (grouped.containsKey(fileRecord.getHostIP())) {
                    grouped.get(fileRecord.getHostIP()).add(fileRecord);
                } else {
                    grouped.put(fileRecord.getHostIP(), Arrays.asList(fileRecord));
                }
            }
            batchDeleteFiles(conn, deleter, grouped);
        });
    }

    public void syncFiles(Worker w, boolean cleanStale) {
        Future<ResultSet> future = $.async(dbClient::queryWithParams, FileSQL.SELECT_FILES_FOR_SYNC, SQLHelper.makeSqlArgument(w.getHostIP()));
        ResultSet resultSet = $.await(future);
        List<FileRecord> files = resultSet.getRows().stream().map(FileRecordHelper::fromDBRecord).collect(Collectors.toList());
        HashMap<String, String> params = buildParams(files.toArray(new FileRecord[0]));
        params.put("cleanStale", String.valueOf(cleanStale));

        $.async(new ForthFunction<String, String, HashMap<String, String>, Handler<AsyncResult<HttpResponse<Buffer>>>, Object>() {
            @Override
            public Object apply(String s, String s2, HashMap<String, String> map, Handler<AsyncResult<HttpResponse<Buffer>>> handler) {
                post(s, s2, map, handler);
                return null;
            }
        }, w.getHostIP(), uri(Constant.FILE_SYNC), params);
    }

    public void finish(Job job) {
        finish(job, conn -> {
        });
    }

    private void finish(Job job, Consumer<SQLConnection> post) {
        ensureTransaction(conn -> {
            globalLock(conn);

            Worker worker = selectWorker(conn, job.getHostIP());
            updateWorkerLoad(conn, worker.getHostIP(), worker.getCurrentLoad() - job.getEstimatedLoad());
            insertActiveJob(conn, job);
            deleteActiveJob(conn, job);
            setFileUnused(conn, job);
            post.accept(conn);
        }, err -> {
        });
        notifyWorkerJobIsFinished(job);
        triggerScheduling();
    }

    private void setFileUsed(SQLConnection conn, Job job) {
        if (job.getType() != JobType.FILE_TRANSFER) {
            Future<UpdateResult> future = $.async(conn::updateWithParams, FileSQL.UPDATE_FILE_AS_USED, SQLHelper.makeSqlArgument(job.getTarget()));
            UpdateResult updateResult = $.await(future);
            SERVICE_ASSERT.isTrue(updateResult.getUpdated() == 1,
                    "Operation " + job.getType().toString() +
                            " failure due to unknown error");
        }
    }

    private void setFileUnused(SQLConnection conn, Job job) {
        if (job.getType() != JobType.FILE_TRANSFER) {
            Future<UpdateResult> future = $.async(conn::updateWithParams, FileSQL.UPDATE_FILE_AS_UNUSED, SQLHelper.makeSqlArgument(job.getTarget()));
            UpdateResult updateResult = $.await(future);
            SERVICE_ASSERT.isTrue(updateResult.getUpdated() == 1,
                    "Operation " + job.getType().toString() +
                            " failure due to unknown error");
        }
    }

    public void postInProgressJob(Job job) {
        switch (job.getType()) {
            case FILE_TRANSFER:
                FileRecord file = new FileRecord(new JsonObject(job.getAttachment()));
                JsonArray params = new JsonArray();
                params.add(file.getUserId());
                params.add(file.getOriginalName());
                params.add(file.getName());
                params.add(file.getType());
                params.add(0);
                params.add(job.getHostIP());
                params.add(FileTransferState.IN_PROGRESS);
                // shared/downloadable/in shared disk/deleted
                params.add(false).add(false).add(false).add(false);
                params.add(0);
                ensureTransaction(conn -> {
                    Future<UpdateResult> future = $.async(dbClient::updateWithParams, FileSQL.INSERT, params);
                    UpdateResult updateResult = $.await(future);
                    SQLAssert.assertUpdated(updateResult);
                    Future<HttpResponse<Buffer>> future1 = $.asyncVoid(WorkerClient::post, job.getHostIP(), file.getTransferWay().getUri(), file.getTransferInfo());
                    HttpResponse<Buffer> resp = $.await(future1);
                    SERVICE_ASSERT
                            .isTrue(resp.statusCode() == Constant.HTTP_POST_CREATED_STATUS,
                                    resp.bodyAsString());

                }, err -> {
                    finish(job);
                });
            case HEAP_DUMP_ANALYSIS:
                break;
            case METASPACE_DUMP_ANALYSIS:
                break;
            case JINSIGHT:
                break;
            case PORTRAIT:
                break;
            default:
                break;
        }
    }

    private void notifyWorkerJobIsFinished(Job job) {
        JobType type = job.getType();
        String hostIP = job.getHostIP();
        String target = job.getTarget();
        switch (type) {
            case HEAP_DUMP_ANALYSIS:
            case METASPACE_DUMP_ANALYSIS:
                vertx.setTimer(notifyWorkerActionDelay, ignored -> {
                    String url = "/jifa-api/" + type.getTag() + "/" + target + "/release";
                    Future<HttpResponse<Buffer>> future = $.async((s, s2, handler) -> {
                        post(s, s2, handler);
                        return null;
                    }, job.getHostIP(), url);
                    HttpResponse<Buffer> resp = $.await(future);
                    if (resp.statusCode() != Constant.HTTP_POST_CREATED_STATUS) {
                        LOGGER.error("Notify worker {} to release task error, result : {}",
                                hostIP, resp.bodyAsString());
                    }
                });
            default:
                break;
        }
    }

    private void triggerScheduling() {
        if (currentMaster.leader) {
            vertx.setTimer(triggerSchedulingActionDelay, ignored -> schedulingTask.trigger());
        }
    }

    public JsonObject config(String name) {
        Future<ResultSet> future = $.async(dbClient::queryWithParams, ConfigSQL.SELECT, SQLHelper.makeSqlArgument(name));
        ResultSet resultSet = $.await(future);
        return SQLHelper.firstRow(resultSet);
    }

    public void globalLock(SQLConnection conn) {
        Future<ResultSet> future = $.async(conn::queryWithParams, GlobalLockSQL.LOCK, SQLHelper.makeSqlArgument(JOB_LOCK_KEY));
        ResultSet resultSet = $.await(future);
        SQLAssert.assertSelected(resultSet);
    }

    private JsonArray buildFileParams(FileRecord file) {
        return makeSqlArgument(file.getUserId(), file.getOriginalName(), file.getName(), file.getType(), file.getSize(),
                file.getHostIP(), file.getTransferState(), file.isShared(), file.isDownloadable(),
                file.isInSharedDisk(), file.isDeleted());
    }

    private JsonArray buildActiveParams(Job job) {
        return makeSqlArgument(job.getUserId(), job.getType(), job.getState(), job.getTarget(), job.getHostIP(),
                job.getState() == JobState.PENDING ? job.getAttachment() : null,
                job.getEstimatedLoad(),
                job.getType().isFileTransfer());
    }

    private JsonArray buildHistoricalParams(Job job) {
        return makeSqlArgument(job.getUserId(), job.getType(), job.getTarget(), job.getHostIP(), job.getEstimatedLoad());
    }

    private int checkPendingCont(SQLConnection conn, Job job) {
        Future<ResultSet> future = job.getHostIP() == null ?
                $.async(conn::query, COUNT_ALL_PENDING) :
                $.async(conn::queryWithParams, COUNT_PENDING_BY_HOST_IP, SQLHelper.makeSqlArgument(job.getHostIP()));
        ResultSet s = $.await(future);
        int pending = SQLHelper.count(s);
        SERVICE_ASSERT.isTrue(pending < pendingJobMaxCount,
                ErrorCode.SERVER_TOO_BUSY);
        return pending;
    }

    private Worker decideWorker(SQLConnection conn, Job job) {
        return job.getHostIP() == null ? selectMostIdleWorker(conn) : selectWorker(conn, job.getHostIP());
    }

    private Master selectMaster(SQLConnection conn, String hostIP) {
        Future<ResultSet> future = $.async(conn::queryWithParams, MasterSQL.SELECT, SQLHelper.makeSqlArgument(hostIP));
        ResultSet resultSet = $.await(future);

        JsonObject jo = SQLHelper.firstRow(resultSet);
        return MasterHelper.fromDBRecord(jo);
    }

    public Worker selectMostIdleWorker(SQLConnection conn) {
        Future<ResultSet> future = $.async(conn::query, WorkerSQL.SELECT_MOST_IDLE);
        ResultSet resultSet = $.await(future);
        JsonObject jo = SQLHelper.firstRow(resultSet);
        return WorkerHelper.fromDBRecord(jo);
    }

    public Worker selectWorker(SQLConnection conn, String hostIP) {
        Future<ResultSet> future = $.async(conn::queryWithParams, WorkerSQL.SELECT_BY_IP, SQLHelper.makeSqlArgument(hostIP));
        ResultSet resultSet = $.await(future);
        JsonObject jo = SQLHelper.firstRow(resultSet);
        return WorkerHelper.fromDBRecord(jo);
    }

    public void updateWorkerLoad(SQLConnection conn, String hostIP, long load) {
        SERVICE_ASSERT.isTrue(load >= 0, ErrorCode.SANITY_CHECK);
        Future<UpdateResult> future = $.async(conn::updateWithParams, WorkerSQL.UPDATE_LOAD, makeSqlArgument(load, hostIP));
        UpdateResult updateResult = $.await(future);
        SQLAssert.assertUpdated(updateResult);
    }

    private void insertActiveJob(SQLConnection conn, Job job) {
        Future<UpdateResult> future = $.async(conn::updateWithParams, INSERT_ACTIVE, buildActiveParams(job));
        UpdateResult updateResult = $.await(future);

        SQLAssert.assertUpdated(updateResult);
    }

    private void deleteActiveJob(SQLConnection conn, Job job) {
        Future<UpdateResult> future = $.async(conn::updateWithParams, DELETE_ACTIVE_BY_TYPE_AND_TARGET, makeSqlArgument(job.getType(), job.getTarget()));
        UpdateResult updateResult = $.await(future);
        SQLAssert.assertUpdated(updateResult);
    }

    private Job selectActiveJob(SQLConnection conn, JobType jobType, String target) {
        Future<ResultSet> future = $.async(conn::queryWithParams, JobSQL.SELECT_ACTIVE_BY_TYPE_AND_TARGET, makeSqlArgument(jobType, target));
        ResultSet resultSet = future.result();
        JsonObject jo = SQLHelper.firstRow(resultSet);
        return JobHelper.fromDBRecord(jo);
    }

    private List<Job> selectActiveJob(SQLConnection conn, String target) {
        Future<ResultSet> future = $.async(conn::queryWithParams, JobSQL.SELECT_ACTIVE_BY_TARGET, SQLHelper.makeSqlArgument(target));
        ResultSet resultSet = $.await(future);

        return resultSet
                .getRows()
                .stream()
                .map(JobHelper::fromDBRecord)
                .collect(Collectors.toList());
    }

    private void insertHistoricalJob(SQLConnection conn, Job job) {
        Future<UpdateResult> future = $.async(conn::updateWithParams, INSERT_HISTORICAL, buildHistoricalParams(job));
        UpdateResult updateResult = $.await(future);
        SQLAssert.assertUpdated(updateResult);
    }

    public void updatePendingJobToInProcess(SQLConnection conn, Job job) {
        JsonArray params = makeSqlArgument(job.getHostIP(), Instant.now(), job.getType(), job.getTarget());
        Future<UpdateResult> future = $.async(conn::updateWithParams, JobSQL.UPDATE_TO_IN_PROGRESS, params);
        UpdateResult updateResult = $.await(future);

        SQLAssert.assertUpdated(updateResult);
    }

    private FileRecord selectFile(SQLConnection conn, String name) {
        Future<ResultSet> future = $.async(conn::queryWithParams, FileSQL.SELECT_FILE_BY_NAME, SQLHelper.makeSqlArgument(name));
        ResultSet resultSet = $.await(future);
        JsonObject jo = SQLHelper.firstRow(resultSet);
        return FileRecordHelper.fromDBRecord(jo);
    }

    private FileRecord selectFileOrNotFount(SQLConnection conn, String name) {
        Future<ResultSet> future = $.async(conn::queryWithParams, FileSQL.SELECT_FILE_BY_NAME, SQLHelper.makeSqlArgument(name));
        ResultSet rs = $.await(future);
        if (rs.getNumRows() > 0) {
            return FileRecordHelper.fromDBRecord(SQLHelper.firstRow(rs));
        }
        return FileRecord.NOT_FOUND;
    }

    private void deleteFileRecords(SQLConnection conn, Deleter deleter, FileRecord... files) {
        for (FileRecord file : files) {
            Future<UpdateResult> future = $.async(conn::updateWithParams, FileSQL.DELETE_FILE_BY_NAME, makeSqlArgument(deleter, file.getName()));
            $.await(future);
        }
    }

    private void updateFileTransferResult(SQLConnection conn, Job job, String name,
                                          FileTransferState transferState,
                                          long size) {
        Future<UpdateResult> future = $.async(conn::updateWithParams, FileSQL.UPDATE_TRANSFER_RESULT, makeSqlArgument(transferState, size, name));
        UpdateResult rs = $.await(future);
        // file record may not exist for some reason
        // SQLAssert.assertUpdated(updateResult);
    }
}
