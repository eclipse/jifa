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
package org.eclipse.jifa.hda.impl;

import com.sun.management.HotSpotDiagnosticMXBean;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jifa.analysis.listener.ProgressListener;
import org.eclipse.jifa.hda.api.HeapDumpAnalyzer;
import org.eclipse.jifa.hda.api.Model;
import org.eclipse.jifa.hda.api.SearchType;
import org.eclipse.jifa.hdp.provider.HeapDumpAnalysisApiExecutor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

@Slf4j
public class TestHeapDumpAnalyzerImpl {

    private static HeapDumpAnalyzer ANALYZER;

    @BeforeAll
    public static void init() throws Exception {
        // init environment
        Path heapFile = Files.createTempFile("test-heap", ".hprof").toAbsolutePath();
        Files.delete(heapFile);
        HotSpotDiagnosticMXBean platformMXBean = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
        platformMXBean.dumpHeap(heapFile.toString(), false);
        if (Files.exists(heapFile)) {
            log.info("Heap dump file: {}", heapFile);
            heapFile.toFile().deleteOnExit();
        }
        Method buildAnalyzer = HeapDumpAnalysisApiExecutor.class.getDeclaredMethod("buildAnalyzer", Path.class, Map.class, ProgressListener.class);
        buildAnalyzer.setAccessible(true);
        ANALYZER = (HeapDumpAnalyzer) buildAnalyzer.invoke(new HeapDumpAnalysisApiExecutor(),
                                                           heapFile,
                                                           Collections.emptyMap(),
                                                           ProgressListener.NoOpProgressListener);
    }

    @Test
    public void testGetDetails() {
        ANALYZER.getDetails();
    }

    @Test
    public void testGetSystemProperties() {
        ANALYZER.getSystemProperties();
    }

    @Test
    public void testGetBiggestObjects() {
        ANALYZER.getBiggestObjects();
    }

    @Test
    public void testGetObjectInfo() {
        ANALYZER.getObjectInfo(1);
    }

    @Test
    public void testGetInspectorView() {
        ANALYZER.getInspectorView(1);
    }

    @Test
    public void testGetFields() {
        ANALYZER.getFields(1, 1, 10);
    }

    @Test
    public void testGetStaticFields() {
        ANALYZER.getStaticFields(1, 1, 10);
    }

    @Test
    public void mapAddressToId() {
        ANALYZER.mapAddressToId(ANALYZER.getInspectorView(1).objectAddress);
    }

    @Test
    public void testGetObjectValue() {
        ANALYZER.getObjectValue(1);
    }

    @Test
    public void testGetSummaryOfClassLoaders() {
        ANALYZER.getSummaryOfClassLoaders();
    }

    @Test
    public void testGetClassLoaders() {
        ANALYZER.getClassLoaders(1, 10);
    }

    @Test
    public void testGetChildrenOfClassLoader() {
        Model.ClassLoader.Item item = ANALYZER.getClassLoaders(1, 1).getData().get(0);
        ANALYZER.getChildrenOfClassLoader(item.objectId, 1, 10);
    }

    @Test
    public void testGetSummaryOfUnreachableObjects() {
        ANALYZER.getSummaryOfUnreachableObjects();
    }

    @Test
    public void testGetUnreachableObjects() {
        ANALYZER.getUnreachableObjects(1, 10);
    }

    @Test
    public void testGetSummaryOfDirectByteBuffers() {
        ANALYZER.getSummaryOfDirectByteBuffers();
    }

    @Test
    public void testGetDirectByteBuffers() {
        ANALYZER.getDirectByteBuffers(1, 10);
    }

    @Test
    public void testGetOutboundOfObject() {
        ANALYZER.getOutboundOfObject(1, 1, 10);
    }

    @Test
    public void testGetInboundOfObject() {
        ANALYZER.getInboundOfObject(1, 1, 10);
    }

    @Test
    public void testGetGCRoots() {
        ANALYZER.getGCRoots();
    }

    @Test
    public void testGetStrings() {
        ANALYZER.getStrings("abc", 1, 10);
    }

    @Test
    public void testGetClassesOfGCRoot() {
        ANALYZER.getClassesOfGCRoot(1, 1, 10);
    }

    @Test
    public void testGetObjectsOfGCRoot() {
        ANALYZER.getObjectsOfGCRoot(1, 1, 1, 10);
    }

    @Test
    public void testGetOutboundClassOfClassReference() {
        ANALYZER.getOutboundClassOfClassReference(1);
    }

    @Test
    public void testGetInboundClassOfClassReference() {
        ANALYZER.getInboundClassOfClassReference(1);
    }

    @Test
    public void testGetOutboundsOfClassReference() {
        ANALYZER.getOutboundsOfClassReference(new int[]{1}, 1, 10);
    }

    @Test
    public void testGetInboundsOfClassReference() {
        ANALYZER.getInboundsOfClassReference(new int[]{1}, 1, 10);
    }

    @Test
    public void testGetPathToGCRoots() {
        ANALYZER.getPathToGCRoots(1, 1, 10);
    }

    @Test
    public void testGetLeakReport() {
        ANALYZER.getLeakReport();
    }

    @Test
    public void testGetOQLResult() {
        ANALYZER.getOQLResult("select * from java.lang.String", "shallowHeap", true, 1, 10);
    }

    @Test
    public void testGetCalciteSQLResult() {
    }

    @Test
    public void testGetSummaryOfThreads() {
    }

    @Test
    public void testGetThreads() {
        ANALYZER.getThreads("id", true, "", SearchType.BY_NAME, 1, 10);
    }

    @Test
    public void testGetStackTrace() {
        int id = ANALYZER.getThreads("id", true, "", SearchType.BY_NAME, 1, 1).getData().get(0).getObjectId();
        ANALYZER.getStackTrace(id);
    }

    @Test
    public void testGetLocalVariables() {
        int id = ANALYZER.getThreads("id", true, "", SearchType.BY_NAME, 1, 1).getData().get(0).getObjectId();
        ANALYZER.getLocalVariables(id, 1, false);
    }

    @Test
    public void testGetDuplicatedClasses() {
        ANALYZER.getDuplicatedClasses("", SearchType.BY_NAME, 1, 10);
    }

    @Test
    public void testGetHistogram() {
        ANALYZER.getHistogram(Model.Histogram.Grouping.BY_CLASS,
                              null,
                              "shallowHeap",
                              true,
                              null,
                              null, 1, 10);
    }

    @Test
    public void testGetHistogramObjects() {
        int id = ANALYZER.getHistogram(Model.Histogram.Grouping.BY_CLASS,
                                       null,
                                       "shallowHeap",
                                       true,
                                       null,
                                       null, 1, 1).getData().get(0).getObjectId();
        ANALYZER.getHistogramObjects(id, 1, 10);
    }

    @Test
    public void testGetRootsOfDominatorTree() {
        ANALYZER.getRootsOfDominatorTree(Model.DominatorTree.Grouping.NONE, "shallowHeap", true, null, SearchType.BY_NAME, 1, 10);
    }
}
