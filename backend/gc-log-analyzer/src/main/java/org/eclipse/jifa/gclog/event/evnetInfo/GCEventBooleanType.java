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
    IGNORE_PAUSE, // pause of this event should not be included in statistics
    TO_SPACE_EXHAUSTED,
    YOUNG_GC_BECOME_FULL_GC,
    INITIAL_MARK,
    PREPARE_MIXED;
}
