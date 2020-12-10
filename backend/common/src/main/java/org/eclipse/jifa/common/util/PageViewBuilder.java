/********************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.common.util;

import org.eclipse.jifa.common.request.PagingRequest;
import org.eclipse.jifa.common.vo.PageView;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PageViewBuilder {

    public static <S, R> PageView<R> build(Callback<S> callback, PagingRequest paging, Function<S, R> mapper) {
        List<R> result = IntStream.range(paging.from(), paging.to(callback.totalSize()))
                                  .mapToObj(callback::get)
                                  .map(mapper)
                                  .collect(Collectors.toList());
        return new PageView<>(paging, callback.totalSize(), result);
    }

    public static <S, R> PageView<R> build(Callback<S> callback, PagingRequest paging, Function<S, R> mapper, Comparator<R> comparator) {
        List<R> result = IntStream.range(paging.from(), paging.to(callback.totalSize()))
                                  .mapToObj(callback::get)
                                  .map(mapper)
                                  .sorted(comparator)
                                  .collect(Collectors.toList());
        return new PageView<>(paging, callback.totalSize(), result);
    }

    public static <R> PageView<R> build(Collection<R> total, PagingRequest paging) {
        List<R> result = total.stream()
                              .skip(paging.from())
                              .limit(paging.getPageSize())
                              .collect(Collectors.toList());
        return new PageView<>(paging, total.size(), result);
    }

    public static <S, R> PageView<R> build(Collection<S> total, PagingRequest paging, Function<S, R> mapper) {
        List<R> result = total.stream()
                              .skip(paging.from())
                              .limit(paging.getPageSize())
                              .map(mapper)
                              .collect(Collectors.toList());
        return new PageView<>(paging, total.size(), result);
    }

    public static <S, R> PageView<R> build(Collection<S> total, PagingRequest paging, Function<S, R> mapper, Comparator<R> comparator) {
        List<R> result = total.stream()
                               .skip(paging.from())
                               .limit(paging.getPageSize())
                               .map(mapper)
                               .sorted(comparator) // sort after applied mapping function
                               .collect(Collectors.toList());
        return new PageView<>(paging, total.size(), result);
    }

    public static <R> PageView<R> build(int[] total, PagingRequest paging, IntFunction<R> mapper) {
        List<R> result = Arrays.stream(total)
                               .skip(paging.from())
                               .limit(paging.getPageSize())
                               .mapToObj(mapper)
                               .collect(Collectors.toList());
        return new PageView<>(paging, total.length, result);
    }

    public interface Callback<O> {
        int totalSize();

        O get(int index);
    }
}
