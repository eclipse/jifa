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

package org.eclipse.jifa.gclog.vo;

import java.util.ArrayList;
import java.util.List;

public class GCCollectionResult {
    private List<GCCollectionResultItem> items;
    private GCCollectionResultItem summary;

    public GCCollectionResult(GCCollectionResultItem summary) {
        this.summary = summary;
    }

    public GCCollectionResult() {
    }

    public void addItem(GCCollectionResultItem item) {
        if (item == null) {
            return;
        }
        if (items == null) {
            items = new ArrayList<>();
        }
        items.add(item);
    }

    public void setSummary(GCCollectionResultItem summary) {
        this.summary = summary;
    }

    public GCCollectionResultItem getFirstItemOfGeneRation(HeapGeneration generation){
        if (items == null){
            return null;
        }
        return items.stream().filter(i -> i. getGeneration() == generation).findFirst().orElse(null);
    }

    public List<GCCollectionResultItem> getItems() {
        return items;
    }

    public GCCollectionResultItem getSummary() {
        return summary;
    }

    @Override
    public String toString() {
        return "GCCollectionResult{" +
                "items=" + items +
                ", summary=" + summary +
                '}';
    }
}
