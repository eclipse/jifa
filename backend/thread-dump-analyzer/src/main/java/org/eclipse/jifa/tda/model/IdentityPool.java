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

package org.eclipse.jifa.tda.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class IdentityPool<O extends Identity> extends Pool<O> {

    private final AtomicInteger id;

    private ConcurrentHashMap<O, AtomicInteger> refCountMap;

    private List<O> objects;

    public IdentityPool() {
        id = new AtomicInteger();
        refCountMap = new ConcurrentHashMap<>();
    }

    @Override
    public O add(O o) {
        O pooled = map.computeIfAbsent(o, k -> {
            k.setId(nextId());
            return k;
        });
        refCountMap.computeIfAbsent(pooled, k -> new AtomicInteger(0)).incrementAndGet();
        return pooled;
    }

    private int nextId() {
        int id = this.id.incrementAndGet();
        assert id > 0;
        return id;
    }

    public void freeze() {
        objects = new ArrayList<>(map.values());
        objects.sort((k1, k2) -> refCountMap.get(k2).get() - refCountMap.get(k1).get());

        super.freeze();
        refCountMap = null;
    }

    public List<O> objects() {
        return objects;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        IdentityPool<?> that = (IdentityPool<?>) o;
        return Objects.equals(id.get(), that.id.get()) && Objects.equals(refCountMap, that.refCountMap) &&
               Objects.equals(objects, that.objects);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, refCountMap, objects);
    }
}
