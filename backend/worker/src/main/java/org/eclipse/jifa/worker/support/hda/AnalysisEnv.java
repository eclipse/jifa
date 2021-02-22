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
package org.eclipse.jifa.worker.support.hda;

import org.eclipse.jifa.common.aux.JifaException;
import org.eclipse.jifa.hda.api.AnalysisContext;
import org.eclipse.jifa.hda.api.HeapDumpAnalyzer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;

public class AnalysisEnv {

    public static boolean INITIALIZED;

    public static HeapDumpAnalyzer<AnalysisContext> HEAP_DUMP_ANALYZER;

    static {
        init();
        INITIALIZED = true;
    }

    private static void init() {
        ServiceLoader<FrameworkFactory> factoryLoader = ServiceLoader.load(FrameworkFactory.class);
        Map<String, String> m = new HashMap<>();
        m.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);

        String apiBase = "org.eclipse.jifa.hda.api";
        String commonBase = "org.eclipse.jifa.common";
        String[] extras = {
            apiBase,
            commonBase,
            commonBase + ".aux",
            commonBase + ".enums",
            commonBase + ".request",
            commonBase + ".util",
            commonBase + ".vo",
            "org.eclipse.jifa.worker.route",
            "org.eclipse.jifa.common.vo.support",
        };
        m.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, String.join(",", extras));
        Framework framework = factoryLoader.iterator().next().newFramework(m);

        try {
            framework.start();

            File[] files = Objects.requireNonNull(new File(System.getProperty("mat-deps")).listFiles());
            List<Bundle> bundles = new ArrayList<>();
            for (File file : files) {
                String name = file.getName();
                // org.eclipse.osgi is the system bundle
                if (name.endsWith(".jar") && !name.contains("org.eclipse.osgi_")) {
                    Bundle b = framework.getBundleContext().installBundle(file.toURI().toString());
                    bundles.add(b);
                }
            }

            for (Bundle bundle : bundles) {
                bundle.start();
            }
        } catch (BundleException be) {
            throw new JifaException(be);
        }
        //noinspection unchecked
        HEAP_DUMP_ANALYZER =
            framework.getBundleContext()
                     .getService(framework.getBundleContext().getServiceReference(HeapDumpAnalyzer.class));
        assert HEAP_DUMP_ANALYZER != null;
    }

}
