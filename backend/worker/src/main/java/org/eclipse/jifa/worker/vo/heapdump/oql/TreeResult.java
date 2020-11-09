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
package org.eclipse.jifa.worker.vo.heapdump.oql;

import lombok.Data;
import org.eclipse.jifa.common.vo.PageView;
import org.eclipse.jifa.worker.vo.heapdump.HeapObject;

@Data
public class TreeResult implements OQLResult {

    PageView<HeapObject> pv;

    private int type = TREE;

    public TreeResult(PageView<HeapObject> pv) {
        this.pv = pv;
    }
}
