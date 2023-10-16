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
package org.eclipse.jifa.analysis;

import org.eclipse.jifa.common.util.Validate;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class ApiServiceImpl implements ApiService {

    private Map<String, Set<Api>> apis;

    private Map<String, ApiExecutor> executors;

    private Map<String, Predicate<byte[]>> matchers;

    private ApiServiceImpl() {
        loadExecutors();
    }

    private void loadExecutors() {
        Map<String, Set<Api>> apis = new HashMap<>();
        Map<String, ApiExecutor> executors = new HashMap<>();
        Map<String, Predicate<byte[]>> matchers = new HashMap<>();

        for (ApiExecutor executor : ServiceLoader.load(ApiExecutor.class)) {
            String namespace = executor.namespace();
            Validate.isTrue(!apis.containsKey(namespace));

            apis.put(namespace, executor.apis());
            executors.put(namespace, executor);
            Predicate<byte[]> matcher = executor.matcher();
            if (matcher != null) {
                matchers.put(namespace, matcher);
            }
        }

        this.apis = Collections.unmodifiableMap(apis);
        this.executors = Collections.unmodifiableMap(executors);
        this.matchers = Collections.unmodifiableMap(matchers);
    }

    @Override
    public Map<String, Set<Api>> supportedApis() {
        return apis;
    }

    @Override
    public CompletableFuture<?> execute(Path target, String namespace, String api, Object[] arguments) {
        Validate.notNull(target, "target must not be null");
        Validate.notNull(namespace, "namespace must not be null");
        Validate.notNull(api, "api must not be null");

        ApiExecutor executor = this.executors.get(namespace);
        Validate.notNull(executor, () -> "Unsupported namespace: " + namespace);
        return executor.execute(new ExecutionContext(target, api, arguments));
    }

    @Override
    public String deduceNamespaceByContent(byte[] content) {
        for (Map.Entry<String, Predicate<byte[]>> entry : matchers.entrySet()) {
            if (entry.getValue().test(content)) {
                return entry.getKey();
            }
        }
        return null;
    }

    static ApiService instance() {
        return Holder.INSTANCE;
    }

    private static class Holder {
        static final ApiServiceImpl INSTANCE = new ApiServiceImpl();
    }
}
