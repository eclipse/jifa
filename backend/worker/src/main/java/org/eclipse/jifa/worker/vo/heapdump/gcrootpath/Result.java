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
package org.eclipse.jifa.worker.vo.heapdump.gcrootpath;

import org.eclipse.jifa.worker.vo.heapdump.HeapObject;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
public class Result {

    private Node tree;
    private int count;
    private boolean hasMore;

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Node extends HeapObject {

        private boolean origin;

        private List<Node> children = new ArrayList<>();

        public void addChild(Node child) {
            children.add(child);
        }

        public Node getChild(int objectId) {
            for (Node child : children) {
                if (child.getObjectId() == objectId) {
                    return child;
                }
            }
            return null;
        }
    }
}
