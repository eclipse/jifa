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

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Api service entry
 */
public interface ApiService {

    /**
     * @return supported api map
     */
    Map<String, Set<Api>> supportedApis();

    /**
     * @param target    analysis target
     * @param namespace api namespace
     * @param api       api name or alias
     * @param arguments api arguments
     * @return api execution result
     */
    CompletableFuture<?> execute(Path target, String namespace, String api, Object[] arguments);

    /**
     * @return the instance of api service implementation
     */
    static ApiService getInstance() {
        return ApiServiceImpl.instance();
    }
}
