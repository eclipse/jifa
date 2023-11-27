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
package org.eclipse.jifa.hdp.provider;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.jifa.analysis.AbstractApiExecutor;
import org.eclipse.jifa.analysis.listener.ProgressListener;
import org.eclipse.jifa.analysis.support.MethodNameConverter;
import org.eclipse.jifa.hda.api.HeapDumpAnalyzer;
import org.eclipse.osgi.internal.location.EquinoxLocations;
import org.eclipse.osgi.service.datalocation.Location;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.function.Predicate;

@Slf4j
public class HeapDumpAnalysisApiExecutor extends AbstractApiExecutor<HeapDumpAnalyzer> {

    static {
        Map<String, String> config = new HashMap<>();
        config.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);

        String apiBase = "org.eclipse.jifa.hda.api";
        String commonBase = "org.eclipse.jifa.common";
        String analysisBase = "org.eclipse.jifa.analysis";
        String[] extras = {
                apiBase,
                commonBase,
                commonBase + ".annotation",
                commonBase + ".domain.exception",
                commonBase + ".domain.request",
                commonBase + ".domain.vo",
                commonBase + ".enums",
                commonBase + ".util",
                analysisBase,
                analysisBase + ".annotation",
                analysisBase + ".cache",
                analysisBase + ".listener",
                analysisBase + ".support",
                analysisBase + ".util",
                "net.sf.cglib.beans",
                "net.sf.cglib.core",
                "net.sf.cglib.core.internal",
                "net.sf.cglib.proxy",
                "net.sf.cglib.reflect",
                "net.sf.cglib.transform",
                "net.sf.cglib.transform.impl",
                "net.sf.cglib.util",
                "org.apache.commons.lang3"
        };
        config.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, String.join(",", extras));
        try {
            String path = System.getProperty("java.io.tmpdir");
            File dir = new File(path);
            if (!dir.exists()) {
                Files.createDirectory(Path.of(path));
            }

            String osgiWorkspace = path + "/osgi_workspace";
            String osgiConfiguration = path + "/osgi_configuration";

            if (Files.exists(Path.of(osgiWorkspace))) {
                FileUtils.deleteQuietly(new File(osgiWorkspace));
            }
            if (Files.exists(Path.of(osgiConfiguration))) {
                FileUtils.deleteQuietly(new File(osgiConfiguration));
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                FileUtils.deleteQuietly(new File(osgiWorkspace));
                FileUtils.deleteQuietly(new File(osgiConfiguration));
            }));

            config.put(EquinoxLocations.PROP_INSTANCE_AREA_DEFAULT, osgiWorkspace);
            config.put(Location.CONFIGURATION_AREA_TYPE, osgiConfiguration);

            Framework framework = ServiceLoader.load(FrameworkFactory.class).iterator().next().newFramework(config);
            framework.start();

            List<String> dependencies;
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(Objects.requireNonNull(HeapDumpAnalysisApiExecutor.class.getClassLoader().getResourceAsStream("mat-deps/list"))))) {
                dependencies = Arrays.asList(br.readLine().split(","));
            }

            List<Bundle> bundles = new ArrayList<>();

            for (String dependency : dependencies) {
                // org.eclipse.osgi is the system bundle
                if (!dependency.startsWith("org.eclipse.osgi-")) {
                    URL resource = HeapDumpAnalysisApiExecutor.class.getClassLoader().getResource("mat-deps/" + dependency);
                    try {
                        assert resource != null;
                        bundles.add(framework.getBundleContext().installBundle(resource.toString()));
                    } catch (Throwable t) {
                        log.error("Failed to install bundle: {}", dependency);
                        throw  t;
                    }
                }
            }

            List<String> validNames = new ArrayList<>();
            validNames.add("org.apache.felix.scr");
            validNames.add("org.eclipse.equinox.event");
            validNames.add("org.eclipse.jifa.hda.implementation");

            for (Bundle bundle : bundles) {
                if (validNames.contains(bundle.getSymbolicName())) {
                    log.debug("starting bundle: {}", bundle);
                    bundle.start();
                }
            }

            PROVIDER = framework.getBundleContext()
                                .getService(framework.getBundleContext()
                                                     .getServiceReference(HeapDumpAnalyzer.Provider.class));

        } catch (Throwable t) {
            if (t instanceof RuntimeException rt) {
                throw rt;
            }
            throw new RuntimeException(t);
        }
    }

    private static final HeapDumpAnalyzer.Provider PROVIDER;

    @Override
    public String namespace() {
        return "heap-dump";
    }

    @Override
    public Predicate<byte[]> matcher() {
        return new Predicate<>() {
            static final String HEADER = "JAVA PROFILE 1.0.2";

            @Override
            public boolean test(byte[] bytes) {
                return bytes.length > HEADER.length() && new String(bytes, 0, HEADER.length()).equals(HEADER);
            }
        };
    }

    @Override
    public boolean needOptionsForAnalysis(Path target) {
        return !indexFile(target).exists() && !errorLogFile(target).exists() && !isActive(target);
    }

    @Override
    public void clean(Path target) {
        super.clean(target);
        File index = indexFile(target);
        if (index.exists()) {
            if (!index.delete()) {
                log.warn("Failed to delete index file: {}", index.getAbsolutePath());
            }
        }
    }

    @Override
    protected MethodNameConverter methodNameConverter() {
        return MethodNameConverter.GETTER_METHOD;
    }

    @Override
    protected HeapDumpAnalyzer buildAnalyzer(Path target, Map<String, String> options, ProgressListener listener) {
        return PROVIDER.provide(target, options, listener);
    }

    @Override
    protected void cachedAnalyzerRemoved(HeapDumpAnalyzer heapDumpAnalyzer) {
        if (heapDumpAnalyzer != null) {
            heapDumpAnalyzer.dispose();
        }
    }

    private File indexFile(Path target) {
        String indexFileNamePrefix;
        String dumpFileName = target.toFile().getName();
        int i = dumpFileName.lastIndexOf('.');
        if (i >= 0) {
            indexFileNamePrefix = dumpFileName.substring(0, i + 1);
        } else {
            indexFileNamePrefix = dumpFileName + '.';
        }
        return target.resolveSibling(indexFileNamePrefix + "index").toFile();
    }
}
