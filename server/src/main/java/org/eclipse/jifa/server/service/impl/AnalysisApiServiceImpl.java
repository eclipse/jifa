/********************************************************************************
 * Copyright (c) 2023, 2024 Contributors to the Eclipse Foundation
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

import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jifa.analysis.Api;
import org.eclipse.jifa.analysis.ApiService;
import org.eclipse.jifa.server.ConfigurationAccessor;
import org.eclipse.jifa.server.Constant;
import org.eclipse.jifa.server.component.CurrentElasticWorker;
import org.eclipse.jifa.server.domain.dto.AnalysisApiRequest;
import org.eclipse.jifa.server.domain.entity.cluster.WorkerEntity;
import org.eclipse.jifa.server.domain.entity.shared.file.FileEntity;
import org.eclipse.jifa.server.enums.FileType;
import org.eclipse.jifa.server.service.AnalysisApiService;
import org.eclipse.jifa.server.service.FileService;
import org.eclipse.jifa.server.service.StorageService;
import org.eclipse.jifa.server.service.WorkerService;
import org.eclipse.jifa.server.support.AnalysisApiArgumentContext;
import org.eclipse.jifa.server.support.AnalysisApiArgumentResolver;
import org.eclipse.jifa.server.support.AnalysisApiArgumentResolverFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static java.util.Optional.ofNullable;
import static org.eclipse.jifa.common.domain.exception.CommonException.CE;
import static org.eclipse.jifa.server.domain.dto.HttpRequestToWorker.createPostRequest;
import static org.eclipse.jifa.server.enums.ServerErrorCode.UNSUPPORTED_API;
import static org.eclipse.jifa.server.enums.ServerErrorCode.UNSUPPORTED_NAMESPACE;

@Service
@Slf4j
public class AnalysisApiServiceImpl extends ConfigurationAccessor implements AnalysisApiService {

    private final FileService fileService;

    private final WorkerService workerService;

    private final CurrentElasticWorker currentElasticWorker;

    private final StorageService storageService;

    private ApiService apiService;

    private Map<String, Map<String, AnalysisApiArgumentResolver>> apis;

    public AnalysisApiServiceImpl(FileService fileService,
                                  @Nullable WorkerService workerService,
                                  @Nullable CurrentElasticWorker currentElasticWorker,
                                  @Nullable StorageService storageService) {
        this.fileService = fileService;
        this.workerService = workerService;
        this.currentElasticWorker = currentElasticWorker;
        this.storageService = storageService;
    }

    @PostConstruct
    private void init() {
        if (isWorker()) {
            apiService = ApiService.getInstance();
            apis = new HashMap<>();
            Map<String, Set<Api>> supportedApis = apiService.supportedApis();
            for (Map.Entry<String, Set<Api>> entry : supportedApis.entrySet()) {
                String namespace = entry.getKey();
                Map<String, AnalysisApiArgumentResolver> argumentResolverMap = new HashMap<>();
                apis.put(namespace, argumentResolverMap);
                for (Api api : entry.getValue()) {
                    AnalysisApiArgumentResolver argumentResolver = AnalysisApiArgumentResolverFactory.build(api);
                    argumentResolverMap.put(api.name(), argumentResolver);
                    if (api.aliases() == null) {
                        continue;
                    }
                    for (String alias : api.aliases()) {
                        argumentResolverMap.put(alias, argumentResolver);
                    }
                }
            }
        }
    }

    @Override
    public final CompletableFuture<?> invoke(AnalysisApiRequest request) {
        FileEntity file = fileService.getFileByUniqueName(request.target(), FileType.getByApiNamespace(request.namespace()));

        if (isMaster()) {
            assert workerService != null;
            WorkerEntity dest = fileService.getStaticWorkerByFile(file).orElse(null);
            if (dest == null) {
                dest = workerService.requestElasticWorkerForAnalysisApiRequest(file);
            }
            return workerService.asyncRequest(dest, createPostRequest(Constant.HTTP_ANALYSIS_API_MAPPING, request, byte[].class));
        }

        String namespace = request.namespace();
        String api = request.api();
        AnalysisApiArgumentResolver resolver =
                ofNullable(ofNullable(apis.get(namespace)).orElseThrow(() -> CE(UNSUPPORTED_NAMESPACE))
                                                          .get(api)).orElseThrow(() -> CE(UNSUPPORTED_API));
        assert storageService != null;
        Path targetPath = storageService.locationOf(file.getType(), file.getUniqueName());
        Object[] args = resolver.resolve(new AnalysisApiArgumentContext(file.getType(), targetPath, request.parameters(), fileService, storageService));

        if (currentElasticWorker != null) {
            currentElasticWorker.preventTermination();
        }

        try {
            return apiService.execute(targetPath, namespace, api, args)
                             .whenComplete((r, t) -> {
                                 if (currentElasticWorker != null) {
                                     currentElasticWorker.revokePreventingTermination();
                                 }
                             });
        } catch (Throwable t) {
            if (currentElasticWorker != null) {
                currentElasticWorker.revokePreventingTermination();
            }
            throw t;
        }
    }

    @Override
    public FileType deduceFileType(Path path) {
        if (!Files.exists(path)) {
            log.warn("File '{}' does not exist", path);
            return null;
        }
        if (!Files.isRegularFile(path)) {
            log.warn("File '{}' is not a regular file", path);
            return null;
        }

        File file = path.toFile();
        byte[] content = new byte[(int) Math.min(file.length(), 16 * 1024)];

        try {
            try (FileInputStream input = new FileInputStream(file)) {
                //noinspection ResultOfMethodCallIgnored
                input.read(content);
            }
            String namespace = apiService.deduceNamespaceByContent(content);
            if (namespace == null) {
                log.warn("Failed to deduce the type of file '{}'", path);
            }
            return FileType.getByApiNamespace(namespace);
        } catch (Exception e) {
            log.warn("Failed to deduce the type of file '{}': {}", path, e.getMessage());
            return null;
        }
    }
}
