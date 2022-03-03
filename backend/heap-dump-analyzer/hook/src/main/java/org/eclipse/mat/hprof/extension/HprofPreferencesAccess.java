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
        System.out.println("set " + parseStrictness(strictness));
        HprofPreferences.setStrictness(parseStrictness(strictness));
    }
}
