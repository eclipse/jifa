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
package org.eclipse.jifa.gclog.diagnoser;

import java.util.HashMap;
import java.util.Map;

public class AbnormalType {
    public static String I18N_PREFIX = "jifa.gclog.diagnose.abnormal.";
    private static Map<String, AbnormalType> name2Type = new HashMap<>();

    // order these members by their general importance
    // Whenever a new type is added, add its default suggestions to DefaultSuggestionGenerator

    // Ultra
    public static AbnormalType OUT_OF_MEMORY = new AbnormalType("outOfMemory");
    public static AbnormalType ALLOCATION_STALL = new AbnormalType("allocationStall");
    public static AbnormalType METASPACE_FULL_GC = new AbnormalType("metaspaceFullGC");
    public static AbnormalType HEAP_MEMORY_FULL_GC = new AbnormalType("heapMemoryFullGC");

    //High
    public static AbnormalType FREQUENT_YOUNG_GC = new AbnormalType("frequentYoungGC");
    public static AbnormalType LONG_YOUNG_GC_PAUSE = new AbnormalType("longYoungGCPause");
    public static AbnormalType SYSTEM_GC = new AbnormalType("systemGC");
    public static AbnormalType LONG_G1_REMARK = new AbnormalType("longG1Remark");
    public static AbnormalType LONG_CMS_REMARK = new AbnormalType("longCMSRemark");

    public static AbnormalType LAST_TYPE = new AbnormalType("lastType");


    private String name;
    private int ordinal;

    private AbnormalType(String name) {
        this.name = name;
        ordinal = name2Type.size();
        name2Type.put(name, this);
    }

    public static AbnormalType getType(String name) {
        return name2Type.getOrDefault(name, null);
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    public int getOrdinal() {
        return ordinal;
    }
}
