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
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Config;
import org.eclipse.jifa.master.Constant;
import org.eclipse.jifa.master.model.WorkerInfo;
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
    public void initialize(Map<String, String> params) {
        // Orders are important
        ApiClient client;
        try {
            client = Config.defaultClient();
            Configuration.setDefaultApiClient(client);
            api = new CoreV1Api();
            NAMESPACE = params.get(Constant.K8S_NAMESPACE);
            WORKER_IMAGE = params.get(Constant.K8S_WORKER_IMAGE);
            MINIMAL_MEM_REQ = Long.parseLong(params.get(Constant.K8S_MINIMAL_MEM_REQ));
            LOGGER.info("K8S Namespace: " + NAMESPACE + ", Image: " + WORKER_IMAGE + ", Minimal memory request:" + MINIMAL_MEM_REQ);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void startWorker(String id, Map<String, String> params) {
        long requestMemSize = 0L;

        String tmp = params.get("requestMemSize");
        if (tmp != null) {
            requestMemSize = Long.parseLong(tmp);
        }

        if (getWorkerInfo(id) != null) {
            LOGGER.debug("Start worker " + id + " but it already exists");
        } else {
            LOGGER.debug("Start worker " + id + "[MemRequest:" + requestMemSize + "bytes]");
            K8SWorkerScheduler.createWorker(id, requestMemSize);
        }
    }

    @Override
    public void stopWorker(String id) {
        if (getWorkerInfo(id) == null) {
            LOGGER.debug("Stop worker " + id + " but it does not exist");
        } else {
            LOGGER.debug("Stop worker " + id);
            removeWorker(id);
        }
    }

    @Override
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
