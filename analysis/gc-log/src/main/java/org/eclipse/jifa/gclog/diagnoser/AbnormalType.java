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

import org.eclipse.jifa.gclog.util.I18nStringView;

import java.util.HashMap;
import java.util.Map;

public class AbnormalType {
    public static String I18N_PREFIX = "jifa.gclog.diagnose.abnormal.";
    private static Map<String, AbnormalType> name2Type = new HashMap<>();

    // Types below can be used in either gc detail diagnose and global diagnose.
    // Order these members by their general importance
    public static AbnormalType OUT_OF_MEMORY = new AbnormalType("outOfMemory");
    public static AbnormalType ALLOCATION_STALL = new AbnormalType("allocationStall");
    public static AbnormalType METASPACE_FULL_GC = new AbnormalType("metaspaceFullGC");
    public static AbnormalType HEAP_MEMORY_FULL_GC = new AbnormalType("heapMemoryFullGC");
    public static AbnormalType FREQUENT_YOUNG_GC = new AbnormalType("frequentYoungGC");
    public static AbnormalType LONG_YOUNG_GC_PAUSE = new AbnormalType("longYoungGCPause");
    public static AbnormalType SYSTEM_GC = new AbnormalType("systemGC");
    public static AbnormalType LONG_G1_REMARK = new AbnormalType("longG1Remark");
    public static AbnormalType LONG_CMS_REMARK = new AbnormalType("longCMSRemark");

    // Types below are used in gc details and simply denote which part of
    // info is not normal and the problem description text. They can be declared in any order.

    public static AbnormalType BAD_DURATION = new AbnormalType("badDuration");
    public static AbnormalType BAD_EVENT_TYPE = new AbnormalType("badEventType");
    public static AbnormalType BAD_CAUSE_FULL_GC = new AbnormalType("badCauseFullGC");
    public static AbnormalType BAD_INTERVAL = new AbnormalType("badInterval");
    public static AbnormalType BAD_PROMOTION = new AbnormalType("badPromotion");
    public static AbnormalType BAD_YOUNG_GEN_CAPACITY = new AbnormalType("smallYoungGen");
    public static AbnormalType BAD_OLD_GEN_CAPACITY = new AbnormalType("smallOldGen");
    public static AbnormalType BAD_HUMONGOUS_USED = new AbnormalType("highHumongousUsed");
    public static AbnormalType BAD_HEAP_USED = new AbnormalType("highHeapUsed");
    public static AbnormalType BAD_OLD_USED = new AbnormalType("highOldUsed");
    public static AbnormalType BAD_METASPACE_USED = new AbnormalType("highMetaspaceUsed");
    public static AbnormalType BAD_SYS = new AbnormalType("badSys");
    public static AbnormalType BAD_USR = new AbnormalType("badUsr");
    public static AbnormalType TO_SPACE_EXHAUSTED = new AbnormalType("toSpaceExhausted");
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

    public I18nStringView toI18nStringView() {
        return new I18nStringView(I18N_PREFIX + name);
    }

    public int getOrdinal() {
        return ordinal;
    }
}
