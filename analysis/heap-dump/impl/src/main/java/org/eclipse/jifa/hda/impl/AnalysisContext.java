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

package org.eclipse.jifa.hda.impl;

import org.eclipse.jifa.hda.api.Model;
import org.eclipse.mat.query.IResult;
import org.eclipse.mat.query.IResultTree;
import org.eclipse.mat.query.refined.RefinedTable;
import org.eclipse.mat.snapshot.ISnapshot;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AnalysisContext {

    final ISnapshot snapshot;

    volatile SoftReference<ClassLoaderExplorerData> classLoaderExplorerData = new SoftReference<>(null);

    volatile SoftReference<DirectByteBufferData> directByteBufferData = new SoftReference<>(null);

    volatile SoftReference<LeakReportData> leakReportData= new SoftReference<>(null);

    AnalysisContext(ISnapshot snapshot) {
        this.snapshot = snapshot;
    }

    static class ClassLoaderExplorerData {

        IResultTree result;

        // classloader object Id -> record
        Map<Integer, Object> classLoaderIdMap;

        List<?> items;

        int definedClasses;

        int numberOfInstances;
    }

    static class DirectByteBufferData {
        static final String JDK_MANAGED_BUFFER_OQL =
                "SELECT s.@displayName as label, s.position as position, s.limit as limit, s.capacity as " +
                        "capacity FROM java.nio.DirectByteBuffer s where s.cleaner != null";
        static final String JNI_ALLOCATED_BUFFER_OQL =
                "SELECT s.@displayName as label, s.position as position, s.limit as limit, s.capacity as " +
                        "capacity FROM java.nio.DirectByteBuffer s where s.cleaner = null and s.att = null";
        static final String ALL_BUFFER_OQL =
                "SELECT s.@displayName as label, s.position as position, s.limit as limit, s.capacity as " +
                        "capacity FROM java.nio.DirectByteBuffer s";

        static final Map<String, Object> JDK_MANAGED_BUFFER_ARGS = new HashMap<>(1);
        static final Map<String, Object> JNI_ALLOC_BUFFER_ARGS = new HashMap<>(1);
        static final Map<String, Object> ALL_BUFFER_ARGS = new HashMap<>(1);

        static {
            JDK_MANAGED_BUFFER_ARGS.put("queryString", JDK_MANAGED_BUFFER_OQL);
            JNI_ALLOC_BUFFER_ARGS.put("queryString", JNI_ALLOCATED_BUFFER_OQL);
            ALL_BUFFER_ARGS.put("queryString", ALL_BUFFER_OQL);
        }

        RefinedTable resultContext;

        Model.DirectByteBuffer.Summary summary;

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

    static class LeakReportData {
        IResult result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AnalysisContext that = (AnalysisContext) o;
        return Objects.equals(snapshot, that.snapshot);
    }

    @Override
    public int hashCode() {
        return Objects.hash(snapshot);
    }
}
