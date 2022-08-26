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

import org.eclipse.jifa.gclog.model.modeInfo.GCCollectorType;

import java.util.List;

import static org.eclipse.jifa.gclog.model.GCEventType.FULL_GC;
import static org.eclipse.jifa.gclog.model.GCEventType.YOUNG_GC;

public class ParallelGCModel extends GenerationalGCModel {
    private static GCCollectorType collector = GCCollectorType.PARALLEL;

    public ParallelGCModel() {
        super(collector);
    }

    private static List<GCEventType> allEventTypes = GCModel.calcAllEventTypes(collector);
    private static List<GCEventType> pauseEventTypes = GCModel.calcPauseEventTypes(collector);
    private static List<GCEventType> mainPauseEventTypes = GCModel.calcMainPauseEventTypes(collector);
    private static List<GCEventType> parentEventTypes = GCModel.calcParentEventTypes(collector);
    private static List<GCEventType> importantEventTypes = List.of(YOUNG_GC, FULL_GC);

    @Override
    protected List<GCEventType> getAllEventTypes() {
        return allEventTypes;
    }

    @Override
    protected List<GCEventType> getPauseEventTypes() {
        return pauseEventTypes;
    }

    @Override
    protected List<GCEventType> getMainPauseEventTypes() {
        return mainPauseEventTypes;
    }

    @Override
    protected List<GCEventType> getImportantEventTypes() {
        return importantEventTypes;
    }

    @Override
    protected List<GCEventType> getParentEventTypes() {
        return parentEventTypes;
    }

}
