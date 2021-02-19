/********************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.hda.impl;

import org.eclipse.jifa.hda.api.HeapDumpAnalyzer;
import org.eclipse.mat.snapshot.SnapshotFactory;
import org.eclipse.mat.util.VoidProgressListener;

import java.io.File;
import java.util.Map;

public class HeapDumpAnalyzerImpl implements HeapDumpAnalyzer {

    @Override
    public void open(File file, Map<String, String> arguments) {
        try {
            SnapshotFactory.openSnapshot(file, arguments, new VoidProgressListener());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
