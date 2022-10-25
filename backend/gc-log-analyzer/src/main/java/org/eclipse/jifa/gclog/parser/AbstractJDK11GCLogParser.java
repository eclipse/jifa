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

package org.eclipse.jifa.gclog.parser;

import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jifa.gclog.model.GCModel;
import org.eclipse.jifa.gclog.util.Constant;
import org.eclipse.jifa.gclog.util.GCLogUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.eclipse.jifa.gclog.parser.ParseRule.ParseRuleContext.UPTIME;

/*
 * Currently, we only consider -Xlog:gc*=info. We will continue support for cases in the future.
 */
public abstract class AbstractJDK11GCLogParser extends AbstractGCLogParser {
    private static List<ParseRule> withoutGCIDRules;
    private static List<ParseRule> withGCIDRules;

    public static List<ParseRule> getSharedWithoutGCIDRules() {
        return withoutGCIDRules;
    }

    public static List<ParseRule> getSharedWithGCIDRules() {
        return withGCIDRules;
    }

    static {
        initializeParseRules();
    }

    private static void initializeParseRules() {
        withoutGCIDRules = new ArrayList<>();
        withoutGCIDRules.add(new ParseRule.PrefixAndValueParseRule("Total time for which application",
                (parser, context, prefix, s) -> parser.parseSafepointStop(context.get(UPTIME), s)));

        withGCIDRules = new ArrayList<>();
        // subclass will add more rules
    }

    @Override
    protected final void doParseLine(String line) {
        JDK11LogLine logLine = parseJDK11LogLine(line);
        if (logLine == null || !logLine.isValid()) {
            return;
        }
        doBeforeParsingLine(logLine);
        if (logLine.getGcid() == Constant.UNKNOWN_INT) {
            doParseLineWithoutGCID(logLine.getDetail(), logLine.getUptime());
        } else {
            // in jdk11 gcid is always logged
            doParseLineWithGCID(logLine.getDetail(), logLine.getGcid(), logLine.getUptime());
        }
    }

    protected abstract void doParseLineWithGCID(String detail, int gcid, double uptime);

    protected abstract void doParseLineWithoutGCID(String detail, double uptime);

    private void doBeforeParsingLine(JDK11LogLine logLine) {
        double uptime = logLine.getUptime();
        GCModel model = getModel();
        // set model reference timestamp
        if (model.getReferenceTimestamp() == Constant.UNKNOWN_LONG && logLine.getTimestamp() != Constant.UNKNOWN_LONG) {
            double startTimestamp = uptime == Constant.UNKNOWN_DOUBLE ? logLine.getTimestamp()
                    : logLine.getTimestamp() - uptime;
            model.setReferenceTimestamp(startTimestamp);
        }
        // set event start time
        if (logLine.getUptime() == Constant.UNKNOWN_DOUBLE) {
            logLine.setUptime(logLine.getTimestamp() - model.getReferenceTimestamp());
        }
        // set model start and end time
        if (model.getStartTime() == Constant.UNKNOWN_DOUBLE) {
            model.setStartTime(uptime);
        }
        model.setEndTime(Math.max(uptime, model.getEndTime()));
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class JDK11LogLine {
        private String timestampString = null;
        private long timestamp = Constant.UNKNOWN_LONG;
        private double uptime = Constant.UNKNOWN_DOUBLE;
        private String loglevel;
        private List<String> tags;
        private int gcid = Constant.UNKNOWN_INT;
        private String detail;

        // parsing timestamp is expensive, do it lazily
        public long getTimestamp() {
            if (timestamp == Constant.UNKNOWN_LONG && timestampString != null) {
                timestamp = GCLogUtil.parseDateStamp(timestampString);
            }
            return timestamp;
        }

        public boolean isValid() {
            if (timestamp == Constant.UNKNOWN_LONG && timestampString == null
                    && getUptime() == Constant.UNKNOWN_DOUBLE) { // need at least one
                return false;
            }
            if (getLoglevel() != null && !"info".equals(getLoglevel())) { // parse info level only now
                return false;
            }
            if (getTags() != null && !getTags().contains("gc")) { // need gc tag
                return false;
            }
            return true;
        }
    }

    /**
     * [2021-05-06T11:25:16.508+0800][2021-05-06T03:25:16.508+0000][0.202s][1620271516508ms][202ms][1489353078113131ns][201765461ns][B-K0S4ML7L-0237.local][45473][14083][info][gc,start     ] GC(0) Pause Young (Normal) (G1 Evacuation Pause)
     * A line in log may have multiple optimal decorations and some of them are redundant.
     * timestamp: [2021-05-06T11:25:16.508+0800][2021-05-06T03:25:16.508+0000][1620271516508ms][1489353078113131ns][201765461ns]
     * uptime: [0.202s][202ms]
     * logLevel: info
     * tags: gc,start
     * detail: GC(0) Pause Young (Normal) (G1 Evacuation Pause)
     * Either timestamp or uptime is necessary. Detail is necessary.
     * Other decorations are useless to us.
     * see https://docs.oracle.com/javase/9/tools/java.htm#JSWOR-GUID-9569449C-525F-4474-972C-4C1F63D5C357    Decorations chapter
     */
    private JDK11LogLine parseJDK11LogLine(String line) {
        if (StringUtils.isBlank(line)) {
            return null;
        }
        JDK11LogLine logLine = new JDK11LogLine();
        int leftBracketIndex = line.indexOf('[');
        int rightBracketIndex = -1;
        while (leftBracketIndex != -1) {
            rightBracketIndex = line.indexOf(']', leftBracketIndex + 1);
            String decoration = line.substring(leftBracketIndex + 1, rightBracketIndex).trim();
            parseDecoration(logLine, decoration);
            leftBracketIndex = line.indexOf('[', rightBracketIndex + 1);
        }
        String remains = line.substring(rightBracketIndex + 1).trim();
        if (remains.startsWith("GC(")) {
            int right = remains.indexOf(')');
            logLine.setGcid(Integer.parseInt(remains.substring(3, right)));
            logLine.setDetail(remains.substring(right + 2));
        } else {
            logLine.setDetail(remains);
        }
        return logLine;
    }

    // parse and fill a decoration
    // return false if there is no need to continue parsing other info
    private static final double TEN_YEAR_MILLISECOND = 10 * 365.25 * 24 * 60 * 60 * 1000;

    private void parseDecoration(JDK11LogLine logLine, String decoration) {
        if (GCLogUtil.isDatestamp(decoration)) {
            logLine.setTimestampString(decoration);
        } else if (Character.isDigit(decoration.charAt(0)) && decoration.endsWith("s")) {
            double period = GCLogUtil.toMillisecond(decoration);
            // this may be either a timestamp or an uptime. we have no way to know which.
            // just assume period longer than 10 years as timestamp
            if (period > TEN_YEAR_MILLISECOND) {
                logLine.setTimestamp((long) period);
            } else {
                logLine.setUptime(period);
            }
        } else if (isLoglevel(decoration)) {
            logLine.setLoglevel(decoration);
        } else if (decoration.contains("gc")) {
            logLine.setTags(Arrays.asList(decoration.trim().split(",")));
        }
    }

    private final static Set<String> LOG_LEVEL_SET = Sets.newHashSet("error", "warning", "info", "debug", "trace");

    private boolean isLoglevel(String decoration) {
        return LOG_LEVEL_SET.contains(decoration);
    }
}
