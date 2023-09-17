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
package org.eclipse.jifa.tda.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.jifa.tda.enums.JavaThreadState;
import org.eclipse.jifa.tda.enums.OSTreadState;
import org.eclipse.jifa.tda.enums.SourceType;
import org.eclipse.jifa.tda.model.Frame;
import org.eclipse.jifa.tda.model.JavaThread;
import org.eclipse.jifa.tda.model.Thread;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class SearchQuery {
    
    private List<String> terms = new ArrayList<>();

    private boolean regex = false;

    private boolean searchStack = true;

    private boolean searchName = true;

    private boolean searchState = true;

    private boolean matchCase = false;

    private Set<JavaThreadState> allowedJavaStates = EnumSet.allOf(JavaThreadState.class);

    private Set<OSTreadState> allowedOSStates = EnumSet.allOf(OSTreadState.class);

    public Predicate<Thread> build() {
        Predicate<String> stringMatcher = regex ? compileRegexMatcher() : compileStringMatcher();
        
        Predicate<Thread> p = t -> false;
        if(searchName) {
            p = p.or(t -> stringMatcher.test(t.getName()));
        }
        if(searchState) {
            p = p.or(t -> {
                if(stringMatcher.test(t.getOsThreadState().toString())) {
                    return true;
                }
                if(t instanceof JavaThread) {
                    JavaThreadState state = ((JavaThread)t).getJavaThreadState();
                    return stringMatcher.test(state.toString());
                }
                return false;
            });
        }
        if(searchStack) {
            p = p.or(t -> {
                if(t instanceof JavaThread) {
                    JavaThread jt = (JavaThread)t;
                    
                    if(jt.getTrace() != null) {
                        Frame[] frames = jt.getTrace().getFrames();
                        for (Frame frame : frames) {
                            if(stringMatcher.test(frameToString(frame))) {
                                return true;
                            }
                        }
                    }
                }
                return false;
            });
        }
        Predicate<Thread> filter = t -> {
            if(!allowedOSStates.contains(t.getOsThreadState())) {
                return false;
            }
            if(t instanceof JavaThread) {
                JavaThreadState state = ((JavaThread)t).getJavaThreadState();
                if(!allowedJavaStates.contains(state)) {
                    return false;
                }
            }
            return true;
        };
        Predicate<Thread> overall = filter.and(p);
        return overall;
    }
    
    private String frameToString(Frame frame) {
        //at java.lang.ref.Reference.processPendingReferences(java.base@11.0.1/Reference.java:241)
        StringBuilder result = new StringBuilder("at ");
        result.append(frame.getClazz());
        result.append(".");
        result.append(frame.getMethod());
        result.append("(");
        if(frame.getModule()!=null) {

            result.append(frame.getModule());
            result.append("/");
        }
        SourceType type = frame.getSourceType() == null ? SourceType.UNKNOWN_SOURCE : frame.getSourceType();
        switch(type) {
            case REDEFINED: 
                result.append("Redefined");
                break;
            case SOURCE_FILE_WITH_LINE_NUMBER:
                result.append(frame.getSource()).append(":").append(frame.getLine());
                break;
            case SOURCE_FILE:
                result.append(frame.getSource());
                break;
            case NATIVE_METHOD:
                result.append("Native Method");
                break;
            case UNKNOWN_SOURCE:
                result.append("Unknown Source");
                break;
        }
        result.append(")");
        return result.toString();
    }

    private Predicate<String> compileStringMatcher() {
        if(!matchCase) {
            terms = terms.stream().map(t -> t.toLowerCase(Locale.ROOT)).collect(Collectors.toList());
        }
        return s -> {
            if(s == null) {
                return false;
            }
            if(!matchCase) {
                s = s.toLowerCase(Locale.ROOT);
            }
            for (String term : terms) {
                if(s.contains(term)) {
                    return true;   
                }
            }
            return false;
        };
    }

    private Predicate<String> compileRegexMatcher() {
        List<Pattern> patterns = new ArrayList<>();
        for (String term : terms) {
            int flags = matchCase ? 0 : Pattern.CASE_INSENSITIVE;
            patterns.add(Pattern.compile(term, flags));
        }
        return s -> {
            if(s == null)
                return false;
            for (Pattern pattern : patterns) {
                if(pattern.matcher(s).find()) {
                    return true;   
                }
            }
            return false;
        };
    }

    public static SearchQuery forTerms(String... terms) {
        return forTerms(Arrays.asList(terms));
    }

    public static SearchQuery forTerms(List<String> terms) {
        SearchQuery query = new SearchQuery();
        query.terms = terms;
        return query;
    }

    public SearchQuery withRegex(boolean regex) {
        setRegex(regex);
        return this;
    }

    public SearchQuery withMatchCase(boolean matchCase) {
        setMatchCase(matchCase);
        return this;
    }

    public SearchQuery withSearchStack(boolean searchStack) {
        setSearchStack(searchStack);
        return this;
    }

    public SearchQuery withSearchName(boolean searchName) {
        setSearchName(searchName);
        return this;
    }

    public SearchQuery withSearchState(boolean searchState) {
        setSearchState(searchState);
        return this;
    }

    public SearchQuery withAllowedJavaStates(Set<JavaThreadState> states) {
        setAllowedJavaStates(states);
        return this;
    }

    public SearchQuery withAllowedOSStates(Set<OSTreadState> states) {
        setAllowedOSStates(states);
        return this;
    }

}
