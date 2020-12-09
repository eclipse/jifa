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
package org.eclipse.jifa.worker.vo.heapdump.inspector;

import lombok.Data;
import org.eclipse.mat.snapshot.model.HeapLayout;

@Data
public class ObjectView {

    private static final int YOUNG = 1;
    private static final int OLD = 2;
    private static final int OTHER = 0;
    private long objectAddress;
    /**
     * for non-class object, name = the name of object's class
     * for class object, name = class name
     */
    private String name;
    private boolean gCRoot;
    private int objectType;
    private String classLabel;
    private boolean classGCRoot;
    private String superClassName;
    private String classLoaderLabel;
    private boolean classLoaderGCRoot;
    private long shallowSize;
    private long retainedSize;
    private String gcRootInfo;
    private int locationType;

    public static int locationTypeOf(HeapLayout.Generation gen) {
        if (gen == HeapLayout.Generation.YOUNG) {
            return YOUNG;
        } else if (gen == HeapLayout.Generation.OLD) {
            return OLD;
        }
        return OTHER;
    }
}
