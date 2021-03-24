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
package org.eclipse.jifa.hda.api;

import org.eclipse.jifa.common.vo.PageView;
import org.eclipse.jifa.common.vo.support.SearchType;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.eclipse.jifa.hda.api.Model.ClassLoader;
import static org.eclipse.jifa.hda.api.Model.*;

public interface HeapDumpAnalyzer<C extends AnalysisContext> {

    interface Provider {
        HeapDumpAnalyzer<AnalysisContext> get();
    }

    C open(File dump, Map<String, String> arguments, ProgressListener listener);

    void dispose(C context);

    Overview.Details getDetails(C context);

    Map<String, String> getSystemProperties(C context);

    List<Overview.BigObject> getBigObjects(C context);

    JavaObject getObjectInfo(C context, int objectId);

    InspectorView getInspectorView(C context, int objectId);

    PageView<FieldView> getFields(C context, int objectId, int page, int pageSize);

    PageView<FieldView> getStaticFields(C context, int objectId, int page, int pageSize);

    int mapAddressToId(C context, long address);

    String getObjectValue(C context, int objectId);

    ClassLoader.Summary getSummaryOfClassLoaders(C context);

    PageView<ClassLoader.Item> getClassLoaders(C context, int page, int pageSize);

    PageView<ClassLoader.Item> getChildrenOfClassLoader(C context, int classLoaderId,
                                                        int page, int pageSize);

    UnreachableObject.Summary getSummaryOfUnreachableObjects(C context);

    PageView<UnreachableObject.Item> getUnreachableObjects(C context, int page, int pageSize);

    DirectByteBuffer.Summary getSummaryOfDirectByteBuffers(C context);

    PageView<DirectByteBuffer.Item> getDirectByteBuffers(C context, int page, int pageSize);

    PageView<JavaObject> getOutboundOfObject(C context, int objectId, int page, int pageSize);

    PageView<JavaObject> getInboundOfObject(C context, int objectId, int page, int pageSize);

    List<GCRoot.Item> getGCRoots(C context);

    PageView<GCRoot.Item> getClassesOfGCRoot(C context, int rootTypeIndex, int page, int pageSize);

    PageView<JavaObject> getObjectsOfGCRoot(C context, int rootTypeIndex, int classIndex, int page, int pageSize);

    ClassReferrer.Item getOutboundClassOfClassReference(C context, int objectId);

    ClassReferrer.Item getInboundClassOfClassReference(C context, int objectId);

    PageView<ClassReferrer.Item> getOutboundsOfClassReference(C context, int[] objectIds, int page, int pageSize);

    PageView<ClassReferrer.Item> getInboundsOfClassReference(C context, int[] objectIds, int page, int pageSize);

    Comparison.Summary getSummaryOfComparison(C base, C other);

    PageView<Comparison.Item> getItemsOfComparison(C base, C other, int page, int pageSize);

    PageView<GCRootPath.MergePathToGCRootsTreeNode> getRootsOfMergePathToGCRootsByClassId(C context, int classId,
                                                                                          GCRootPath.Grouping grouping,
                                                                                          int page, int pageSize);

    PageView<GCRootPath.MergePathToGCRootsTreeNode> getChildrenOfMergePathToGCRootsByClassId(C context, int classId,
                                                                                             int[] objectIdPathInGCPathTree,
                                                                                             GCRootPath.Grouping grouping,
                                                                                             int page, int pageSize);

    GCRootPath.Item getPathToGCRoots(C context, int originId, int skip, int count);

    LeakReport getLeakReport(C context);

    OQLResult getOQLResult(C context, String oql, String sortBy, boolean ascendingOrder, int page, int pageSize);

    Model.Thread.Summary getSummaryOfThreads(C context, String searchText, SearchType searchType);

    PageView<Model.Thread.Item> getThreads(C context, String sortBy, boolean ascendingOrder, String searchText,
                                           SearchType searchType, int page, int pageSize);

    List<Model.Thread.StackFrame> getStackTrace(C context, int objectId);

    List<Model.Thread.LocalVariable> getLocalVariables(C context, int objectId, int depth, boolean firstNonNativeFrame);

    PageView<DuplicatedClass.ClassItem> getDuplicatedClasses(C context, String searchText, SearchType searchType,
                                                             int page, int pageSize);

    PageView<DuplicatedClass.ClassLoaderItem> getClassloadersOfDuplicatedClass(C context, int index, int page,
                                                                               int pageSize);

    PageView<Histogram.Item> getHistogram(C context, Histogram.Grouping groupingBy, int[] ids,
                                          String sortBy, boolean ascendingOrder,
                                          String searchText, SearchType searchType, int page, int pageSize);

    PageView<Histogram.Item> getChildrenOfHistogram(C context, Histogram.Grouping groupBy, int[] ids,
                                                    String sortBy, boolean ascendingOrder, int parentObjectId,
                                                    int page, int pageSize);

    PageView<? extends DominatorTree.Item> getRootsOfDominatorTree(C context, DominatorTree.Grouping groupBy,
                                                                   String sortBy,
                                                                   boolean ascendingOrder, String searchText,
                                                                   SearchType searchType, int page, int pageSize);

    PageView<? extends DominatorTree.Item> getChildrenOfDominatorTree(C context, DominatorTree.Grouping groupBy,
                                                                      String sortBy, boolean ascendingOrder,
                                                                      int parentObjectId, int[] idPathInResultTree,
                                                                      int page,
                                                                      int pageSize);

}
