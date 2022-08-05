package org.eclipse.jifa.gclog.diagnoser;

import java.util.HashMap;
import java.util.Map;

public class AbnormalType {
    // order these members by their importance
    // Whenever a new type is added, add its default suggestions to DefaultSuggestionGenerator
    public static AbnormalType OUT_OF_MEMORY = new AbnormalType("Out of memory");
    public static AbnormalType ALLOCATION_STALL = new AbnormalType("Allocation stall");
    public static AbnormalType METASPACE_FULL_GC = new AbnormalType("Metaspace full gc");
    public static AbnormalType SYSTEM_GC = new AbnormalType("System.gc()");
    public static AbnormalType HEAP_MEMORY_FULL_GC = new AbnormalType("Heap memory full gc");
    public static AbnormalType LONG_YOUNG_GC_PAUSE = new AbnormalType("Long young gc pause");
    public static AbnormalType LAST_TYPE = new AbnormalType("Last type");


    private String name;
    private int ordinal;

    private static Map<String, AbnormalType> name2Type = new HashMap<>();

    private AbnormalType(String name) {
        this.name = name;
        ordinal = name2Type.size();
        name2Type.put(name, this);
    }

    public static AbnormalType getType(String name) {
        return name2Type.getOrDefault(name, null);
    }

    public String getName() {
        return name;
    }

    public int getOrdinal() {
        return ordinal;
    }
}
