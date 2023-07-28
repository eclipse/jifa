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

import org.eclipse.jifa.gclog.model.GCModel;
import org.eclipse.jifa.gclog.model.modeInfo.GCCollectorType;
import org.eclipse.jifa.gclog.model.modeInfo.GCLogStyle;
import org.eclipse.jifa.gclog.util.I18nStringView;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import static org.eclipse.jifa.gclog.diagnoser.SuggestionType.*;
import static org.eclipse.jifa.gclog.util.Constant.UNKNOWN_INT;

public abstract class SuggestionGenerator {
    protected GCModel model;
    protected BitSet givenCause = new BitSet();
    protected List<I18nStringView> result = new ArrayList<>();

    public SuggestionGenerator(GCModel model) {
        this.model = model;
    }

    protected void addSuggestion(SuggestionType type, Object... params) {
        // don't add duplicate suggestions
        if (givenCause.get(type.ordinal())) {
            return;
        }
        givenCause.set(type.ordinal());
        result.add(new I18nStringView(SuggestionType.I18N_PREFIX + type.toString(), params));
    }

    protected void suggestExpandYoungGen() {
        if (model.getCollectorType() == GCCollectorType.G1) {
            addSuggestion(EXPAND_YOUNG_GEN_G1);
        } else {
            addSuggestion(EXPAND_YOUNG_GEN);
        }
    }
    protected void suggestShrinkYoungGen() {
        if (model.getCollectorType() == GCCollectorType.G1) {
            addSuggestion(SHRINK_YOUNG_GEN_G1);
        } else {
            addSuggestion(SHRINK_YOUNG_GEN);
        }
    }

    protected void suggestUseMoreDetailedLogging() {
        if (model.getLogStyle() == GCLogStyle.UNIFIED) {
            addSuggestion(USE_MORE_DETAILED_LOGGING_UNIFIED);
        }
    }

    protected void suggestOldSystemGC() {
        if (model.hasOldGC()) {
            addSuggestion(OLD_SYSTEM_GC);
        }
    }

    protected void suggestEnlargeHeap(boolean suggestHeapSize) {
        if (suggestHeapSize) {
            long size = model.getRecommendMaxHeapSize();
            if (size != UNKNOWN_INT) {
                addSuggestion(ENLARGE_HEAP, "recommendSize", size);
            } else {
                addSuggestion(ENLARGE_HEAP);
            }
        } else {
            addSuggestion(ENLARGE_HEAP);
        }
    }

    protected void fullGCSuggestionCommon() {
        if (model.getCollectorType() == GCCollectorType.G1 && model.getLogStyle() == GCLogStyle.PRE_UNIFIED) {
            addSuggestion(UPGRADE_TO_11_G1_FULL_GC);
        }
    }

    protected void suggestStartOldGCEarly() {
        switch (model.getCollectorType()) {
            case CMS:
                addSuggestion(DECREASE_CMSIOF);
                break;
            case G1:
                addSuggestion(DECREASE_IHOP);
                break;
        }
    }

    protected void suggestCheckEvacuationFailure() {
        if (model.getCollectorType() == GCCollectorType.G1) {
            addSuggestion(CHECK_EVACUATION_FAILURE);
        }
    }
}
