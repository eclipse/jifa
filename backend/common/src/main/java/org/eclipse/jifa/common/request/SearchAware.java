package org.eclipse.jifa.common.request;

import lombok.Data;
import org.eclipse.jifa.common.vo.support.SearchType;

@Data
public class SearchAware {
    String searchText;
    SearchType searchType;

    public SearchAware(String searchText, SearchType searchType) {
        this.searchText = searchText;
        this.searchType = searchType;
    }
}
