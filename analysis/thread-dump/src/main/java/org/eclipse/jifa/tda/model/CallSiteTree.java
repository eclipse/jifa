/********************************************************************************
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
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

package org.eclipse.jifa.tda.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Data
public class CallSiteTree {

    private final Node root;

    private List<List<Node>> allChildren;

    private int count;

    private Node[] id2Node;

    public CallSiteTree() {
        root = new Node();
        allChildren = new ArrayList<>();
    }

    public synchronized void add(Trace trace) {
        Frame[] frames = trace.getFrames();
        root.weight++;
        Node parent = root;
        for (Frame frame : frames) {
            parent = addChildren(parent, frame);
        }
    }

    public void freeze() {
        id2Node = new Node[count + 1];
        id2Node[0] = root;
        int index = 1;
        for (List<Node> children : allChildren) {
            children.sort((o1, o2) -> o2.weight - o1.weight);

            for (Node n : children) {
                n.setId(index);
                id2Node[index++] = n;
            }
        }

        assert index == count + 1;

        allChildren = null;
    }

    private Node addChildren(Node parent, Frame frame) {
        List<Node> children = parent.children;
        if (children == null) {
            Node node = new Node(frame);
            count++;
            children = new ArrayList<>();
            children.add(node);
            parent.children = children;
            allChildren.add(children);
            return node;
        }

        int low = 0;
        int high = children.size() - 1;
        while (low <= high) {
            int mid = low + (high - low) / 2;
            Node node = children.get(mid);
            if (node.frame.equals(frame)) {
                node.weight++;
                return node;
            } else if (node.frame.hashCode() < frame.hashCode()) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        Node node = new Node(frame);
        count++;
        children.add(low, node);
        return node;
    }

    @Override
    public String toString() {
        return "CallSiteTree{" +
               "root=" + root +
               ", allChildren=" + allChildren +
               '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        CallSiteTree tree = (CallSiteTree) o;
        return count == tree.count && Objects.equals(root, tree.root) &&
               Objects.equals(allChildren, tree.allChildren) && Arrays.equals(id2Node, tree.id2Node);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(root, allChildren, count);
        result = 31 * result + Arrays.hashCode(id2Node);
        return result;
    }

    @Data
    public static class Node extends Identity {

        Frame frame;

        int weight;

        List<Node> children;

        public Node(Frame frame) {
            this.frame = frame;
            weight = 1;
        }

        public Node() {
            frame = null;
            weight = 0;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Node node = (Node) o;
            return weight == node.weight && Objects.equals(frame, node.frame) &&
                   Objects.equals(children, node.children);
        }

        @Override
        public int hashCode() {
            return Objects.hash(frame, weight, children);
        }
    }
}
