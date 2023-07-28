/********************************************************************************
 * Copyright (c) 2021, 2022 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.hda.api;

import org.eclipse.jifa.analysis.annotation.ApiMeta;
import org.eclipse.jifa.analysis.annotation.ApiParameterMeta;
import org.eclipse.jifa.analysis.annotation.Exclude;
import org.eclipse.jifa.analysis.listener.ProgressListener;
import org.eclipse.jifa.common.domain.vo.PageView;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.eclipse.jifa.hda.api.Model.ClassLoader;
import static org.eclipse.jifa.hda.api.Model.*;

public interface HeapDumpAnalyzer {

    @Exclude
    void dispose();

    Overview.Details getDetails();

    Map<String, String> getSystemProperties();

    List<Overview.BigObject> getBiggestObjects();

    @ApiMeta(aliases = "object")
    JavaObject getObjectInfo(int objectId);

    @ApiMeta(aliases = "inspector.objectView")
    InspectorView getInspectorView(int objectId);

    @ApiMeta(aliases = "inspector.fields")
    PageView<FieldView> getFields(int objectId, int page, int pageSize);

    @ApiMeta(aliases = "inspector.staticFields")
    PageView<FieldView> getStaticFields(int objectId, int page, int pageSize);

    @ApiMeta(aliases = "inspector.addressToId")
    int mapAddressToId(long address);

    @ApiMeta(aliases = "inspector.value")
    String getObjectValue(int objectId);

    @ApiMeta(aliases = "classLoaderExplorer.summary")
    ClassLoader.Summary getSummaryOfClassLoaders();

    @ApiMeta(aliases = "classLoaderExplorer.classLoader")
    PageView<ClassLoader.Item> getClassLoaders(int page, int pageSize);

    @ApiMeta(aliases = "classLoaderExplorer.children")
    PageView<ClassLoader.Item> getChildrenOfClassLoader(int classLoaderId,
                                                        int page, int pageSize);

    @ApiMeta(aliases = "unreachableObjects.summary")
    UnreachableObject.Summary getSummaryOfUnreachableObjects();

    @ApiMeta(aliases = "unreachableObjects.records")
    PageView<UnreachableObject.Item> getUnreachableObjects(int page, int pageSize);

    @ApiMeta(aliases = "directByteBuffer.summary")
    DirectByteBuffer.Summary getSummaryOfDirectByteBuffers();

    @ApiMeta(aliases = "directByteBuffer.records")
    PageView<DirectByteBuffer.Item> getDirectByteBuffers(int page, int pageSize);

    @ApiMeta(aliases = "outbounds")
    PageView<JavaObject> getOutboundOfObject(int objectId, int page, int pageSize);

    @ApiMeta(aliases = "inbounds")
    PageView<JavaObject> getInboundOfObject(int objectId, int page, int pageSize);

    List<GCRoot.Item> getGCRoots();

    @ApiMeta(aliases = "findStrings")
    PageView<TheString.Item> getStrings(String pattern, int page, int pageSize);

    @ApiMeta(aliases = "GCRoots.classes")
    PageView<GCRoot.Item> getClassesOfGCRoot(int rootTypeIndex, int page, int pageSize);

    @ApiMeta(aliases = "GCRoots.class.objects")
    PageView<JavaObject> getObjectsOfGCRoot(int rootTypeIndex, int classIndex, int page, int pageSize);

    @ApiMeta(aliases = "classReference.outbounds.class")
    ClassReferrer.Item getOutboundClassOfClassReference(int objectId);

    @ApiMeta(aliases = "classReference.inbounds.class")
    ClassReferrer.Item getInboundClassOfClassReference(int objectId);

    @ApiMeta(aliases = "classReference.outbounds.children")
    PageView<ClassReferrer.Item> getOutboundsOfClassReference(int[] objectIds, int page, int pageSize);

    @ApiMeta(aliases = "classReference.inbounds.children")
    PageView<ClassReferrer.Item> getInboundsOfClassReference(int[] objectIds, int page, int pageSize);

    Comparison.Summary getSummaryOfComparison(@ApiParameterMeta(comparisonTargetPath = true) Path other);

    PageView<Comparison.Item> getItemsOfComparison(@ApiParameterMeta(comparisonTargetPath = true) Path other, int page, int pageSize);

    @ApiMeta(aliases = "mergePathToGCRoots.roots.byClassId")
    PageView<GCRootPath.MergePathToGCRootsTreeNode> getRootsOfMergePathToGCRootsByClassId(int classId,
                                                                                          GCRootPath.Grouping grouping,
                                                                                          int page, int pageSize);

    @ApiMeta(aliases = "mergePathToGCRoots.roots.byObjectIds")
    PageView<GCRootPath.MergePathToGCRootsTreeNode> getRootsOfMergePathToGCRootsByObjectIds(int[] objectIds,
                                                                                            GCRootPath.Grouping grouping,
                                                                                            int page, int pageSize);

    @ApiMeta(aliases = "mergePathToGCRoots.children.byClassId")
    PageView<GCRootPath.MergePathToGCRootsTreeNode> getChildrenOfMergePathToGCRootsByClassId(int classId,
                                                                                             int[] objectIdPathInGCPathTree,
                                                                                             GCRootPath.Grouping grouping,
                                                                                             int page, int pageSize);

    @ApiMeta(aliases = "mergePathToGCRoots.children.byObjectIds")
    PageView<GCRootPath.MergePathToGCRootsTreeNode> getChildrenOfMergePathToGCRootsByObjectIds(int[] objectIds,
                                                                                               int[] objectIdPathInGCPathTree,
                                                                                               GCRootPath.Grouping grouping,
                                                                                               int page, int pageSize);

    GCRootPath.Item getPathToGCRoots(int originId, int skip, int count);

    @ApiMeta(aliases = "leak.report")
    LeakReport getLeakReport();

    @ApiMeta(aliases = "oql")
    OQLResult getOQLResult(String oql, String sortBy, boolean ascendingOrder, int page, int pageSize);

    @ApiMeta(aliases = "sql")
    CalciteSQLResult getCalciteSQLResult(String sql, String sortBy, boolean ascendingOrder, int page, int pageSize);

    @ApiMeta(aliases = "threadsSummary")
    Model.Thread.Summary getSummaryOfThreads(String searchText, SearchType searchType);

    PageView<Model.Thread.Item> getThreads(String sortBy, boolean ascendingOrder, String searchText,
                                           SearchType searchType, int page, int pageSize);

    List<Model.Thread.StackFrame> getStackTrace(int objectId);

    @ApiMeta(aliases = "locals")
    List<Model.Thread.LocalVariable> getLocalVariables(int objectId, int depth, boolean firstNonNativeFrame);

    @ApiMeta(aliases = "duplicatedClasses.classes")
    PageView<DuplicatedClass.ClassItem> getDuplicatedClasses(String searchText, SearchType searchType,
                                                             int page, int pageSize);

    @ApiMeta(aliases = "duplicatedClasses.classLoaders")
    PageView<DuplicatedClass.ClassLoaderItem> getClassloadersOfDuplicatedClass(int index, int page,
                                                                               int pageSize);

    PageView<Histogram.Item> getHistogram(Histogram.Grouping groupingBy,
                                          @ApiParameterMeta(required = false) int[] ids,
                                          String sortBy, boolean ascendingOrder,
                                          String searchText, SearchType searchType, int page, int pageSize);

    @ApiMeta(aliases = "histogram.objects")
    PageView<JavaObject> getHistogramObjects(int classId, int page, int pageSize);

    @ApiMeta(aliases = "histogram.children")
    PageView<Histogram.Item> getChildrenOfHistogram(Histogram.Grouping groupBy, int[] ids,
                                                    String sortBy, boolean ascendingOrder, int parentObjectId,
                                                    int page, int pageSize);

    @ApiMeta(aliases = "dominatorTree.roots")
    PageView<? extends DominatorTree.Item> getRootsOfDominatorTree(DominatorTree.Grouping groupBy,
                                                                   String sortBy,
                                                                   boolean ascendingOrder, String searchText,
                                                                   SearchType searchType, int page, int pageSize);

    @ApiMeta(aliases = "dominatorTree.children")
    PageView<? extends DominatorTree.Item> getChildrenOfDominatorTree(DominatorTree.Grouping groupBy,
                                                                      String sortBy, boolean ascendingOrder,
                                                                      int parentObjectId, int[] idPathInResultTree,
                                                                      int page,
                                                                      int pageSize);

    interface Provider {
        HeapDumpAnalyzer provide(Path path, Map<String, String> options, ProgressListener listener);
    }

}
