/********************************************************************************
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.worker.route.threaddump;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.jifa.tda.ThreadDumpAnalyzer;
import org.eclipse.jifa.tda.enums.JavaThreadState;
import org.eclipse.jifa.tda.enums.OSTreadState;
import org.eclipse.jifa.tda.util.SearchQuery;
import org.eclipse.jifa.tda.vo.VSearchResult;
import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;
import org.eclipse.jifa.worker.support.Analyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Promise;

public class ThreadDumpSearchRoute extends ThreadDumpBaseRoute {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadDumpSearchRoute.class);
    
    @RouteMeta(path = "/searchThreads")
    public void search(Promise<List<VSearchResult>> promise, @ParamKey("file") List<String> files, @ParamKey("term") List<String> terms, 
                        @ParamKey(value = "regex", mandatory = false, defaultValue = "false" ) boolean regex,
                        @ParamKey(value = "searchStack", mandatory = false, defaultValue = "true") boolean searchStack,
                        @ParamKey(value = "searchName", mandatory = false, defaultValue = "true") boolean searchName,
                        @ParamKey(value = "searchState", mandatory = false, defaultValue = "true") boolean searchState,
                        @ParamKey(value = "matchCase", mandatory = false, defaultValue = "false") boolean matchCase,
                        @ParamKey(value = "allowedOSStates", mandatory = false) List<String> allowedOSStates,
                        @ParamKey(value = "allowedJavaStates", mandatory = false) List<String> allowedJavaStates) {
        SearchQuery query = SearchQuery.forTerms(terms);
        query.withRegex(regex);
        query.withSearchName(searchName);
        query.withSearchStack(searchStack);
        query.withSearchState(searchState);
        query.withMatchCase(matchCase);
        if(allowedJavaStates!=null && allowedJavaStates.size()>0) {
            query.withAllowedJavaStates(allowedJavaStates.stream().map(JavaThreadState::valueOf).collect(Collectors.toSet()));
        }
        if(allowedOSStates!=null && allowedOSStates.size()>0) {
            query.withAllowedOSStates(allowedOSStates.stream().map(OSTreadState::valueOf).collect(Collectors.toSet()));
        }
        LOGGER.debug("Searching with query {}", query);
        Predicate<org.eclipse.jifa.tda.model.Thread> search = query.build();
        List<VSearchResult> results = new ArrayList<>();
        for (String file : files) {
            ThreadDumpAnalyzer analyzer = Analyzer.threadDumpAnalyzerOf(file);
            results.addAll(analyzer.search(search));
        }
        promise.complete(results);
    }
    
}
