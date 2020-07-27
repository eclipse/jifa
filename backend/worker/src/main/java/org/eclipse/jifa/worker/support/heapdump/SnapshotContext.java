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
package org.eclipse.jifa.worker.support.heapdump;

import lombok.Data;
import org.eclipse.mat.inspections.ClassLoaderExplorerQuery;
import org.eclipse.mat.inspections.LeakHunterQuery;
import org.eclipse.mat.inspections.OQLQuery;
import org.eclipse.mat.internal.snapshot.SnapshotQueryContext;
import org.eclipse.mat.internal.snapshot.inspections.OldObjRefYoungObjQuery;
import org.eclipse.mat.query.Column;
import org.eclipse.mat.query.IResult;
import org.eclipse.mat.query.IResultTable;
import org.eclipse.mat.query.IResultTree;
import org.eclipse.mat.query.refined.RefinedResultBuilder;
import org.eclipse.mat.query.refined.RefinedTable;
import org.eclipse.mat.snapshot.ISnapshot;

import java.lang.ref.SoftReference;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SnapshotContext {

    private ISnapshot snapshot;

    private SoftReference<DirectByteBuffer> directByteBuffer;
    private SoftReference<ClassLoaderExplorer> classLoaderExplorer;
    private SoftReference<LeakReport> leakReport;

    public SnapshotContext(ISnapshot snapshot) {
        this.snapshot = snapshot;
    }

    private synchronized <T> T fetch(SoftReference<T> cache, Builder<T> builder) throws Exception {
        T res;
        if (cache == null || (res = cache.get()) == null) {
            res = builder.build();
        }
        return res;
    }

    public synchronized ClassLoaderExplorer classLoaderExplorer() throws Exception {
        return fetch(classLoaderExplorer, () -> {
            ClassLoaderExplorer result = new ClassLoaderExplorer();
            classLoaderExplorer = new SoftReference<>(result);
            return result;
        });
    }

    public synchronized DirectByteBuffer directByteBuffer() throws Exception {
        return fetch(directByteBuffer, () -> {
            DirectByteBuffer result = new DirectByteBuffer();
            directByteBuffer = new SoftReference<>(result);
            return result;
        });
    }

    public synchronized LeakReport leakReport() throws Exception {
        return fetch(leakReport, () -> {
            LeakReport result = new LeakReport();
            leakReport = new SoftReference<>(result);
            return result;
        });
    }

    public ISnapshot getSnapshot() {
        return snapshot;
    }

    interface Builder<T> {
        T build() throws Exception;
    }

    @Data
    public class LeakReport {
        IResult result;

        LeakReport() throws Exception {
            LeakHunterQuery query = new LeakHunterQuery();
            query.snapshot = snapshot;
            result = query.execute(HeapDumpSupport.VOID_LISTENER);
        }
    }

    @Data
    public class DirectByteBuffer {
        private static final String OQL =
            "SELECT s.@displayName as label, s.position as position, s.limit as limit, s.capacity as " +
            "capacity FROM java.nio.DirectByteBuffer s where s.cleaner != null";
        RefinedTable resultContext;
        private long position;
        private long limit;
        private long capacity;
        private int totalSize;

        DirectByteBuffer() throws Exception {
            OQLQuery query = new OQLQuery();
            query.snapshot = snapshot;
            query.queryString = OQL;
            IResult result = query.execute(HeapDumpSupport.VOID_LISTENER);
            if (result instanceof IResultTable) {
                RefinedResultBuilder builder =
                    new RefinedResultBuilder(new SnapshotQueryContext(snapshot), (IResultTable) result);
                builder.setSortOrder(3, Column.SortDirection.DESC);
                resultContext = (RefinedTable) builder.build();
                totalSize = resultContext.getRowCount();
                for (int i = 0; i < totalSize; i++) {
                    Object row = resultContext.getRow(i);
                    position += position(row);
                    limit += limit(row);
                    capacity += capacity(row);
                }
            }

        }

        public String label(Object row) {
            return (String) resultContext.getColumnValue(row, 0);
        }

        public int position(Object row) {
            return (Integer) resultContext.getColumnValue(row, 1);
        }

        public int limit(Object row) {
            return (Integer) resultContext.getColumnValue(row, 2);
        }

        public int capacity(Object row) {
            return (Integer) resultContext.getColumnValue(row, 3);
        }

    }

    @Data
    public class OldObjRefYoungObj {
        private OldObjRefYoungObjQuery.Tree result;
        private List<?> records;
        private int refCount;

        OldObjRefYoungObj() throws Exception {
            OldObjRefYoungObjQuery query = new OldObjRefYoungObjQuery();
            query.snapshot = snapshot;
            query.groupBy = OldObjRefYoungObjQuery.Grouping.BY_CLASS;
            result = query.execute(HeapDumpSupport.VOID_LISTENER);
            records = result.getElements();
            for (Object record : records) {
                refCount += (int) result.getColumnValue(record, 1);
            }
        }
    }

    @Data
    public class ClassLoaderExplorer {
        private IResult resultContext;

        // classloader object Id -> record
        private Map<Integer, Object> classLoaderIdMap;

        private List<?> records;

        private int definedClasses;

        private int numberOfInstances;

        ClassLoaderExplorer() throws Exception {
            ClassLoaderExplorerQuery query = new ClassLoaderExplorerQuery();
            query.snapshot = snapshot;
            IResultTree result = (IResultTree) query.execute(HeapDumpSupport.VOID_LISTENER);
            this.resultContext = result;
            Map<Integer, Object> classLoaderIdMap = new HashMap<>();
            for (Object r : result.getElements()) {
                classLoaderIdMap.put(result.getContext(r).getObjectId(), r);
            }
            this.records = result.getElements();
            this.records.sort((Comparator<Object>) (o1, o2) -> Integer
                .compare((int) result.getColumnValue(o2, 1), (int) result.getColumnValue(o1, 1)));
            this.classLoaderIdMap = classLoaderIdMap;

            for (Object record : records) {
                definedClasses += (int) result.getColumnValue(record, 1);
                numberOfInstances += (int) result.getColumnValue(record, 2);
            }
        }
    }
}
