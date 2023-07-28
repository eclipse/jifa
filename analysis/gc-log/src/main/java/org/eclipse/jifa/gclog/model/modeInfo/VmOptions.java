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

package org.eclipse.jifa.gclog.model.modeInfo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.jifa.gclog.util.GCLogUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static org.eclipse.jifa.gclog.util.Constant.UNKNOWN_INT;
import static org.eclipse.jifa.gclog.util.Constant.KB2MB;

public class VmOptions {
    private Map<String, VmOption> options = new LinkedHashMap<>(); // use LinkedHashMap to preserve option order
    private String originalOptionString;
    private static Map<String, Integer> optionPriority = new ConcurrentHashMap<>();

    static {
        saveOptionsPriority();
    }

    private static void saveOptionsPriority() {
        List.of("Xms",
                "Xmx",
                "Xmn",
                "InitialHeapSize",
                "MaxHeapSize",
                "NewSize",
                "NewRatio",
                "MaxNewSize",
                "MetaspaceSize",
                "MaxMetaspaceSize",
                "MaxDirectMemorySize").forEach(s -> optionPriority.put(s, 9));
        List.of("UseCMSInitiatingOccupancyOnly",
                "UseCMSCompactAtFullCollection",
                "MaxGCPauseMillis",
                "InitiatingHeapOccupancyPercent").forEach(s -> optionPriority.put(s, 7));
        List.of("MinHeapFreeRatio",
                "MaxHeapFreeRatio",
                "MaxMetaspaceFreeRatio",
                "MinMetaspaceFreeRatio",
                "UseCompressedOops",
                "UseCompressedClassPointers",
                "SurvivorRatio",
                "ExplicitGCInvokesConcurrent",
                "DisableExplicitGC",
                "ParallelRefProcEnabled",
                "MaxTenuringThreshold",
                "PrintPromotionFailure",
                "TargetSurvivorRatio",
                "ParGCCardsPerStrideChunk",
                "UseGCOverheadLimit",
                "ScavengeBeforeFullGC",
                "PretenureSizeThreshold",
                "InitialTenuringThreshold",
                "GCTimeRatio",
                "ExplicitGCInvokesConcurrentAndUnloadsClasses",
                "SoftRefLRUPolicyMSPerMB",
                "GCLockerRetryAllocationCount",
                "UseCountedLoopSafepoints",
                "ReduceInitialCardMarks",
                "UseAdaptiveSizePolicy",
                "ClassUnloading",
                "ClassUnloadingWithConcurrentMark").forEach(s -> optionPriority.put(s, 6));
        List.of("verbose",
                "PrintHeapAtGC",
                "PrintTenuringDistribution",
                "PrintAdaptiveSizePolicy",
                "UseAsyncGCLog",
                "AsyncGCLogBufferSize",
                "AsyncGCLogBufferFlushThreshold",
                "UseGCLogFileRotation",
                "NumberOfGCLogFiles",
                "GCLogFileSize",
                "PrintStringDeduplicationStatistics",
                "PrintStringTableStatistics",
                "PrintSafepointStatistics",
                "PrintSafepointStatisticsCount",
                "PrintFLSStatistics",
                "PrintJNIGCStalls").forEach(s -> optionPriority.put(s, 5));
    }


    /* priority rule:
     * 10: UseXXGC
     * 9: Heap. generation and metaspace size
     * 8: GC Threads related
     * 7: GC specific tuning options
     * 6: GC tuning options shared by gc collectors
     * 5: GC log related
     * 0: other
     */
    private int getOptionPriority(String optionName) {
        if (optionPriority.containsKey(optionName)) {
            return optionPriority.get(optionName);
        }
        int priority;
        if (optionName.startsWith("Use") && optionName.endsWith("GC")) {
            priority = 10;
        } else if (optionName.endsWith("GCThreads")) {
            priority = 8;
        } else if (optionName.startsWith("Z") || optionName.startsWith("G1") || optionName.startsWith("CMS")) {
            priority = 7;
        } else if (optionName.contains("PLAB") || optionName.contains("TLAB")) {
            priority = 6;
        } else if (optionName.startsWith("PrintGC") || optionName.startsWith("Xlog")) {
            priority = 5;
        } else {
            priority = 0;
        }
        optionPriority.put(optionName, priority);
        return priority;
    }

    // notice that integers are long type and options indicating size are in Byte
    @SuppressWarnings("unchecked")
    public <T> T getOptionValue(String key) {
        if (options.containsKey(key)) {
            return (T) options.get(key).getValue();
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getOptionValue(String key, T defaultValue) {
        if (options.containsKey(key)) {
            return (T) options.get(key).getValue();
        } else {
            return defaultValue;
        }
    }

    public boolean containOption(String key) {
        return options.containsKey(key);
    }

    public VmOptions(String vmOptionsString) {
        originalOptionString = vmOptionsString;
        if (vmOptionsString == null) {
            return;
        }
        for (String option : vmOptionsString.split(" +")) {
            addVmOption(option);
        }
    }

    private void addVmOption(String optionString) {
        if (optionString.startsWith("-XX:")) {
            parseSingleOption(optionString, optionString.substring(4));
        } else if (optionString.startsWith("-D")) {
            parseSingleOption(optionString, optionString.substring(2));
        } else if (optionString.startsWith("-X")) {
            parseSingleOptionWithX(optionString, optionString.substring(2));
        } else if (optionString.startsWith("-")) {
            parseSingleOption(optionString, optionString.substring(1));
        }
    }

    private void parseSingleOptionWithX(String optionString, String content) {
        if (content == null) {
            return;
        }

        if (content.startsWith("mn") || content.startsWith("ms") || content.startsWith("mx") || content.startsWith("ss")) {
            // add 'X' for convention
            String optionName = "X" + content.substring(0, 2);
            options.put(optionName, new VmOption(optionString, optionName,
                    GCLogUtil.toByte(content.substring(2)) * (long) KB2MB, getOptionPriority(optionName)));
        }
        int mid = content.indexOf('=');
        if (mid < 0) {
            mid = content.indexOf(':');
        }
        if (mid >= 0) {
            String optionName = 'X' + content.substring(0, mid);
            options.put(optionName, new VmOption(optionString, optionName,
                    decideTypeAndParse(content.substring(mid + 1)), getOptionPriority(optionName)));
            return;
        }
        options.put(content, new VmOption(optionString, content, true, getOptionPriority(content)));
    }

    private void parseSingleOption(String optionString, String content) {
        if (content == null || content.isEmpty()) {
            return;
        }
        if (content.charAt(0) == '+') {
            String optionName = content.substring(1);
            options.put(optionName, new VmOption(optionString, optionName, true, getOptionPriority(optionName)));
            return;
        }
        if (content.charAt(0) == '-') {
            String optionName = content.substring(1);
            options.put(optionName, new VmOption(optionString, optionName, false, getOptionPriority(optionName)));
            return;
        }
        int mid = content.indexOf('=');
        if (mid < 0) {
            mid = content.indexOf(':');
        }
        if (mid >= 0) {
            String optionName = content.substring(0, mid);
            options.put(optionName, new VmOption(optionString, optionName,
                    decideTypeAndParse(content.substring(mid + 1)), getOptionPriority(optionName)));
            return;
        }
        options.put(content, new VmOption(optionString, content, true, getOptionPriority(content)));
    }

    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");
    private static final Pattern SIZE_PATTERN = Pattern.compile("\\d+[kmgt]]?[b]?");

    private Object decideTypeAndParse(String s) {
        s = s.toLowerCase();
        if (NUMBER_PATTERN.matcher(s).matches()) {
            return Long.parseLong(s);
        } else if (SIZE_PATTERN.matcher(s).matches()) {
            return GCLogUtil.toByte(s);
        } else {
            return s;
        }
    }

    public String getOriginalOptionString() {
        return originalOptionString;
    }

    public long getMetaspaceSize() {
        Long metaspaceSize = getOptionValue("MetaspaceSize");
        Long maxMetaspaceSize = getOptionValue("MaxMetaspaceSize");
        if (metaspaceSize != null && metaspaceSize.equals(maxMetaspaceSize)) {
            return metaspaceSize;
        } else {
            return UNKNOWN_INT;
        }
    }

    public VmOptionResult getVmOptionResult() {
        VmOptionResult optionResult = new VmOptionResult();
        options.values().stream()
                .sorted((o1, o2) -> o2.priority - o1.priority)
                .forEach(o -> (o.isGCRelated() ? optionResult.gcRelated : optionResult.other).add(new VmOptionVo(o.originalText)));
        return optionResult;
    }

    @Override
    public String toString() {
        return options.toString();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VmOption {
        private String originalText;
        private String optionName;
        private Object value;
        private int priority;

        public boolean isGCRelated() {
            return priority != 0;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VmOptionVo {
        private String text;
    }

    @Data
    @NoArgsConstructor
    public static class VmOptionResult {
        private List<VmOptionVo> gcRelated = new ArrayList<>();
        private List<VmOptionVo> other = new ArrayList<>();
    }
}
