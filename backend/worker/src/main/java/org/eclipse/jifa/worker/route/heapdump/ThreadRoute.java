/********************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.worker.route.heapdump;

import io.vertx.core.Future;
import org.eclipse.jifa.common.aux.JifaException;
import org.eclipse.jifa.common.request.PagingRequest;
import org.eclipse.jifa.common.util.PageViewBuilder;
import org.eclipse.jifa.common.vo.PageView;
import org.eclipse.jifa.worker.Constant;
import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;
import org.eclipse.jifa.worker.support.Analyzer;
import org.eclipse.jifa.worker.support.heapdump.HeapDumpSupport;
import org.eclipse.jifa.worker.vo.heapdump.HeapObject;
import org.eclipse.jifa.worker.vo.heapdump.thread.Info;
import org.eclipse.jifa.worker.vo.heapdump.thread.LocalVariable;
import org.eclipse.jifa.worker.vo.heapdump.thread.StackFrame;
import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.inspections.threads.ThreadOverviewQuery;
import org.eclipse.mat.internal.Messages;
import org.eclipse.mat.query.Bytes;
import org.eclipse.mat.query.IResultTree;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.model.GCRootInfo;
import org.eclipse.mat.snapshot.model.IObject;
import org.eclipse.mat.snapshot.query.IHeapObjectArgument;
import org.eclipse.mat.util.IProgressListener;

import java.util.*;
import java.util.stream.Collectors;

import static org.eclipse.jifa.common.util.Assertion.ASSERT;

class ThreadRoute extends HeapBaseRoute {

    @RouteMeta(path = "/threadsSummary")
    void threadsSummary(Future<Map<String, Long>> future, @ParamKey("file") String file) throws Exception {

        ThreadOverviewQuery query = new ThreadOverviewQuery();
        query.snapshot = Analyzer.getOrOpenSnapshotContext(file).getSnapshot();
        IResultTree result = (IResultTree) query.execute(HeapDumpSupport.VOID_LISTENER);
        List<?> elements = result.getElements();
        long totalShallowSize = 0;
        long totalRetainedSize = 0;
        for (Object t : elements) {
            totalShallowSize += ((Bytes) result.getColumnValue(t, 2)).getValue();
            totalRetainedSize += ((Bytes) result.getColumnValue(t, 3)).getValue();
        }
        Map<String, Long> info = new HashMap<>();
        info.put(Constant.Heap.TOTAL_SIZE_KEY, (long) elements.size());
        info.put(Constant.Heap.SHALLOW_HEAP_KEY, totalShallowSize);
        info.put(Constant.Heap.RETAINED_HEAP_KEY, totalRetainedSize);
        future.complete(info);
    }

    @RouteMeta(path = "/threads")
    void threads(Future<PageView<Info>> future, @ParamKey("file") String file, PagingRequest paging) throws Exception {

        ThreadOverviewQuery query = new ThreadOverviewQuery();
        query.snapshot = Analyzer.getOrOpenSnapshotContext(file).getSnapshot();
        IResultTree result = (IResultTree) query.execute(HeapDumpSupport.VOID_LISTENER);
        List<?> elements = result.getElements();


        future.complete(PageViewBuilder.build(elements, paging, e -> new Info(result.getContext(e).getObjectId(),
                                                                              (String) result.getColumnValue(e, 0),
                                                                              (String) result.getColumnValue(e, 1),
                                                                              ((Bytes) result.getColumnValue(e, 2))
                                                                                  .getValue(),
                                                                              ((Bytes) result.getColumnValue(e, 3))
                                                                                  .getValue(),
                                                                              (String) result.getColumnValue(e, 4),
                                                                              result.hasChildren(e),
                                                                              (Boolean) result.getColumnValue(e, 5))));
    }

    private IResultTree fetchStaceTrace(ISnapshot snapshot, int objectId) throws Exception {
        ThreadOverviewQuery query = new ThreadOverviewQuery();
        query.snapshot = snapshot;
        query.objects = new IHeapObjectArgument() {
            @Override
            public int[] getIds(IProgressListener iProgressListener) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getLabel() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Iterator<int[]> iterator() {
                return new Iterator<int[]>() {

                    boolean hasNext = true;

                    @Override
                    public boolean hasNext() {
                        return hasNext;
                    }

                    @Override
                    public int[] next() {
                        ASSERT.isTrue(hasNext);
                        hasNext = false;
                        return new int[]{objectId};
                    }
                };
            }
        };
        return (IResultTree) query.execute(HeapDumpSupport.VOID_LISTENER);
    }

    @RouteMeta(path = "/stackTrace")
    void stackTrace(Future<List<StackFrame>> future, @ParamKey("file") String file,
                    @ParamKey("objectId") int objectId) throws Exception {


        IResultTree result = fetchStaceTrace(Analyzer.getOrOpenSnapshotContext(file).getSnapshot(), objectId);

        List<?> elements = result.getElements();

        if (result.hasChildren(elements.get(0))) {

            List<?> frames = result.getChildren(elements.get(0));

            List<StackFrame> res = frames.stream().map(
                frame -> new StackFrame((String) result.getColumnValue(frame, 0), result.hasChildren(frame)))
                                         .collect(Collectors.toList());
            res.stream().filter(t -> !t.getStack().contains("Native Method")).findFirst()
               .ifPresent(sf -> sf.setFirstNonNativeFrame(true));
            future.complete(res);
        } else {
            future.complete(Collections.emptyList());
        }
    }

    @RouteMeta(path = "/locals")
    void locals(Future<List<LocalVariable>> future, @ParamKey("file") String file, @ParamKey("objectId") int objectId,
                @ParamKey("depth") int depth,
                @ParamKey("firstNonNativeFrame") boolean firstNonNativeFrame) throws Exception {
        ISnapshot snapshot = Analyzer.getOrOpenSnapshotContext(file).getSnapshot();
        IResultTree result = fetchStaceTrace(snapshot, objectId);

        List<?> elements = result.getElements();
        if (result.hasChildren(elements.get(0))) {
            List<?> frames = result.getChildren(elements.get(0));
            Object frame = frames.get(depth - 1);
            if (result.hasChildren(frame)) {
                List<?> locals = result.getChildren(frame);
                future.complete(locals.stream().map(local -> {
                    int id = result.getContext(local).getObjectId();
                    LocalVariable var = new LocalVariable();
                    var.setObjectId(id);
                    try {
                        IObject object = snapshot.getObject(id);
                        var.setLabel(object.getDisplayName());
                        var.setShallowSize(object.getUsedHeapSize());
                        var.setRetainedSize(object.getRetainedHeapSize());
                        var.setObjectType(HeapObject.Type.typeOf(object));
                        var.setGCRoot(snapshot.isGCRoot(id));
                        var.setHasOutbound(result.hasChildren(var));
                        var.setPrefix(Messages.ThreadStackQuery_Label_Local);
                        if (firstNonNativeFrame) {
                            GCRootInfo[] gcRootInfos = object.getGCRootInfo();
                            if (gcRootInfos != null) {
                                for (GCRootInfo gcRootInfo : gcRootInfos) {
                                    if (gcRootInfo.getContextId() != 0 &&
                                        (gcRootInfo.getType() & GCRootInfo.Type.BUSY_MONITOR) != 0 &&
                                        gcRootInfo.getContextId() == objectId) {
                                        var.setPrefix(Messages.ThreadStackQuery_Label_Local_Blocked_On);
                                    }
                                }
                            }
                        }
                        var.setSuffix(HeapDumpSupport.suffix(snapshot, id));
                        return var;
                    } catch (SnapshotException e) {
                        throw new JifaException(e);
                    }
                }).collect(Collectors.toList()));
                return;
            }
        }
        future.complete(Collections.emptyList());
    }
}
