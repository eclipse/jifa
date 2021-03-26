package org.eclipse.jifa.common.request;

import lombok.Data;

@Data
public class SortAware {
    String sortBy;
    boolean ascendingOrder;

    public SortAware(String sortBy, boolean ascendingOrder) {
        this.sortBy = sortBy;
        this.ascendingOrder = ascendingOrder;
    }
}
