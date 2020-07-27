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
package org.eclipse.jifa.worker.vo.heapdump;

import lombok.Data;
import org.eclipse.mat.snapshot.model.IClass;
import org.eclipse.mat.snapshot.model.IClassLoader;
import org.eclipse.mat.snapshot.model.IObject;

@Data
public class HeapObject {
    private int objectId;
    private String prefix;
    private String label;
    private String suffix;
    private long shallowSize;
    private long retainedSize;
    private boolean hasInbound;
    private boolean hasOutbound;
    private int objectType;
    private boolean gCRoot;

    public static class Type {

        static final int CLASS = 1;
        static final int CLASS_LOADER = 2;
        static final int ARRAY = 3;
        static final int NORMAL = 4;

        public static int typeOf(IObject object) {
            if (object instanceof IClass) {
                return CLASS;
            }

            if (object instanceof IClassLoader) {
                return CLASS_LOADER;
            }

            if (object.getClazz().isArrayType()) {
                return ARRAY;
            }
            return NORMAL;
        }
    }
}


