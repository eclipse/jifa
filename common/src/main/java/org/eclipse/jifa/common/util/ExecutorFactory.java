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
package org.eclipse.jifa.common.util;

import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Executor factory
 */
public class ExecutorFactory {

    private static final int DEFAULT_COMMON_THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;

    private static int COMMON_THREAD_POOL_SIZE;

    private static Map<ThreadPoolExecutor, String> EXECUTORS;

    private static volatile boolean initialized;

    /**
     * set the common thread pool size
     *
     * @param commonThreadPoolSize common thread pool size
     */
    public static synchronized void initialize(int commonThreadPoolSize) {

        if (initialized) {
            throw new IllegalStateException("ExecutorFactory is already configured");
        }

        doInitialize(commonThreadPoolSize);
    }

    /**
     * Create a new executor with a specified name prefix
     *
     * @param namePrefix the thread name prefix
     * @return a new executor
     */
    public static Executor newExecutor(String namePrefix) {
        ensureInitialized();
        return newExecutor(namePrefix, COMMON_THREAD_POOL_SIZE, Integer.MAX_VALUE);
    }

    /**
     * Create a new executor with a specified name prefix, pool size and queue capacity
     *
     * @param namePrefix    the thread name prefix
     * @param nThreads      pool size
     * @param queueCapacity queue capacity
     * @return a new executor
     */
    public static Executor newExecutor(String namePrefix, int nThreads, int queueCapacity) {
        ensureInitialized();

        if (namePrefix == null || nThreads <= 0) {
            throw new IllegalArgumentException();
        }

        AtomicInteger counter = new AtomicInteger(1);

        ThreadPoolExecutor executor =
                new ThreadPoolExecutor(nThreads, nThreads,
                                       0L, TimeUnit.MILLISECONDS,
                                       new LinkedBlockingQueue<>(queueCapacity),
                                       r -> {
                                           Thread thread = new Thread(r, namePrefix + " - " + counter.getAndIncrement());
                                           thread.setDaemon(true);
                                           return thread;
                                       });

        EXECUTORS.put(executor, namePrefix);
        return executor;
    }

    /**
     * Create a new scheduled executor service with a specified name prefix and pool size
     *
     * @param namePrefix the thread name prefix
     * @param nThreads   pool size
     * @return a new executor
     */
    public static ScheduledExecutorService newScheduledExecutorService(String namePrefix, int nThreads) {
        ensureInitialized();

        if (namePrefix == null || nThreads <= 0) {
            throw new IllegalArgumentException();
        }

        AtomicInteger counter = new AtomicInteger(1);

        ScheduledThreadPoolExecutor executor =
                new ScheduledThreadPoolExecutor(nThreads,
                                                r -> {
                                                    Thread thread = new Thread(r, namePrefix + " - " + counter.getAndIncrement());
                                                    thread.setDaemon(true);
                                                    return thread;
                                                });

        EXECUTORS.put(executor, namePrefix);
        return executor;

    }

    /**
     * Print the statistic of all executors created by this factory
     *
     * @param logger the logger
     */
    public static void printStatistic(Logger logger) {
        for (Map.Entry<ThreadPoolExecutor, String> entry : EXECUTORS.entrySet()) {
            ThreadPoolExecutor pool = entry.getKey();
            logger.info("{}[{}]: active thread count = {}, total thread count = {}, queue size = {}, queue remaining capacity = {}, completed task count = {}",
                        pool instanceof ScheduledExecutorService ? "Scheduled Executor" : "Executor",
                        entry.getValue(),
                        pool.getActiveCount(),
                        pool.getMaximumPoolSize(),
                        pool.getQueue().size(),
                        pool.getQueue().remainingCapacity(),
                        pool.getCompletedTaskCount());
        }
    }

    private static void ensureInitialized() {
        if (!initialized) {
            doInitialize(DEFAULT_COMMON_THREAD_POOL_SIZE);
        }
    }

    private static synchronized void doInitialize(int commonThreadPoolSize) {
        if (initialized) {
            return;
        }

        COMMON_THREAD_POOL_SIZE = commonThreadPoolSize;
        EXECUTORS = new ConcurrentHashMap<>();
        initialized = true;
    }
}
