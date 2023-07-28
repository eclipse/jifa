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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

import static org.eclipse.jifa.gclog.util.Constant.UNKNOWN_INT;
import static org.eclipse.jifa.gclog.util.Constant.KB2MB;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GCMemoryItem {
    private MemoryArea area;

    // memory size in kb
    private long preUsed = UNKNOWN_INT;
    private long preCapacity = UNKNOWN_INT;
    private long postUsed = UNKNOWN_INT;
    private long postCapacity = UNKNOWN_INT;

    public GCMemoryItem(MemoryArea area) {
        this.area = area;
    }

    public GCMemoryItem(MemoryArea area, long preUsed, long postUsed, long postCapacity) {
        this.area = area;
        this.preUsed = preUsed;
        this.postUsed = postUsed;
        this.postCapacity = postCapacity;
    }

    public GCMemoryItem(MemoryArea area, long[] memories) {
        this(area, memories[0], memories[1], memories[2], memories[3]);
    }

    public long getMemoryReduction() {
        return minus(preUsed, postUsed);
    }

    /**
     * unknown value in this or anotherItem will lead result to be unknown.
     */
    public GCMemoryItem merge(GCMemoryItem anotherItem) {
        if (anotherItem == null) {
            return new GCMemoryItem(area);
        }
        return new GCMemoryItem(area,
                plus(preUsed, anotherItem.preUsed),
                plus(preCapacity, anotherItem.preCapacity),
                plus(postUsed, anotherItem.postUsed),
                plus(postCapacity, anotherItem.postCapacity));
    }

    /**
     * unknown value in this will lead result to be unknown.
     * unknown value in anotherItem are seen as 0
     */
    public GCMemoryItem mergeIfPresent(GCMemoryItem anotherItem) {
        if (anotherItem == null) {
            return this;
        }
        return new GCMemoryItem(area,
                plusIfPresent(preUsed, anotherItem.preUsed),
                plusIfPresent(preCapacity, anotherItem.preCapacity),
                plusIfPresent(postUsed, anotherItem.postUsed),
                plusIfPresent(postCapacity, anotherItem.postCapacity));
    }

    /**
     * unknown value in this or anotherItem will lead result to be unknown.
     */
    public GCMemoryItem subtract(GCMemoryItem anotherItem) {
        if (anotherItem == null) {
            return new GCMemoryItem(area);
        }
        return new GCMemoryItem(area,
                minus(preUsed, anotherItem.preUsed),
                minus(preCapacity, anotherItem.preCapacity),
                minus(postUsed, anotherItem.postUsed),
                minus(postCapacity, anotherItem.postCapacity));
    }

    /**
     * unknown value in this will lead result to be unknown.
     * unknown value in anotherItem are seen as 0
     */
    public GCMemoryItem subtractIfPresent(GCMemoryItem anotherItem) {
        if (anotherItem == null) {
            return this;
        }
        return new GCMemoryItem(area,
                minusIfPresent(preUsed, anotherItem.preUsed),
                minusIfPresent(preCapacity, anotherItem.preCapacity),
                minusIfPresent(postUsed, anotherItem.postUsed),
                minusIfPresent(postCapacity, anotherItem.postCapacity));
    }

    public GCMemoryItem updateIfAbsent(GCMemoryItem anotherItem) {
        if (anotherItem == null) {
            return this;
        }
        return new GCMemoryItem(area,
                preUsed == UNKNOWN_INT ? anotherItem.preUsed : preUsed,
                preCapacity == UNKNOWN_INT ? anotherItem.preCapacity : preCapacity,
                postUsed == UNKNOWN_INT ? anotherItem.postUsed : postUsed,
                postCapacity == UNKNOWN_INT ? anotherItem.postCapacity : postCapacity);
    }

    private static long plus(long x, long y) {
        if (x == UNKNOWN_INT || y == UNKNOWN_INT) {
            return UNKNOWN_INT;
        }
        return x + y;
    }

    private static long plusIfPresent(long x, long y) {
        if (x == UNKNOWN_INT || y == UNKNOWN_INT) {
            return x;
        }
        return x + y;
    }

    private static long minus(long x, long y) {
        if (x == UNKNOWN_INT || y == UNKNOWN_INT) {
            return UNKNOWN_INT;
        }
        return x >= y ? x - y : 0;
    }

    private static long minusIfPresent(long x, long y) {
        if (x == UNKNOWN_INT || y == UNKNOWN_INT) {
            return x;
        }
        return x >= y ? x - y : 0;
    }

    public void multiply(long x) {
        if (preUsed != UNKNOWN_INT) {
            preUsed *= x;
        }
        if (preCapacity != UNKNOWN_INT) {
            preCapacity *= x;
        }
        if (postUsed != UNKNOWN_INT) {
            postUsed *= x;
        }
        if (postCapacity != UNKNOWN_INT) {
            postCapacity *= x;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GCMemoryItem item = (GCMemoryItem) o;
        return preUsed == item.preUsed && preCapacity == item.preCapacity && postUsed == item.postUsed && postCapacity == item.postCapacity && area == item.area;
    }

    @Override
    public int hashCode() {
        return Objects.hash(area, preUsed, preCapacity, postUsed, postCapacity);
    }

    public boolean isEmpty() {
        return preUsed == UNKNOWN_INT && preCapacity == UNKNOWN_INT && postUsed == UNKNOWN_INT && postCapacity == UNKNOWN_INT;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String area = this.area.toString().toLowerCase();
        sb.append((char) (area.charAt(0) - 32)).append(area.substring(1)).append(": ");
        if (isEmpty()) {
            sb.append("unknown");
        } else {
            if (preUsed != UNKNOWN_INT) {
                sb.append((long) (Math.max(0, preUsed) / KB2MB / KB2MB)).append("M");
            }
            if (preCapacity != UNKNOWN_INT) {
                sb.append('(').append((long) (Math.max(0, preCapacity) / KB2MB / KB2MB)).append("M)");
            }
            if (preUsed != UNKNOWN_INT || preCapacity != UNKNOWN_INT) {
                sb.append("->");
            }
            if (postUsed != UNKNOWN_INT) {
                sb.append((long) (Math.max(0, postUsed) / KB2MB / KB2MB)).append('M');
            } else {
                sb.append("unknown");
            }
            if (postCapacity != UNKNOWN_INT) {
                sb.append('(').append((long) (Math.max(0, postCapacity) / KB2MB / KB2MB)).append("M)");
            }
        }
        return sb.toString();
    }
}
