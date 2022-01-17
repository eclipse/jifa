/********************************************************************************
 * Copyright (c) 2020, 2022 Contributors to the Eclipse Foundation
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

import org.eclipse.jifa.gclog.vo.GCCollectorType;

import java.util.Arrays;
import java.util.List;

import static org.eclipse.jifa.gclog.model.GCEventType.*;

public class ParallelGCModel extends GenerationalGCModel {

    public ParallelGCModel() {
        super(GCCollectorType.PARALLEL);
    }

    private final static List<String> PAUSE_EVENT_NAMES = Arrays.asList(
            YOUNG_GC.getName(),
            FULL_GC.getName()
    );

    @Override
    protected List<String> getPauseEventNames() {
        return PAUSE_EVENT_NAMES;
    }

    private final static List<GCEventType> SUPPORTED_PHASE_EVENT_TYPES = Arrays.asList(
            YOUNG_GC,
            FULL_GC,
            PARALLEL_PHASE_MARKING,
            PARALLEL_PHASE_SUMMARY,
            PARALLEL_PHASE_ADJUST_ROOTS,
            PARALLEL_PHASE_COMPACTION,
            PARALLEL_PHASE_POST_COMPACT
    );

    @Override
    protected List<GCEventType> getSupportedPhaseEventTypes() {
        return SUPPORTED_PHASE_EVENT_TYPES;
    }

    private final static List<String> METADATA_EVENT_TYPES = Arrays.asList(
            YOUNG_GC.getName(),
            FULL_GC.getName()
    );

    @Override
    protected List<String> getMetadataEventTypes() {
        return METADATA_EVENT_TYPES;
    }
}
