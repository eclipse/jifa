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
package org.eclipse.jifa.gclog.fragment;

import com.google.common.collect.ImmutableSet;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Context {
    private List<String> context;
    private final ContextComparator contextComparator = new ContextComparator();

    /* to match the following timestamp pattern:
   [2023-08-25T14:28:44.980+0800][0.076s] GC(374) Pause Mark Start 4.459ms
   2022-11-28T14:57:05.341+0800: 6.340: [CMS-concurrent-mark-start]
   [7.006s] GC(374) Pause Mark Start 4.459ms
   675.461: [CMS-concurrent-mark-start]
    */
    static Pattern[] timestampPatternToChoose = {
            Pattern.compile("\\[[\\d-T:+.]+]\\[(\\d+\\.\\d+)s][\\s\\S]*"),
            Pattern.compile("[\\d-T:+.]+ (\\d+\\.\\d+): \\[[\\s\\S]*"),
            Pattern.compile("\\[(\\d+\\.\\d+)s][\\s\\S]*"),
            Pattern.compile("(\\d+\\.\\d+): \\[[\\s\\S]*")
    };
    private Pattern timestampPattern;

    public Context(List<String> context) throws RuntimeException{
        this.context = context;
        selectTimestampPattern();
        sort();
    }

    public BufferedReader toBufferedReader() {
        String joinedString = String.join("\n", context);
        InputStream inputStream = new ByteArrayInputStream(joinedString.getBytes());
        return new BufferedReader(new InputStreamReader(inputStream));
    }

    private void selectTimestampPattern() throws RuntimeException{
        for (String str : context) {
            for (Pattern pattern : timestampPatternToChoose) {
                if (pattern.matcher(str).matches()) {
                    timestampPattern = pattern;
                    return;
                }
            }
        }
        throw new RuntimeException("fail to parse timestamp");
    }

    class ContextComparator implements Comparator<String> {

        static Set<String> precedentPatternSet = new ImmutableSet.Builder<String>()
                .add("Pause Young", "Pause Initial Mark", "CMS Initial Mark", "Concurrent Cycle", "Concurrent Mark").build();

        boolean isPrecedent(String str) {
            return precedentPatternSet.stream().anyMatch(str::contains);
        }
        /*
        return value:
        1:  place string2 in prior to string1
        -1: place string1 in prior to string2
         */
        @Override
        public int compare(String o1, String o2) {
            Matcher matcher;
            String timestampString1 = null, timestampString2 = null;
            if ((matcher = timestampPattern.matcher(o1)).matches()) {
                timestampString1 = matcher.group(1);
            }
            if ((matcher = timestampPattern.matcher(o2)).matches()) {
                timestampString2 = matcher.group(1);
            }
            if (timestampString1 == null && timestampString2 == null) {
                return 0;
            } else if (timestampString1 == null) {
                // string2 in prior to string1:
                // string1 doesn't match, thus place it at the tail of the list.
                return 1;
            } else if (timestampString2 == null) {
                // place string1 in prior to string2:
                // string2 doesn't match, thus place string1 before it.
                return -1;
            }

            if (timestampString1.equals(timestampString2)) {
                boolean string1IsPrecedent = isPrecedent(o1), string2IsPrecedent = isPrecedent(o2);
                if (string1IsPrecedent == string2IsPrecedent) {
                    // both string1 and string2 are precedent or
                    // neither one is precedent
                    return 0;
                } else {
                    if (string1IsPrecedent) {
                        // place string1 in prior to string2:
                        return -1;
                    } else {
                        // place string2 in prior to string1:
                        return 1;
                    }
                }
            } else {
                return Double.parseDouble(timestampString1) > Double.parseDouble(timestampString2) ? 1 : -1;
            }
        }
    }

    private void sort() {
        context.sort(contextComparator);
    }

    // for debug
    private void checkStartWithTimeStamp() {
        for (String str : context) {
            if (!timestampPattern.matcher(str).matches()) {
                throw new RuntimeException("found invalid string which doesn't start with a required timestamp: " + str);
            }
        }
    }

    // for debug
    private void filterInvalidFragment() {
        context.removeIf(str -> !timestampPattern.matcher(str).matches());
    }
}
