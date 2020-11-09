/********************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.worker.route;

import com.google.common.io.Files;
import com.sun.management.HotSpotDiagnosticMXBean;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.commons.io.FileUtils;
import org.eclipse.jifa.common.enums.FileTransferState;
import org.eclipse.jifa.common.enums.FileType;
import org.eclipse.jifa.worker.Worker;
import org.eclipse.jifa.worker.WorkerGlobal;
import org.eclipse.jifa.worker.support.FileSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.management.ManagementFactory;

@RunWith(VertxUnitRunner.class)
public class TestRoutes {

    private static Logger LOGGER = LoggerFactory.getLogger(TestRoutes.class);

    @Before
    public void setup(TestContext context) throws Exception {
        // start worker
        Worker.main(new String[]{});

        // prepare heap dump file
        HotSpotDiagnosticMXBean mxBean = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
        String name = "test_dump_" + System.currentTimeMillis() + ".hprof";
        Base.TEST_HEAP_DUMP_FILENAME = name;
        mxBean.dumpHeap(name, false);
        FileSupport.initInfoFile(FileType.HEAP_DUMP, name, name);
        Files.move(new File(name), new File(FileSupport.filePath(FileType.HEAP_DUMP, name)));
        FileSupport.updateTransferState(FileType.HEAP_DUMP, name, FileTransferState.SUCCESS);
    }

    @Test
    public void testRoutes(TestContext context) throws Exception {
        FileRouteSuite.test(context);
        HeapDumpRouteSuite.test(context);
    }

    @After
    public void tearDown(TestContext context) {
        try {
            System.out.println(context);
            FileUtils.deleteDirectory(new File(WorkerGlobal.workspace()));
            WorkerGlobal.VERTX.close(context.asyncAssertSuccess());
        } catch (Throwable t) {
            LOGGER.error("Error", t);
        }
    }
}
