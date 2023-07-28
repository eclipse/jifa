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
package org.eclipse.jifa.common.util;

import org.eclipse.jifa.common.domain.request.PagingRequest;
import org.eclipse.jifa.common.domain.vo.PageView;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Page view builder.
 *
 * @param <ST> source type
 * @param <TT> target type
 */
public class PageViewBuilder<ST, TT> {

    public static <S, T> PageView<T> build(Callback<S> callback, PagingRequest paging, Function<S, T> mapper) {
        List<T> result = IntStream.range(paging.from(), paging.to(callback.totalSize()))
                                  .mapToObj(callback::get)
                                  .map(mapper)
                                  .collect(Collectors.toList());
        return new PageView<>(paging, callback.totalSize(), result);
    }

    public static <T> PageView<T> build(Collection<T> total, PagingRequest paging) {
        List<T> result = total.stream()
                              .skip(paging.from())
                              .limit(paging.getPageSize())
                              .collect(Collectors.toList());
        return new PageView<>(paging, total.size(), result);
    }

    public static <S, T> PageView<T> build(Collection<S> total, PagingRequest paging, Function<S, T> mapper) {
        List<T> result = total.stream()
                              .skip(paging.from())
                              .limit(paging.getPageSize())
                              .map(mapper)
                              .collect(Collectors.toList());
        return new PageView<>(paging, total.size(), result);
    }

    public static <S, IT, T> PageView<T> build(Collection<S> total, PagingRequest paging, Function<S, IT> mapper1,
                                               Function<IT, T> mapper2,
                                               Comparator<IT> comparator) {
        List<T> result = total.stream()
                              .map(mapper1)
                              .sorted(comparator)
                              .skip(paging.from())
                              .limit(paging.getPageSize())
                              .map(mapper2)
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

    public static <S, R> PageView<R> build(S[] total, PagingRequest paging, Function<S, R> mapper) {
        List<R> result = Arrays.stream(total)
                               .skip(paging.from())
                               .limit(paging.getPageSize())
                               .map(mapper)
                               .collect(Collectors.toList());
        return new PageView<>(paging, total.length, result);
    }

    public interface Callback<O> {
        int totalSize();

        O get(int index);
    }

    // complex builder
    private List<ST> list;
    private Function<ST, TT> mapper;
    private PagingRequest paging;
    private Comparator<TT> comparator;
    private Predicate<TT> filter;

    private PageViewBuilder() {

    }

    public static <A, B> PageViewBuilder<A, B> fromList(List<A> list) {
        PageViewBuilder<A, B> builder = new PageViewBuilder<>();
        builder.list = list;
        return builder;
    }

    public PageViewBuilder<ST, TT> beforeMap(Consumer<ST> consumer) {
        this.list.forEach(consumer);
        return this;
    }

    public PageViewBuilder<ST, TT> map(Function<ST, TT> mapper) {
        this.mapper = mapper;
        return this;
    }

    public PageViewBuilder<ST, TT> sort(Comparator<TT> mapper) {
        this.comparator = mapper;
        return this;
    }

    public PageViewBuilder<ST, TT> filter(Predicate<TT> mapper) {
        this.filter = mapper;
        return this;
    }

    public PageViewBuilder<ST, TT> paging(PagingRequest paging) {
        this.paging = paging;
        return this;
    }

    public PageView<TT> done() {
        Stream<TT> stream = list.stream().map(mapper);

        if (filter != null) {
            stream = stream.filter(filter);
        }
        if (comparator != null) {
            stream = stream.sorted(comparator);
        }

        List<TT> processedList = stream.toList();

        // paging must exist since this is PageView builder.
        List<TT> finalList = processedList
                .stream()
                .skip(paging.from())
                .limit(paging.getPageSize())
                .collect(Collectors.toList());
        return new PageView<>(paging, processedList.size(), finalList);
    }
}
