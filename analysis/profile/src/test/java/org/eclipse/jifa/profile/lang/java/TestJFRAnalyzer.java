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
import org.eclipse.jifa.profile.model.*;
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
        Path path = createTmpFileForResource("jfr.jfr");
        AnalysisRequest request = new AnalysisRequest(path, DimensionBuilder.CPU);
        AnalysisResult result = analyzer.execute(request, ProgressListener.NoOpProgressListener);
        Assertions.assertNotNull(result.getCpuTime());

        List<TaskCPUTime> cpuTimes = result.getCpuTime().getList();
        Optional<TaskCPUTime> optional = cpuTimes.stream()
                .filter(item -> item.getTask().getName().equals("Thread-6")).findAny();
        Assertions.assertTrue(optional.isPresent());
        TaskCPUTime tct = optional.get();
        SimpleFlameGraph g = SimpleFlameGraph.parse((JavaThreadCPUTime) tct);

        Assertions.assertEquals(18852L, nanoToMillis(g.totalSampleValue.longValue()));

        List<Triple<String, String, String>> list = g.queryLeafNodes(10);
        Assertions.assertEquals(1, list.size());

        optional = cpuTimes.stream().filter(item -> item.getTask().getName().equals("main")).findAny();
        Assertions.assertTrue(optional.isPresent());
        tct = optional.get();
        g = SimpleFlameGraph.parse((JavaThreadCPUTime) tct);
        list = g.queryLeafNodes(10);
        Assertions.assertEquals(1, list.size());
        Optional<Triple<String, String, String>> t = list.stream().filter(
                item -> item.getLeft().contains("Test.<init>()")).findAny();
        Assertions.assertTrue(t.isPresent());
        Assertions.assertEquals(1260, nanoToMillis(Long.parseLong(t.get().getMiddle())));
        Assertions.assertEquals("42.0", t.get().getRight());

        optional = cpuTimes.stream().filter(item -> item.getTask().getName().equals("GC Thread")).findAny();
        Assertions.assertTrue(optional.isPresent());
        tct = optional.get();
        g = SimpleFlameGraph.parse((JavaThreadCPUTime) tct);
        list = g.queryLeafNodes(0);
        Assertions.assertEquals(1, list.size());
        t = list.stream().filter(item -> item.getLeft().contains("JVM.GC")).findAny();
        Assertions.assertTrue(t.isPresent());
        Assertions.assertEquals(402, nanoToMillis(Long.parseLong(t.get().getMiddle())));
    }

    @Test
    public void testCpu2() throws IOException, ProfileAnalysisException {
        JFRAnalyzer analyzer = new JFRAnalyzerImpl();
        Path path = createTmpFileForResource("ap-cpu-default.jfr");
        AnalysisRequest request = new AnalysisRequest(path, DimensionBuilder.CPU);
        AnalysisResult result = analyzer.execute(request, ProgressListener.NoOpProgressListener);
        Assertions.assertNotNull(result.getCpuTime());

        List<TaskCPUTime> cpuTimes = result.getCpuTime().getList();
        Optional<TaskCPUTime> optional = cpuTimes.stream()
                .filter(item -> item.getTask().getName().equals("main")).findAny();
        Assertions.assertTrue(optional.isPresent());
        TaskCPUTime tct = optional.get();
        SimpleFlameGraph g = SimpleFlameGraph.parse((JavaThreadCPUTime) tct);

        Assertions.assertEquals(9860L, nanoToMillis(g.totalSampleValue.longValue()));

        List<Triple<String, String, String>> list = g.queryLeafNodes(10);
        Assertions.assertEquals(1, list.size());

        optional = cpuTimes.stream().filter(item -> item.getTask().getName().equals("GC task thread#-9")).findAny();
        Assertions.assertTrue(optional.isPresent());
        tct = optional.get();
        g = SimpleFlameGraph.parse((JavaThreadCPUTime) tct);
        Assertions.assertEquals(30L, nanoToMillis(g.totalSampleValue.longValue()));
    }

    @Test
    public void testCpu3() throws IOException, ProfileAnalysisException {
        JFRAnalyzer analyzer = new JFRAnalyzerImpl();
        Path path = createTmpFileForResource("jfr.jfr");
        AnalysisRequest request = new AnalysisRequest(path, DimensionBuilder.CPU_SAMPLE);
        AnalysisResult result = analyzer.execute(request, ProgressListener.NoOpProgressListener);
        Assertions.assertNotNull(result.getCpuSample());

        List<TaskCount> cpuTimes = result.getCpuSample().getList();
        Optional<TaskCount> optional = cpuTimes.stream()
                .filter(item -> item.getTask().getName().equals("Thread-6")).findAny();
        Assertions.assertTrue(optional.isPresent());
        TaskCount tct = optional.get();
        SimpleFlameGraph g = SimpleFlameGraph.parse(tct);

        Assertions.assertEquals(952, g.totalSampleValue.longValue());

        List<Triple<String, String, String>> list = g.queryLeafNodes(10);
        Assertions.assertEquals(1, list.size());
        Optional<Triple<String, String, String>> t = list.stream().filter(
                item -> item.getLeft().contains("Test.realEatCpu()")).findAny();
        Assertions.assertTrue(t.isPresent());
        Assertions.assertEquals(952, Long.parseLong(t.get().getMiddle()));
    }

    @Test
    public void testWall() throws IOException, ProfileAnalysisException {
        JFRAnalyzer analyzer = new JFRAnalyzerImpl();
        Path path = createTmpFileForResource("ap-wall-default.jfr");
        AnalysisRequest request = new AnalysisRequest(path, DimensionBuilder.WALL_CLOCK);
        AnalysisResult result = analyzer.execute(request, ProgressListener.NoOpProgressListener);
        Assertions.assertFalse(result.getWallClock().getList().isEmpty());

        List<TaskSum> cpuTimes = result.getWallClock().getList();
        Optional<TaskSum> optional = cpuTimes.stream()
                .filter(item -> item.getTask().getName().equals("SleepThread")).findAny();
        Assertions.assertTrue(optional.isPresent());
        TaskSum tct = optional.get();
        SimpleFlameGraph g = SimpleFlameGraph.parse(tct);

        Assertions.assertEquals(9949, nanoToMillis(g.totalSampleValue.longValue()));

        List<Triple<String, String, String>> list = g.queryLeafNodes(10);
        Assertions.assertEquals(1, list.size());
        Optional<Triple<String, String, String>> t = list.stream().filter(
                item -> item.getLeft().contains("libpthread-2.17.so.__pthread_cond_timedwait()")).findAny();
        Assertions.assertTrue(t.isPresent());
        Assertions.assertEquals(9800, nanoToMillis(Long.parseLong(t.get().getMiddle())));
    }

    @Test
    public void testNative() throws IOException, ProfileAnalysisException {
        Path path = createTmpFileForResource("jfr.jfr");
        JFRAnalyzer analyzer = new JFRAnalyzerImpl();
        AnalysisRequest request = new AnalysisRequest(path, DimensionBuilder.NATIVE_EXECUTION_SAMPLES);
        AnalysisResult result = analyzer.execute(request, ProgressListener.NoOpProgressListener);
        Assertions.assertNotNull(result.getNativeExecutionSamples());

        List<TaskCount> nativeSamples = result.getNativeExecutionSamples().getList();
        Optional<TaskCount> optional =
                nativeSamples.stream().filter(item -> item.getTask().getName().equals("Thread-2")).findAny();
        Assertions.assertTrue(optional.isPresent());
        TaskCount ta = optional.get();
        SimpleFlameGraph g = SimpleFlameGraph.parse(ta);
        Assertions.assertEquals(1001, g.totalSampleValue.intValue());

        List<Triple<String, String, String>> list = g.queryLeafNodes(10);
        Assertions.assertEquals(1, list.size());

        Optional<Triple<String, String, String>> t =
                list.stream().filter(item -> item.getLeft().equals("java.net.SocketInputStream.socketRead0(FileDescriptor, byte[], int, int, int)")).findAny();
        Assertions.assertTrue(t.isPresent());
        Assertions.assertEquals(951, Long.valueOf(t.get().getMiddle()));
        Assertions.assertEquals("95.0", t.get().getRight());
    }

    @Test
    public void testAllocations() throws IOException, ProfileAnalysisException {
        Path path = createTmpFileForResource("jfr.jfr");
        JFRAnalyzer analyzer = new JFRAnalyzerImpl();
        AnalysisRequest request = new AnalysisRequest(path, DimensionBuilder.ALLOC);
        AnalysisResult result = analyzer.execute(request, ProgressListener.NoOpProgressListener);
        Assertions.assertNotNull(result.getAllocations());

        List<TaskAllocations> taskAllocations = result.getAllocations().getList();
        Optional<TaskAllocations> optional =
                taskAllocations.stream().filter(item -> item.getTask().getName().equals("main")).findAny();
        Assertions.assertTrue(optional.isPresent());
        TaskAllocations ta = optional.get();
        SimpleFlameGraph g = SimpleFlameGraph.parse(ta);
        Assertions.assertEquals(13674, g.totalSampleValue.intValue());

        List<Triple<String, String, String>> list = g.queryLeafNodes(10);
        Assertions.assertEquals(list.size(), 1);

        Optional<Triple<String, String, String>> t =
                list.stream().filter(item -> item.getLeft().contains("Test.<init>()")).findAny();
        Assertions.assertTrue(t.isPresent());
        Assertions.assertEquals(13663, Long.valueOf(t.get().getMiddle()));
        Assertions.assertEquals("99.92", t.get().getRight());
    }

    @Test
    public void testAllocatedMemory() throws IOException, ProfileAnalysisException {
        Path path = createTmpFileForResource("jfr.jfr");
        JFRAnalyzer analyzer = new JFRAnalyzerImpl();
        AnalysisRequest request = new AnalysisRequest(path, DimensionBuilder.MEM);
        AnalysisResult result = analyzer.execute(request, ProgressListener.NoOpProgressListener);
        Assertions.assertNotNull(result.getAllocatedMemory());

        List<TaskAllocatedMemory> taskAllocations = result.getAllocatedMemory().getList();
        Optional<TaskAllocatedMemory> optional =
                taskAllocations.stream().filter(item -> item.getTask().getName().equals("main")).findAny();
        Assertions.assertTrue(optional.isPresent());
        TaskAllocatedMemory ta = optional.get();
        SimpleFlameGraph g = SimpleFlameGraph.parse(ta);
        Assertions.assertEquals(28609059912L, g.totalSampleValue.longValue());

        List<Triple<String, String, String>> list = g.queryLeafNodes(10);
        Assertions.assertEquals(list.size(), 1);

        Optional<Triple<String, String, String>> t =
                list.stream().filter(item -> item.getLeft().contains("Test.<init>()")).findAny();
        Assertions.assertTrue(t.isPresent());
        Assertions.assertEquals(28602671000L, Long.valueOf(t.get().getMiddle()));
        Assertions.assertEquals("99.98", t.get().getRight());
    }

    @Test
    public void testCpuAndAllocAndMem() throws IOException, ProfileAnalysisException {
        JFRAnalyzer analyzer = new JFRAnalyzerImpl();
        Path path = createTmpFileForResource("jfr.jfr");
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
    public void testFileIO() throws IOException, ProfileAnalysisException {
        JFRAnalyzer analyzer = new JFRAnalyzerImpl();
        Path path = createTmpFileForResource("jfr.jfr");
        AnalysisRequest request = new AnalysisRequest(path, DimensionBuilder.FILE_IO_TIME | DimensionBuilder.FILE_READ_SIZE | DimensionBuilder.FILE_WRITE_SIZE);
        AnalysisResult result = analyzer.execute(request, ProgressListener.NoOpProgressListener);
        Assertions.assertNotNull(result.getFileIOTime());
        Assertions.assertFalse(result.getFileIOTime().getList().isEmpty());

        List<TaskSum> taskSumList = result.getFileIOTime().getList();
        Optional<TaskSum> optional =
                taskSumList.stream().filter(item -> item.getTask().getName().equals("Thread-2")).findAny();
        Assertions.assertTrue(optional.isPresent());
        TaskSum ta = optional.get();
        SimpleFlameGraph g = SimpleFlameGraph.parse(ta);
        Assertions.assertEquals(123070, g.totalSampleValue.longValue());
        List<Triple<String, String, String>> list = g.queryLeafNodes(10);
        Assertions.assertEquals(2, list.size());
        Optional<Triple<String, String, String>> t =
                list.stream().filter(item -> item.getLeft().contains("java.io.FileOutputStream.write(byte[], int, int)")).findAny();
        Assertions.assertTrue(t.isPresent());
        Assertions.assertEquals(58214, Long.valueOf(t.get().getMiddle()));
        Assertions.assertEquals("47.3", t.get().getRight());

        Assertions.assertNotNull(result.getFileReadSize());
        Assertions.assertFalse(result.getFileReadSize().getList().isEmpty());
        taskSumList = result.getFileReadSize().getList();
        optional = taskSumList.stream().filter(item -> item.getTask().getName().equals("Thread-3")).findAny();
        Assertions.assertTrue(optional.isPresent());
        ta = optional.get();
        g = SimpleFlameGraph.parse(ta);
        Assertions.assertEquals(6651, g.totalSampleValue.longValue());
        list = g.queryLeafNodes(20);
        Assertions.assertEquals(1, list.size());
        t = list.stream().filter(item -> item.getLeft().contains("java.io.FileInputStream.read(byte[], int, int)")).findAny();
        Assertions.assertTrue(t.isPresent());
        Assertions.assertEquals(5352, Long.valueOf(t.get().getMiddle()));
        Assertions.assertEquals("80.47", t.get().getRight());

        Assertions.assertNotNull(result.getFileWriteSize());
        Assertions.assertFalse(result.getFileWriteSize().getList().isEmpty());
        taskSumList = result.getFileWriteSize().getList();
        optional = taskSumList.stream().filter(item -> item.getTask().getName().equals("Thread-2")).findAny();
        Assertions.assertTrue(optional.isPresent());
        ta = optional.get();
        g = SimpleFlameGraph.parse(ta);
        Assertions.assertEquals(31, g.totalSampleValue.longValue());
        list = g.queryLeafNodes(20);
        Assertions.assertEquals(1, list.size());
        t = list.stream().filter(item -> item.getLeft().contains("java.io.FileOutputStream.write(byte[], int, int)")).findAny();
        Assertions.assertTrue(t.isPresent());
        Assertions.assertEquals(29, Long.valueOf(t.get().getMiddle()));
        Assertions.assertEquals("93.55", t.get().getRight());
    }

    @Test
    public void testSocketIO() throws IOException, ProfileAnalysisException {
        JFRAnalyzer analyzer = new JFRAnalyzerImpl();
        Path path = createTmpFileForResource("jfr.jfr");
        AnalysisRequest request = new AnalysisRequest(path,
                DimensionBuilder.SOCKET_WRITE_SIZE
                        | DimensionBuilder.SOCKET_WRITE_TIME
                        | DimensionBuilder.SOCKET_READ_SIZE
                        | DimensionBuilder.SOCKET_READ_TIME);
        AnalysisResult result = analyzer.execute(request, ProgressListener.NoOpProgressListener);
        Assertions.assertNotNull(result.getSocketWriteSize());
        Assertions.assertFalse(result.getSocketWriteSize().getList().isEmpty());
        List<TaskSum> taskSumList = result.getSocketWriteSize().getList();
        Optional<TaskSum> optional =
                taskSumList.stream().filter(item -> item.getTask().getName().equals("Thread-3")).findAny();
        Assertions.assertTrue(optional.isPresent());
        TaskSum ta = optional.get();
        SimpleFlameGraph g = SimpleFlameGraph.parse(ta);
        Assertions.assertEquals(114, g.totalSampleValue.longValue());
        List<Triple<String, String, String>> list = g.queryLeafNodes(10);
        Assertions.assertEquals(1, list.size());
        Optional<Triple<String, String, String>> t =
                list.stream().filter(item -> item.getLeft().contains("java.net.SocketOutputStream.socketWrite(byte[], int, int)")).findAny();
        Assertions.assertTrue(t.isPresent());
        Assertions.assertEquals(114, Long.valueOf(t.get().getMiddle()));
        Assertions.assertEquals("100.0", t.get().getRight());

        Assertions.assertNotNull(result.getSocketWriteTime());
        Assertions.assertFalse(result.getSocketWriteTime().getList().isEmpty());
        taskSumList = result.getSocketWriteTime().getList();
        optional = taskSumList.stream().filter(item -> item.getTask().getName().equals("Thread-3")).findAny();
        Assertions.assertTrue(optional.isPresent());
        ta = optional.get();
        g = SimpleFlameGraph.parse(ta);
        Assertions.assertEquals(685179, g.totalSampleValue.longValue());
        list = g.queryLeafNodes(20);
        Assertions.assertEquals(1, list.size());
        t = list.stream().filter(item -> item.getLeft().contains("java.net.SocketOutputStream.socketWrite(byte[], int, int)")).findAny();
        Assertions.assertTrue(t.isPresent());
        Assertions.assertEquals(685179, Long.valueOf(t.get().getMiddle()));
        Assertions.assertEquals("100.0", t.get().getRight());

        Assertions.assertNotNull(result.getSocketReadTime());
        Assertions.assertFalse(result.getSocketReadTime().getList().isEmpty());
        taskSumList = result.getSocketReadTime().getList();
        optional = taskSumList.stream().filter(item -> item.getTask().getName().equals("Thread-2")).findAny();
        Assertions.assertTrue(optional.isPresent());
        ta = optional.get();
        g = SimpleFlameGraph.parse(ta);
        Assertions.assertEquals(19001, nanoToMillis(g.totalSampleValue.longValue()));
        list = g.queryLeafNodes(20);
        Assertions.assertEquals(1, list.size());
        t = list.stream().filter(item -> item.getLeft().contains("java.net.SocketInputStream.read(byte[], int, int, int)")).findAny();
        Assertions.assertTrue(t.isPresent());
        Assertions.assertEquals(19001, nanoToMillis(Long.valueOf(t.get().getMiddle())));
        Assertions.assertEquals("100.0", t.get().getRight());

        Assertions.assertNotNull(result.getSocketReadSize());
        Assertions.assertFalse(result.getSocketReadSize().getList().isEmpty());
        taskSumList = result.getSocketReadSize().getList();
        optional = taskSumList.stream().filter(item -> item.getTask().getName().equals("Thread-2")).findAny();
        Assertions.assertTrue(optional.isPresent());
        ta = optional.get();
        g = SimpleFlameGraph.parse(ta);
        Assertions.assertEquals(114, g.totalSampleValue.longValue());
        list = g.queryLeafNodes(20);
        Assertions.assertEquals(1, list.size());
        t = list.stream().filter(item -> item.getLeft().contains("java.net.SocketInputStream.read(byte[], int, int, int)")).findAny();
        Assertions.assertTrue(t.isPresent());
        Assertions.assertEquals(114, Long.valueOf(t.get().getMiddle()));
        Assertions.assertEquals("100.0", t.get().getRight());
    }

    @Test
    public void testLock() throws IOException, ProfileAnalysisException {
        JFRAnalyzer analyzer = new JFRAnalyzerImpl();
        Path path = createTmpFileForResource("jfr.jfr");
        AnalysisRequest request = new AnalysisRequest(path, DimensionBuilder.LOCK_WAIT_TIME | DimensionBuilder.LOCK_ACQUIRE | DimensionBuilder.SYNCHRONIZATION);
        AnalysisResult result = analyzer.execute(request, ProgressListener.NoOpProgressListener);
        Assertions.assertNotNull(result.getLockWaitTime());
        Assertions.assertFalse(result.getLockWaitTime().getList().isEmpty());
        List<TaskSum> taskSumList = result.getLockWaitTime().getList();
        Optional<TaskSum> optional =
                taskSumList.stream().filter(item -> item.getTask().getName().equals("Thread-2")).findAny();
        Assertions.assertTrue(optional.isPresent());
        TaskSum ta = optional.get();
        SimpleFlameGraph g = SimpleFlameGraph.parse(ta);
        Assertions.assertEquals(33093, g.totalSampleValue.longValue());
        List<Triple<String, String, String>> list = g.queryLeafNodes(10);
        Assertions.assertEquals(1, list.size());
        Optional<Triple<String, String, String>> t =
                list.stream().filter(item -> item.getLeft().contains("java.io.PrintStream.println(String)")).findAny();
        Assertions.assertTrue(t.isPresent());
        Assertions.assertEquals(33093, Long.valueOf(t.get().getMiddle()));
        Assertions.assertEquals("100.0", t.get().getRight());

        Assertions.assertNotNull(result.getLockAcquire());
        Assertions.assertFalse(result.getLockAcquire().getList().isEmpty());
        List<TaskCount> taskCountList = result.getLockAcquire().getList();
        Optional<TaskCount> optional2 = taskCountList.stream().filter(item -> item.getTask().getName().equals("Thread-2")).findAny();
        Assertions.assertTrue(optional2.isPresent());
        TaskCount ta2 = optional2.get();
        g = SimpleFlameGraph.parse(ta2);
        Assertions.assertEquals(1, g.totalSampleValue.longValue());
        list = g.queryLeafNodes(20);
        Assertions.assertEquals(1, list.size());
        t = list.stream().filter(item -> item.getLeft().contains("java.io.PrintStream.println(String)")).findAny();
        Assertions.assertTrue(t.isPresent());
        Assertions.assertEquals(1, Long.valueOf(t.get().getMiddle()));
        Assertions.assertEquals("100.0", t.get().getRight());

        Assertions.assertNotNull(result.getSynchronization());
        Assertions.assertFalse(result.getSynchronization().getList().isEmpty());
        taskSumList = result.getSynchronization().getList();
        optional = taskSumList.stream().filter(item -> item.getTask().getName().equals("JFR Periodic Tasks")).findAny();
        Assertions.assertTrue(optional.isPresent());
        ta = optional.get();
        g = SimpleFlameGraph.parse(ta);
        Assertions.assertEquals(20175, nanoToMillis(g.totalSampleValue.longValue()));
        list = g.queryLeafNodes(20);
        Assertions.assertEquals(1, list.size());
        t = list.stream().filter(item -> item.getLeft().contains("java.lang.Object.wait(long)")).findAny();
        Assertions.assertTrue(t.isPresent());
        Assertions.assertEquals(20175, nanoToMillis(Long.valueOf(t.get().getMiddle())));
        Assertions.assertEquals("100.0", t.get().getRight());
    }

    @Test
    public void testClassLoad() throws IOException, ProfileAnalysisException {
        JFRAnalyzer analyzer = new JFRAnalyzerImpl();
        Path path = createTmpFileForResource("jfr.jfr");
        AnalysisRequest request = new AnalysisRequest(path, DimensionBuilder.CLASS_LOAD_COUNT | DimensionBuilder.CLASS_LOAD_WALL_TIME);
        AnalysisResult result = analyzer.execute(request, ProgressListener.NoOpProgressListener);
        Assertions.assertNotNull(result.getClassLoadWallTime());
        Assertions.assertFalse(result.getClassLoadWallTime().getList().isEmpty());
        List<TaskSum> taskSumList = result.getClassLoadWallTime().getList();
        Optional<TaskSum> optional =
                taskSumList.stream().filter(item -> item.getTask().getName().equals("Thread-2")).findAny();
        Assertions.assertTrue(optional.isPresent());
        TaskSum ta = optional.get();
        SimpleFlameGraph g = SimpleFlameGraph.parse(ta);
        Assertions.assertEquals(2, nanoToMillis(g.totalSampleValue.longValue()));
        List<Triple<String, String, String>> list = g.queryLeafNodes(7);
        Assertions.assertEquals(1, list.size());
        Optional<Triple<String, String, String>> t =
                list.stream().filter(item -> item.getLeft().contains("java.net.ServerSocket.accept()")).findAny();
        Assertions.assertTrue(t.isPresent());
        Assertions.assertEquals(222770, Long.valueOf(t.get().getMiddle()));
        Assertions.assertEquals("7.66", t.get().getRight());

        Assertions.assertNotNull(result.getClassLoadCount());
        Assertions.assertFalse(result.getClassLoadCount().getList().isEmpty());
        List<TaskCount> taskCountList = result.getClassLoadCount().getList();
        Optional<TaskCount> optional2 = taskCountList.stream().filter(item -> item.getTask().getName().equals("Thread-2")).findAny();
        Assertions.assertTrue(optional2.isPresent());
        TaskCount ta2 = optional2.get();
        g = SimpleFlameGraph.parse(ta2);
        Assertions.assertEquals(24, g.totalSampleValue.longValue());
        list = g.queryLeafNodes(8);
        Assertions.assertEquals(1, list.size());
        t = list.stream().filter(item -> item.getLeft().contains("sun.net.NetHooks.<clinit>()")).findAny();
        Assertions.assertTrue(t.isPresent());
        Assertions.assertEquals(2, Long.valueOf(t.get().getMiddle()));
        Assertions.assertEquals("8.33", t.get().getRight());
    }

    @Test
    public void testSleep() throws IOException, ProfileAnalysisException {
        Path path = createTmpFileForResource("jfr.jfr");
        JFRAnalyzer analyzer = new JFRAnalyzerImpl();
        AnalysisRequest request = new AnalysisRequest(path, DimensionBuilder.THREAD_SLEEP);
        AnalysisResult result = analyzer.execute(request, ProgressListener.NoOpProgressListener);
        Assertions.assertNotNull(result.getThreadSleepTime());

        List<TaskSum> list = result.getThreadSleepTime().getList();
        Optional<TaskSum> optional = list.stream().filter(item -> item.getTask().getName().equals("main")).findAny();
        Assertions.assertTrue(optional.isPresent());
        TaskSum ta = optional.get();
        SimpleFlameGraph g = SimpleFlameGraph.parse(ta);
        Assertions.assertEquals(16543, nanoToMillis(g.totalSampleValue.longValue()));

        List<Triple<String, String, String>> list2 = g.queryLeafNodes(10);
        Assertions.assertEquals(list2.size(), 1);

        Optional<Triple<String, String, String>> t =
                list2.stream().filter(item -> item.getLeft().contains("java.lang.Thread.sleep(long)")).findAny();
        Assertions.assertTrue(t.isPresent());
        Assertions.assertEquals(15543, nanoToMillis(Long.valueOf(t.get().getMiddle())));
        Assertions.assertEquals("93.96", t.get().getRight());
    }

    @Test
    public void tesJfrNoWall() throws IOException, ProfileAnalysisException {
        JFRAnalyzer analyzer = new JFRAnalyzerImpl();
        Path path = createTmpFileForResource("jfr.jfr");
        AnalysisRequest request = new AnalysisRequest(path, DimensionBuilder.CPU | DimensionBuilder.CPU_SAMPLE | DimensionBuilder.WALL_CLOCK);
        AnalysisResult result = analyzer.execute(request, ProgressListener.NoOpProgressListener);
        Assertions.assertTrue(result.getWallClock().getList().isEmpty());
        Assertions.assertFalse(result.getCpuSample().getList().isEmpty());
        Assertions.assertFalse(result.getCpuTime().getList().isEmpty());
    }

    @Test
    public void testAsyncCpuNoWall() throws IOException, ProfileAnalysisException {
        JFRAnalyzer analyzer = new JFRAnalyzerImpl();
        // asprof -e cpu -d 10 -f ap.jfr jps
        // event=cpu, interval=0
        Path path = createTmpFileForResource("ap-cpu-default.jfr");
        AnalysisRequest request = new AnalysisRequest(path, DimensionBuilder.CPU | DimensionBuilder.CPU_SAMPLE | DimensionBuilder.WALL_CLOCK);
        AnalysisResult result = analyzer.execute(request, ProgressListener.NoOpProgressListener);
        Assertions.assertTrue(result.getWallClock().getList().isEmpty());
        Assertions.assertFalse(result.getCpuTime().getList().isEmpty());
        Assertions.assertFalse(result.getCpuSample().getList().isEmpty());

        // asprof -e cpu -i 20ms -d 10 -f ap.jfr jps
        // event=cpu, interval=20
        path = createTmpFileForResource("ap-cpu-20.jfr");
        request = new AnalysisRequest(path, DimensionBuilder.CPU | DimensionBuilder.CPU_SAMPLE | DimensionBuilder.WALL_CLOCK);
        result = analyzer.execute(request, ProgressListener.NoOpProgressListener);
        Assertions.assertTrue(result.getWallClock().getList().isEmpty());
        Assertions.assertFalse(result.getCpuTime().getList().isEmpty());
        Assertions.assertFalse(result.getCpuSample().getList().isEmpty());
    }

    @Test
    public void testAsyncWallNoCpu() throws IOException, ProfileAnalysisException {
        JFRAnalyzer analyzer = new JFRAnalyzerImpl();
        // asprof -e wall -d 10 -f ap.jfr jps
        // event=wall, interval=0
        Path path = createTmpFileForResource("ap-wall-default.jfr");
        AnalysisRequest request = new AnalysisRequest(path, DimensionBuilder.CPU | DimensionBuilder.CPU_SAMPLE | DimensionBuilder.WALL_CLOCK);
        AnalysisResult result = analyzer.execute(request, ProgressListener.NoOpProgressListener);
        Assertions.assertFalse(result.getWallClock().getList().isEmpty());
        Assertions.assertTrue(result.getCpuTime().getList().isEmpty());
        Assertions.assertTrue(result.getCpuSample().getList().isEmpty());

        // asprof --wall 20ms -d 10 -f ap.jfr jps
        // wall=20
        path = createTmpFileForResource("ap-wall-20.jfr");
        request = new AnalysisRequest(path, DimensionBuilder.CPU | DimensionBuilder.CPU_SAMPLE | DimensionBuilder.WALL_CLOCK);
        result = analyzer.execute(request, ProgressListener.NoOpProgressListener);
        Assertions.assertFalse(result.getWallClock().getList().isEmpty());
        Assertions.assertTrue(result.getCpuTime().getList().isEmpty());
        Assertions.assertTrue(result.getCpuSample().getList().isEmpty());

        // asprof -e wall -i 20ms -d 10 -f ap.jfr jps
        // event=wall, interval=20
        path = createTmpFileForResource("ap-wall-20-1.jfr");
        request = new AnalysisRequest(path, DimensionBuilder.CPU | DimensionBuilder.CPU_SAMPLE | DimensionBuilder.WALL_CLOCK);
        result = analyzer.execute(request, ProgressListener.NoOpProgressListener);
        Assertions.assertFalse(result.getWallClock().getList().isEmpty());
        Assertions.assertTrue(result.getCpuTime().getList().isEmpty());
        Assertions.assertTrue(result.getCpuSample().getList().isEmpty());
    }

    public static Path createTmpFileForResource(String resource) throws IOException {
        Path path = Files.createTempFile("temp", ".jfr");
        path.toFile().deleteOnExit();
        FileUtils.copyInputStreamToFile(Objects.requireNonNull(
                        TestJFRAnalyzer.class.getClassLoader().getResourceAsStream(resource)),
                path.toFile());
        return path;
    }

    private static long nanoToMillis(long nano) {
        return nano / 1000 / 1000;
    }
}
