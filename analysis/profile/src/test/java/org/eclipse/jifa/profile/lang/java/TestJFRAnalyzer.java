/********************************************************************************
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.profile.lang.java;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.eclipse.jifa.analysis.listener.ProgressListener;
import org.eclipse.jifa.profile.exception.ProfileAnalysisException;
import org.eclipse.jifa.profile.lang.java.model.AnalysisResult;
import org.eclipse.jifa.profile.lang.java.model.JavaThreadCPUTime;
import org.eclipse.jifa.profile.lang.java.request.AnalysisRequest;
import org.eclipse.jifa.profile.lang.java.request.DimensionBuilder;
import org.eclipse.jifa.profile.lang.java.helper.SimpleFlameGraph;
import org.eclipse.jifa.profile.model.TaskAllocatedMemory;
import org.eclipse.jifa.profile.model.TaskAllocations;
import org.eclipse.jifa.profile.model.TaskCPUTime;
import org.eclipse.jifa.profile.model.TaskCount;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class TestJFRAnalyzer {
    @Test
    public void testCpu() throws IOException, ProfileAnalysisException {
        JFRAnalyzer analyzer = new JFRAnalyzerImpl();
        Path path = createTmpFileForResource("main.jfr");
        AnalysisRequest request = new AnalysisRequest(path, DimensionBuilder.CPU);
        AnalysisResult result = analyzer.execute(request, ProgressListener.NoOpProgressListener);
        Assertions.assertNotNull(result.getCpuTime());

        List<TaskCPUTime> cpuTimes = result.getCpuTime().getList();
        Optional<TaskCPUTime> optional = cpuTimes.stream()
                .filter(item -> item.getTask().getName().equals("dd-profiler-recording-scheduler")).findAny();
        Assertions.assertTrue(optional.isPresent());
        TaskCPUTime tct = optional.get();
        SimpleFlameGraph g = SimpleFlameGraph.parse((JavaThreadCPUTime) tct);

        Assertions.assertEquals(9000000L, g.totalSampleValue.longValue());

        List<Triple<String, String, String>> list = g.queryLeafNodes(10);
        Assertions.assertEquals(list.size(), 1);

        optional = cpuTimes.stream().filter(item -> item.getTask().getName().equals("http-nio-8080-exec-1")).findAny();
        Assertions.assertTrue(optional.isPresent());
        tct = optional.get();
        g = SimpleFlameGraph.parse((JavaThreadCPUTime) tct);

        list = g.queryLeafNodes(9);
        Assertions.assertEquals(list.size(), 10);

        Optional<Triple<String, String, String>> t = list.stream().filter(item -> item.getLeft().contains(
                "StringUtils.tokenizeToStringArray(String, String, boolean, boolean)")).findAny();
        Assertions.assertTrue(t.isPresent());
        Assertions.assertEquals(9000000L, Long.valueOf(t.get().getMiddle()));
        Assertions.assertEquals(t.get().getRight(), "10.0");
    }

    @Test
    public void testNative() throws IOException, ProfileAnalysisException {
        Path path = createTmpFileForResource("native.jfr");
        JFRAnalyzer analyzer = new JFRAnalyzerImpl();
        AnalysisRequest request = new AnalysisRequest(path, DimensionBuilder.NATIVE_EXECUTION_SAMPLES);
        AnalysisResult result = analyzer.execute(request, ProgressListener.NoOpProgressListener);
        Assertions.assertNotNull(result.getNativeExecutionSamples());

        List<TaskCount> nativeSamples = result.getNativeExecutionSamples().getList();
        Optional<TaskCount> optional =
                nativeSamples.stream().filter(item -> item.getTask().getName().equals("pool-1-thread-3")).findAny();
        Assertions.assertTrue(optional.isPresent());
        TaskCount ta = optional.get();
        SimpleFlameGraph g = SimpleFlameGraph.parse(ta);
        Assertions.assertEquals(g.totalSampleValue.intValue(), 6324);

        List<Triple<String, String, String>> list = g.queryLeafNodes(10);
        Assertions.assertEquals(list.size(), 1);

        Optional<Triple<String, String, String>> t =
                list.stream().filter(item -> item.getLeft().equals("PerfMain2.nativeCostCpu()")).findAny();
        Assertions.assertTrue(t.isPresent());
        Assertions.assertEquals(Long.valueOf(t.get().getMiddle()), 6324);
        Assertions.assertEquals(t.get().getRight(), "100.0");
    }

    @Test
    public void testAllocations() throws IOException, ProfileAnalysisException {
        Path path = createTmpFileForResource("main.jfr");
        JFRAnalyzer analyzer = new JFRAnalyzerImpl();
        AnalysisRequest request = new AnalysisRequest(path, DimensionBuilder.ALLOC);
        AnalysisResult result = analyzer.execute(request, ProgressListener.NoOpProgressListener);
        Assertions.assertNotNull(result.getAllocations());

        List<TaskAllocations> taskAllocations = result.getAllocations().getList();
        Optional<TaskAllocations> optional =
                taskAllocations.stream().filter(item -> item.getTask().getName().equals("dd-trace-processor")).findAny();
        Assertions.assertTrue(optional.isPresent());
        TaskAllocations ta = optional.get();
        SimpleFlameGraph g = SimpleFlameGraph.parse(ta);
        Assertions.assertEquals(g.totalSampleValue.intValue(), 7285);

        List<Triple<String, String, String>> list = g.queryLeafNodes(10);
        Assertions.assertEquals(list.size(), 2);

        Optional<Triple<String, String, String>> t =
                list.stream().filter(item -> item.getLeft().contains("Arrays.copyOf(byte[], int)")).findAny();
        Assertions.assertTrue(t.isPresent());
        Assertions.assertEquals(Long.valueOf(t.get().getMiddle()), 1090);
        Assertions.assertEquals(t.get().getRight(), "14.96");
    }

    @Test
    public void testAllocatedMemory() throws IOException, ProfileAnalysisException {
        Path path = createTmpFileForResource("main.jfr");
        JFRAnalyzer analyzer = new JFRAnalyzerImpl();
        AnalysisRequest request = new AnalysisRequest(path, DimensionBuilder.MEM);
        AnalysisResult result = analyzer.execute(request, ProgressListener.NoOpProgressListener);
        Assertions.assertNotNull(result.getAllocatedMemory());

        List<TaskAllocatedMemory> taskAllocatedMemoryList =
                result.getAllocatedMemory().getList();
        Optional<TaskAllocatedMemory> optional = taskAllocatedMemoryList.stream()
                .filter(item -> item.getTask().getName().equals("http-nio-8080-exec-1")).findAny();
        Assertions.assertTrue(optional.isPresent());
        TaskAllocatedMemory ta = optional.get();
        SimpleFlameGraph g = SimpleFlameGraph.parse(ta);
        List<Triple<String, String, String>> list = g.queryLeafNodes(2);
        Assertions.assertEquals(list.size(), 7);

        Optional<Triple<String, String, String>> t =
                list.stream().filter(item -> item.getLeft().contains("Arrays.copyOfRange(char[], int, int)")).findAny();
        Assertions.assertTrue(t.isPresent());
        Assertions.assertEquals(Long.valueOf(t.get().getMiddle()), 8004704);
        Assertions.assertEquals(t.get().getRight(), "2.06");
    }

    @Test
    public void testCpuAndAllocAndMem() throws IOException, ProfileAnalysisException {
        JFRAnalyzer analyzer = new JFRAnalyzerImpl();
        Path path = createTmpFileForResource("main.jfr");
        AnalysisRequest request = new AnalysisRequest(path, DimensionBuilder.CPU | DimensionBuilder.ALLOC | DimensionBuilder.MEM);
        AnalysisResult result = analyzer.execute(request, ProgressListener.NoOpProgressListener);
        Assertions.assertNotNull(result.getCpuTime());
        Assertions.assertFalse(result.getCpuTime().getList().isEmpty());
        Assertions.assertNotNull(result.getAllocations());
        Assertions.assertFalse(result.getAllocations().getList().isEmpty());
        Assertions.assertNotNull(result.getAllocatedMemory());
        Assertions.assertFalse(result.getAllocatedMemory().getList().isEmpty());
    }

    @Test
    public void testSocketIO() throws IOException, ProfileAnalysisException {
        JFRAnalyzer analyzer = new JFRAnalyzerImpl();
        Path path = createTmpFileForResource("socket-io.jfr");
        AnalysisRequest request = new AnalysisRequest(path, DimensionBuilder.SOCKET_WRITE_SIZE | DimensionBuilder.SOCKET_WRITE_TIME);
        AnalysisResult result = analyzer.execute(request, ProgressListener.NoOpProgressListener);
        Assertions.assertNotNull(result.getSocketWriteTime());
        Assertions.assertFalse(result.getSocketWriteTime().getList().isEmpty());
        Assertions.assertNotNull(result.getSocketWriteSize());
        Assertions.assertFalse(result.getSocketWriteSize().getList().isEmpty());
    }

    @Test
    public void testLock() throws IOException, ProfileAnalysisException {
        JFRAnalyzer analyzer = new JFRAnalyzerImpl();
        Path path = createTmpFileForResource("lock-sync.jfr");
        AnalysisRequest request = new AnalysisRequest(path, DimensionBuilder.LOCK_WAIT_TIME | DimensionBuilder.LOCK_ACQUIRE | DimensionBuilder.SYNCHRONIZATION);
        AnalysisResult result = analyzer.execute(request, ProgressListener.NoOpProgressListener);
        Assertions.assertNotNull(result.getLockWaitTime());
        Assertions.assertFalse(result.getLockWaitTime().getList().isEmpty());
        Assertions.assertNotNull(result.getLockAcquire());
        Assertions.assertFalse(result.getLockAcquire().getList().isEmpty());
        Assertions.assertNotNull(result.getSynchronization());
        Assertions.assertFalse(result.getSynchronization().getList().isEmpty());
    }

    @Test
    public void testClassLoad() throws IOException, ProfileAnalysisException {
        JFRAnalyzer analyzer = new JFRAnalyzerImpl();
        Path path = createTmpFileForResource("classload.jfr");
        AnalysisRequest request = new AnalysisRequest(path, DimensionBuilder.CLASS_LOAD_COUNT | DimensionBuilder.CLASS_LOAD_WALL_TIME);
        AnalysisResult result = analyzer.execute(request, ProgressListener.NoOpProgressListener);
        Assertions.assertNotNull(result.getClassLoadCount());
        Assertions.assertFalse(result.getClassLoadCount().getList().isEmpty());
        Assertions.assertNotNull(result.getClassLoadWallTime());
        Assertions.assertFalse(result.getClassLoadWallTime().getList().isEmpty());
    }

    @Test
    public void testFileIO() throws IOException, ProfileAnalysisException {
        JFRAnalyzer analyzer = new JFRAnalyzerImpl();
        Path path = createTmpFileForResource("file-io.jfr");
        AnalysisRequest request = new AnalysisRequest(path, DimensionBuilder.FILE_IO_TIME | DimensionBuilder.FILE_READ_SIZE | DimensionBuilder.FILE_WRITE_SIZE);
        AnalysisResult result = analyzer.execute(request, ProgressListener.NoOpProgressListener);
        Assertions.assertNotNull(result.getFileIOTime());
        Assertions.assertFalse(result.getFileIOTime().getList().isEmpty());
        Assertions.assertNotNull(result.getFileReadSize());
        Assertions.assertFalse(result.getFileReadSize().getList().isEmpty());
        Assertions.assertNotNull(result.getFileWriteSize());
        Assertions.assertFalse(result.getFileWriteSize().getList().isEmpty());
    }

    public static Path createTmpFileForResource(String resource) throws IOException {
        Path path = Files.createTempFile("temp", ".jfr");
        path.toFile().deleteOnExit();
        FileUtils.copyInputStreamToFile(Objects.requireNonNull(
                        TestJFRAnalyzer.class.getClassLoader().getResourceAsStream(resource)),
                path.toFile());
        return path;
    }
}
