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

import org.eclipse.jifa.common.vo.PageView;
import lombok.Data;

import java.util.List;

@Data
public class TableResult implements OQLResult {
    private int type = TABLE;

    private List<String> columns;

    private PageView<Entry> pv;

    public TableResult(List<String> columns, PageView<Entry> pv) {
        this.columns = columns;
        this.pv = pv;
    }

    @Data
    public static class Entry {

        private int objectId;

        private List<Object> values;

        public Entry(int objectId, List<Object> values) {
            this.objectId = objectId;
            this.values = values;
        }
    }
}
