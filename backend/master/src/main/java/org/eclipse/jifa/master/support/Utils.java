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

package org.eclipse.jifa.master.support;

/**
 * Commonly used across master side
 */
public class Utils {

    // TODO: current algorithm used isn't good enough
    public static long calculateLoadFromSize(double size) {
        double G = 1024 * 1024 * 1024;

        long load = (long) Math.ceil(size / G) * 10;
        load = Math.max(load, 10);
        load = Math.min(load, 900);

        return load;
    }

    // Roughly a reverse operation of calculateLoad
    public static double calculateSizeFromLoad(long size) {
        long estimateLoad = size;
        estimateLoad = Math.max(10, estimateLoad);
        return estimateLoad / 10.0;
    }
}
