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

import org.eclipse.mat.query.IResultTree;

import java.util.List;
import java.util.function.Function;

import static org.eclipse.jifa.common.util.Assertion.ASSERT;

// find elements in this exotic tree
// MAT's APIs really astonished me, I'm climbing the s*** mountains of unbelievable awful smell;
// 2020-12-11
public class ExoticTreeFinder {
    private final IResultTree tree;
    private BinFunction<IResultTree, Object, Integer> predicate;
    private Function<Object, List<?>> getChildrenCallback;

    public ExoticTreeFinder(IResultTree tree) {
        ASSERT.notNull(tree);
        this.tree = tree;
    }

    public ExoticTreeFinder setGetChildrenCallback(Function<Object, List<?>> getChildrenCallback) {
        this.getChildrenCallback = getChildrenCallback;
        return this;
    }

    public ExoticTreeFinder setPredicate(BinFunction<IResultTree, Object, Integer> predicate) {
        this.predicate = predicate;
        return this;
    }

    public List<?> findChildrenOf(int parentNodeId) {
        Object targetParentNode = null;
        try {
            targetParentNode = findTargetParentNodeImpl(tree.getElements(), parentNodeId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (targetParentNode != null) {
            return getChildrenCallback.apply(targetParentNode);
        }
        return null;
    }

    public Object findTargetParentNode(int parentNodeId) {
        try {
            return findTargetParentNodeImpl(tree.getElements(), parentNodeId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Object findTargetParentNodeImpl(List<?> nodes, int parentNodeId) throws Exception {
        if (nodes == null) {
            return null;
        }

        for (Object node : nodes) {
            Integer nodeId = predicate.apply(tree, node);
            if (nodeId != null && nodeId == parentNodeId) {
                return node;
            }
        }

        for (Object node : nodes) {
            List<?> children = getChildrenCallback.apply(node);
            if (children != null) {
                Object targetParentNode = findTargetParentNodeImpl(children, parentNodeId);
                if (targetParentNode != null) {
                    return targetParentNode;
                }
            }
        }

        return null;
    }

    public interface BinFunction<A, B, R> {
        R apply(A a, B b) throws Exception;
    }
}
