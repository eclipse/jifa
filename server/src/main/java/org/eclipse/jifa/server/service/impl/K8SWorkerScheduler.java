/********************************************************************************
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.server.service.impl;

import io.kubernetes.client.custom.IntOrString;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1ContainerPort;
import io.kubernetes.client.openapi.models.V1ContainerStatus;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1HTTPGetAction;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaimVolumeSource;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1PodStatus;
import io.kubernetes.client.openapi.models.V1Probe;
import io.kubernetes.client.openapi.models.V1ResourceRequirements;
import io.kubernetes.client.openapi.models.V1Volume;
import io.kubernetes.client.openapi.models.V1VolumeMount;
import io.kubernetes.client.util.Config;
import org.eclipse.jifa.common.util.Validate;
import org.eclipse.jifa.server.ConfigurationAccessor;
import org.eclipse.jifa.server.condition.ElasticCluster;
import org.eclipse.jifa.server.repository.ElasticWorkerRepo;
import org.eclipse.jifa.server.service.ElasticWorkerScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import static org.eclipse.jifa.server.Constant.DEFAULT_WORKER_PORT;
import static org.eclipse.jifa.server.Constant.ELASTIC_WORKER_IDENTITY_ENV_KEY;
import static org.eclipse.jifa.server.Constant.HTTP_API_PREFIX;
import static org.eclipse.jifa.server.Constant.HTTP_HEALTH_CHECK_MAPPING;
import static org.eclipse.jifa.server.Constant.K8S_NAMESPACE;
import static org.eclipse.jifa.server.Constant.POD_NAME_PREFIX;
import static org.eclipse.jifa.server.Constant.WORKER_CONTAINER_NAME;

@ElasticCluster
@Service
public class K8SWorkerScheduler extends ConfigurationAccessor implements ElasticWorkerScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ElasticWorkerRepo elasticWorkerRepo;

    private final CoreV1Api api;

    public K8SWorkerScheduler(ElasticWorkerRepo elasticWorkerRepo) throws IOException {
        this.elasticWorkerRepo = elasticWorkerRepo;

        ApiClient client = Config.defaultClient();
        Configuration.setDefaultApiClient(client);
        api = new CoreV1Api();
    }

    @Override
    public void scheduleAsync(long identity, long requestedMemSize, BiConsumer<String, Throwable> callback) {
        Validate.isTrue(isMaster());
        new Thread(() -> {
            String hostAddress;
            try {
                V1Volume volume = new V1Volume();
                volume.setName("jifa-pv");
                volume.persistentVolumeClaim(new V1PersistentVolumeClaimVolumeSource().claimName(config.getStoragePVCName()));

                String podName = buildPodUniqueName(identity);

                V1Pod pod = new V1Pod();
                pod.metadata(new V1ObjectMeta().name(podName));

                V1ResourceRequirements resourceRequirements = new V1ResourceRequirements()
                        .requests(Map.of("memory", new Quantity(String.valueOf(requestedMemSize))));

                V1Probe healthCheck = new V1Probe();
                healthCheck.httpGet(new V1HTTPGetAction().path(HTTP_API_PREFIX + HTTP_HEALTH_CHECK_MAPPING)
                                                         .port(new IntOrString(DEFAULT_WORKER_PORT)))
                           .initialDelaySeconds(5)
                           .periodSeconds(2)
                           .failureThreshold(30);

                V1Container container = new V1Container()
                        .name(WORKER_CONTAINER_NAME)
                        .image(config.getWorkerImage())
                        .imagePullPolicy("Always")
                        .addVolumeMountsItem(new V1VolumeMount().name("jifa-pv").mountPath(config.getStoragePath().toString()))
                        .addEnvItem(new V1EnvVar().name("MYSQL_HOST").value(System.getenv("MYSQL_HOST")))
                        .addEnvItem(new V1EnvVar().name("MYSQL_DATABASE_NAME").value(System.getenv("MYSQL_DATABASE_NAME")))
                        .addEnvItem(new V1EnvVar().name("MYSQL_USERNAME").value(System.getenv("MYSQL_USERNAME")))
                        .addEnvItem(new V1EnvVar().name("MYSQL_PASSWORD").value(System.getenv("MYSQL_PASSWORD")))
                        .addEnvItem(new V1EnvVar().name(ELASTIC_WORKER_IDENTITY_ENV_KEY).value(Long.toString(identity)))
                        .command(List.of("java","-jar","/wd/jifa.jar"))
                        .args(List.of("--jifa.role=elastic-worker",
                                      "--jifa.storage-path=" + config.getStoragePath().toString()))
                        .addPortsItem(new V1ContainerPort().containerPort(DEFAULT_WORKER_PORT))
                        .resources(resourceRequirements)
                        .startupProbe(healthCheck);

                pod.spec(new V1PodSpec().addContainersItem(container).addVolumesItem(volume)
                                        .serviceAccountName("jifa-service-account")
                                        .restartPolicy("Never"));

                api.createNamespacedPod(K8S_NAMESPACE, pod, null, null, null, null);

                while (true) {
                    pod = api.readNamespacedPod(podName, K8S_NAMESPACE, null);
                    V1PodStatus status = pod.getStatus();
                    String podIP = status != null ? status.getPodIP() : null;
                    if (podIP != null) {
                        hostAddress = podIP;
                        break;
                    }
                }

                outerLoop:
                while (true) {
                    V1PodStatus status = pod.getStatus();
                    List<V1ContainerStatus> containerStatuses = status.getContainerStatuses();
                    if (containerStatuses != null) {
                        for (V1ContainerStatus containerStatus : containerStatuses) {
                            if (WORKER_CONTAINER_NAME.equals(containerStatus.getName())) {
                                if (containerStatus.getReady()) {
                                    break outerLoop;
                                }
                            }
                        }
                    }
                    pod = api.readNamespacedPod(podName, K8S_NAMESPACE, null);
                }
            } catch (Throwable t) {
                if (t instanceof ApiException apiException) {
                    System.out.println(apiException.getResponseBody());
                    LOGGER.error("Failed to start elastic worker, response body: {}", apiException.getResponseBody());
                } else {
                    LOGGER.error("Failed to start elastic worker", t);
                }
                callback.accept(null, t);
                return;
            }

            callback.accept(hostAddress, null);
        }, "Elastic worker - " + identity + " Starter").start();
    }

    @Override
    public void terminate(long identity) throws ApiException {
        api.deleteNamespacedPod(buildPodUniqueName(identity), K8S_NAMESPACE, null, null, 0, null, null, null);
    }

    @Override
    public void terminateInconsistentInstancesQuietly() {
        try {
            V1PodList pods = api.listNamespacedPod(K8S_NAMESPACE, null, null, null, null, null, null, null, null, null, null);
            for (V1Pod pod : pods.getItems()) {
                try {
                    String name = pod.getMetadata().getName();
                    if (!name.startsWith(POD_NAME_PREFIX)) {
                        continue;
                    }
                    long identity = Long.parseLong(name.substring(POD_NAME_PREFIX.length()));
                    if (elasticWorkerRepo.findById(identity).isEmpty()) {
                        terminate(identity);
                    }
                } catch (Throwable t) {
                    LOGGER.error("Error occurred when processing pod");
                }
            }
        } catch (Throwable t) {
            LOGGER.error("Error occurred when terminating inconsistent instances", t);
        }
    }

    private String buildPodUniqueName(long identity) {
        return POD_NAME_PREFIX + identity;
    }
}
