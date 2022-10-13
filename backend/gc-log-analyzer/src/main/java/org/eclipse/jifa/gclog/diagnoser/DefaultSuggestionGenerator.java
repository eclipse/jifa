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
package org.eclipse.jifa.gclog.diagnoser;

import org.eclipse.jifa.common.JifaException;
import org.eclipse.jifa.common.util.ErrorUtil;
import org.eclipse.jifa.gclog.model.GCModel;
import org.eclipse.jifa.gclog.util.I18nStringView;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.jifa.gclog.diagnoser.SuggestionType.*;

// This class generates common suggestions when we can not find the exact cause of problem.
public class DefaultSuggestionGenerator extends SuggestionGenerator {
    private AbnormalPoint ab;

    public DefaultSuggestionGenerator(GCModel model, AbnormalPoint ab) {
        super(model);
        this.ab = ab;
    }

    private static Map<AbnormalType, Method> rules = new HashMap<>();

    static {
        initializeRules();
    }

    private static void initializeRules() {
        Method[] methods = DefaultSuggestionGenerator.class.getDeclaredMethods();
        for (Method method : methods) {
            GeneratorRule annotation = method.getAnnotation(GeneratorRule.class);
            if (annotation != null) {
                method.setAccessible(true);
                int mod = method.getModifiers();
                if (Modifier.isAbstract(mod) || Modifier.isFinal(mod)) {
                    throw new JifaException("Illegal method modifier: " + method);
                }
                rules.put(AbnormalType.getType(annotation.value()), method);
            }
        }
    }

    @GeneratorRule("metaspaceFullGC")
    private void metaspaceFullGC() {
        addSuggestion(CHECK_METASPACE);
        addSuggestion(ENLARGE_METASPACE);
        fullGCSuggestionCommon();
    }

    @GeneratorRule("systemGC")
    private void systemGC() {
        addSuggestion(CHECK_SYSTEM_GC);
        addSuggestion(DISABLE_SYSTEM_GC);
        suggestOldSystemGC();
        fullGCSuggestionCommon();
    }

    @GeneratorRule("outOfMemory")
    private void outOfMemory() {
        addSuggestion(CHECK_MEMORY_LEAK);
        suggestEnlargeHeap(false);
    }

    @GeneratorRule("allocationStall")
    private void allocationStall() {
        addSuggestion(CHECK_MEMORY_LEAK);
        suggestEnlargeHeap(true);
        addSuggestion(INCREASE_CONC_GC_THREADS);
        addSuggestion(INCREASE_Z_ALLOCATION_SPIKE_TOLERANCE);
    }

    @GeneratorRule("heapMemoryFullGC")
    private void heapMemoryFullGC() {
        addSuggestion(CHECK_MEMORY_LEAK);
        addSuggestion(CHECK_FAST_PROMOTION);
        suggestStartOldGCEarly();
        fullGCSuggestionCommon();
    }

    @GeneratorRule("longYoungGCPause")
    private void longYoungGCPause() {
        addSuggestion(CHECK_LIVE_OBJECTS);
        addSuggestion(CHECK_CPU_TIME);
        addSuggestion(CHECK_REFERENCE_GC);
        suggestCheckEvacuationFailure();
        suggestShrinkYoungGen();
        suggestUseMoreDetailedLogging();
    }

    @GeneratorRule("frequentYoungGC")
    private void frequentYoungGC() {
        suggestExpandYoungGen();
        addSuggestion(CHECK_FAST_OBJECT_ALLOCATION);
    }

    @GeneratorRule("longG1Remark")
    private void longG1Remark() {
        addSuggestion(CHECK_REFERENCE_GC);
        addSuggestion(CHECK_CLASS_UNLOADING);
        suggestUseMoreDetailedLogging();
    }

    @GeneratorRule("longCMSRemark")
    private void longCMSRemark() {
        addSuggestion(CHECK_RESCAN);
        addSuggestion(CHECK_REFERENCE_GC);
        addSuggestion(CHECK_CLASS_UNLOADING);
        suggestUseMoreDetailedLogging();
    }

    public List<I18nStringView> generate() {
        if (ab.getType() == null) {
            return result;
        }
        Method rule = rules.getOrDefault(ab.getType(), null);
        if (rule != null) {
            try {
                rule.invoke(this);
            } catch (Exception e) {
                ErrorUtil.shouldNotReachHere();
            }
        }
        return result;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    private @interface GeneratorRule {
        String value();
    }
}
