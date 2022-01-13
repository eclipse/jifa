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

package org.eclipse.jifa.gclog;

import org.eclipse.jifa.gclog.model.VmOptions;
import org.junit.Assert;
import org.junit.Test;

public class TestVmOptions {
    @Test
    public void testVmOptions(){
        String optionString =
                        "-server " +
                        "-XX:+UseG1GC " +
                        "-XX:-DisableExplicitGC " +
                        "-verbose:gc " +
                        "-XX:+PrintGCDetails " +
                        "-XX:+PrintGCDateStamps " +
                        "-XX:+HeapDumpOnOutOfMemoryError " +
                        "-XX:HeapDumpPath=/home/admin/logs " +
                        "-XX:ErrorFile=/home/admin/logs/hs_err_pid%p.log " +
                        "-Xms4200m " +
                        "-Xmx4200m " +
                        "-XX:MaxNewSize=1500m " +
                        "-XX:InitiatingHeapOccupancyPercent=50 " +
                        "-XX:G1HeapRegionSize=8m " +
                        "-Xss512k " +
                        "-XX:MetaspaceSize=10240 " +
                        "-XX:MaxMetaspaceSize=512m\n";
        VmOptions options = new VmOptions(optionString);
        Assert.assertEquals(options.getOriginalOptionString(),optionString);
        Assert.assertNull(options.getOptionValue("Xmn"));
        Assert.assertEquals(4200L * 1024 * 1024, (long)options.getOptionValue("Xmx"));
        Assert.assertTrue( options.getOptionValue("server"));
        Assert.assertFalse( options.getOptionValue("DisableExplicitGC"));
        Assert.assertEquals(50L,(long)options.getOptionValue("InitiatingHeapOccupancyPercent"));
        Assert.assertEquals(10240L,(long)options.getOptionValue("MetaspaceSize"));
        Assert.assertEquals("/home/admin/logs/hs_err_pid%p.log",options.getOptionValue("ErrorFile"));
    }
}
