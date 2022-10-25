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

package org.eclipse.jifa.hda.impl;

import org.eclipse.jifa.common.util.EscapeUtil;
import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.query.Bytes;
import org.eclipse.mat.query.IContextObjectSet;
import org.eclipse.mat.query.IStructuredResult;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.jifa.common.util.UseAccessor;
import org.eclipse.jifa.hda.api.AnalysisException;
import static org.eclipse.jifa.hda.api.Model.DominatorTree;

@UseAccessor
public class VirtualClassLoaderItem extends DominatorTree.ClassLoaderItem {
    static final int COLUMN_LABEL = 0;
    static final int COLUMN_OBJECTS = 1;
    static final int COLUMN_SHALLOW = 2;
    static final int COLUMN_RETAINED = 3;
    static final int COLUMN_PERCENT = 4;

    transient final ISnapshot snapshot;
    transient final IStructuredResult results;
    transient final Object e;

    public VirtualClassLoaderItem(final ISnapshot snapshot, final IStructuredResult results, final Object e) {
        this.snapshot = snapshot;
        this.results = results;
        this.e = e;
        this.objectId = results.getContext(e).getObjectId();
    }

    @Override
    public String getSuffix() {
        return null;
    }

    @Override
    public int getObjectId() {
        return objectId;
    }

    @Override
    public int getObjectType() {
        try {
            return HeapDumpAnalyzerImpl.typeOf(snapshot.getObject(objectId));
        } catch (SnapshotException se) {
            throw new AnalysisException(se);
        }
    }

    @Override
    public boolean isGCRoot() {
        return snapshot.isGCRoot(objectId);
    }

    @Override
    public String getLabel() {
        return EscapeUtil.unescapeLabel((String) results.getColumnValue(e, COLUMN_LABEL));
    }

    @Override
    public long getObjects() {
        Object value = results.getColumnValue(e, COLUMN_OBJECTS);
        if (value != null) {
            return (Integer) value;
        } else {
            return 0;
        }
    }

    @Override
    public int[] getObjectIds() {
        return ((IContextObjectSet) results.getContext(e)).getObjectIds();
    }

    @Override
    public long getShallowSize() {
        return ((Bytes) results.getColumnValue(e, COLUMN_SHALLOW)).getValue();
    }

    @Override
    public long getRetainedSize() {
        return ((Bytes) results.getColumnValue(e, COLUMN_RETAINED)).getValue();
    }

    @Override
    public double getPercent() {
        return (Double) results.getColumnValue(e, COLUMN_PERCENT);
    }
}