package org.eclipse.jifa.gclog.diagnoser;

import org.eclipse.jifa.gclog.model.G1GCModel;
import org.eclipse.jifa.gclog.model.GCModel;
import org.eclipse.jifa.gclog.util.I18nStringView;
import org.eclipse.jifa.gclog.vo.GCCollectorType;
import org.eclipse.jifa.gclog.vo.GCLogStyle;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import static org.eclipse.jifa.gclog.diagnoser.SuggestionType.*;
import static org.eclipse.jifa.gclog.model.GCEvent.UNKNOWN_INT;

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
                addSuggestion(DECREASE_CMSIOP);
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
