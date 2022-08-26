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

import org.eclipse.jifa.common.util.ErrorUtil;
import org.eclipse.jifa.gclog.model.modeInfo.GCCollectorType;

public class GCModelFactory {
    public static GCModel getModel(GCCollectorType collectorType) {
        switch (collectorType) {
            case G1:
                return new G1GCModel();
            case CMS:
                return new CMSGCModel();
            case SERIAL:
                return new SerialGCModel();
            case PARALLEL:
                return new ParallelGCModel();
            case ZGC:
                return new ZGCModel();
            case UNKNOWN:
                return new UnknownGCModel();
            default:
                ErrorUtil.shouldNotReachHere();
        }
        return null;
    }
}
