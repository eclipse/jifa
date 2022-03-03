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

package org.eclipse.mat.hprof.extension;

import org.eclipse.mat.hprof.ui.HprofPreferences;

public final class HprofPreferencesAccess {

    private static HprofPreferences.HprofStrictness parseStrictness(String strictness) {
        if (strictness == null) {
            return HprofPreferences.DEFAULT_STRICTNESS;
        }
        switch (strictness) {
            case "warn":
                return HprofPreferences.HprofStrictness.STRICTNESS_WARNING;
            case "permissive":
                return HprofPreferences.HprofStrictness.STRICTNESS_PERMISSIVE;
            default:
                return HprofPreferences.DEFAULT_STRICTNESS;
        }
    }

    public static void setStrictness(String strictness) {
        HprofPreferences.setStrictness(parseStrictness(strictness));
    }
}
