/********************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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
import io.kubernetes.client.openapi.models.V1ContainerPort;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaimVolumeSource;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodBuilder;
import io.kubernetes.client.openapi.models.V1ResourceRequirements;
import io.kubernetes.client.openapi.models.V1Volume;
import io.kubernetes.client.openapi.models.V1VolumeMount;
import io.kubernetes.client.util.Config;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.sql.SQLConnection;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import org.eclipse.jifa.master.Constant;
import org.eclipse.jifa.master.entity.Job;
import org.eclipse.jifa.master.entity.Worker;
import org.eclipse.jifa.master.entity.enums.JobType;
import org.eclipse.jifa.master.model.WorkerInfo;
import org.eclipse.jifa.master.service.impl.Pivot;
import org.eclipse.jifa.master.task.PVCCleanupTask;
import org.eclipse.jifa.master.task.RetiringTask;
import org.eclipse.jifa.master.task.TransferJobResultFillingTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class K8SWorkerScheduler implements WorkerScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(K8SWorkerScheduler.class);

    private static String NAMESPACE;

    private static String WORKER_IMAGE;

    private static CoreV1Api api;

    private static long MINIMAL_MEM_REQ;

    private static V1Pod createWorker(String name, long requestMemSize) {
        requestMemSize = Math.max(requestMemSize, MINIMAL_MEM_REQ);

        V1Volume volume = new V1Volume();
        volume.setName("dumpfile-volume");
        volume.persistentVolumeClaim(new V1PersistentVolumeClaimVolumeSource().claimName("worker-pvc"));

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
    public void initialize(Pivot pivot, Vertx vertx, Map<String, String> configs) {
        new RetiringTask(pivot, vertx);
        new TransferJobResultFillingTask(pivot, vertx);
        new PVCCleanupTask(pivot, vertx);

        // Orders are important
        ApiClient client;
        try {
            client = Config.defaultClient();
            Configuration.setDefaultApiClient(client);
            api = new CoreV1Api();
            NAMESPACE = configs.get(Constant.K8S_NAMESPACE);
            WORKER_IMAGE = configs.get(Constant.K8S_WORKER_IMAGE);
            MINIMAL_MEM_REQ = Long.parseLong(configs.get(Constant.K8S_MINIMAL_MEM_REQ));
            LOGGER.info("K8S Namespace: " + NAMESPACE + ", Image: " + WORKER_IMAGE + ", Minimal memory request:" +
                        MINIMAL_MEM_REQ);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Single<Worker> decide(Job job, SQLConnection conn) {
        return null;
    }

    @Override
    public boolean supportPendingJob() {
        return false;
    }

    private String buildWorkerName(Job job) {
        return "my-worker" + job.getTarget().hashCode();
    }

    @Override
    public Completable start(Job job) {

        return Completable.fromAction(() -> {
            String name = buildWorkerName(job);
            Map<String, String> config = new HashMap<>();
            if (job.getType() != JobType.FILE_TRANSFER) {
                // see AnalyzerRoute.calculateLoad
                long size = job.getEstimatedLoad() / 10 * 1024 * 1024 * 1024;
                config.put("requestMemSize", Long.toString(size));
            }
            schedule(name, config);

            // FIXME
            while (true) {
                HttpResponse<Buffer> response =
                    WorkerClient.get(getWorkerInfo(name).getIp(), Constant.uri(Constant.PING)).blockingGet();
                if (response.statusCode() == Constant.HTTP_GET_OK_STATUS_CODE) {
                    return;
                }
            }
        });
    }

    public void schedule(String id, Map<String, String> config) {
        long requestMemSize = 0L;

        String tmp = config.get("requestMemSize");
        if (tmp != null) {
            requestMemSize = Long.parseLong(tmp);
        }

        if (getWorkerInfo(id) != null) {
            LOGGER.debug("Start worker " + id + " but it already exists");
        } else {
            LOGGER.debug("Start worker " + id + "[MemRequest:" + requestMemSize + "bytes]");
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

    public WorkerInfo getWorkerInfo(String id) {
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
