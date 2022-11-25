package org.eclipse.jifa.gclog;

import org.eclipse.jifa.common.listener.DefaultProgressListener;
import org.eclipse.jifa.gclog.model.CMSGCModel;
import org.eclipse.jifa.gclog.model.G1GCModel;
import org.eclipse.jifa.gclog.parser.GCLogParserFactory;
import org.eclipse.jifa.gclog.parser.UnifiedG1GCLogParser;
import org.eclipse.jifa.gclog.parser.PreUnifiedGenerationalGCLogParser;
import org.eclipse.jifa.gclog.vo.MemoryStatistics;
import org.eclipse.jifa.gclog.vo.TimeRange;
import org.junit.Assert;
import org.junit.Test;

import static org.eclipse.jifa.gclog.TestUtil.stringToBufferedReader;
import static org.eclipse.jifa.gclog.util.Constant.UNKNOWN_INT;

public class TestObjectStatistics {
    public static final double DELTA = 1e-6;

    @Test
    public void testJDk8CMS() throws Exception {
        String log = "OpenJDK 64-Bit Server VM (25.152-b187) for linux-amd64 JRE (1.8.0_152-b187), built on Dec 23 2017 19:26:28 by \"admin\" with gcc 4.4.7\n" +
                "Memory: 4k page, physical 8388608k(7983060k free), swap 0k(0k free)\n" +
                "CommandLine flags: -XX:+CMSClassUnloadingEnabled -XX:CMSInitiatingOccupancyFraction=80 -XX:CMSMaxAbortablePrecleanTime=5000 -XX:+ExplicitGCInvokesConcurrent -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/home/admin/logs/java.hprof -XX:InitialHeapSize=4294967296 -XX:MaxDirectMemorySize=1073741824 -XX:MaxHeapSize=4294967296 -XX:MaxMetaspaceSize=536870912 -XX:MaxNewSize=2147483648 -XX:MetaspaceSize=536870912 -XX:NewSize=2147483648 -XX:OldPLABSize=16 -XX:ParallelGCThreads=4 -XX:+PrintGC -XX:+PrintGCDateStamps -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:SurvivorRatio=10 -XX:+UseCMSInitiatingOccupancyOnly -XX:+UseCompressedClassPointers -XX:+UseCompressedOops -XX:+UseConcMarkSweepGC -XX:+UseParNewGC\n" +
                "63218.274: [GC (Allocation Failure) 63218.274: [ParNew: 1886196K->174720K(1922432K), 0.2894588 secs] 3212959K->1553242K(4019584K), 0.2897776 secs] [Times: user=0.84 sys=0.03, real=0.29 secs]\n" +
                "63471.854: [GC (CMS Initial Mark) [1 CMS-initial-mark: 1735874K(2097152K)] 1925568K(4019584K), 0.0760575 secs] [Times: user=0.17 sys=0.00, real=0.08 secs]\n" +
                "63471.930: [CMS-concurrent-mark-start]\n" +
                "63472.403: [CMS-concurrent-mark: 0.472/0.473 secs] [Times: user=0.68 sys=0.04, real=0.47 secs]\n" +
                "63472.403: [CMS-concurrent-preclean-start]\n" +
                "63472.413: [CMS-concurrent-preclean: 0.010/0.010 secs] [Times: user=0.00 sys=0.00, real=0.01 secs]\n" +
                "63472.413: [CMS-concurrent-abortable-preclean-start]\n" +
                " CMS: abort preclean due to time 63478.523: [CMS-concurrent-abortable-preclean: 6.102/6.109 secs] [Times: user=6.13 sys=0.09, real=6.11 secs]\n" +
                "63478.524: [GC (CMS Final Remark) [YG occupancy: 388009 K (1922432 K)]63478.524: [Rescan (parallel) , 0.1034279 secs]\n" +
                "63478.627: [weak refs processing, 0.0063173 secs]\n" +
                "63478.634: [class unloading, 0.1256964 secs]\n" +
                "63478.760: [scrub symbol table, 0.0364073 secs]\n" +
                "63478.796: [scrub string table, 0.0056749 secs][1 CMS-remark: 1735874K(2097152K)] 2123883K(4019584K), 0.3066740 secs] [Times: user=0.54 sys=0.00, real=0.31 secs]\n" +
                "63478.833: [CMS-concurrent-sweep-start]\n" +
                "63479.175: [GC (Allocation Failure) 63529.175: [ParNew: 1919804K->174720K(1922432K), 0.5404068 secs] 2298321K->734054K(4019584K), 0.5407823 secs] [Times: user=1.66 sys=0.00, real=0.54 secs]\n" +
                "63479.993: [CMS-concurrent-sweep: 1.160/1.161 secs] [Times: user=1.23 sys=0.03, real=1.16 secs]\n" +
                "63479.993: [CMS-concurrent-reset-start]\n" +
                "63480.008: [CMS-concurrent-reset: 0.015/0.015 secs] [Times: user=0.00 sys=0.01, real=0.02 secs]\n" +
                "63529.175: [GC (Allocation Failure) 63529.175: [ParNew: 1919804K->174720K(1922432K), 0.5404068 secs] 2298321K->734054K(4019584K), 0.5407823 secs] [Times: user=1.66 sys=0.00, real=0.54 secs]\n" +
                "63533.175: [Full GC (GCLocker Initiated GC) 63533.775: [CMS: 1572864K->1048576K(2097152K), 15.0887099 secs] 2298321K->1048576K(4019584K), [Metaspace: 864432K->244432K(1869824K)], 15.0888402 secs] [Times: user=15.06 sys=0.05, real=15.08 secs]\n" +
                "63581.039: [GC (Allocation Failure) 63581.040: [ParNew: 1922432K->174720K(1922432K), 0.4617450 secs] 2481766K->896372K(4019584K), 0.4620492 secs] [Times: user=1.49 sys=0.00, real=0.46 secs]\n" +
                "63600.175: [Full GC (GCLocker Initiated GC) 63600.775: [CMS: 1572984K->1048592K(2097152K), 12.0887099 secs] 2988617K->1048592K(4019584K), [Metaspace: 878932K->27432K(1869824K)], 15.0888402 secs] [Times: user=15.06 sys=0.05, real=12.08 secs]\n" +
                "63713.814: [GC (Allocation Failure) 63713.814: [ParNew: 1922432K->174720K(1922432K), 0.4928452 secs] 3167180K->1615581K(4019584K), 0.4931707 secs] [Times: user=1.67 sys=0.00, real=0.50 secs]\n" +
                "63811.838: [GC (CMS Initial Mark) [1 CMS-initial-mark: 1729404K(2097152K)] 1914444K(4019584K), 0.0531800 secs] [Times: user=0.15 sys=0.00, real=0.05 secs]\n" +
                "63811.891: [CMS-concurrent-mark-start]\n" +
                "63812.437: [CMS-concurrent-mark: 0.546/0.546 secs] [Times: user=0.67 sys=0.00, real=0.55 secs]\n" +
                "63812.437: [CMS-concurrent-preclean-start]\n" +
                "63812.446: [CMS-concurrent-preclean: 0.008/0.009 secs] [Times: user=0.01 sys=0.00, real=0.00 secs]\n" +
                "63812.446: [CMS-concurrent-abortable-preclean-start]\n" +
                " CMS: abort preclean due to time 63817.771: [CMS-concurrent-abortable-preclean: 5.320/5.325 secs] [Times: user=5.41 sys=0.07, real=5.33 secs]\n" +
                "63817.772: [GC (CMS Final Remark) [YG occupancy: 268095 K (1922432 K)]63817.772: [Rescan (parallel) , 0.0782293 secs]63817.850: [weak refs processing, 0.0003177 secs]63817.850: [class unloading, 0.1284138 secs]63817.979: [scrub symbol table, 0.0407089 secs]63818.020: [scrub string table, 0.0038912 secs][1 CMS-remark: 1729404K(2097152K)] 1997499K(4019584K), 0.2742426 secs] [Times: user=0.49 sys=0.01, real=0.28 secs]\n" +
                "63818.047: [CMS-concurrent-sweep-start]\n" +
                "63819.309: [CMS-concurrent-sweep: 1.261/1.262 secs] [Times: user=1.34 sys=0.02, real=1.26 secs]\n" +
                "63819.309: [CMS-concurrent-reset-start]\n" +
                "63819.314: [CMS-concurrent-reset: 0.005/0.005 secs] [Times: user=0.02 sys=0.00, real=0.00 secs]\n" +
                "63966.141: [GC (Allocation Failure) 63966.141: [ParNew: 1920339K->174720K(1922492K), 0.2086826 secs] 2338509K->621384K(4019584K), 0.2089897 secs] [Times: user=0.72 sys=0.00, real=0.21 secs]";
        PreUnifiedGenerationalGCLogParser parser = (PreUnifiedGenerationalGCLogParser)
                (new GCLogParserFactory().getParser(stringToBufferedReader(log)));

        CMSGCModel model = (CMSGCModel) parser.parse(stringToBufferedReader(log));
        model.calculateDerivedInfo(new DefaultProgressListener());
        Assert.assertNotNull(model);

        MemoryStatistics memStats = model.getMemoryStatistics(new TimeRange(0, 9999999999.0));
        Assert.assertEquals(memStats.getYoung(), new MemoryStatistics.MemoryStatisticsItem(
                (1922432 * 7 + 1922492) * 1024L / 8, 1922432 * 1024L, UNKNOWN_INT, UNKNOWN_INT));
        Assert.assertEquals(memStats.getOld(), new MemoryStatistics.MemoryStatisticsItem(
                (2097152 * 7 + 4019584 - 1922492) * 1024L / 8, 1572984 * 1024L, (1048576 + 1048592) * 1024L / 2, (2338509 - 1920339 + 2298321 - 1919804) * 1024L / 2));
        Assert.assertEquals(memStats.getHumongous(), new MemoryStatistics.MemoryStatisticsItem(UNKNOWN_INT, UNKNOWN_INT, UNKNOWN_INT, UNKNOWN_INT));
        Assert.assertEquals(memStats.getHeap(), new MemoryStatistics.MemoryStatisticsItem(
                4019584 * 1024L, 3212959 * 1024L, (1048576 + 1048592) * 1024L / 2, UNKNOWN_INT));
        Assert.assertEquals(memStats.getMetaspace(), new MemoryStatistics.MemoryStatisticsItem(
                536870912, 878932L * 1024L, (244432 + 27432) * 1024L / 2, UNKNOWN_INT));
    }


    @Test
    public void testJDK11G1() throws Exception {
        String log = "[0.029s][info][gc,heap] Heap region size: 1M\n" +
                "[0.033s][info][gc     ] Using G1\n" +
                "[0.033s][info][gc,heap,coops] Heap address: 0x0000000740000000, size: 3072 MB, Compressed Oops mode: Zero based, Oop shift amount: 3\n" +
                "[28.224s][info][gc,start      ] GC(0) Pause Young (Concurrent Start) (G1 Humongous Allocation)\n" +
                "[28.224s][info][gc,task       ] GC(0) Using 8 workers of 8 for evacuation\n" +
                "[28.242s][info][gc,phases     ] GC(0)   Pre Evacuate Collection Set: 0.3ms\n" +
                "[28.242s][info][gc,phases     ] GC(0)   Evacuate Collection Set: 15.9ms\n" +
                "[28.242s][info][gc,phases     ] GC(0)   Post Evacuate Collection Set: 1.8ms\n" +
                "[28.242s][info][gc,phases     ] GC(0)   Other: 0.3ms\n" +
                "[28.242s][info][gc,heap       ] GC(0) Eden regions: 148->0(172)\n" +
                "[28.242s][info][gc,heap       ] GC(0) Survivor regions: 20->10(23)\n" +
                "[28.242s][info][gc,heap       ] GC(0) Old regions: 214->220\n" +
                "[28.242s][info][gc,heap       ] GC(0) Humongous regions: 13->13\n" +
                "[28.242s][info][gc,metaspace  ] GC(0) Metaspace: 222590K->222590K(1251328K)\n" +
                "[28.242s][info][gc            ] GC(0) Pause Young (Concurrent Start) (G1 Humongous Allocation) 393M->242M(505M) 18.390ms\n" +
                "[28.242s][info][gc,cpu        ] GC(0) User=0.10s Sys=0.00s Real=0.02s\n" +
                "[28.242s][info][gc            ] GC(1) Concurrent Cycle\n" +
                "[28.242s][info][gc,marking    ] GC(1) Concurrent Clear Claimed Marks\n" +
                "[28.242s][info][gc,marking    ] GC(1) Concurrent Clear Claimed Marks 0.301ms\n" +
                "[28.243s][info][gc,marking    ] GC(1) Concurrent Scan Root Regions\n" +
                "[28.250s][info][gc,marking    ] GC(1) Concurrent Scan Root Regions 7.413ms\n" +
                "[28.250s][info][gc,marking    ] GC(1) Concurrent Mark (28.250s)\n" +
                "[28.250s][info][gc,marking    ] GC(1) Concurrent Mark From Roots\n" +
                "[28.250s][info][gc,task       ] GC(1) Using 2 workers of 2 for marking\n" +
                "[28.794s][info][gc,marking    ] GC(1) Concurrent Mark From Roots 544.204ms\n" +
                "[28.794s][info][gc,marking    ] GC(1) Concurrent Preclean\n" +
                "[28.795s][info][gc,marking    ] GC(1) Concurrent Preclean 0.996ms\n" +
                "[28.795s][info][gc,marking    ] GC(1) Concurrent Mark (28.250s, 28.795s) 545.303ms\n" +
                "[28.795s][info][gc,start      ] GC(1) Pause Remark\n" +
                "[28.818s][info][gc,stringtable] GC(1) Cleaned string and symbol table, strings: 93320 processed, 4 removed, symbols: 553250 processed, 79 removed\n" +
                "[28.818s][info][gc            ] GC(1) Pause Remark 341M->334M(505M) 22.698ms\n" +
                "[28.818s][info][gc,cpu        ] GC(1) User=0.11s Sys=0.01s Real=0.03s\n" +
                "[28.818s][info][gc,marking    ] GC(1) Concurrent Rebuild Remembered Sets\n" +
                "[29.125s][info][gc,marking    ] GC(1) Concurrent Rebuild Remembered Sets 306.244ms\n" +
                "[29.126s][info][gc,start      ] GC(1) Pause Cleanup\n" +
                "[29.126s][info][gc            ] GC(1) Pause Cleanup 395M->395M(505M) 0.225ms\n" +
                "[29.126s][info][gc,cpu        ] GC(1) User=0.00s Sys=0.00s Real=0.00s\n" +
                "[29.126s][info][gc,marking    ] GC(1) Concurrent Cleanup for Next Mark\n" +
                "[29.130s][info][gc,marking    ] GC(1) Concurrent Cleanup for Next Mark 3.930ms\n" +
                "[29.130s][info][gc            ] GC(1) Concurrent Cycle 888.287ms\n" +
                "[29.228s][info][gc,start      ] GC(2) Pause Young (Prepare Mixed) (G1 Evacuation Pause)\n" +
                "[29.228s][info][gc,task       ] GC(2) Using 8 workers of 8 for evacuation\n" +
                "[29.240s][info][gc,phases     ] GC(2)   Pre Evacuate Collection Set: 0.0ms\n" +
                "[29.240s][info][gc,phases     ] GC(2)   Evacuate Collection Set: 10.4ms\n" +
                "[29.240s][info][gc,phases     ] GC(2)   Post Evacuate Collection Set: 1.3ms\n" +
                "[29.240s][info][gc,phases     ] GC(2)   Other: 0.2ms\n" +
                "[29.240s][info][gc,heap       ] GC(2) Eden regions: 172->0(9)\n" +
                "[29.240s][info][gc,heap       ] GC(2) Survivor regions: 10->16(23)\n" +
                "[29.240s][info][gc,heap       ] GC(2) Old regions: 217->217\n" +
                "[29.240s][info][gc,heap       ] GC(2) Humongous regions: 29->11\n" +
                "[29.240s][info][gc,metaspace  ] GC(2) Metaspace: 224850K->224850K(1253376K)\n" +
                "[29.240s][info][gc            ] GC(2) Pause Young (Prepare Mixed) (G1 Evacuation Pause) 427M->243M(505M) 12.048ms\n" +
                "[29.240s][info][gc,cpu        ] GC(2) User=0.08s Sys=0.00s Real=0.01s\n" +
                "[29.268s][info][gc,start      ] GC(3) Pause Young (Mixed) (G1 Evacuation Pause)\n" +
                "[29.268s][info][gc,task       ] GC(3) Using 8 workers of 8 for evacuation\n" +
                "[29.280s][info][gc,phases     ] GC(3)   Pre Evacuate Collection Set: 0.0ms\n" +
                "[29.280s][info][gc,phases     ] GC(3)   Evacuate Collection Set: 10.9ms\n" +
                "[29.280s][info][gc,phases     ] GC(3)   Post Evacuate Collection Set: 0.4ms\n" +
                "[29.280s][info][gc,phases     ] GC(3)   Other: 0.2ms\n" +
                "[29.280s][info][gc,heap       ] GC(3) Eden regions: 9->0(176)\n" +
                "[29.280s][info][gc,heap       ] GC(3) Survivor regions: 16->3(4)\n" +
                "[29.280s][info][gc,heap       ] GC(3) Old regions: 217->223\n" +
                "[29.280s][info][gc,heap       ] GC(3) Humongous regions: 11->11\n" +
                "[29.280s][info][gc,metaspace  ] GC(3) Metaspace: 224854K->224854K(1253376K)\n" +
                "[29.280s][info][gc            ] GC(3) Pause Young (Mixed) (G1 Evacuation Pause) 252M->236M(505M) 11.771ms\n" +
                "[29.280s][info][gc,cpu        ] GC(3) User=0.09s Sys=0.00s Real=0.01s\n" +
                "[29.356s][info][gc,start      ] GC(4) Pause Young (Concurrent Start) (G1 Humongous Allocation)\n" +
                "[29.356s][info][gc,task       ] GC(4) Using 8 workers of 8 for evacuation\n" +
                "[29.365s][info][gc,phases     ] GC(4)   Pre Evacuate Collection Set: 0.3ms\n" +
                "[29.365s][info][gc,phases     ] GC(4)   Evacuate Collection Set: 6.9ms\n" +
                "[29.365s][info][gc,phases     ] GC(4)   Post Evacuate Collection Set: 1.2ms\n" +
                "[29.365s][info][gc,phases     ] GC(4)   Other: 0.2ms\n" +
                "[29.365s][info][gc,heap       ] GC(4) Eden regions: 23->0(137)\n" +
                "[29.365s][info][gc,heap       ] GC(4) Survivor regions: 3->5(23)\n" +
                "[29.365s][info][gc,heap       ] GC(4) Old regions: 223->223\n" +
                "[29.365s][info][gc,heap       ] GC(4) Humongous regions: 11->11\n" +
                "[29.365s][info][gc,metaspace  ] GC(4) Metaspace: 225002K->225002K(1253376K)\n" +
                "[29.365s][info][gc            ] GC(4) Pause Young (Concurrent Start) (G1 Humongous Allocation) 258M->237M(505M) 8.709ms\n" +
                "[29.365s][info][gc,cpu        ] GC(4) User=0.05s Sys=0.00s Real=0.00s";
        UnifiedG1GCLogParser parser = (UnifiedG1GCLogParser)
                (new GCLogParserFactory().getParser(stringToBufferedReader(log)));
        G1GCModel model = (G1GCModel) parser.parse(stringToBufferedReader(log));
        model.calculateDerivedInfo(new DefaultProgressListener());

        MemoryStatistics memStats = model.getMemoryStatistics(new TimeRange(0, 9999999999.0));
        Assert.assertEquals(memStats.getHumongous(), new MemoryStatistics.MemoryStatisticsItem(
                UNKNOWN_INT, 29 * 1024 * 1024, UNKNOWN_INT, 29 * 1024 * 1024));
        Assert.assertEquals(memStats.getOld(), new MemoryStatistics.MemoryStatisticsItem(
                (505 * 4 - 195 - 32 - 180 - 160) * 1024 * 1024 / 4, 223 * 1024 * 1024, UNKNOWN_INT, 223 * 1024 * 1024));
        Assert.assertEquals(memStats.getMetaspace(), new MemoryStatistics.MemoryStatisticsItem(
                UNKNOWN_INT, 225002 * 1024, UNKNOWN_INT, 224850 * 1024));

    }
}
