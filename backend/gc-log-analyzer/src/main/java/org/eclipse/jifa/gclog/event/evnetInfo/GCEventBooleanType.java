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

package org.eclipse.jifa.gclog.event.evnetInfo;

/*
 * This class records some boolean types related to a GCEvent. They will be saved in a BitSet of GCEvent,
 * Each boolean should have the default value false.
 */
public enum GCEventBooleanType {

    PROMOTION_FAILED,
    // this gc is just after a cms or g1 remark
    GC_AFTER_REMARK,
    // this event is just after a cms cycle, or last mixed gc of an old cycle in g1, or the Prepare Mixed gc
    // because no mixed gc will be scheduled
    GC_AT_END_OF_OLD_CYCLE,
    // pause of this event should not be included in statistics
    IGNORE_PAUSE,
    TO_SPACE_EXHAUSTED,
    YOUNG_GC_BECOME_FULL_GC,
    INITIAL_MARK,
    PREPARE_MIXED;
}
