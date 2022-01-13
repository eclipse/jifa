/********************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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

package org.eclipse.jifa.gclog.model;

import org.eclipse.jifa.gclog.util.GCLogUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static org.eclipse.jifa.gclog.model.GCModel.KB2MB;

public class VmOptions {
    private Map<String, Object> options = new HashMap<>();
    private String originalOptionString;

    // notice that integers are long type and options indicating size are in Byte
    @SuppressWarnings("unchecked")
    public <T> T getOptionValue(String key) {
        return (T) options.getOrDefault(key, null);
    }

    public boolean containOption(String key) {
        return options.containsKey(key);
    }

    public VmOptions(String vmOptionsString) {
        originalOptionString = vmOptionsString;
        if (vmOptionsString == null) {
            return;
        }
        for (String option : vmOptionsString.split(" +")) {
            addVmOption(option);
        }
    }

    private void addVmOption(String optionString) {
        if (optionString.startsWith("-XX:")) {
            parseSingleOption(optionString.substring(4));
        } else if (optionString.startsWith("-D")) {
            parseSingleOption(optionString.substring(2));
        } else if (optionString.startsWith("-X")) {
            parseSingleOptionWithX(optionString.substring(2));
        } else if (optionString.startsWith("-")) {
            parseSingleOption(optionString.substring(1));
        }
    }

    private void parseSingleOptionWithX(String content) {
        if (content == null) {
            return;
        }

        if (content.startsWith("mn") || content.startsWith("ms") || content.startsWith("mx") || content.startsWith("ss")) {
            // add 'X' for convention
            options.put("X" + content.substring(0, 2), GCLogUtil.toKB(content.substring(2)) * (long) KB2MB);
        } else {
            options.put(content, true);
        }
    }

    private void parseSingleOption(String content) {
        if (content == null || content.isEmpty()) {
            return;
        }
        if (content.charAt(0) == '+') {
            options.put(content.substring(1), true);
            return;
        }
        if (content.charAt(0) == '-') {
            options.put(content.substring(1), false);
            return;
        }
        int mid = content.indexOf('=');
        if (mid > -1) {
            options.put(content.substring(0, mid), decideTypeAndParse(content.substring(mid + 1)));
            return;
        }
        options.put(content, true);
    }

    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");
    private static final Pattern SIZE_PATTERN = Pattern.compile("\\d+[kmgt]]?[b]?");

    private Object decideTypeAndParse(String s) {
        s = s.toLowerCase();
        if (NUMBER_PATTERN.matcher(s).matches()) {
            return Long.parseLong(s);
        } else if (SIZE_PATTERN.matcher(s).matches()) {
            return GCLogUtil.toKB(s) * (long) KB2MB;
        } else {
            return s;
        }
    }

    public String getOriginalOptionString() {
        return originalOptionString;
    }

    @Override
    public String toString() {
        return options.toString();
    }
}
