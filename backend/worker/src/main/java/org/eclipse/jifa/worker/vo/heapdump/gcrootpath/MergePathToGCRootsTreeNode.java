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

import lombok.Data;

@Data
public class MergePathToGCRootsTreeNode {
    private int objectId;
    private String className;
    private int refObjects;
    private long shallowHeap;
    private long refShallowHeap;
    private long retainedHeap;
    private String suffix;
    private int objectType;
    private boolean gCRoot;
}
