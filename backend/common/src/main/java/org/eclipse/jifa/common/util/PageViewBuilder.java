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

import org.eclipse.jifa.common.request.PagingRequest;
import org.eclipse.jifa.common.vo.PageView;
import org.eclipse.jifa.common.vo.support.Searchable;

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

public class PageViewBuilder<A, B extends Searchable> {

    // simple builder
    public static <S, R> PageView<R> build(Callback<S> callback, PagingRequest paging, Function<S, R> mapper) {
        List<R> result = IntStream.range(paging.from(), paging.to(callback.totalSize()))
                .mapToObj(callback::get)
                .map(mapper)
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

    public static <S, T, R> PageView<R> build(Collection<S> total, PagingRequest paging, Function<S, T> mapper1,
                                           Function<T, R> mapper2,
                                           Comparator<T> comparator) {
        List<R> result = total.stream()
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
    private List<A> list;
    private Function<A, B> mapper;
    private PagingRequest paging;
    private Comparator<B> comparator;
    private Predicate<B> filter;
    private boolean noPagingNeeded;

    private PageViewBuilder() {

    }

    public static <A, B extends Searchable> PageViewBuilder<A, B> fromList(List<A> list) {
        PageViewBuilder<A, B> builder = new PageViewBuilder<>();
        builder.list = list;
        return builder;
    }

    public PageViewBuilder<A, B> beforeMap(Consumer<A> consumer) {
        this.list.forEach(consumer);
        return this;
    }

    public PageViewBuilder<A, B> map(Function<A, B> mapper) {
        this.mapper = mapper;
        return this;
    }

    public PageViewBuilder<A, B> sort(Comparator<B> mapper) {
        this.comparator = mapper;
        return this;
    }

    public PageViewBuilder<A, B> filter(Predicate<B> mapper) {
        this.filter = mapper;
        return this;
    }

    public PageViewBuilder<A, B> paging(PagingRequest paging) {
        this.paging = paging;
        return this;
    }

    public PageView<B> done() {
        Stream<B> stream = list.stream().map(mapper);

        if (filter != null) {
            stream = stream.filter(filter);
        }
        if (comparator != null) {
            stream = stream.sorted(comparator);
        }
        List<B> processedList = stream.collect(Collectors.toList());
        // paging must be exist since this is PAGEVIEW builder.
        List<B> finalList = processedList
                .stream()
                .skip(paging.from())
                .limit(paging.getPageSize())
                .collect(Collectors.toList());
        return new PageView<>(paging,/*totalSize*/ processedList.size(), /*display list*/finalList);
    }
}
