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

import org.junit.jupiter.api.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestExecutorFactory {

    @Test
    public void test() throws InterruptedException {
        Executor executor = ExecutorFactory.newExecutor("test1");
        assertNotNull(executor);
        CountDownLatch countDownLatch1 = new CountDownLatch(1);
        executor.execute(() -> {
            if (Thread.currentThread().getName().startsWith("test1")) {
                countDownLatch1.countDown();
            }
        });
        assertTrue(countDownLatch1.await(1, TimeUnit.SECONDS));

        executor = ExecutorFactory.newExecutor("test2", 1, 1);
        assertNotNull(executor);
        CountDownLatch countDownLatch2 = new CountDownLatch(1);
        executor.execute(() -> {
            if (Thread.currentThread().getName().startsWith("test2")) {
                countDownLatch2.countDown();
            }
        });
        assertTrue(countDownLatch2.await(1, TimeUnit.SECONDS));

        ScheduledExecutorService scheduledExecutorService = ExecutorFactory.newScheduledExecutorService("test3", 1);
        assertNotNull(scheduledExecutorService);
        CountDownLatch countDownLatch3 = new CountDownLatch(1);
        scheduledExecutorService.schedule(() -> {
            if (Thread.currentThread().getName().startsWith("test3")) {
                countDownLatch3.countDown();
            }
        }, 100, TimeUnit.MILLISECONDS);
        assertTrue(countDownLatch3.await(1, TimeUnit.SECONDS));

        ExecutorFactory.printStatistic();
    }

    @Test
    public void testThrows() {
        assertThrows(IllegalStateException.class, () -> {
            ExecutorFactory.initialize(8);
            ExecutorFactory.initialize(8);
        });

        assertThrows(IllegalArgumentException.class, () -> ExecutorFactory.newExecutor(null, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> ExecutorFactory.newExecutor("prefix", 0, 1));
        assertThrows(IllegalArgumentException.class, () -> ExecutorFactory.newScheduledExecutorService(null, 1));
        assertThrows(IllegalArgumentException.class, () -> ExecutorFactory.newScheduledExecutorService("prefix", 0));
    }
}
