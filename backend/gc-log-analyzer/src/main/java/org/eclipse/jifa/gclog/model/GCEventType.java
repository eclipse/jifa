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

package org.eclipse.jifa.gclog.model;

import org.eclipse.jifa.gclog.vo.GCEventLevel;
import org.eclipse.jifa.gclog.vo.GCPause;

import java.util.HashMap;
import java.util.Map;

import static org.eclipse.jifa.gclog.vo.GCPause.*;

/*
 * store all info that may affect the way model is organized and explained
 */
public class GCEventType {
    private String name;
    private GCPause pause;
    private GCEventType[] phaseParentEventType;
    private GCEventLevel level;

    public static final GCEventType UNDEFINED = new GCEventType("Undefined", GCPause.PARTIAL);

    // external event types
    public static final GCEventType YOUNG_GC = new GCEventType("Young GC", PAUSE);
    public static final GCEventType OLD_GC = new GCEventType("Old GC", PAUSE);
    public static final GCEventType G1_YOUNG_MIXED_GC = new GCEventType("Young GC(Mixed)", PAUSE);
    public static final GCEventType FULL_GC = new GCEventType("Full GC", PAUSE);
    public static final GCEventType G1_CONCURRENT_CYCLE = new GCEventType("Concurrent Cycle", GCPause.PARTIAL);
    public static final GCEventType CMS_CONCURRENT_MARK_SWEPT = new GCEventType("CMS", GCPause.PARTIAL);
    public static final GCEventType ZGC_GARBAGE_COLLECTION = new GCEventType("Garbage Collection", PARTIAL);

    // shared parent
    private static final GCEventType[] PARENT_CONCURRENT_MARK_CYCLE = {G1_CONCURRENT_CYCLE, CMS_CONCURRENT_MARK_SWEPT, ZGC_GARBAGE_COLLECTION};
    private static final GCEventType[] PARENT_YOUNG_OLD_FULL_GC = {YOUNG_GC, OLD_GC, FULL_GC, G1_YOUNG_MIXED_GC, ZGC_GARBAGE_COLLECTION};

    // internal phase types
    // shared by serial and cms
    public static final GCEventType SERIAL_MARK_LIFE_OBJECTS = new GCEventType("Mark live objects", PAUSE, PARENT_YOUNG_OLD_FULL_GC);
    public static final GCEventType SERIAL_COMPUTE_NEW_OBJECT_ADDRESSES = new GCEventType("Compute new object addresses", PAUSE, PARENT_YOUNG_OLD_FULL_GC);
    public static final GCEventType SERIAL_ADJUST_POINTERS = new GCEventType("Adjust pointers", PAUSE, PARENT_YOUNG_OLD_FULL_GC);
    public static final GCEventType SERIAL_MOVE_OBJECTS = new GCEventType("Move objects", PAUSE, PARENT_YOUNG_OLD_FULL_GC);
    public static final GCEventType WEAK_REFS_PROCESSING = new GCEventType("Weak Refs Processing", PAUSE, PARENT_CONCURRENT_MARK_CYCLE, GCEventLevel.SUBPHASE);

    // shared by g1 and cms
    public static final GCEventType REMARK = new GCEventType("Remark", PAUSE, PARENT_CONCURRENT_MARK_CYCLE);
    public static final GCEventType INITIAL_MARK = new GCEventType("Initial Mark", PAUSE, PARENT_CONCURRENT_MARK_CYCLE);
    public static final GCEventType CONCURRENT_MARK = new GCEventType("Concurrent Mark", GCPause.CONCURRENT, PARENT_CONCURRENT_MARK_CYCLE);

    // Parallel
    public static final GCEventType PARALLEL_PHASE_MARKING = new GCEventType("Marking Phase", PAUSE, PARENT_YOUNG_OLD_FULL_GC);
    public static final GCEventType PARALLEL_PHASE_SUMMARY = new GCEventType("Summary Phase", PAUSE, PARENT_YOUNG_OLD_FULL_GC);
    public static final GCEventType PARALLEL_PHASE_ADJUST_ROOTS = new GCEventType("Adjust Roots", PAUSE, PARENT_YOUNG_OLD_FULL_GC);
    public static final GCEventType PARALLEL_PHASE_COMPACTION = new GCEventType("Compaction Phase", PAUSE, PARENT_YOUNG_OLD_FULL_GC);
    public static final GCEventType PARALLEL_PHASE_POST_COMPACT = new GCEventType("Post Compact", PAUSE, PARENT_YOUNG_OLD_FULL_GC);

    // G1
    public static final GCEventType G1_COLLECT_PRE_EVACUATION = new GCEventType("Pre Evacuate Collection Set", PAUSE, PARENT_YOUNG_OLD_FULL_GC);
    public static final GCEventType G1_COLLECT_EVACUATION = new GCEventType("Evacuate Collection Set", PAUSE, PARENT_YOUNG_OLD_FULL_GC);
    public static final GCEventType G1_COLLECT_POST_EVACUATION = new GCEventType("Post Evacuate Collection Set", PAUSE, PARENT_YOUNG_OLD_FULL_GC);
    public static final GCEventType G1_COLLECT_OTHER = new GCEventType("Other", PAUSE, PARENT_YOUNG_OLD_FULL_GC);

    public static final GCEventType G1_CONCURRENT_CLEAR_CLAIMED_MARKS = new GCEventType("Concurrent Clear Claimed Marks", GCPause.CONCURRENT, PARENT_CONCURRENT_MARK_CYCLE);
    public static final GCEventType G1_CONCURRENT_SCAN_ROOT_REGIONS = new GCEventType("Root Region Scanning", GCPause.CONCURRENT, PARENT_CONCURRENT_MARK_CYCLE);
    public static final GCEventType G1_CONCURRENT_MARK_FROM_ROOTS = new GCEventType("Concurrent Mark From Roots", GCPause.CONCURRENT, PARENT_CONCURRENT_MARK_CYCLE);

    public static final GCEventType G1_CONCURRENT_PRECLEAN = new GCEventType("Concurrent Preclean", GCPause.CONCURRENT, PARENT_CONCURRENT_MARK_CYCLE);
    public static final GCEventType G1_CONCURRENT_REBUILD_REMEMBERED_SETS = new GCEventType("Concurrent Rebuild Remembered Sets", GCPause.CONCURRENT, PARENT_CONCURRENT_MARK_CYCLE);
    public static final GCEventType G1_PAUSE_CLEANUP = new GCEventType("Pause Cleanup", PAUSE, PARENT_CONCURRENT_MARK_CYCLE);
    public static final GCEventType G1_CONCURRENT_CLEANUP_FOR_NEXT_MARK = new GCEventType("Concurrent Cleanup", GCPause.CONCURRENT, PARENT_CONCURRENT_MARK_CYCLE);
    public static final GCEventType G1_FINALIZE_MARKING= new GCEventType("Finalize Marking", PAUSE, PARENT_CONCURRENT_MARK_CYCLE, GCEventLevel.SUBPHASE);
    public static final GCEventType G1_UNLOADING= new GCEventType("Unloading", PAUSE, PARENT_CONCURRENT_MARK_CYCLE, GCEventLevel.SUBPHASE);
    public static final GCEventType G1_GC_REFPROC = new GCEventType("GC ref-proc", PAUSE, PARENT_CONCURRENT_MARK_CYCLE, GCEventLevel.SUBPHASE);
    public static final GCEventType G1_CONCURRENT_MARK_ABORT = new GCEventType("Concurrent Mark Abort", CONCURRENT, PARENT_CONCURRENT_MARK_CYCLE);
    public static final GCEventType G1_CONCURRENT_MARK_RESET_FOR_OVERFLOW = new GCEventType("Concurrent Mark Reset For Overflow", CONCURRENT, PARENT_CONCURRENT_MARK_CYCLE);

    public static final GCEventType G1_MARK_LIVE_OBJECTS = new GCEventType("Mark Live Objects", PAUSE, PARENT_YOUNG_OLD_FULL_GC);
    public static final GCEventType G1_PREPARE_FOR_COMPACTION = new GCEventType("Prepare for Compaction", PAUSE, PARENT_YOUNG_OLD_FULL_GC);
    public static final GCEventType G1_ADJUST_POINTERS = new GCEventType("Adjust Pointers", PAUSE, PARENT_YOUNG_OLD_FULL_GC);
    public static final GCEventType G1_COMPACT_HEAP = new GCEventType("Compact Heap", PAUSE, PARENT_YOUNG_OLD_FULL_GC);

    public static final GCEventType G1_EXT_ROOT_SCANNING = new GCEventType("Ext Root Scanning", PAUSE, PARENT_YOUNG_OLD_FULL_GC);
    public static final GCEventType G1_UPDATE_RS = new GCEventType("Update RS", PAUSE, PARENT_YOUNG_OLD_FULL_GC);
    public static final GCEventType G1_SCAN_RS = new GCEventType("Scan RS", PAUSE, PARENT_YOUNG_OLD_FULL_GC);
    public static final GCEventType G1_CODE_ROOT_SCANNING = new GCEventType("Code Root Scanning", PAUSE, PARENT_YOUNG_OLD_FULL_GC);
    public static final GCEventType G1_OBJECT_COPY = new GCEventType("Object Copy", PAUSE, PARENT_YOUNG_OLD_FULL_GC);
    public static final GCEventType G1_TERMINATION = new GCEventType("Termination", PAUSE, PARENT_YOUNG_OLD_FULL_GC);
    public static final GCEventType G1_CODE_ROOT_FIXUP = new GCEventType("Code Root Fixup", PAUSE, PARENT_YOUNG_OLD_FULL_GC);
    public static final GCEventType G1_CODE_ROOT_PURGE = new GCEventType("Code Root Purge", PAUSE, PARENT_YOUNG_OLD_FULL_GC);
    public static final GCEventType G1_CLEAR_CT = new GCEventType("Clear CT", PAUSE, PARENT_YOUNG_OLD_FULL_GC);
    public static final GCEventType G1_CHOOSE_CSET = new GCEventType("Choose CSet", PAUSE, PARENT_YOUNG_OLD_FULL_GC);
    public static final GCEventType G1_REF_ENQ = new GCEventType("Ref Enq", PAUSE, PARENT_YOUNG_OLD_FULL_GC);
    public static final GCEventType G1_REDIRTY_CARDS = new GCEventType("Redirty Cards", PAUSE, PARENT_YOUNG_OLD_FULL_GC);
    public static final GCEventType G1_HUMONGOUS_REGISTER = new GCEventType("Humongous Register", PAUSE, PARENT_YOUNG_OLD_FULL_GC);
    public static final GCEventType G1_HUMONGOUS_RECLAIM = new GCEventType("Humongous Reclaim", PAUSE, PARENT_YOUNG_OLD_FULL_GC);
    public static final GCEventType G1_FREE_CSET = new GCEventType("Free CSet", PAUSE, PARENT_YOUNG_OLD_FULL_GC);

    // CMS
    public static final GCEventType CMS_CONCURRENT_PRECLEAN = new GCEventType("Concurrent Preclean", GCPause.CONCURRENT, PARENT_CONCURRENT_MARK_CYCLE);
    public static final GCEventType CMS_CONCURRENT_ABORTABLE_PRECLEAN = new GCEventType("Concurrent Abortable preclean", GCPause.CONCURRENT, PARENT_CONCURRENT_MARK_CYCLE);
    public static final GCEventType CMS_CONCURRENT_SWEEP = new GCEventType("Concurrent Sweep", GCPause.CONCURRENT, PARENT_CONCURRENT_MARK_CYCLE);
    public static final GCEventType CMS_CONCURRENT_RESET = new GCEventType("Concurrent Reset", GCPause.CONCURRENT, PARENT_CONCURRENT_MARK_CYCLE);
    public static final GCEventType CMS_CONCURRENT_INTERRUPTED = new GCEventType("Concurrent Mode Interrupted", CONCURRENT, PARENT_CONCURRENT_MARK_CYCLE);
    public static final GCEventType CMS_CONCURRENT_FAILURE = new GCEventType("Concurrent Mode Failure", CONCURRENT, PARENT_CONCURRENT_MARK_CYCLE);
    public static final GCEventType CMS_RESCAN = new GCEventType("Rescan", PAUSE, PARENT_CONCURRENT_MARK_CYCLE, GCEventLevel.SUBPHASE);
    public static final GCEventType CMS_CLASS_UNLOADING = new GCEventType("Class unloading", PAUSE, PARENT_CONCURRENT_MARK_CYCLE, GCEventLevel.SUBPHASE);
    public static final GCEventType CMS_SCRUB_SYMBOL_TABLE = new GCEventType("Scrub Symbol Table", PAUSE, PARENT_CONCURRENT_MARK_CYCLE, GCEventLevel.SUBPHASE);
    public static final GCEventType CMS_SCRUB_STRING_TABLE = new GCEventType("Scrub STring Table", PAUSE, PARENT_CONCURRENT_MARK_CYCLE, GCEventLevel.SUBPHASE);

    // ZGC
    public static final GCEventType ZGC_PAUSE_MARK_START = new GCEventType("Pause Mark Start", PAUSE, PARENT_YOUNG_OLD_FULL_GC);
    public static final GCEventType ZGC_PAUSE_MARK_END = new GCEventType("Pause Mark End", PAUSE, PARENT_YOUNG_OLD_FULL_GC);
    public static final GCEventType ZGC_PAUSE_RELOCATE_START = new GCEventType("Pause Relocate Start", PAUSE, PARENT_YOUNG_OLD_FULL_GC);
    public static final GCEventType ZGC_CONCURRENT_MARK = new GCEventType("Concurrent Mark", CONCURRENT, PARENT_YOUNG_OLD_FULL_GC);
    public static final GCEventType ZGC_CONCURRENT_NONREF = new GCEventType("Concurrent Process Non-Strong References", CONCURRENT, PARENT_YOUNG_OLD_FULL_GC);
    public static final GCEventType ZGC_CONCURRENT_RESET_RELOC_SET = new GCEventType("Concurrent Reset Relocation Set", CONCURRENT, PARENT_YOUNG_OLD_FULL_GC);
    public static final GCEventType ZGC_CONCURRENT_DETATCHED_PAGES = new GCEventType("Concurrent Destroy Detached Pages", CONCURRENT, PARENT_YOUNG_OLD_FULL_GC);
    public static final GCEventType ZGC_CONCURRENT_SELECT_RELOC_SET = new GCEventType("Concurrent Select Relocation Set", CONCURRENT, PARENT_YOUNG_OLD_FULL_GC);
    public static final GCEventType ZGC_CONCURRENT_PREPARE_RELOC_SET = new GCEventType("Concurrent Prepare Relocation Set", CONCURRENT, PARENT_YOUNG_OLD_FULL_GC);
    public static final GCEventType ZGC_CONCURRENT_RELOCATE = new GCEventType("Concurrent Relocate", CONCURRENT, PARENT_YOUNG_OLD_FULL_GC);
    public static final GCEventType ZGC_ALLOCATION_STALL = new GCEventType("Allocation Stall", PAUSE);

    // other
    public static final GCEventType SAFEPOINT = new GCEventType("Safepoint", PAUSE);

    public boolean hasObjectPromotion() {
        return this == YOUNG_GC || this == G1_YOUNG_MIXED_GC;
    }

    public GCPause getPause() {
        return pause;
    }

    public String getName() {
        return name;
    }

    public GCEventType[] getPhaseParentEventType() {
        return phaseParentEventType;
    }

    // construction from outside not allowed, all instances are created in advance
    private GCEventType(String name, GCPause pause) {
        this(name, pause, null, GCEventLevel.EVENT);
    }

    private GCEventType(String name, GCPause pause, GCEventType[] phaseParentEventType) {
        this(name, pause, phaseParentEventType, phaseParentEventType == null ? GCEventLevel.EVENT : GCEventLevel.PHASE);
    }

    public GCEventType(String name, GCPause pause, GCEventType[] phaseParentEventType, GCEventLevel level) {
        this.name = name;
        this.pause = pause;
        this.phaseParentEventType = phaseParentEventType;
        this.level = level;
    }

    @Override
    public String toString() {
        return name;
    }

    public GCEventLevel getLevel() {
        return level;
    }

    public boolean isYoungGC() {
        return this == GCEventType.YOUNG_GC || this == GCEventType.G1_YOUNG_MIXED_GC;
    }

    public boolean isOldGC() {
        return this == GCEventType.G1_CONCURRENT_CYCLE || this == GCEventType.CMS_CONCURRENT_MARK_SWEPT;
    }

    public boolean isFullGC() {
        return this == GCEventType.FULL_GC || this == ZGC_GARBAGE_COLLECTION;
    }
}
