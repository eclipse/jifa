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
package org.eclipse.jifa.server.support;

public interface AnalysisApiArgumentResolver {

    AnalysisApiArgumentResolver NO_ARGS = new AnalysisApiArgumentResolver() {

        private static final Object[] EMPTY = new Object[0];

        @Override
        public Object[] resolve(AnalysisApiArgumentContext context) {
            return EMPTY;
        }
    };

    Object[] resolve(AnalysisApiArgumentContext context);
}
