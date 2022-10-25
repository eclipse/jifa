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
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.util.Hashtable;

public class Activator implements BundleActivator {

    @Override
    public void start(BundleContext bundleContext) {
        bundleContext
            .registerService(HeapDumpAnalyzer.Provider.class, HeapDumpAnalyzerImpl.PROVIDER, new Hashtable<>());
    }

    @Override
    public void stop(BundleContext bundleContext) {
    }
}
