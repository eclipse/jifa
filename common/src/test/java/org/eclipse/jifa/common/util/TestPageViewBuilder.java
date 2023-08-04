/********************************************************************************
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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

import com.google.common.collect.Lists;
import org.eclipse.jifa.common.domain.request.PagingRequest;
import org.eclipse.jifa.common.domain.vo.PageView;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestPageViewBuilder {

    @Test
    public void test() {
        {
            List<Integer> list = new ArrayList<>();
            list.add(1);
            list.add(2);
            PageView<Integer> pv = PageViewBuilder.build(new PageViewBuilder.Callback<Integer>() {
                @Override
                public int totalSize() {
                    return list.size();
                }

                @Override
                public Integer get(int index) {
                    return list.get(index);
                }
            }, new PagingRequest(1, 1), i -> i + 1);

            assertEquals(2, pv.getTotalSize());
            assertEquals(1, pv.getPage());
            assertEquals(1, pv.getPageSize());
            assertEquals(1, pv.getData().size());
            assertEquals(2, pv.getData().get(0));
        }

        {
            List<String> list = new ArrayList<>();
            list.add("a");
            list.add("b");
            list.add("c");
            PageView<String> pv = PageViewBuilder.build(list, new PagingRequest(1, 2));

            assertEquals(3, pv.getTotalSize());
            assertEquals(2, pv.getData().size());
        }

        {
            List<String> list = new ArrayList<>();
            list.add("a");
            list.add("b");
            list.add("c");
            PageView<String> stringPageView = PageViewBuilder.build(list, new PagingRequest(1, 3), s -> s + s);

            assertEquals(3, stringPageView.getTotalSize());
            assertEquals(3, stringPageView.getData().size());
            assertEquals("aa", stringPageView.getData().get(0));
            assertEquals("bb", stringPageView.getData().get(1));
            assertEquals("cc", stringPageView.getData().get(2));
        }

        {
            List<Integer> list = new ArrayList<>();
            list.add(3);
            list.add(2);
            list.add(1);
            PageView<Integer> pv = PageViewBuilder.build(list, new PagingRequest(1, 3), i -> i + 1, i -> i * 2, Integer::compareTo);

            assertEquals(4, pv.getData().get(0));
            assertEquals(6, pv.getData().get(1));
            assertEquals(8, pv.getData().get(2));
        }

        {
            int[] ints = {6, 5, 4, 3, 2, 1};
            PageView<Integer> pv = PageViewBuilder.build(ints, new PagingRequest(1, 4), i -> i * i);

            assertEquals(6, pv.getTotalSize());
            assertEquals(36, pv.getData().get(0));
            assertEquals(25, pv.getData().get(1));
            assertEquals(16, pv.getData().get(2));
            assertEquals(9, pv.getData().get(3));
        }

        {
            String[] strings = {"6", "5", "4", "3", "2", "1"};
            PageView<String> pv = PageViewBuilder.build(strings, new PagingRequest(1, 2), i -> i + i);

            assertEquals(6, pv.getTotalSize());
            assertEquals("66", pv.getData().get(0));
            assertEquals("55", pv.getData().get(1));
        }

        {
            List<String> list = Lists.newArrayList("a", "b", "c");
            StringBuilder sb = new StringBuilder();
            PageView<String> pv = PageViewBuilder.<String, String>fromList(list)
                                                 .beforeMap(sb::append)
                                                 .map(s -> s + s)
                                                 .paging(new PagingRequest(1, 2))
                                                 .done();
            assertEquals("abc", sb.toString());
            assertEquals(3, pv.getTotalSize());
            assertEquals("aa", pv.getData().get(0));
            assertEquals("bb", pv.getData().get(1));
        }

        {
            List<String> list = Lists.newArrayList("a", "b", "c");
            StringBuilder sb = new StringBuilder();
            PageView<String> pv = PageViewBuilder.<String, String>fromList(list)
                                                 .beforeMap(sb::append)
                                                 .map(s -> s + s)
                                                 .filter(s -> !s.equals("aa"))
                                                 .sort(Comparator.reverseOrder())
                                                 .paging(new PagingRequest(1, 2))
                                                 .done();
            assertEquals("abc", sb.toString());
            assertEquals(2, pv.getTotalSize());
            assertEquals("cc", pv.getData().get(0));
            assertEquals("bb", pv.getData().get(1));
        }
    }
}
