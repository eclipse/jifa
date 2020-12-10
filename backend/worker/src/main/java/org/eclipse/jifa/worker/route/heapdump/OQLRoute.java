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
import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;
import org.eclipse.jifa.worker.support.Analyzer;
import org.eclipse.jifa.worker.support.heapdump.HeapDumpSupport;
import org.eclipse.jifa.worker.vo.heapdump.HeapObject;
import org.eclipse.jifa.worker.vo.heapdump.oql.TableResult;
import org.eclipse.jifa.worker.vo.heapdump.oql.TextResult;
import org.eclipse.jifa.worker.vo.heapdump.oql.TreeResult;
import org.eclipse.mat.inspections.OQLQuery;
import org.eclipse.mat.query.Column;
import org.eclipse.mat.query.IContextObject;
import org.eclipse.mat.query.IResultTable;
import org.eclipse.mat.query.IResultTree;
import org.eclipse.mat.snapshot.IOQLQuery;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.model.IObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class OQLRoute extends HeapBaseRoute {

    @RouteMeta(path = "/oql")
    void oql(Future<Object> future, @ParamKey("file") String file, @ParamKey("oql") String oql, @ParamKey(value = "sortBy", mandatory = false) String sortBy,
             @ParamKey(value = "ascendingOrder", mandatory = false) boolean ascendingOrder,
             PagingRequest pagingRequest) throws Exception {
        OQLQuery query = new OQLQuery();
        ISnapshot snapshot = Analyzer.getOrOpenSnapshotContext(file).getSnapshot();
        query.snapshot = snapshot;
        query.queryString = oql;
        IOQLQuery.Result qr = query.execute(HeapDumpSupport.VOID_LISTENER);

        if (qr instanceof IResultTree) {
            future.complete(new TreeResult(PageViewBuilder.build(((IResultTree) qr).getElements(), pagingRequest, e -> {
                try {
                    int objectId = ((IResultTree) qr).getContext(e).getObjectId();
                    IObject o = snapshot.getObject(objectId);
                    HeapObject ho = new HeapObject();
                    ho.setObjectId(objectId);
                    ho.setLabel(o.getDisplayName());
                    ho.setSuffix(HeapDumpSupport.suffix(o.getGCRootInfo()));
                    ho.setShallowSize(o.getUsedHeapSize());
                    ho.setRetainedSize(o.getRetainedHeapSize());
                    ho.setGCRoot(snapshot.isGCRoot(objectId));
                    ho.setObjectType(HeapObject.Type.typeOf(o));
                    ho.setHasOutbound(true);
                    return ho;
                } catch (Exception ex) {
                    throw new JifaException(ex);
                }
            }, HeapObject.sortBy(sortBy, ascendingOrder))));
        } else if (qr instanceof IResultTable) {
            IResultTable table = (IResultTable) qr;
            Column[] columns = table.getColumns();
            List<String> cs = Arrays.stream(columns).map(Column::getLabel).collect(Collectors.toList());
            PageView<TableResult.Entry> pv = PageViewBuilder.build(new PageViewBuilder.Callback<Object>() {
                @Override
                public int totalSize() {
                    return table.getRowCount();
                }

                @Override
                public Object get(int index) {
                    return table.getRow(index);
                }
            }, pagingRequest, o -> {
                List<Object> l = new ArrayList<>();
                for (int i = 0; i < columns.length; i++) {
                    Object columnValue = table.getColumnValue(o, i);

                    l.add(columnValue != null ? columnValue.toString() : null);
                }
                IContextObject context = table.getContext(o);
                return new TableResult.Entry(
                        context != null ? context.getObjectId() : HeapDumpSupport.ILLEGAL_OBJECT_ID, l);
            });
            future.complete(new TableResult(cs, pv));
        } else if (qr instanceof org.eclipse.mat.query.results.TextResult) {
            future.complete(new TextResult(((org.eclipse.mat.query.results.TextResult) qr).getText()));
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
