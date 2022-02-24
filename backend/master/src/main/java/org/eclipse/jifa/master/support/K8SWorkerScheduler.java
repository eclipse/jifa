/********************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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

import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Config;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.sql.SQLConnection;
import io.vertx.serviceproxy.ServiceException;
import org.apache.commons.codec.digest.DigestUtils;
import org.eclipse.jifa.common.ErrorCode;
import org.eclipse.jifa.common.JifaException;
import org.eclipse.jifa.master.entity.Job;
import org.eclipse.jifa.master.entity.Worker;
import org.eclipse.jifa.master.model.WorkerInfo;
import org.eclipse.jifa.master.service.impl.Pivot;
import org.eclipse.jifa.master.task.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static org.eclipse.jifa.master.Constant.*;
import static org.eclipse.jifa.master.Constant.K8S_WORKER_PVC_NAME;

public class K8SWorkerScheduler implements WorkerScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(K8SWorkerScheduler.class);

    private static final String WORKER_PREFIX = "jifa-worker";

    private static final String SPECIAL_WORKER_PREFIX = "jifa-special";

    private static String NAMESPACE;

    private static String WORKER_IMAGE;

    private static CoreV1Api api;

    private static long MINIMAL_MEM_REQ;

    private static String MASTER_POD_NAME;

    private static String WORKER_PVC_NAME;

    private static V1Pod createWorker(String name, long requestMemSize) {
        requestMemSize = Math.max(requestMemSize, MINIMAL_MEM_REQ);

        V1Volume volume = new V1Volume();
        volume.setName("dumpfile-volume");
        volume.persistentVolumeClaim(new V1PersistentVolumeClaimVolumeSource().claimName(WORKER_PVC_NAME));

        V1Pod npod;
        npod = new V1PodBuilder()
            .withNewMetadata()
            .withName(name)
            .withLabels(new HashMap<String, String>() {{
                put("name", "jifa-worker");
            }})
            .endMetadata()
            .withNewSpec()
            .addNewContainer()
            .withResources(
                new V1ResourceRequirements()
                    .requests(Map.of("memory", new Quantity(String.valueOf(requestMemSize))))
            )
            .withName("my-jifa-worker")
            .withImage(WORKER_IMAGE)
            .withPorts(
                new V1ContainerPort()
                    .containerPort(8102)
            )
            .withVolumeMounts(
                new V1VolumeMount()
                    .mountPath("/root")
                    .name("dumpfile-volume")
            )
            .endContainer()
            .withVolumes(volume)
            .endSpec()
            .build();
        try {
            npod = api.createNamespacedPod(NAMESPACE, npod, null, null, null);
        } catch (ApiException e) {
            e.printStackTrace();
        }
        return npod;
    }

    public static String getNormalWorkerPrefix() {
        return WORKER_PREFIX;
    }

    public static String getSpecialWorkerPrefix() {
        return SPECIAL_WORKER_PREFIX;
    }

    private static List<V1Pod> listWorker() {
        List<V1Pod> pods = null;
        try {
            V1PodList list = api.listNamespacedPod(NAMESPACE, null, null, null, null, null, null, null, null, null);
            pods = list.getItems();
        } catch (ApiException e) {
            e.printStackTrace();
        }
        return pods;
    }

    private static V1Pod removeWorker(String name) {
        V1Pod npod = null;
        try {
            npod = api.deleteNamespacedPod(name, NAMESPACE, null, null, 0, null, null, null);
        } catch (ApiException e) {
            e.printStackTrace();
        }
        return npod;
    }

    @Override
    public void initialize(Pivot pivot, Vertx vertx, JsonObject config) {
        new RetiringTask(pivot, vertx);
        new TransferJobResultFillingTask(pivot, vertx);
        new PVCCleanupTask(pivot, vertx);
        new StopAbnormalWorkerTask(pivot, vertx);
        new FileSyncForK8STask(pivot, vertx);

        // Order is important
        ApiClient client;
        try {
            client = Config.defaultClient();
            Configuration.setDefaultApiClient(client);
            api = new CoreV1Api();
            JsonObject k8sConfig = config.getJsonObject(K8S_KEYWORD);
            NAMESPACE = k8sConfig.getString(K8S_NAMESPACE);
            WORKER_IMAGE = k8sConfig.getString(K8S_WORKER_IMAGE);
            MINIMAL_MEM_REQ = k8sConfig.getLong(K8S_MINIMAL_MEM_REQ);
            MASTER_POD_NAME = k8sConfig.getString(K8S_MASTER_POD_NAME);
            WORKER_PVC_NAME = k8sConfig.getString(K8S_WORKER_PVC_NAME);
            LOGGER.info("K8S Namespace: " + NAMESPACE + ", Image: " + WORKER_IMAGE + ", Minimal memory request:" +
                        MINIMAL_MEM_REQ);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Single<Worker> decide(Job job, SQLConnection conn) {
        String name = buildWorkerName(job);
        WorkerInfo workerInfo = getWorkerInfo(name);
        if (workerInfo == null) {
            Worker none = Worker.NOT_FOUND;
            none.setHostName(name);
            return Single.just(none);
        } else {
            String workerIp = getWorkerInfo(name).getIp();
            Worker handmake = new Worker();
            handmake.setHostIP(workerIp);
            handmake.setHostName(name);
            return Single.just(handmake);
        }
    }

    @Override
    public boolean supportPendingJob() {
        return false;
    }

    private String buildWorkerName(Job job) {
        String target = job.getTarget();
        if (target.startsWith(SPECIAL_WORKER_PREFIX)) {
            return target;
        } else {
            target = DigestUtils.md5Hex(job.getTarget().getBytes(StandardCharsets.UTF_8)).substring(0, 16);
            return WORKER_PREFIX + "-" + target;
        }
    }

    @Override
    public Completable start(Job job) {
        String name = buildWorkerName(job);
        Map<String, String> config = new HashMap<>();

        double fileSizeGb = Utils.calculateSizeFromLoad(job.getEstimatedLoad());
        fileSizeGb *= 1.3; // occupy 130% memory of filesize
        fileSizeGb = Math.min(fileSizeGb, 18.0); // limit to 18g
        long fileSizeKb = (long) (fileSizeGb * 1024 * 1024 * 1024); // convert gb to kb
        config.put("requestMemSize", Long.toString(fileSizeKb));

        schedule(name, config);

        String workerIp = getWorkerInfo(name).getIp();

        if (workerIp == null) {
            // Front-end would retry original request until worker pod has been started or
            // timeout threshold reached.
            return Completable.error(new ServiceException(ErrorCode.RETRY.ordinal(), job.getTarget()));
        }
        final String MSG_RETRY = "RETRY";
        final String MSG_OK = "OK";
        return WorkerClient.get(workerIp, uri(PING))
                .flatMap(resp -> Single.just(MSG_OK))
                .onErrorReturn(err -> {
                    if (err instanceof ConnectException) {
                        // ConnectionException is tolerable because it simply indicates worker is still
                        // starting
                        return MSG_RETRY;
                    } else if (err instanceof IOException) {
                        if (err.getMessage() != null && err.getMessage().contains("Connection reset by peer")) {
                            return MSG_RETRY;
                        }
                    }
                    return err.getMessage();
                }).flatMapCompletable(msg -> {
                    if (msg.equals(MSG_OK)) {
                        return Completable.complete();
                    } else if (msg.equals(MSG_RETRY)) {
                        return Completable.error(new ServiceException(ErrorCode.RETRY.ordinal(), job.getTarget()));
                    } else {
                        return Completable.error(new JifaException("Can not start worker due to internal error: " + msg));
                    }
                });
    }

    private void schedule(String id, Map<String, String> config) {
        long requestMemSize = 0L;

        String tmp = config.get("requestMemSize");
        if (tmp != null) {
            requestMemSize = Long.parseLong(tmp);
        }

        if (getWorkerInfo(id) != null) {
            LOGGER.debug("Create worker {} but it already exists", id);
        } else {
            LOGGER.debug("Create worker {} [MemRequest: {}bytes]", id, requestMemSize);
            createWorker(id, requestMemSize);
        }
    }

    @Override
    public Completable stop(Job job) {
        return Completable.fromAction(() -> {
            String id = buildWorkerName(job);
            if (getWorkerInfo(id) == null) {
                LOGGER.debug("Stop worker " + id + " but it does not exist");
            } else {
                LOGGER.debug("Stop worker " + id);
                removeWorker(id);
            }
        });
    }

    @Override
    public Completable stop(Worker worker) {
        return Completable.fromAction(() -> {
            String id = worker.getHostName();
            if (getWorkerInfo(id) == null) {
                LOGGER.debug("Stop worker " + id + " but it does not exist");
            } else {
                LOGGER.debug("Stop worker " + id);
                removeWorker(id);
            }
        });
    }

    @Override
    public Single<List<Worker>> list() {
        List<V1Pod> pods = listWorker();
        if (pods != null) {
            List<Worker> workers = pods.stream().map(pod -> {
                Worker w = new Worker();
                w.setHostName(pod.getMetadata().getName());
                w.setHostIP(pod.getStatus().getPodIP());
                return w;
            }).collect(Collectors.toList());
            return Single.just(workers);
        }
        return Single.just(new ArrayList<>() {{
            add(Worker.NOT_FOUND);
        }});
    }

    private WorkerInfo getWorkerInfo(String id) {
        V1Pod npod = null;
        try {
            npod = api.readNamespacedPod(id, NAMESPACE, null, null, null);
        } catch (ApiException ignored) {
        }
        if (null != npod) {
            WorkerInfo info = new WorkerInfo();
            info.setName(id);
            info.setIp(Objects.requireNonNull(npod.getStatus()).getPodIP());
            return info;
        } else {
            return null;
        }
    }
}