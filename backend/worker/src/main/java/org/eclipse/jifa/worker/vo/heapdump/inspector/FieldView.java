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

@Data
public class FieldView {

    /**
     * @see org.eclipse.mat.snapshot.model.IObject.Type
     */
    private int fieldType;

    private String name;

    private String value;

    private int objectId;

    public FieldView(int fieldType, String name, String value) {
        this.fieldType = fieldType;
        this.name = name;
        this.value = value;
    }

    public FieldView(int fieldType, String name, String value, int objectId) {
        this(fieldType, name, value);
        this.objectId = objectId;
    }

    public FieldView() {

    }
}
