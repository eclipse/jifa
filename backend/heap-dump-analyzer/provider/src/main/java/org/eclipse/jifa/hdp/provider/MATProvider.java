/********************************************************************************
 * Copyright (c) 2021, 2022 Contributors to the Eclipse Foundation
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

package org.eclipse.jifa.hdp.provider;

import org.eclipse.jifa.common.JifaException;
import org.eclipse.jifa.common.listener.ProgressListener;
import org.eclipse.jifa.hda.api.HeapDumpAnalyzer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;

public class MATProvider implements HeapDumpAnalyzer.Provider {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private HeapDumpAnalyzer.Provider provider;

    public MATProvider() {
        init();
    }

    @Override
    public HeapDumpAnalyzer provide(Path path, Map<String, String> arguments, ProgressListener listener) {
        return provider.provide(path, arguments, listener);
    }

    private void init() {
        Map<String, String> config = new HashMap<>();
        config.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);

        String apiBase = "org.eclipse.jifa.hda.api";
        String commonBase = "org.eclipse.jifa.common";
        String[] extras = {
            apiBase,
            commonBase,
            commonBase + ".aux",
            commonBase + ".enums",
            commonBase + ".request",
            commonBase + ".util",
            commonBase + ".cache",
            commonBase + ".vo",
            commonBase + ".vo.support",
            commonBase + ".listener",
            "net.sf.cglib.beans",
            "net.sf.cglib.core",
            "net.sf.cglib.core.internal",
            "net.sf.cglib.proxy",
            "net.sf.cglib.reflect",
            "net.sf.cglib.transform",
            "net.sf.cglib.transform.impl",
            "net.sf.cglib.util",
        };
        config.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, String.join(",", extras));
        try {
            Framework framework = ServiceLoader.load(FrameworkFactory.class).iterator().next().newFramework(config);
            framework.start();

            File[] files = Objects.requireNonNull(new File(System.getProperty("mat-deps")).listFiles());
            List<Bundle> bundles = new ArrayList<>();
            for (File file : files) {
                String name = file.getName();
                // org.eclipse.osgi is the system bundle
                if (name.endsWith(".jar") && !name.startsWith("org.eclipse.osgi-")) {
                    Bundle b = framework.getBundleContext().installBundle(file.toURI().toString());
                    bundles.add(b);
                }
            }

            List<String> validNames = new ArrayList<>();
            validNames.add("org.apache.felix.scr");
            validNames.add("org.eclipse.equinox.event");
            validNames.add("org.eclipse.jifa.hda.implementation");

            for (Bundle bundle : bundles) {
                if (validNames.contains(bundle.getSymbolicName())) {
                    LOGGER.info("starting bundle: {}", bundle);
                    bundle.start();
                }
            }

            provider = framework.getBundleContext()
                                .getService(framework.getBundleContext()
                                                     .getServiceReference(HeapDumpAnalyzer.Provider.class));

        } catch (BundleException be) {
            throw new JifaException(be);
        }
    }
}
