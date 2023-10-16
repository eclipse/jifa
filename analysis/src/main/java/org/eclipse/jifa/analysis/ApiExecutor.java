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

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/**
 * Analysis api executor
 */
public interface ApiExecutor {

    /**
     * @return the namespace supported by this executor
     */
    String namespace();

    /**
     * @return the apis supported by this executor
     */
    Set<Api> apis();

    /**
     * Execute the api specified by the context
     *
     * @param context the execution context
     * @return result
     */
    CompletableFuture<?> execute(ExecutionContext context);

    /**
     * @return a matcher to tell the byte array is supported by this executor, default is null
     */
    default Predicate<byte[]> matcher() {
        return null;
    }
}
