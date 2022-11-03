/********************************************************************************
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
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

package org.eclipse.jifa.gclog.parser;

import org.eclipse.jifa.common.util.ErrorUtil;
import org.eclipse.jifa.gclog.event.GCEvent;
import org.eclipse.jifa.gclog.event.ThreadEvent;
import org.eclipse.jifa.gclog.event.evnetInfo.GCMemoryItem;
import org.eclipse.jifa.gclog.model.GCEventType;
import org.eclipse.jifa.gclog.model.GCModel;
import org.eclipse.jifa.gclog.model.ZGCModel;
import org.eclipse.jifa.gclog.model.ZGCModel.ZStatistics;
import org.eclipse.jifa.gclog.util.GCLogUtil;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.jifa.gclog.util.Constant.UNKNOWN_INT;
import static org.eclipse.jifa.gclog.event.evnetInfo.MemoryArea.METASPACE;
import static org.eclipse.jifa.gclog.event.evnetInfo.MemoryArea.HEAP;
import static org.eclipse.jifa.gclog.model.GCEventType.*;
import static org.eclipse.jifa.gclog.parser.ParseRule.ParseRuleContext.GCID;
import static org.eclipse.jifa.gclog.parser.ParseRule.ParseRuleContext.UPTIME;

public class JDK11ZGCLogParser extends AbstractJDK11GCLogParser {
    /*
     * [2021-08-31T08:08:17.108+0800] GC(374) Garbage Collection (Proactive)
     * [2021-08-31T08:08:17.114+0800] GC(374) Pause Mark Start 4.459ms
     * [2021-08-31T08:08:17.421+0800] GC(374) Concurrent Mark 306.720ms
     * [2021-08-31T08:08:17.423+0800] GC(374) Pause Mark End 0.606ms
     * [2021-08-31T08:08:17.424+0800] GC(374) Concurrent Process Non-Strong References 1.290ms
     * [2021-08-31T08:08:17.424+0800] GC(374) Pause Class Unloading 10.234ms
     * [2021-08-31T08:08:17.425+0800] GC(374) Concurrent Reset Relocation Set 0.550ms
     * [2021-08-31T08:08:17.425+0800] GC(374) Concurrent Destroy Detached Pages 0.001ms
     * [2021-08-31T08:08:17.427+0800] GC(374) Concurrent Select Relocation Set 2.418ms
     * [2021-08-31T08:08:17.433+0800] GC(374) Concurrent Prepare Relocation Set 5.719ms
     * [2021-08-31T08:08:17.438+0800] GC(374) Pause Relocate Start 3.791ms
     * [2021-08-31T08:08:17.471+0800] GC(374) Concurrent Relocate 32.974ms
     * [2021-08-31T08:08:17.471+0800] GC(374) Load: 1.68/1.99/2.04
     * [2021-08-31T08:08:17.471+0800] GC(374) MMU: 2ms/0.0%, 5ms/0.0%, 10ms/0.0%, 20ms/0.0%, 50ms/0.0%, 100ms/0.0%
     * [2021-08-31T08:08:17.471+0800] GC(374) Mark: 8 stripe(s), 2 proactive flush(es), 1 terminate flush(es), 0 completion(s), 0 continuation(s)
     * [2021-08-31T08:08:17.471+0800] GC(374) Relocation: Successful, 359M relocated
     * [2021-08-31T08:08:17.471+0800] GC(374) NMethods: 21844 registered, 609 unregistered
     * [2021-08-31T08:08:17.471+0800] GC(374) Metaspace: 125M used, 128M capacity, 128M committed, 130M reserved
     * [2021-08-31T08:08:17.471+0800] GC(374) Soft: 18634 encountered, 0 discovered, 0 enqueued
     * [2021-08-31T08:08:17.471+0800] GC(374) Weak: 56186 encountered, 18454 discovered, 3112 enqueued
     * [2021-08-31T08:08:17.471+0800] GC(374) Final: 64 encountered, 16 discovered, 7 enqueued
     * [2021-08-31T08:08:17.471+0800] GC(374) Phantom: 1882 encountered, 1585 discovered, 183 enqueued
     * [2021-08-31T08:08:17.471+0800] GC(374)                Mark Start          Mark End        Relocate Start      Relocate End           High               Low
     * [2021-08-31T08:08:17.471+0800] GC(374)  Capacity:    40960M (100%)      40960M (100%)      40960M (100%)      40960M (100%)      40960M (100%)      40960M (100%)
     * [2021-08-31T08:08:17.471+0800] GC(374)   Reserve:       96M (0%)           96M (0%)           96M (0%)           96M (0%)           96M (0%)           96M (0%)
     * [2021-08-31T08:08:17.471+0800] GC(374)      Free:    35250M (86%)       35210M (86%)       35964M (88%)       39410M (96%)       39410M (96%)       35210M (86%)
     * [2021-08-31T08:08:17.471+0800] GC(374)      Used:     5614M (14%)        5654M (14%)        4900M (12%)        1454M (4%)         5654M (14%)        1454M (4%)
     * [2021-08-31T08:08:17.471+0800] GC(374)      Live:         -              1173M (3%)         1173M (3%)         1173M (3%)             -                  -
     * [2021-08-31T08:08:17.471+0800] GC(374) Allocated:         -                40M (0%)           40M (0%)          202M (0%)             -                  -
     * [2021-08-31T08:08:17.471+0800] GC(374)   Garbage:         -              4440M (11%)        3686M (9%)          240M (1%)             -                  -
     * [2021-08-31T08:08:17.471+0800] GC(374) Reclaimed:         -                  -               754M (2%)         4200M (10%)            -                  -
     * [2021-08-31T08:08:17.471+0800] GC(374) Garbage Collection (Proactive) 5614M(14%)->1454M(4%)
     * [2021-08-31T08:08:25.209+0800] === Garbage Collection Statistics =======================================================================================================================
     * [2021-08-31T08:08:25.210+0800]                                                              Last 10s              Last 10m              Last 10h                Total
     * [2021-08-31T08:08:25.210+0800]                                                              Avg / Max             Avg / Max             Avg / Max             Avg / Max
     * [2021-08-31T08:08:25.210+0800]   Collector: Garbage Collection Cycle                    362.677 / 362.677     365.056 / 529.211     315.229 / 868.961     315.229 / 868.961     ms
     * [2021-08-31T08:08:25.210+0800]  Contention: Mark Segment Reset Contention                     0 / 0                 1 / 106               0 / 238               0 / 238         ops/s
     * [2021-08-31T08:08:25.210+0800]  Contention: Mark SeqNum Reset Contention                      0 / 0                 0 / 1                 0 / 1                 0 / 1           ops/s
     * [2021-08-31T08:08:25.210+0800]  Contention: Relocation Contention                             1 / 10                0 / 52                0 / 87                0 / 87          ops/s
     * [2021-08-31T08:08:25.210+0800]    Critical: Allocation Stall                              0.000 / 0.000         0.000 / 0.000         0.000 / 0.000         0.000 / 0.000       ms
     * [2021-08-31T08:08:25.210+0800]    Critical: Allocation Stall                                  0 / 0                 0 / 0                 0 / 0                 0 / 0           ops/s
     * [2021-08-31T08:08:25.210+0800]    Critical: GC Locker Stall                               0.000 / 0.000         0.000 / 0.000         0.000 / 0.000         0.000 / 0.000       ms
     * [2021-08-31T08:08:25.210+0800]    Critical: GC Locker Stall                                   0 / 0                 0 / 0                 0 / 0                 0 / 0           ops/s
     * [2021-08-31T08:08:25.210+0800]      Memory: Allocation Rate                                  85 / 210             104 / 826              54 / 2628             54 / 2628        MB/s
     * [2021-08-31T08:08:25.210+0800]      Memory: Heap Used After Mark                           5654 / 5654           5727 / 6416           5588 / 14558          5588 / 14558       MB
     * [2021-08-31T08:08:25.210+0800]      Memory: Heap Used After Relocation                     1454 / 1454           1421 / 1814           1224 / 2202           1224 / 2202        MB
     * [2021-08-31T08:08:25.210+0800]      Memory: Heap Used Before Mark                          5614 / 5614           5608 / 6206           5503 / 14268          5503 / 14268       MB
     * [2021-08-31T08:08:25.210+0800]      Memory: Heap Used Before Relocation                    4900 / 4900           4755 / 5516           4665 / 11700          4665 / 11700       MB
     * [2021-08-31T08:08:25.210+0800]      Memory: Out Of Memory                                     0 / 0                 0 / 0                 0 / 0                 0 / 0           ops/s
     * [2021-08-31T08:08:25.210+0800]      Memory: Page Cache Flush                                  0 / 0                 0 / 0                 0 / 0                 0 / 0           MB/s
     * [2021-08-31T08:08:25.210+0800]      Memory: Page Cache Hit L1                                49 / 105              53 / 439              27 / 1353             27 / 1353        ops/s
     * [2021-08-31T08:08:25.210+0800]      Memory: Page Cache Hit L2                                 0 / 0                 0 / 0                 0 / 0                 0 / 0           ops/s
     * [2021-08-31T08:08:25.210+0800]      Memory: Page Cache Miss                                   0 / 0                 0 / 0                 0 / 551               0 / 551         ops/s
     * [2021-08-31T08:08:25.210+0800]      Memory: Undo Object Allocation Failed                     0 / 0                 0 / 0                 0 / 8                 0 / 8           ops/s
     * [2021-08-31T08:08:25.210+0800]      Memory: Undo Object Allocation Succeeded                  1 / 10                0 / 52                0 / 87                0 / 87          ops/s
     * [2021-08-31T08:08:25.210+0800]      Memory: Undo Page Allocation                              0 / 0                 0 / 1                 0 / 16                0 / 16          ops/s
     * [2021-08-31T08:08:25.210+0800]       Phase: Concurrent Destroy Detached Pages             0.001 / 0.001         0.001 / 0.001         0.001 / 0.012         0.001 / 0.012       ms
     * [2021-08-31T08:08:25.210+0800]       Phase: Concurrent Mark                             306.720 / 306.720     303.979 / 452.112     255.790 / 601.718     255.790 / 601.718     ms
     * [2021-08-31T08:08:25.210+0800]       Phase: Concurrent Mark Continue                      0.000 / 0.000         0.000 / 0.000       189.372 / 272.607     189.372 / 272.607     ms
     * [2021-08-31T08:08:25.210+0800]       Phase: Concurrent Prepare Relocation Set             5.719 / 5.719         6.314 / 14.492        6.150 / 36.507        6.150 / 36.507      ms
     * [2021-08-31T08:08:25.210+0800]       Phase: Concurrent Process Non-Strong References      1.290 / 1.290         1.212 / 1.657         1.179 / 2.334         1.179 / 2.334       ms
     * [2021-08-31T08:08:25.210+0800]       Phase: Concurrent Relocate                          32.974 / 32.974       35.964 / 86.278       31.599 / 101.253      31.599 / 101.253     ms
     * [2021-08-31T08:08:25.210+0800]       Phase: Concurrent Reset Relocation Set               0.550 / 0.550         0.615 / 0.937         0.641 / 5.411         0.641 / 5.411       ms
     * [2021-08-31T08:08:25.210+0800]       Phase: Concurrent Select Relocation Set              2.418 / 2.418         2.456 / 3.131         2.509 / 4.753         2.509 / 4.753       ms
     * [2021-08-31T08:08:25.210+0800]       Phase: Pause Mark End                                0.606 / 0.606         0.612 / 0.765         0.660 / 5.543         0.660 / 5.543       ms
     * [2021-08-31T08:08:25.210+0800]       Phase: Pause Mark Start                              4.459 / 4.459         4.636 / 6.500         6.160 / 547.572       6.160 / 547.572     ms
     * [2021-08-31T08:08:25.210+0800]       Phase: Pause Relocate Start                          3.791 / 3.791         3.970 / 5.443         4.047 / 8.993         4.047 / 8.993       ms
     * [2021-08-31T08:08:25.210+0800]    Subphase: Concurrent Mark                             306.253 / 306.593     303.509 / 452.030     254.759 / 601.564     254.759 / 601.564     ms
     * [2021-08-31T08:08:25.210+0800]    Subphase: Concurrent Mark Idle                          1.069 / 1.110         1.527 / 18.317        1.101 / 18.317        1.101 / 18.317      ms
     * [2021-08-31T08:08:25.210+0800]    Subphase: Concurrent Mark Try Flush                     0.554 / 0.685         0.872 / 18.247        0.507 / 18.247        0.507 / 18.247      ms
     * [2021-08-31T08:08:25.210+0800]    Subphase: Concurrent Mark Try Terminate                 0.978 / 1.112         1.386 / 18.318        0.998 / 18.318        0.998 / 18.318      ms
     * [2021-08-31T08:08:25.210+0800]    Subphase: Concurrent References Enqueue                 0.007 / 0.007         0.008 / 0.013         0.009 / 0.037         0.009 / 0.037       ms
     * [2021-08-31T08:08:25.210+0800]    Subphase: Concurrent References Process                 0.628 / 0.628         0.638 / 1.153         0.596 / 1.789         0.596 / 1.789       ms
     * [2021-08-31T08:08:25.210+0800]    Subphase: Concurrent Weak Roots                         0.497 / 0.618         0.492 / 0.670         0.502 / 1.001         0.502 / 1.001       ms
     * [2021-08-31T08:08:25.210+0800]    Subphase: Concurrent Weak Roots JNIWeakHandles          0.001 / 0.001         0.001 / 0.006         0.001 / 0.007         0.001 / 0.007       ms
     * [2021-08-31T08:08:25.210+0800]    Subphase: Concurrent Weak Roots StringTable             0.476 / 0.492         0.402 / 0.523         0.400 / 0.809         0.400 / 0.809       ms
     * [2021-08-31T08:08:25.210+0800]    Subphase: Concurrent Weak Roots VMWeakHandles           0.105 / 0.123         0.098 / 0.150         0.103 / 0.903         0.103 / 0.903       ms
     * [2021-08-31T08:08:25.210+0800]    Subphase: Pause Mark Try Complete                       0.000 / 0.000         0.001 / 0.004         0.156 / 1.063         0.156 / 1.063       ms
     * [2021-08-31T08:08:25.210+0800]    Subphase: Pause Remap TLABS                             0.040 / 0.040         0.046 / 0.073         0.050 / 0.140         0.050 / 0.140       ms
     * [2021-08-31T08:08:25.210+0800]    Subphase: Pause Retire TLABS                            0.722 / 0.722         0.835 / 1.689         0.754 / 1.919         0.754 / 1.919       ms
     * [2021-08-31T08:08:25.210+0800]    Subphase: Pause Roots                                   1.581 / 2.896         1.563 / 3.787         1.592 / 545.902       1.592 / 545.902     ms
     * [2021-08-31T08:08:25.210+0800]    Subphase: Pause Roots ClassLoaderDataGraph              1.461 / 2.857         1.549 / 3.782         1.554 / 6.380         1.554 / 6.380       ms
     * [2021-08-31T08:08:25.210+0800]    Subphase: Pause Roots CodeCache                         1.130 / 1.312         0.999 / 1.556         0.988 / 6.322         0.988 / 6.322       ms
     * [2021-08-31T08:08:25.210+0800]    Subphase: Pause Roots JNIHandles                        0.010 / 0.015         0.004 / 0.028         0.005 / 1.709         0.005 / 1.709       ms
     * [2021-08-31T08:08:25.210+0800]    Subphase: Pause Roots JNIWeakHandles                    0.000 / 0.000         0.000 / 0.000         0.000 / 0.000         0.000 / 0.000       ms
     * [2021-08-31T08:08:25.210+0800]    Subphase: Pause Roots JRFWeak                           0.000 / 0.000         0.000 / 0.000         0.000 / 0.000         0.000 / 0.000       ms
     * [2021-08-31T08:08:25.210+0800]    Subphase: Pause Roots JVMTIExport                       0.001 / 0.001         0.001 / 0.003         0.001 / 0.005         0.001 / 0.005       ms
     * [2021-08-31T08:08:25.210+0800]    Subphase: Pause Roots JVMTIWeakExport                   0.001 / 0.001         0.001 / 0.001         0.001 / 0.012         0.001 / 0.012       ms
     * [2021-08-31T08:08:25.210+0800]    Subphase: Pause Roots Management                        0.002 / 0.002         0.003 / 0.006         0.003 / 0.305         0.003 / 0.305       ms
     * [2021-08-31T08:08:25.210+0800]    Subphase: Pause Roots ObjectSynchronizer                0.000 / 0.000         0.000 / 0.001         0.000 / 0.006         0.000 / 0.006       ms
     * [2021-08-31T08:08:25.210+0800]    Subphase: Pause Roots Setup                             0.474 / 0.732         0.582 / 1.791         0.526 / 2.610         0.526 / 2.610       ms
     * [2021-08-31T08:08:25.210+0800]    Subphase: Pause Roots StringTable                       0.000 / 0.000         0.000 / 0.000         0.000 / 0.000         0.000 / 0.000       ms
     * [2021-08-31T08:08:25.210+0800]    Subphase: Pause Roots SystemDictionary                  0.028 / 0.039         0.027 / 0.075         0.033 / 2.777         0.033 / 2.777       ms
     * [2021-08-31T08:08:25.210+0800]    Subphase: Pause Roots Teardown                          0.003 / 0.005         0.003 / 0.009         0.003 / 0.035         0.003 / 0.035       ms
     * [2021-08-31T08:08:25.210+0800]    Subphase: Pause Roots Threads                           0.262 / 1.237         0.309 / 1.791         0.358 / 544.610       0.358 / 544.610     ms
     * [2021-08-31T08:08:25.210+0800]    Subphase: Pause Roots Universe                          0.003 / 0.004         0.003 / 0.009         0.003 / 0.047         0.003 / 0.047       ms
     * [2021-08-31T08:08:25.210+0800]    Subphase: Pause Roots VMWeakHandles                     0.000 / 0.000         0.000 / 0.000         0.000 / 0.000         0.000 / 0.000       ms
     * [2021-08-31T08:08:25.210+0800]    Subphase: Pause Weak Roots                              0.000 / 0.003         0.000 / 0.007         0.000 / 0.020         0.000 / 0.020       ms
     * [2021-08-31T08:08:25.210+0800]    Subphase: Pause Weak Roots JFRWeak                      0.001 / 0.001         0.001 / 0.002         0.001 / 0.012         0.001 / 0.012       ms
     * [2021-08-31T08:08:25.210+0800]    Subphase: Pause Weak Roots JNIWeakHandles               0.000 / 0.000         0.000 / 0.000         0.000 / 0.000         0.000 / 0.000       ms
     * [2021-08-31T08:08:25.210+0800]    Subphase: Pause Weak Roots JVMTIWeakExport              0.001 / 0.001         0.001 / 0.001         0.001 / 0.008         0.001 / 0.008       ms
     * [2021-08-31T08:08:25.210+0800]    Subphase: Pause Weak Roots Setup                        0.000 / 0.000         0.000 / 0.000         0.000 / 0.001         0.000 / 0.001       ms
     * [2021-08-31T08:08:25.210+0800]    Subphase: Pause Weak Roots StringTable                  0.000 / 0.000         0.000 / 0.000         0.000 / 0.000         0.000 / 0.000       ms
     * [2021-08-31T08:08:25.210+0800]    Subphase: Pause Weak Roots SymbolTable                  0.000 / 0.000         0.000 / 0.000         0.000 / 0.000         0.000 / 0.000       ms
     * [2021-08-31T08:08:25.210+0800]    Subphase: Pause Weak Roots Teardown                     0.001 / 0.001         0.001 / 0.001         0.001 / 0.015         0.001 / 0.015       ms
     * [2021-08-31T08:08:25.210+0800]    Subphase: Pause Weak Roots VMWeakHandles                0.000 / 0.000         0.000 / 0.000         0.000 / 0.000         0.000 / 0.000       ms
     * [2021-08-31T08:08:25.210+0800]      System: Java Threads                                    911 / 911             910 / 911             901 / 913             901 / 913         threads
     * [2021-08-31T08:08:25.210+0800] =========================================================================================================================================================
     * [2021-08-31T11:29:12.823+0800] Allocation Stall (ThreadPoolTaskScheduler-1) 0.204ms
     * [2021-08-31T11:29:12.823+0800] Allocation Stall (http-nio-8080-exec-71) 1.032ms
     * [2021-08-31T11:29:12.823+0800] Allocation Stall (NioProcessor-2) 0.391ms
     * [2021-08-31T11:29:12.823+0800] Allocation Stall (http-nio-8080-exec-85) 0.155ms
     * [2021-08-31T11:29:12.823+0800] Allocation Stall (http-nio-8080-exec-49) 277.588ms
     * [2021-08-31T11:29:12.825+0800] Out Of Memory (thread 8)
     */
    private static List<ParseRule> withGCIDRules;

    private static List<ParseRule> withoutGCIDRules;

    static {
        initializeParseRules();
    }

    private static void initializeParseRules() {
        withoutGCIDRules = new ArrayList<>(AbstractJDK11GCLogParser.getSharedWithoutGCIDRules());
        withoutGCIDRules.add(new ParseRule.PrefixAndValueParseRule("Allocation Stall", JDK11ZGCLogParser::parseAllocationStall));
        withoutGCIDRules.add(new ParseRule.PrefixAndValueParseRule("Out Of Memory", JDK11ZGCLogParser::pauseOutOfMemory));
        withoutGCIDRules.add(JDK11ZGCLogParser::parseZGCStatisticLine);

        withGCIDRules = new ArrayList<>(AbstractJDK11GCLogParser.getSharedWithGCIDRules());
        withGCIDRules.add(new ParseRule.PrefixAndValueParseRule("Pause Mark Start", JDK11ZGCLogParser::parsePhase));
        withGCIDRules.add(new ParseRule.PrefixAndValueParseRule("Concurrent Mark", JDK11ZGCLogParser::parsePhase));
        withGCIDRules.add(new ParseRule.PrefixAndValueParseRule("Pause Mark End", JDK11ZGCLogParser::parsePhase));
        withGCIDRules.add(new ParseRule.PrefixAndValueParseRule("Concurrent Process Non-Strong References", JDK11ZGCLogParser::parsePhase));
        withGCIDRules.add(new ParseRule.PrefixAndValueParseRule("Concurrent Reset Relocation Set", JDK11ZGCLogParser::parsePhase));
        withGCIDRules.add(new ParseRule.PrefixAndValueParseRule("Concurrent Destroy Detached Pages", JDK11ZGCLogParser::parsePhase));
        withGCIDRules.add(new ParseRule.PrefixAndValueParseRule("Concurrent Select Relocation Set", JDK11ZGCLogParser::parsePhase));
        withGCIDRules.add(new ParseRule.PrefixAndValueParseRule("Concurrent Prepare Relocation Set", JDK11ZGCLogParser::parsePhase));
        withGCIDRules.add(new ParseRule.PrefixAndValueParseRule("Pause Relocate Start", JDK11ZGCLogParser::parsePhase));
        withGCIDRules.add(new ParseRule.PrefixAndValueParseRule("Concurrent Relocate", JDK11ZGCLogParser::parsePhase));
        withGCIDRules.add(new ParseRule.PrefixAndValueParseRule("Metaspace", JDK11ZGCLogParser::parseMetaspace));
        // some heap items are not listed because we do not use them
        withGCIDRules.add(new ParseRule.PrefixAndValueParseRule(" Capacity", JDK11ZGCLogParser::parseHeap));
        withGCIDRules.add(new ParseRule.PrefixAndValueParseRule("     Used", JDK11ZGCLogParser::parseHeap));
        withGCIDRules.add(new ParseRule.PrefixAndValueParseRule("Allocated", JDK11ZGCLogParser::parseHeap));
        withGCIDRules.add(new ParseRule.PrefixAndValueParseRule("Reclaimed", JDK11ZGCLogParser::parseHeap));
        withGCIDRules.add(new ParseRule.PrefixAndValueParseRule("Garbage Collection", JDK11ZGCLogParser::parseGarbageCollection));
    }

    @Override
    protected void doParseLineWithGCID(String detail, int gcid, double uptime) {
        ParseRule.ParseRuleContext context = new ParseRule.ParseRuleContext();
        context.put(UPTIME, uptime);
        context.put(GCID, gcid);
        doParseUsingRules(this, context, detail, withGCIDRules);
    }

    @Override
    protected void doParseLineWithoutGCID(String detail, double uptime) {
        ParseRule.ParseRuleContext context = new ParseRule.ParseRuleContext();
        context.put(UPTIME, uptime);
        doParseUsingRules(this, context, detail, withoutGCIDRules);
    }

    //  [2021-08-31T08:08:17.471+0800] GC(374) Metaspace: 125M used, 128M capacity, 128M committed, 130M reserved
    private static void parseMetaspace(AbstractGCLogParser parser, ParseRule.ParseRuleContext context, String prefix, String value) {
        GCModel model = parser.getModel();
        String[] parts = GCLogUtil.splitBySpace(value);
        GCEvent event = model.getLastEventOfType(ZGC_GARBAGE_COLLECTION);
        if (event == null) {
            return;
        }
        GCMemoryItem item = new GCMemoryItem(METASPACE, UNKNOWN_INT,
                GCLogUtil.toByte(parts[0]), GCLogUtil.toByte(parts[4]));
        event.setMemoryItem(item);
    }

    // [2021-08-31T11:29:12.825+0800] Out Of Memory (thread 8)
    private static void pauseOutOfMemory(AbstractGCLogParser parser, ParseRule.ParseRuleContext context, String prefix, String value) {
        GCModel model = parser.getModel();
        ThreadEvent event = new ThreadEvent();
        event.setThreadName(value.substring(1, value.length() - 1));
        event.setStartTime(context.get(UPTIME));
        model.addOom(event);
    }

    /*
     * [2021-08-31T08:08:17.471+0800] GC(374)  Capacity:    40960M (100%)      40960M (100%)      40960M (100%)      40960M (100%)      40960M (100%)      40960M (100%)
     * [2021-08-31T08:08:17.471+0800] GC(374)   Reserve:       96M (0%)           96M (0%)           96M (0%)           96M (0%)           96M (0%)           96M (0%)
     * [2021-08-31T08:08:17.471+0800] GC(374)      Free:    35250M (86%)       35210M (86%)       35964M (88%)       39410M (96%)       39410M (96%)       35210M (86%)
     * [2021-08-31T08:08:17.471+0800] GC(374)      Used:     5614M (14%)        5654M (14%)        4900M (12%)        1454M (4%)         5654M (14%)        1454M (4%)
     * [2021-08-31T08:08:17.471+0800] GC(374)      Live:         -              1173M (3%)         1173M (3%)         1173M (3%)             -                  -
     * [2021-08-31T08:08:17.471+0800] GC(374) Allocated:         -                40M (0%)           40M (0%)          202M (0%)             -                  -
     * [2021-08-31T08:08:17.471+0800] GC(374)   Garbage:         -              4440M (11%)        3686M (9%)          240M (1%)             -                  -
     * [2021-08-31T08:08:17.471+0800] GC(374) Reclaimed:         -                  -               754M (2%)         4200M (10%)            -                  -
     */
    private static void parseHeap(AbstractGCLogParser parser, ParseRule.ParseRuleContext context, String prefix, String value) {
        GCModel model = parser.getModel();
        prefix = prefix.trim();
        String[] parts = GCLogUtil.splitBySpace(value);
        GCEvent event = model.getLastEventOfType(ZGC_GARBAGE_COLLECTION);
        if (event == null) {
            return;
        }
        switch (prefix) {
            case "Capacity":
                GCMemoryItem item = new GCMemoryItem(HEAP);
                item.setTotal(GCLogUtil.toByte(parts[6]));
                event.setMemoryItem(item);
                break;
            case "Used":
                item = event.getMemoryItem(HEAP);
                item.setPreUsed(GCLogUtil.toByte(parts[0]));
                item.setPostUsed(GCLogUtil.toByte(parts[6]));
                break;
            case "Reclaimed":
                event.setReclamation(GCLogUtil.toByte(parts[4]));
                break;
            case "Allocated":
                event.setAllocation(GCLogUtil.toByte(parts[5]));
                break;
        }
    }

    /*
     * [2021-08-31T08:08:17.108+0800] GC(374) Garbage Collection (Proactive)
     * [2021-08-31T08:08:17.471+0800] GC(374) Garbage Collection (Proactive) 5614M(14%)->1454M(4%)
     */
    private static void parseGarbageCollection(AbstractGCLogParser parser, ParseRule.ParseRuleContext context, String prefix, String value) {
        GCModel model = parser.getModel();
        int index = value.indexOf(')');
        GCEvent event;
        if (index == value.length() - 1) {
            event = new GCEvent();
            model.putEvent(event);
            event.setStartTime(context.get(UPTIME));
            event.setEventType(ZGC_GARBAGE_COLLECTION);
            event.setCause(value.substring(1, index));
            event.setGcid(context.get(GCID));
        } else if (value.endsWith("%)")) {
            event = model.getLastEventOfType(ZGC_GARBAGE_COLLECTION);
            if (event == null) {
                return;
            }
            event.setDuration(context.<Double>get(UPTIME) - event.getStartTime());
            // parsing heap change here is done in parseHeap. Don't do that here
        }
    }

    /*
     * [2021-08-31T11:29:12.823+0800] Allocation Stall (ThreadPoolTaskScheduler-1) 0.204ms
     * [2021-08-31T11:29:12.823+0800] Allocation Stall (http-nio-8080-exec-71) 1.032ms
     * [2021-08-31T11:29:12.823+0800] Allocation Stall (NioProcessor-2) 0.391ms
     * [2021-08-31T11:29:12.823+0800] Allocation Stall (http-nio-8080-exec-85) 0.155ms
     * [2021-08-31T11:29:12.823+0800] Allocation Stall (http-nio-8080-exec-49) 277.588ms
     */
    private static void parseAllocationStall(AbstractGCLogParser parser, ParseRule.ParseRuleContext context, String prefix, String value) {
        GCModel model = parser.getModel();
        String[] parts = GCLogUtil.splitByBracket(value);
        ThreadEvent event = new ThreadEvent();
        double endTime = context.get(UPTIME);
        double duration = GCLogUtil.toMillisecond(parts[1]);
        event.setStartTime(endTime - duration);
        event.setDuration(duration);
        event.setEventType(ZGC_ALLOCATION_STALL);
        event.setThreadName(parts[0]);
        ((ZGCModel) model).addAllocationStalls(event);
    }

    /*
     * [2021-08-31T08:08:17.114+0800] GC(374) Pause Mark Start 4.459ms
     * [2021-08-31T08:08:17.421+0800] GC(374) Concurrent Mark 306.720ms
     * [2021-08-31T08:08:17.423+0800] GC(374) Pause Mark End 0.606ms
     * [2021-08-31T08:08:17.424+0800] GC(374) Concurrent Process Non-Strong References 1.290ms
     * [2021-08-31T08:08:17.425+0800] GC(374) Concurrent Reset Relocation Set 0.550ms
     * [2021-08-31T08:08:17.425+0800] GC(374) Concurrent Destroy Detached Pages 0.001ms
     * [2021-08-31T08:08:17.427+0800] GC(374) Concurrent Select Relocation Set 2.418ms
     * [2021-08-31T08:08:17.433+0800] GC(374) Concurrent Prepare Relocation Set 5.719ms
     * [2021-08-31T08:08:17.438+0800] GC(374) Pause Relocate Start 3.791ms
     * [2021-08-31T08:08:17.471+0800] GC(374) Concurrent Relocate 32.974ms
     */
    private static void parsePhase(AbstractGCLogParser parser, ParseRule.ParseRuleContext context, String phaseName, String value) {
        GCModel model = parser.getModel();
        GCEventType eventType = getGCEventType(phaseName);
        GCEvent event = model.getLastEventOfType(eventType.getPhaseParentEventType());
        if (event == null) {
            // log may be incomplete
            return;
        }
        GCEvent phase = new GCEvent();
        double endTime = context.get(UPTIME);
        double duration = GCLogUtil.toMillisecond(value);
        phase.setGcid(event.getGcid());
        phase.setStartTime(endTime - duration);
        phase.setDuration(duration);
        phase.setEventType(eventType);
        model.addPhase(event, phase);
    }

    /*
     * [2021-08-31T08:08:25.210+0800]   Collector: Garbage Collection Cycle                    362.677 / 362.677     365.056 / 529.211     315.229 / 868.961     315.229 / 868.961     ms
     * [2021-08-31T08:08:25.210+0800]  Contention: Mark Segment Reset Contention                     0 / 0                 1 / 106               0 / 238               0 / 238         ops/s
     * [2021-08-31T08:08:25.210+0800]  Contention: Mark SeqNum Reset Contention                      0 / 0                 0 / 1                 0 / 1                 0 / 1           ops/s
     * [2021-08-31T08:08:25.210+0800]  Contention: Relocation Contention                             1 / 10                0 / 52                0 / 87                0 / 87          ops/s
     * [2021-08-31T08:08:25.210+0800]    Critical: Allocation Stall                              0.000 / 0.000         0.000 / 0.000         0.000 / 0.000         0.000 / 0.000       ms
     * [2021-08-31T08:08:25.210+0800]    Critical: Allocation Stall                                  0 / 0                 0 / 0                 0 / 0                 0 / 0           ops/s
     */
    private static boolean parseZGCStatisticLine(AbstractGCLogParser parser, ParseRule.ParseRuleContext context, String text) {
        ZGCModel model = (ZGCModel) parser.getModel();
        String[] tokens = GCLogUtil.splitBySpace(text);
        int length = tokens.length;
        if (length > 15 && "/".equals(tokens[length - 3]) && "/".equals(tokens[length - 6]) &&
                "/".equals(tokens[length - 9]) && "/".equals(tokens[length - 12])) {
            // make unit a part of type name to deduplicate
            String type = text.substring(0, text.indexOf('/') - 1 - tokens[length - 13].length()).trim()
                    + " " + tokens[length - 1];
            List<ZStatistics> statisticsList = model.getStatistics();
            ZStatistics statistics;
            if ("Collector: Garbage Collection Cycle ms".equals(type)) {
                statistics = new ZStatistics();
                statistics.setStartTime(context.get(UPTIME));
                statisticsList.add(statistics);
            } else if (statisticsList.isEmpty()) {
                // log is incomplete
                return true;
            } else {
                statistics = statisticsList.get(statisticsList.size() - 1);
            }
            ZGCModel.ZStatisticsItem item = new ZGCModel.ZStatisticsItem(
                    Double.parseDouble(tokens[length - 13]),
                    Double.parseDouble(tokens[length - 11]),
                    Double.parseDouble(tokens[length - 10]),
                    Double.parseDouble(tokens[length - 8]),
                    Double.parseDouble(tokens[length - 7]),
                    Double.parseDouble(tokens[length - 5]),
                    Double.parseDouble(tokens[length - 4]),
                    Double.parseDouble(tokens[length - 2]));
            statistics.put(type, item);
            return true;
        } else {
            return false;
        }
    }

    private static GCEventType getGCEventType(String eventString) {
        switch (eventString) {
            case "Pause Mark Start":
                return ZGC_PAUSE_MARK_START;
            case "Concurrent Mark":
                return ZGC_CONCURRENT_MARK;
            case "Pause Mark End":
                return ZGC_PAUSE_MARK_END;
            case "Concurrent Process Non-Strong References":
                return ZGC_CONCURRENT_NONREF;
            case "Concurrent Reset Relocation Set":
                return ZGC_CONCURRENT_RESET_RELOC_SET;
            case "Concurrent Destroy Detached Pages":
                return ZGC_CONCURRENT_DETATCHED_PAGES;
            case "Concurrent Select Relocation Set":
                return ZGC_CONCURRENT_SELECT_RELOC_SET;
            case "Concurrent Prepare Relocation Set":
                return ZGC_CONCURRENT_PREPARE_RELOC_SET;
            case "Pause Relocate Start":
                return ZGC_PAUSE_RELOCATE_START;
            case "Concurrent Relocate":
                return ZGC_CONCURRENT_RELOCATE;
            default:
                ErrorUtil.shouldNotReachHere();
        }
        return null;
    }


}
