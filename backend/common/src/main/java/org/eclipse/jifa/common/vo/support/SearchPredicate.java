/********************************************************************************
 * Copyright (c) 2020, 2021 Contributors to the Eclipse Foundation
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

package org.eclipse.jifa.common.vo.support;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Predicate;

public class SearchPredicate {
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchPredicate.class);

    public static <T extends Searchable> Predicate<T> createPredicate(String searchText, SearchType searchType) {
        if (searchText == null || searchType == null) {
            return null;
        }
        return (T record) -> {
            try {
                // don't filter any items if search content is empty
                if (searchText.isEmpty()) {
                    return true;
                }

                switch (searchType) {
                    // string comparing
                    case BY_NAME:
                    case BY_CONTEXT_CLASSLOADER_NAME: {
                        return ((String) record.getBySearchType(searchType)).matches(searchText);
                    }
                    // double comparing
                    case BY_PERCENT: {
                        String prefix = extractPrefix(searchText);
                        double num = Double.parseDouble(extractNumberText(searchText));
                        num /= 100; // [1.80]% => 0.0180
                        switch (prefix) {
                            case "==":
                                return Double.compare((double) record.getBySearchType(searchType), num) == 0;
                            case ">=":
                                return (double) record.getBySearchType(searchType) >= num;
                            case "<=":
                                return (double) record.getBySearchType(searchType) <= num;
                            case ">":
                                return (double) record.getBySearchType(searchType) > num;
                            case "<":
                                return (double) record.getBySearchType(searchType) < num;
                            case "!=":
                                return Double.compare((double) record.getBySearchType(searchType), num) != 0;
                            default: {
                                return false;
                            }
                        }
                    }
                    // long comparing
                    default: {
                        String prefix = extractPrefix(searchText);
                        long num = Long.parseLong(extractNumberText(searchText));
                        switch (prefix) {
                            case "==":
                                return (long) record.getBySearchType(searchType) == num;
                            case ">=":
                                return (long) record.getBySearchType(searchType) >= num;
                            case "<=":
                                return (long) record.getBySearchType(searchType) <= num;
                            case ">":
                                return (long) record.getBySearchType(searchType) > num;
                            case "<":
                                return (long) record.getBySearchType(searchType) < num;
                            case "!=":
                                return (long) record.getBySearchType(searchType) != num;
                            default: {
                                return false;
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
                LOGGER.debug("unexpected exception when search `" + searchText + "` with type " + searchType.name());
            }
            return false;
        };
    }

    private static String extractPrefix(String text) {
        if (StringUtils.isNumeric(text)) {
            return "==";
        }

        String prefix = "";
        prefix += text.charAt(0);
        if (text.charAt(1) == '=') {
            prefix += "=";
            return prefix;
        }

        return prefix;
    }

    private static String extractNumberText(String text) {
        for (int i = 0; i < 3; i++) {
            if (Character.isDigit(text.charAt(i))) {
                return text.substring(i);
            }
        }
        return "";
    }
}
