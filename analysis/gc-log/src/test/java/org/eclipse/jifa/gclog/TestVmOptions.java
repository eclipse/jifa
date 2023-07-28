/********************************************************************************
 * Copyright (c) 2022, 2023 Contributors to the Eclipse Foundation
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

package org.eclipse.jifa.gclog;

import org.eclipse.jifa.gclog.model.modeInfo.VmOptions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class TestVmOptions {
    @Test
    public void testVmOptions() {
        String optionString =
                "-server " +
                        "-XX:+UseG1GC " +
                        "-XX:-DisableExplicitGC " +
                        "-verbose:gc " +
                        "-Xloggc:gc.log " +
                        "-XX:+PrintGCDetails " +
                        "-XX:+PrintGCDateStamps " +
                        "-XX:+HeapDumpOnOutOfMemoryError " +
                        "-XX:HeapDumpPath=/home/admin/logs " +
                        "-XX:ErrorFile=/home/admin/logs/hs_err_pid%p.log " +
                        "-Xms4200m " +
                        "-Xmx4200m " +
                        "-XX:ParallelGCThreads=8 " +
                        "-XX:MaxNewSize=1500m " +
                        "-XX:InitiatingHeapOccupancyPercent=50 " +
                        "-XX:G1HeapRegionSize=8m " +
                        "-Xss512k " +
                        "-XX:MetaspaceSize=10240 " +
                        "-XX:MaxMetaspaceSize=512m\n";
        VmOptions options = new VmOptions(optionString);
        Assertions.assertEquals(options.getOriginalOptionString(), optionString);
        Assertions.assertNull(options.getOptionValue("Xmn"));
        Assertions.assertEquals(4200L * 1024 * 1024 * 1024, (long) options.getOptionValue("Xmx"));
        Assertions.assertTrue((boolean) options.getOptionValue("server"));
        Assertions.assertFalse((boolean)options.getOptionValue("DisableExplicitGC"));
        Assertions.assertEquals(50L, (long) options.getOptionValue("InitiatingHeapOccupancyPercent"));
        Assertions.assertEquals(10240L, (long) options.getOptionValue("MetaspaceSize"));
        Assertions.assertEquals("/home/admin/logs/hs_err_pid%p.log", options.getOptionValue("ErrorFile"));
        Assertions.assertEquals("gc", options.getOptionValue("verbose"));
        Assertions.assertEquals("gc.log", options.getOptionValue("Xloggc"));

        VmOptions.VmOptionResult result = options.getVmOptionResult();
        Assertions.assertTrue(result.getOther().contains(new VmOptions.VmOptionVo("-XX:ErrorFile=/home/admin/logs/hs_err_pid%p.log")));
        Map<String, Integer> optionIndex = new HashMap<>();
        for (int i = 0; i < result.getGcRelated().size(); i++) {
            optionIndex.put(result.getGcRelated().get(i).getText(), i);
        }
        Assertions.assertTrue(optionIndex.get("-XX:+UseG1GC") < optionIndex.get("-Xms4200m"));
        Assertions.assertTrue(optionIndex.get("-Xms4200m") < optionIndex.get("-XX:ParallelGCThreads=8"));
        Assertions.assertTrue(optionIndex.get("-XX:ParallelGCThreads=8") < optionIndex.get("-XX:InitiatingHeapOccupancyPercent=50"));
        Assertions.assertTrue(optionIndex.get("-XX:InitiatingHeapOccupancyPercent=50") < optionIndex.get("-XX:-DisableExplicitGC"));
        Assertions.assertTrue(optionIndex.get("-XX:-DisableExplicitGC") < optionIndex.get("-XX:+PrintGCDetails"));
    }
}
