/********************************************************************************
 * Copyright (c) 2022, 2023 Contributors to the Eclipse Foundation
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

package org.eclipse.jifa.gclog;

import org.junit.jupiter.api.Assertions;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class TestUtil {
    public static BufferedReader stringToBufferedReader(String source) {
        InputStream inputStream = new ByteArrayInputStream(source.getBytes());
        return new BufferedReader(new InputStreamReader(inputStream));
    }

    public static BufferedReader getGCLog(String name) {
        InputStream is = Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream(name));
        return new BufferedReader(new InputStreamReader(is));
    }

    public static List<String> generateShuffledGCLog(String name) {
        StringBuffer gclog = new StringBuffer("\n");
        try {
            BufferedReader bufferedReader = getGCLog(name);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                gclog.append(line);
                gclog.append('\n');
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        String regexForSplit = null;
        switch (name) {
            case "11CMSUpTime.log" -> regexForSplit = "(?=\n\\[[\\d-T:+.]+]\\[[0-9]+\\.[0-9]+s])";
            case "11G1Parser.log", "11CMSGCParser.log", "IncompleteGCLog.log" -> regexForSplit = "(?=\n\\[[0-9]+\\.[0-9]+s])";
            case "8CMSParser.log", "8CMSPrintGC.log", "8G1PrintGC.log", "8ParallelGCParser.log" -> regexForSplit = "(?=\n[0-9]+\\.[0-9]+: \\[)";
            case "8G1GCParser.log", "8G1GCParserAdaptiveSize.log", "8ConcurrentPrintDateTimeStamp.log", "8CMSCPUTime.log", "8CMSPromotionFailed.log", "8CMSScavengeBeforeRemark.log", "8GenerationalGCInterleave.log" -> regexForSplit = "(?=\n[\\d-T:+.]+ \\d+\\.\\d+: \\[)";
            default -> Assertions.fail("can't find timestamp pattern for gc log " + name);
        }

        String originalLog = gclog.toString();
        List<String> shuffledLog = new ArrayList<>(Arrays.asList(originalLog.split(regexForSplit)));
        shuffledLog = shuffledLog.stream().map(str -> str.substring(1)).collect(Collectors.toList());
        Collections.shuffle(shuffledLog);
        return shuffledLog;
    }
}
