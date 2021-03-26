package org.eclipse.jifa.common.request;

import lombok.Data;
import org.eclipse.jifa.common.vo.support.SearchType;

@Data
public class AnalysisParmPack {
    PagingRequest paging;
    SortAware sort;
    SearchAware search;
    String foo;
}
