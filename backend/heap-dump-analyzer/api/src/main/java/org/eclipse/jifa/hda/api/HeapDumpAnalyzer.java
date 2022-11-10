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

import org.eclipse.jifa.common.listener.ProgressListener;
import org.eclipse.jifa.common.vo.PageView;
import org.eclipse.jifa.common.vo.support.SearchType;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.eclipse.jifa.hda.api.Model.ClassLoader;
import static org.eclipse.jifa.hda.api.Model.*;

public interface HeapDumpAnalyzer {

    void dispose();

    Overview.Details getDetails();

    Map<String, String> getSystemProperties();

    List<Overview.BigObject> getBigObjects();

    JavaObject getObjectInfo(int objectId);

    InspectorView getInspectorView(int objectId);

    PageView<FieldView> getFields(int objectId, int page, int pageSize);

    PageView<FieldView> getStaticFields(int objectId, int page, int pageSize);

    int mapAddressToId(long address);

    String getObjectValue(int objectId);

    ClassLoader.Summary getSummaryOfClassLoaders();

    PageView<ClassLoader.Item> getClassLoaders(int page, int pageSize);

    PageView<ClassLoader.Item> getChildrenOfClassLoader(int classLoaderId,
                                                        int page, int pageSize);

    UnreachableObject.Summary getSummaryOfUnreachableObjects();

    PageView<UnreachableObject.Item> getUnreachableObjects(int page, int pageSize);

    DirectByteBuffer.Summary getSummaryOfDirectByteBuffers();

    PageView<DirectByteBuffer.Item> getDirectByteBuffers(int page, int pageSize);

    PageView<JavaObject> getOutboundOfObject(int objectId, int page, int pageSize);

    PageView<JavaObject> getInboundOfObject(int objectId, int page, int pageSize);

    List<GCRoot.Item> getGCRoots();

    PageView<TheString.Item> getStrings(String pattern, int page, int pageSize);

    PageView<GCRoot.Item> getClassesOfGCRoot(int rootTypeIndex, int page, int pageSize);

    PageView<JavaObject> getObjectsOfGCRoot(int rootTypeIndex, int classIndex, int page, int pageSize);

    ClassReferrer.Item getOutboundClassOfClassReference(int objectId);

    ClassReferrer.Item getInboundClassOfClassReference(int objectId);

    PageView<ClassReferrer.Item> getOutboundsOfClassReference(int[] objectIds, int page, int pageSize);

    PageView<ClassReferrer.Item> getInboundsOfClassReference(int[] objectIds, int page, int pageSize);

    Comparison.Summary getSummaryOfComparison(Path other);

    PageView<Comparison.Item> getItemsOfComparison(Path other, int page, int pageSize);

    PageView<GCRootPath.MergePathToGCRootsTreeNode> getRootsOfMergePathToGCRootsByClassId(int classId,
                                                                                          GCRootPath.Grouping grouping,
                                                                                          int page, int pageSize);

    PageView<GCRootPath.MergePathToGCRootsTreeNode> getRootsOfMergePathToGCRootsByObjectIds(int[] objectIds,
                                                                                            GCRootPath.Grouping grouping,
                                                                                            int page, int pageSize);

    PageView<GCRootPath.MergePathToGCRootsTreeNode> getChildrenOfMergePathToGCRootsByClassId(int classId,
                                                                                             int[] objectIdPathInGCPathTree,
                                                                                             GCRootPath.Grouping grouping,
                                                                                             int page, int pageSize);

    PageView<GCRootPath.MergePathToGCRootsTreeNode> getChildrenOfMergePathToGCRootsByObjectIds(int[] objectIds,
                                                                                               int[] objectIdPathInGCPathTree,
                                                                                               GCRootPath.Grouping grouping,
                                                                                               int page, int pageSize);

    GCRootPath.Item getPathToGCRoots(int originId, int skip, int count);

    LeakReport getLeakReport();

    OQLResult getOQLResult(String oql, String sortBy, boolean ascendingOrder, int page, int pageSize);

    CalciteSQLResult getCalciteSQLResult(String sql, String sortBy, boolean ascendingOrder, int page, int pageSize);

    Model.Thread.Summary getSummaryOfThreads(String searchText, SearchType searchType);

    PageView<Model.Thread.Item> getThreads(String sortBy, boolean ascendingOrder, String searchText,
                                           SearchType searchType, int page, int pageSize);

    List<Model.Thread.StackFrame> getStackTrace(int objectId);

    List<Model.Thread.LocalVariable> getLocalVariables(int objectId, int depth, boolean firstNonNativeFrame);

    PageView<DuplicatedClass.ClassItem> getDuplicatedClasses(String searchText, SearchType searchType,
                                                             int page, int pageSize);

    PageView<DuplicatedClass.ClassLoaderItem> getClassloadersOfDuplicatedClass(int index, int page,
                                                                               int pageSize);

    PageView<Histogram.Item> getHistogram(Histogram.Grouping groupingBy, int[] ids,
                                          String sortBy, boolean ascendingOrder,
                                         String searchText, SearchType searchType, int page, int pageSize);

    PageView<JavaObject> getHistogramObjects(int classId, int page, int pageSize);

    PageView<Histogram.Item> getChildrenOfHistogram(Histogram.Grouping groupBy, int[] ids,
                                                    String sortBy, boolean ascendingOrder, int parentObjectId,
                                                    int page, int pageSize);

    PageView<? extends DominatorTree.Item> getRootsOfDominatorTree(DominatorTree.Grouping groupBy,
                                                                   String sortBy,
                                                                   boolean ascendingOrder, String searchText,
                                                                   SearchType searchType, int page, int pageSize);

    PageView<? extends DominatorTree.Item> getChildrenOfDominatorTree(DominatorTree.Grouping groupBy,
                                                                      String sortBy, boolean ascendingOrder,
                                                                      int parentObjectId, int[] idPathInResultTree,
                                                                      int page,
                                                                      int pageSize);

    interface Provider {
        HeapDumpAnalyzer provide(Path path, Map<String, String> arguments, ProgressListener listener);
    }

}
