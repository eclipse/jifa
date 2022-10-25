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

public enum SuggestionType {
    // The order of enums doesn't matter
    // Whenever a new suggestion is added here, add its text to the frontend
    UPGRADE_TO_11_G1_FULL_GC("upgradeTo11G1FullGC"),
    CHECK_SYSTEM_GC("checkSystemGC"),
    DISABLE_SYSTEM_GC("disableSystemGC"),
    OLD_SYSTEM_GC("oldSystemGC"),
    CHECK_METASPACE("checkMetaspace"),
    ENLARGE_METASPACE("enlargeMetaspace"),
    ENLARGE_HEAP("enlargeHeap"),
    INCREASE_CONC_GC_THREADS("increaseConcGCThreads"),
    INCREASE_Z_ALLOCATION_SPIKE_TOLERANCE("increaseZAllocationSpikeTolerance"),
    DECREASE_IHOP("decreaseIHOP"),
    DECREASE_CMSIOF("decreaseCMSIOF"),
    CHECK_LIVE_OBJECTS("checkLiveObjects"),
    CHECK_REFERENCE_GC("checkReferenceGC"),
    CHECK_CPU_TIME("checkCPUTime"),
    SHRINK_YOUNG_GEN("shrinkYoungGen"),
    SHRINK_YOUNG_GEN_G1("shrinkYoungGenG1"),
    CHECK_EVACUATION_FAILURE("checkEvacuationFailure"),
    CHECK_FAST_PROMOTION("checkFastPromotion"),
    CHECK_RESCAN("checkRescan"),
    CHECK_CLASS_UNLOADING("checkClassUnloading"),
    EXPAND_YOUNG_GEN("expandYoungGen"),
    EXPAND_YOUNG_GEN_G1("expandYoungGenG1"),
    CHECK_FAST_OBJECT_ALLOCATION("checkFastObjectAllocation"),
    USE_MORE_DETAILED_LOGGING_PREUNIFIED("useMoreDetailedLoggingPreunified"),
    USE_MORE_DETAILED_LOGGING_UNIFIED("useMoreDetailedLoggingUnified"),
    CHECK_MEMORY_LEAK("checkMemoryLeak");

    public static final String I18N_PREFIX = "jifa.gclog.diagnose.suggestion.";

    private String name;

    SuggestionType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
