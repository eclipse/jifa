package org.eclipse.jifa.gclog.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

import static org.eclipse.jifa.gclog.model.GCEvent.*;

@Data
@NoArgsConstructor
public class ReferenceGC {

    private double softReferenceStartTime = UNKNOWN_DOUBLE;
    private int softReferenceCount = UNKNOWN_INT;
    private double softReferencePauseTime = UNKNOWN_DOUBLE;

    private double weakReferenceStartTime = UNKNOWN_DOUBLE;
    private int weakReferenceCount = UNKNOWN_INT;
    private double weakReferencePauseTime = UNKNOWN_DOUBLE;

    private double finalReferenceStartTime = UNKNOWN_DOUBLE;
    private int finalReferenceCount = UNKNOWN_INT;
    private double finalReferencePauseTime = UNKNOWN_DOUBLE;

    private double phantomReferenceStartTime = UNKNOWN_DOUBLE;
    private int phantomReferenceCount = UNKNOWN_INT;
    private int phantomReferenceFreedCount;
    private double phantomReferencePauseTime = UNKNOWN_DOUBLE;

    private double jniWeakReferenceStartTime = UNKNOWN_DOUBLE;
    private double jniWeakReferencePauseTime = UNKNOWN_DOUBLE;
}
