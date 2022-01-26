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

package org.eclipse.jifa.gclog.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

import static org.eclipse.jifa.gclog.model.GCEvent.UNKNOWN_INT;
import static org.eclipse.jifa.gclog.model.GCModel.KB2MB;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GCCollectionResultItem {
    private HeapGeneration generation;

    // memory size in kb
    private int preUsed = UNKNOWN_INT;
    private int postUsed = UNKNOWN_INT;
    private int total = UNKNOWN_INT;

    public GCCollectionResultItem(HeapGeneration generation) {
        this.generation = generation;
    }

    public GCCollectionResultItem(HeapGeneration generation, int[] memories) {
        this(generation, memories[0], memories[1], memories[2]);
    }

    public int getMemoryReduction() {
        return minus(preUsed, postUsed);
    }

    /**
     * unknown value in this or anotherItem will lead result to be unknown.
     */
    public GCCollectionResultItem merge(GCCollectionResultItem anotherItem) {
        return new GCCollectionResultItem(generation,
                plus(preUsed, anotherItem.preUsed),
                plus(postUsed, anotherItem.postUsed),
                plus(total, anotherItem.total));
    }

    /**
     * unknown value in this will lead result to be unknown.
     * unknown value in anotherItem are seen as 0
     */
    public GCCollectionResultItem mergeIfPresent(GCCollectionResultItem anotherItem) {
        return new GCCollectionResultItem(generation,
                plusIfPresent(preUsed, anotherItem.preUsed),
                plusIfPresent(postUsed, anotherItem.postUsed),
                plusIfPresent(total, anotherItem.total));
    }

    /**
     * unknown value in this or anotherItem will lead result to be unknown.
     */
    public GCCollectionResultItem subtract(GCCollectionResultItem anotherItem) {
        return new GCCollectionResultItem(generation,
                minus(preUsed, anotherItem.preUsed),
                minus(postUsed, anotherItem.postUsed),
                minus(total, anotherItem.total));
    }

    /**
     * unknown value in this will lead result to be unknown.
     * unknown value in anotherItem are seen as 0
     */
    public GCCollectionResultItem subtractIfPresent(GCCollectionResultItem anotherItem) {
        return new GCCollectionResultItem(generation,
                minusIfPresent(preUsed, anotherItem.preUsed),
                minusIfPresent(postUsed, anotherItem.postUsed),
                minusIfPresent(total, anotherItem.total));
    }

    public GCCollectionResultItem updateIfAbsent(GCCollectionResultItem anotherItem) {
        return new GCCollectionResultItem(generation,
                preUsed == UNKNOWN_INT ? anotherItem.preUsed : preUsed,
                postUsed == UNKNOWN_INT ? anotherItem.postUsed : postUsed,
                total == UNKNOWN_INT ? anotherItem.total : total);
    }

    private static int plus(int x, int y) {
        if (x == UNKNOWN_INT || y == UNKNOWN_INT) {
            return UNKNOWN_INT;
        }
        return x + y;
    }

    private static int plusIfPresent(int x, int y) {
        if (x == UNKNOWN_INT || y == UNKNOWN_INT) {
            return x;
        }
        return x + y;
    }

    private static int minus(int x, int y) {
        if (x == UNKNOWN_INT || y == UNKNOWN_INT) {
            return UNKNOWN_INT;
        }
        return x - y;
    }

    private static int minusIfPresent(int x, int y) {
        if (x == UNKNOWN_INT || y == UNKNOWN_INT) {
            return x;
        }
        return x - y;
    }

    public void setGeneration(HeapGeneration generation) {
        this.generation = generation;
    }

    public void setPreUsed(int preUsed) {
        this.preUsed = preUsed;
    }

    public void setPostUsed(int postUsed) {
        this.postUsed = postUsed;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public void multiply(int x) {
        if (preUsed != UNKNOWN_INT) {
            preUsed *= x;
        }
        if (postUsed != UNKNOWN_INT) {
            postUsed *= x;
        }
        if (total != UNKNOWN_INT) {
            total *= x;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GCCollectionResultItem that = (GCCollectionResultItem) o;
        return preUsed == that.preUsed && postUsed == that.postUsed && total == that.total && generation == that.generation;
    }

    @Override
    public int hashCode() {
        return Objects.hash(generation, preUsed, postUsed, total);
    }

    public boolean isEmpty() {
        return preUsed == UNKNOWN_INT && postUsed == UNKNOWN_INT && total == UNKNOWN_INT;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String generation = this.generation.toString().toLowerCase();
        sb.append((char) (generation.charAt(0) - 32)).append(generation.substring(1)).append(": ");
        if (isEmpty()) {
            sb.append("unknown");
        } else {
            if (preUsed != UNKNOWN_INT) {
                sb.append(Math.max(0, preUsed) / (int) KB2MB).append("M->");
            }
            if (postUsed != UNKNOWN_INT) {
                sb.append(Math.max(0, postUsed) / (int) KB2MB).append('M');
            } else {
                sb.append("unknown");
            }
            if (total != UNKNOWN_INT) {
                sb.append('(').append(Math.max(0, total) / (int) KB2MB).append("M)");
            }
        }
        return sb.toString();
    }
}
