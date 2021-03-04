/********************************************************************************
 * Copyright (c) 2020, 2021 Contributors to the Eclipse Foundation
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

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.query.IContextObject;
import org.eclipse.mat.query.IResultTree;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.model.GCRootInfo;
import org.eclipse.mat.snapshot.model.IObject;
import org.eclipse.mat.snapshot.model.NamedReference;
import org.eclipse.mat.snapshot.query.IHeapObjectArgument;
import org.eclipse.mat.util.IProgressListener;
import org.eclipse.mat.util.VoidProgressListener;

import java.util.Iterator;
import java.util.List;

import static org.eclipse.jifa.common.Constant.EMPTY_STRING;
import static org.eclipse.jifa.common.util.Assertion.ASSERT;

public class Helper {
    public static final int ILLEGAL_OBJECT_ID = -1;

    public static IProgressListener VOID_LISTENER = new VoidProgressListener();

    public static int fetchObjectId(IContextObject context) {
        return context == null ? ILLEGAL_OBJECT_ID : context.getObjectId();
    }

    public static String suffix(ISnapshot snapshot, int objectId) throws SnapshotException {
        GCRootInfo[] gc = snapshot.getGCRootInfo(objectId);
        return gc != null ? GCRootInfo.getTypeSetAsString(gc) : EMPTY_STRING;
    }

    public static String suffix(GCRootInfo[] gcRootInfo) {
        return gcRootInfo != null ? GCRootInfo.getTypeSetAsString(gcRootInfo) : EMPTY_STRING;
    }

    public static String prefix(ISnapshot snapshot, int objectId, int outbound) throws SnapshotException {
        IObject object = snapshot.getObject(objectId);

        long address = snapshot.mapIdToAddress(outbound);

        StringBuilder s = new StringBuilder(64);

        List<NamedReference> refs = object.getOutboundReferences();
        for (NamedReference reference : refs) {
            if (reference.getObjectAddress() == address) {
                if (s.length() > 0) {
                    s.append(", ");
                }
                s.append(reference.getName());
            }
        }
        return s.toString();
    }

    public static IHeapObjectArgument buildHeapObjectArgument(int[] ids) {
        return new IHeapObjectArgument() {
            @Override
            public int[] getIds(IProgressListener iProgressListener) {
                return ids;
            }

            @Override
            public String getLabel() {
                return "";
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
                        return ids;
                    }
                };
            }
        };
    }

    private static Object findObjectInTree(IResultTree tree, List<?> levelElements, int targetId) {
        if (levelElements != null) {
            for (Object o : levelElements) {
                if (tree.getContext(o).getObjectId() == targetId) {
                    return o;
                }
            }
        }
        return null;
    }

    public static Object fetchObjectInResultTree(IResultTree tree, int[] idPathInResultTree) {
        if (idPathInResultTree == null || idPathInResultTree.length == 0) {
            return null;
        }

        // find the object in root tree
        Object objectInTree = findObjectInTree(tree, tree.getElements(), idPathInResultTree[0]);

        // find the object in children tree
        for (int i = 1; i < idPathInResultTree.length; i++) {
            if (objectInTree == null) {
                return null;
            }
            objectInTree = findObjectInTree(tree, tree.getChildren(objectInTree), idPathInResultTree[i]);
        }

        return objectInTree;
    }
}
