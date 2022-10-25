/********************************************************************************
 * Copyright (c) 2020, 2022 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.master.service.impl;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observables.GroupedObservable;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.sql.SQLClientHelper;
import io.vertx.reactivex.ext.sql.SQLConnection;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import org.apache.commons.io.FileUtils;
import org.eclipse.jifa.common.ErrorCode;
import org.eclipse.jifa.common.enums.FileTransferState;
import org.eclipse.jifa.common.enums.ProgressState;
import org.eclipse.jifa.common.vo.TransferProgress;
import org.eclipse.jifa.master.Constant;
import org.eclipse.jifa.master.entity.File;
import org.eclipse.jifa.master.entity.Job;
import org.eclipse.jifa.master.entity.Master;
import org.eclipse.jifa.master.entity.Worker;
import org.eclipse.jifa.master.entity.enums.Deleter;
import org.eclipse.jifa.master.entity.enums.JobState;
import org.eclipse.jifa.master.entity.enums.JobType;
import org.eclipse.jifa.master.model.TransferWay;
import org.eclipse.jifa.master.service.impl.helper.*;
import org.eclipse.jifa.master.service.sql.*;
import org.eclipse.jifa.master.support.Factory;
import org.eclipse.jifa.master.support.Pattern;
import org.eclipse.jifa.master.support.WorkerClient;
import org.eclipse.jifa.master.support.WorkerScheduler;
import org.eclipse.jifa.master.task.SchedulingTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.eclipse.jifa.common.util.GsonHolder.GSON;
import static org.eclipse.jifa.master.Constant.LOCAL_HOST;
import static org.eclipse.jifa.master.Constant.uri;
import static org.eclipse.jifa.master.service.ServiceAssertion.SERVICE_ASSERT;
import static org.eclipse.jifa.master.service.impl.helper.SQLHelper.ja;
import static org.eclipse.jifa.master.service.sql.JobSQL.*;
import static org.eclipse.jifa.master.support.WorkerClient.post;

public class Pivot {

    private static final Logger LOGGER = LoggerFactory.getLogger(Pivot.class);

    private static final String JOB_LOCK_KEY = "MASTER-JOB-LOCK";

    private static final String SCHEDULER_PATTERN = "SCHEDULER-PATTERN";

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

    private WorkerScheduler scheduler;

    private boolean isDefaultPattern;

    private Pivot() {
    }

    public WorkerScheduler getScheduler() {
        return scheduler;
    }

    public static Pivot getInstance() {
        return SINGLETON;
    }

    public static synchronized Pivot createInstance(Vertx vertx, JDBCClient dbClient, JsonObject config) {
        if (SINGLETON != null) {
            return SINGLETON;
        }

        Pivot jm = new Pivot();
        try {
            jm.dbClient = dbClient;
            jm.vertx = vertx;

            Pattern pattern = Pattern.valueOf(ConfigHelper.getString(jm.config(SCHEDULER_PATTERN)));
            jm.isDefaultPattern = pattern == Pattern.DEFAULT;
            jm.scheduler = Factory.create(pattern);

            jm.pendingJobMaxCount = ConfigHelper.getInt(jm.config(PENDING_JOB_MAX_COUNT_KEY));

            jm.notifyWorkerActionDelay = ConfigHelper.getLong(jm.config(NOTIFY_WORKER_ACTION_DELAY_KEY));

            jm.triggerSchedulingActionDelay = ConfigHelper.getLong(jm.config(TRIGGER_SCHEDULING_ACTION_DELAY));

            String ip =
                org.eclipse.jifa.master.Master.DEV_MODE || pattern == Pattern.K8S ?
                        LOCAL_HOST :
                        InetAddress.getLocalHost().getHostAddress();
            LOGGER.info("Current Master Host IP is {}", ip);

            SQLConnection sqlConnection = dbClient.rxGetConnection().blockingGet();
            jm.currentMaster = jm.selectMaster(sqlConnection, ip).doFinally(sqlConnection::close).blockingGet();

            jm.scheduler.initialize(jm, vertx, config);
        } catch (Throwable t) {
            LOGGER.error("Init job service error", t);
            System.exit(-1);
        }

        SINGLETON = jm;
        return jm;
    }

    public boolean isLeader() {
        return currentMaster.isLeader();
    }

    public boolean isDefaultPattern() {
        return isDefaultPattern;
    }

    public void setSchedulingTask(SchedulingTask task) {
        this.schedulingTask = task;
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

    public JDBCClient getDbClient() {
        return dbClient;
    }

    Single<Job> allocate(String userId, String hostIP, JobType jobType, String target,
                         String attachment, long estimatedLoad, boolean immediate) {

        Job job = buildPendingJob(userId, hostIP, jobType, target, attachment, estimatedLoad, immediate);

        return doAllocate(job);
    }

    private Single<Job> doAllocate(Job job) {
        return scheduler.start(job)
                        .andThen(inTransactionAndLock(
                            conn -> checkPendingCont(conn, job)
                                .flatMap(pendingCount -> doAllocate(conn, job, pendingCount))
                        ).flatMap(i -> postInProgressJob(job).andThen(Single.just(job))));
    }

    private Single<Job> doAllocate(SQLConnection conn, Job job, int pendingCount) {
        job.setState(JobState.PENDING);

        if (pendingCount > 0) {
            return conn.rxUpdateWithParams(INSERT_ACTIVE, buildActiveParams(job)).map(i -> job);
        }

        return setFileUsed(conn, job).andThen(scheduler.decide(job, conn).flatMap(
            worker -> {
                if (scheduler.supportPendingJob()) {
                    String selectedHostIP = worker.getHostIP();
                    long loadSum = worker.getCurrentLoad() + job.getEstimatedLoad();
                    if (loadSum <= worker.getMaxLoad()) {
                        // new in progress job
                        job.setHostIP(selectedHostIP);
                        job.setState(JobState.IN_PROGRESS);

                        return insertActiveJob(conn, job)
                            .andThen(updateWorkerLoad(conn, selectedHostIP, loadSum))
                            .toSingleDefault(job);
                    }
                    SERVICE_ASSERT.isTrue(!job.isImmediate(), ErrorCode.IMMEDIATE_JOB);
                    return insertActiveJob(conn, job).toSingleDefault(job);
                } else {
                    String selectedHostIP = worker.getHostIP();
                    // new in progress job
                    job.setHostIP(selectedHostIP);
                    job.setState(JobState.IN_PROGRESS);
                    return insertActiveJob(conn, job).toSingleDefault(job);
                }
            }
        ));
    }

    public Single<Boolean> isDBConnectivity() {
        return dbClient.rxGetConnection()
            .flatMap(conn -> selectTrue(conn).doOnTerminate(
                () -> {
                    LOGGER.info("Close connection for DB connectivity test");
                    conn.close();
                }))
            .onErrorReturn(t -> {
                t.printStackTrace();
                return Boolean.FALSE;
            });
    }

    private Map<String, String> buildQueryFileTransferProgressParams(File file) {
        Map<String, String> map = new HashMap<>();
        map.put("name", file.getName());
        map.put("type", file.getType().name());
        return map;
    }

    private Completable processTransferProgressResult(String name, HttpResponse resp) {
        if (resp.statusCode() != Constant.HTTP_GET_OK_STATUS_CODE) {
            LOGGER.warn("Query file transfer progress error : {}", resp.bodyAsString());
            return Completable.complete();
        }

        TransferProgress progress = GSON.fromJson(resp.bodyAsString(), TransferProgress.class);
        ProgressState state = progress.getState();
        if (state.isFinal()) {
            return transferDone(name, FileTransferState.fromProgressState(state), progress.getTotalSize());
        }

        return Completable.complete();
    }

    public Completable processTimeoutTransferJob(Job job) {
        return dbClient.rxGetConnection()
            .flatMapCompletable(conn ->
                selectFileOrNotFount(conn, job.getTarget())
                    .flatMapCompletable(file -> {
                        if (file.found()) {
                            return scheduler.decide(job, conn)
                                .flatMapCompletable(worker -> {
                                    if (worker == Worker.NOT_FOUND) {
                                        // Job has been timeout, but there is no corresponding worker which indicated
                                        // by Job's hostIP, this only happens in K8S mode, i.e. worker has been stopped
                                        // due to some reasons but Job is still presented. We would update Job transferState
                                        // as Error unconditionally.
                                        return Completable.complete()
                                            .doOnComplete(() -> SERVICE_ASSERT.isTrue(!isDefaultPattern, "Only happens in K8S Mode"))
                                            .andThen(transferDone(job.getTarget(), FileTransferState.ERROR, 0));
                                    } else {
                                        return WorkerClient.get(job.getHostIP(), uri(Constant.TRANSFER_PROGRESS),
                                                buildQueryFileTransferProgressParams(file))
                                            .flatMapCompletable(
                                                resp -> processTransferProgressResult(job.getTarget(), resp));
                                    }
                                });
                        } else {
                            return finish(job);
                        }
                    })
                    .doOnTerminate(conn::close)
            )
            .doOnError((t) -> LOGGER.warn("Process time out transfer job {} error", job.getTarget(), t))
            .onErrorComplete();
    }

    Completable transferDone(String name, FileTransferState transferState, long size) {
        return dbClient.rxGetConnection()
                       .flatMap(conn -> selectActiveJob(conn, JobType.FILE_TRANSFER, name).doOnTerminate(conn::close))
                       .flatMapCompletable(
                           job -> finish(job, conn -> updateFileTransferResult(conn, job, name, transferState, size))
                       );
    }

    private HashMap<String, String> buildParams(File... files) {
        @SuppressWarnings("rawtypes")
        Map[] maps = new Map[files.length];
        for (int i = 0; i < files.length; i++) {
            Map<String, String> map = new HashMap<>();
            map.put("name", files[i].getName());
            map.put("type", files[i].getType().name());
            maps[i] = map;
        }

        return new HashMap<>(1) {{
            put("files", GSON.toJson(maps));
        }};
    }

    private Completable batchDeleteFiles(SQLConnection conn, Deleter deleter,
                                         GroupedObservable<String, File> groupedFiles) {
        String hostIP = groupedFiles.getKey();
        return groupedFiles
            .toList()
            .flatMapCompletable(files -> Observable.fromIterable(files)
                                                   .flatMapSingle(file -> selectActiveJob(conn, file.getName())
                                                       .doOnSuccess(jobs -> {
                                                           if (deleter != Deleter.SYSTEM) {
                                                               SERVICE_ASSERT
                                                                   .isTrue(jobs.size() == 0, ErrorCode.FILE_IS_IN_USED);
                                                           }
                                                       })
                                                   )
                                                   .ignoreElements()
                                                   .andThen(
                                                       deleteFileRecords(conn, deleter, files.toArray(new File[0])))
                                                   .andThen(
                                                       isDefaultPattern ?
                                                       post(hostIP, uri(Constant.FILE_BATCH_DELETE),
                                                            buildParams(files.toArray(new File[0])))
                                                           .doOnSuccess(resp -> SERVICE_ASSERT
                                                               .isTrue(resp.statusCode() ==
                                                                       Constant.HTTP_POST_CREATED_STATUS,
                                                                       resp.bodyAsString())).ignoreElement()
                                                                        :
                                                       Completable.fromAction(
                                                           () -> {
                                                               for (File file : files) {
                                                                   // FIXME: consider adding delete files
                                                                   //        api in worker scheduler
                                                                   // copy from pvc clean up task
                                                                   java.io.File pf =
                                                                       new java.io.File("/root/jifa_workspace" +
                                                                                        java.io.File.separator +
                                                                                        file.getType().getTag() +
                                                                                        java.io.File.separator +
                                                                                        file.getName());
                                                                   FileUtils.deleteDirectory(pf);
                                                               }
                                                           }
                                                       )
                                                   )
            );
    }

    public Completable deleteFile(Deleter deleter, String... fileNames) {
        return dbClient.rxGetConnection()
                       .flatMapCompletable(
                           conn -> Observable.fromArray(fileNames)
                                             .flatMapSingle(fileName -> selectFile(conn, fileName))
                                             .groupBy(File::getHostIP)
                                             .flatMapCompletable(
                                                 groupedFiles -> batchDeleteFiles(conn, deleter, groupedFiles))
                                             .doOnTerminate(conn::close)
                       );
    }

    public Completable syncFiles(Worker w, boolean cleanStale) {
        return dbClient.rxQueryWithParams(FileSQL.SELECT_FILES_FOR_SYNC, SQLHelper.ja(w.getHostIP()))
                       .map(ar -> ar.getRows().stream().map(FileHelper::fromDBRecord).collect(Collectors.toList()))
                       .flatMapCompletable(
                           files -> {
                               HashMap<String, String> params = buildParams(files.toArray(new File[0]));
                               params.put("cleanStale", String.valueOf(cleanStale));
                               return post(w.getHostIP(), uri(Constant.FILE_SYNC), params)
                                   .ignoreElement();
                           }
                       );
    }

    public Completable finish(Job job) {
        return finish(job, conn -> Completable.complete());
    }

    private Completable finish(Job job, Function<SQLConnection, Completable> post) {
        return inTransactionAndLock(
            conn -> scheduler.decide(job, conn)
                    .flatMapCompletable(worker -> {
                    if (isDefaultPattern()) {
                        return updateWorkerLoad(conn, worker.getHostIP(), worker.getCurrentLoad() - job.getEstimatedLoad());
                    } else {
                        return Completable.complete();
                    }
                })
                .andThen(insertHistoricalJob(conn, job))
                // delete old job
                .andThen(deleteActiveJob(conn, job))
                .andThen(this.setFileUnused(conn, job))
                .andThen(post.apply(conn))
                .toSingleDefault(job)
        ).ignoreElement()
         // notify worker
         .andThen(this.notifyWorkerJobIsFinished(job))
         // stop worker
         .andThen(scheduler.stop(job))
         // trigger scheduling
         .andThen(this.triggerScheduling());
    }

    private Completable setFileUsed(SQLConnection conn, Job job) {
        if (job.getType() != JobType.FILE_TRANSFER) {
            return conn.rxUpdateWithParams(FileSQL.UPDATE_FILE_AS_USED, ja(job.getTarget()))
                       .doOnSuccess(
                           record -> SERVICE_ASSERT.isTrue(record.getUpdated() == 1,
                                                           "Operation " + job.getType().toString() +
                                                           " failure due to unknown error"))
                       .ignoreElement();
        }
        return Completable.complete();
    }

    private Completable setFileUnused(SQLConnection conn, Job job) {
        if (job.getType() != JobType.FILE_TRANSFER) {
            return conn.rxUpdateWithParams(FileSQL.UPDATE_FILE_AS_UNUSED, ja(job.getTarget()))
                       .doOnSuccess(
                           record -> SERVICE_ASSERT.isTrue(record.getUpdated() == 1,
                                                           "Operation " + job.getType().toString() +
                                                           " failure due to unknown error"))
                       .ignoreElement();
        }
        return Completable.complete();
    }

    public Completable postInProgressJob(Job job) {
        switch (job.getType()) {
            case FILE_TRANSFER:
                File file = new File(new JsonObject(job.getAttachment()));
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

                return inTransaction(
                    conn -> conn.rxUpdateWithParams(FileSQL.INSERT, params)
                                .doOnSuccess(SQLAssert::assertUpdated).ignoreElement()
                                .andThen(
                                    // upload file need special process
                                    file.getTransferWay() != TransferWay.UPLOAD ?
                                    post(job.getHostIP(), file.getTransferWay().getUri(), file.getTransferInfo())
                                        .doOnSuccess(resp -> SERVICE_ASSERT.isTrue(
                                            resp.statusCode() == Constant.HTTP_POST_CREATED_STATUS,
                                            resp.bodyAsString())).ignoreElement() :
                                    Completable.complete()
                                ).toSingleDefault(job))
                    .doOnError(e -> finish(job).subscribe()).ignoreElement();
            default:
                return Completable.complete();
        }
    }

    private Completable notifyWorkerJobIsFinished(Job job) {
        JobType type = job.getType();
        String hostIP = job.getHostIP();
        String target = job.getTarget();
        return Completable.fromAction(() -> {
            switch (type) {
                case HEAP_DUMP_ANALYSIS:
                case GCLOG_ANALYSIS:
                case THREAD_DUMP_ANALYSIS:
                    vertx.setTimer(notifyWorkerActionDelay, ignored -> {
                        String url = "/jifa-api/" + type.getTag() + "/" + target + "/release";
                        Single<HttpResponse<Buffer>> post = post(job.getHostIP(), url);
                        post.subscribe(resp -> {
                                           if (resp.statusCode() != Constant.HTTP_POST_CREATED_STATUS) {
                                               LOGGER.error("Notify worker {} to release task error, result : {}",
                                                            hostIP, resp.bodyAsString());
                                           }
                                       },
                                       t -> LOGGER.error("Notify worker {} to release task error", hostIP));
                    });
                default:
            }
        });
    }

    private Completable triggerScheduling() {
        if (scheduler.supportPendingJob()) {
            return Completable
                .fromAction(() -> {
                    if (currentMaster.leader) {
                        vertx.setTimer(triggerSchedulingActionDelay, ignored -> schedulingTask.trigger());
                    }
                });
        }
        return Completable.complete();
    }

    public JsonObject config(String name) {
        return SQLHelper.singleRow(dbClient.rxQueryWithParams(ConfigSQL.SELECT, ja(name)).blockingGet());
    }

    public <T> Single<T> inTransaction(Function<SQLConnection, Single<T>> sourceSupplier) {
        return SQLClientHelper.inTransactionSingle(dbClient, sourceSupplier);
    }

    public <T> Single<T> inTransactionAndLock(Function<SQLConnection, Single<T>> sourceSupplier) {
        return SQLClientHelper.inTransactionSingle(dbClient, conn ->
            lock(conn).andThen(sourceSupplier.apply(conn))
        );
    }

    private Completable lock(SQLConnection conn) {
        return conn.rxQueryWithParams(GlobalLockSQL.LOCK, ja(JOB_LOCK_KEY))
                   .doOnSuccess(SQLAssert::assertSelected)
                   .ignoreElement();
    }

    private JsonArray buildFileParams(File file) {
        return ja(file.getUserId(), file.getOriginalName(), file.getName(), file.getType(), file.getSize(),
                  file.getHostIP(), file.getTransferState(), file.isShared(), file.isDownloadable(),
                  file.isInSharedDisk(), file.isDeleted());
    }

    private JsonArray buildActiveParams(Job job) {
        return ja(job.getUserId(), job.getType(), job.getState(), job.getTarget(), job.getHostIP(),
                  job.getState() == JobState.PENDING ? job.getAttachment() : null,
                  job.getEstimatedLoad(),
                  job.getType().isFileTransfer());
    }

    private JsonArray buildHistoricalParams(Job job) {
        return ja(job.getUserId(), job.getType(), job.getTarget(), job.getHostIP(), job.getEstimatedLoad());
    }

    private Single<Integer> checkPendingCont(SQLConnection conn, Job job) {
        if (scheduler.supportPendingJob()) {
            Single<ResultSet> s = job.getHostIP() == null ?
                                  conn.rxQuery(COUNT_ALL_PENDING) :
                                  conn.rxQueryWithParams(COUNT_PENDING_BY_HOST_IP, ja(job.getHostIP()));
            return s.map(SQLHelper::count)
                    .doOnSuccess((pending) -> SERVICE_ASSERT.isTrue(pending < pendingJobMaxCount,
                                                                    ErrorCode.SERVER_TOO_BUSY));
        }
        return Single.just(0);
    }

    public Single<Worker> decideWorker(SQLConnection conn, Job job) {
        return job.getHostIP() == null ? selectMostIdleWorker(conn) : selectWorker(conn, job.getHostIP());
    }

    private Single<Master> selectMaster(SQLConnection conn, String hostIP) {
        return conn.rxQueryWithParams(MasterSQL.SELECT, ja(hostIP))
                   .map(SQLHelper::singleRow)
                   .map(MasterHelper::fromDBRecord);
    }

    public Single<Worker> selectMostIdleWorker(SQLConnection conn) {
        return conn.rxQuery(WorkerSQL.SELECT_MOST_IDLE)
                   .map(SQLHelper::singleRow)
                   .map(WorkerHelper::fromDBRecord);
    }

    public Single<Worker> selectWorker(SQLConnection conn, String hostIP) {
        return conn.rxQueryWithParams(WorkerSQL.SELECT_BY_IP, ja(hostIP))
                   .map(SQLHelper::singleRow)
                   .map(WorkerHelper::fromDBRecord);
    }

    public Completable updateWorkerLoad(SQLConnection conn, String hostIP, long load) {
        SERVICE_ASSERT.isTrue(load >= 0, ErrorCode.SANITY_CHECK);
        return conn.rxUpdateWithParams(WorkerSQL.UPDATE_LOAD, ja(load, hostIP))
                   .doOnSuccess(SQLAssert::assertUpdated)
                   .ignoreElement();
    }

    private Completable insertActiveJob(SQLConnection conn, Job job) {
        return conn.rxUpdateWithParams(INSERT_ACTIVE, buildActiveParams(job))
                   .doOnSuccess(SQLAssert::assertUpdated)
                   .ignoreElement();
    }

    private Single<Boolean> selectTrue(SQLConnection conn) {
        return conn.rxQuery(SQL.SELECT_TRUE)
            .map(SQLHelper::singleRow)
            .map(value -> value.getInteger("TRUE") == 1 ? Boolean.TRUE : Boolean.FALSE)
            .onErrorReturn(e -> {
                e.printStackTrace();
                return Boolean.FALSE;
            });
    }

    private Completable deleteActiveJob(SQLConnection conn, Job job) {
        return conn.rxUpdateWithParams(DELETE_ACTIVE_BY_TYPE_AND_TARGET, ja(job.getType(), job.getTarget()))
                   .doOnSuccess(SQLAssert::assertUpdated)
                   .ignoreElement();
    }

    private Single<Job> selectActiveJob(SQLConnection conn, JobType jobType, String target) {
        return conn.rxQueryWithParams(JobSQL.SELECT_ACTIVE_BY_TYPE_AND_TARGET, ja(jobType, target))
                   .map(SQLHelper::singleRow)
                   .map(JobHelper::fromDBRecord);
    }

    private Single<List<Job>> selectActiveJob(SQLConnection conn, String target) {
        return conn.rxQueryWithParams(JobSQL.SELECT_ACTIVE_BY_TARGET, ja(target))
                   .map(records ->
                            records.getRows().stream().map(JobHelper::fromDBRecord).collect(Collectors.toList())
                   );
    }

    private Completable insertHistoricalJob(SQLConnection conn, Job job) {
        return conn.rxUpdateWithParams(INSERT_HISTORICAL, buildHistoricalParams(job))
                   .doOnSuccess(SQLAssert::assertUpdated)
                   .ignoreElement();
    }

    public Completable updatePendingJobToInProcess(SQLConnection conn, Job job) {
        JsonArray params = ja(job.getHostIP(), Instant.now(), job.getType(), job.getTarget());

        return conn.rxUpdateWithParams(JobSQL.UPDATE_TO_IN_PROGRESS, params)
                   .doOnSuccess(SQLAssert::assertUpdated)
                   .ignoreElement();
    }

    private Single<File> selectFile(SQLConnection conn, String name) {
        return conn.rxQueryWithParams(FileSQL.SELECT_FILE_BY_NAME, ja(name))
                   .map(SQLHelper::singleRow)
                   .map(FileHelper::fromDBRecord);
    }

    private Single<File> selectFileOrNotFount(SQLConnection conn, String name) {
        return conn.rxQueryWithParams(FileSQL.SELECT_FILE_BY_NAME, ja(name))
                   .map(res -> {
                       if (res.getNumRows() > 0) {
                           return FileHelper.fromDBRecord(SQLHelper.singleRow(res));
                       }
                       return File.NOT_FOUND;
                   });
    }

    private Completable deleteFileRecords(SQLConnection conn, Deleter deleter, File... files) {
        return Observable.fromArray(files)
                         .flatMapCompletable(
                             file -> conn.rxUpdateWithParams(FileSQL.DELETE_FILE_BY_NAME, ja(deleter, file.getName()))
                                         .doOnSuccess(SQLAssert::assertUpdated).ignoreElement());
    }

    private Completable updateFileTransferResult(SQLConnection conn, Job job, String name,
                                                 FileTransferState transferState,
                                                 long size) {
        return conn.rxUpdateWithParams(FileSQL.UPDATE_TRANSFER_RESULT, ja(transferState, size, name))
                   // file record may not exist for some reason
                   // .doOnSuccess(SQLAssert::assertUpdated)
                   .ignoreElement();
    }
}
