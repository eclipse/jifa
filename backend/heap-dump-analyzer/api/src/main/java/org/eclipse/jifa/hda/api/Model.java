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

package org.eclipse.jifa.hda.api;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.eclipse.jifa.common.util.ErrorUtil;
import org.eclipse.jifa.common.vo.PageView;
import org.eclipse.jifa.common.vo.support.SearchType;
import org.eclipse.jifa.common.vo.support.Searchable;
import org.eclipse.jifa.common.vo.support.SortTableGenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public interface Model {

    interface DominatorTree {
        interface ItemType {
            int CLASS = 1;
            int CLASS_LOADER = 2;
            int SUPER_CLASS = 5;
            int PACKAGE = 6;
        }

        enum Grouping {

            NONE,

            BY_CLASS,

            BY_CLASSLOADER,

            BY_PACKAGE;
        }

        @Data
        class Item {
            public String label;

            public String suffix;

            public int objectId;

            public int objectType;

            public boolean gCRoot;

            public long shallowSize;

            public long retainedSize;

            public double percent;

            public boolean isObjType = true;
        }

        @Data
        @EqualsAndHashCode(callSuper = true)
        class ClassLoaderItem extends Item implements Searchable {
            private static Map<String, Comparator<ClassLoaderItem>> sortTable =
                new SortTableGenerator<ClassLoaderItem>()
                    .add("id", ClassLoaderItem::getObjectId)
                    .add("shallowHeap", ClassLoaderItem::getShallowSize)
                    .add("retainedHeap", ClassLoaderItem::getRetainedSize)
                    .add("percent", ClassLoaderItem::getPercent)
                    .add("Objects", ClassLoaderItem::getObjects)
                    .build();

            public long objects;
            private int[] objectIds;

            public static Comparator<ClassLoaderItem> sortBy(String field, boolean ascendingOrder) {
                return ascendingOrder ? sortTable.get(field) : sortTable.get(field).reversed();
            }

            @Override
            public Object getBySearchType(SearchType type) {
                switch (type) {
                    case BY_NAME:
                        return getLabel();
                    case BY_PERCENT:
                        return getPercent();
                    case BY_OBJ_NUM:
                        return getObjects();
                    case BY_RETAINED_SIZE:
                        return getRetainedSize();
                    case BY_SHALLOW_SIZE:
                        return getShallowSize();
                    default:
                        ErrorUtil.shouldNotReachHere();
                }
                return null;
            }
        }

        @Data
        @EqualsAndHashCode(callSuper = true)
        class ClassItem extends Item implements Searchable {
            private static Map<String, Comparator<ClassItem>> sortTable = new SortTableGenerator<ClassItem>()
                .add("id", ClassItem::getObjectId)
                .add("shallowHeap", ClassItem::getShallowSize)
                .add("retainedHeap", ClassItem::getRetainedSize)
                .add("percent", ClassItem::getPercent)
                .add("Objects", ClassItem::getObjects)
                .build();
            private int objects;
            private int[] objectIds;

            public static Comparator<ClassItem> sortBy(String field, boolean ascendingOrder) {
                return ascendingOrder ? sortTable.get(field) : sortTable.get(field).reversed();
            }

            @Override
            public Object getBySearchType(SearchType type) {
                switch (type) {
                    case BY_NAME:
                        return getLabel();
                    case BY_PERCENT:
                        return getPercent();
                    case BY_OBJ_NUM:
                        return getObjects();
                    case BY_RETAINED_SIZE:
                        return getRetainedSize();
                    case BY_SHALLOW_SIZE:
                        return getShallowSize();
                    default:
                        ErrorUtil.shouldNotReachHere();
                }
                return null;
            }
        }

        @Data
        @EqualsAndHashCode(callSuper = true)
        class DefaultItem extends Item implements Searchable {
            private static Map<String, Comparator<DefaultItem>> sortTable = new SortTableGenerator<DefaultItem>()
                .add("id", DefaultItem::getObjectId)
                .add("shallowHeap", DefaultItem::getShallowSize)
                .add("retainedHeap", DefaultItem::getRetainedSize)
                .add("percent", DefaultItem::getPercent)
                .build();

            public static Comparator<DefaultItem> sortBy(String field, boolean ascendingOrder) {
                return ascendingOrder ? sortTable.get(field) : sortTable.get(field).reversed();
            }

            @Override
            public Object getBySearchType(SearchType type) {
                switch (type) {
                    case BY_NAME:
                        return getLabel();
                    case BY_PERCENT:
                        return getPercent();
                    case BY_OBJ_NUM:
                        return null;
                    case BY_RETAINED_SIZE:
                        return getRetainedSize();
                    case BY_SHALLOW_SIZE:
                        return getShallowSize();
                    default:
                        ErrorUtil.shouldNotReachHere();
                }
                return null;
            }
        }

        @Data
        @EqualsAndHashCode(callSuper = true)
        class PackageItem extends Item implements Searchable {
            private static Map<String, Comparator<PackageItem>> sortTable = new SortTableGenerator<PackageItem>()
                .add("id", PackageItem::getObjectId)
                .add("shallowHeap", PackageItem::getShallowSize)
                .add("retainedHeap", PackageItem::getRetainedSize)
                .add("percent", PackageItem::getPercent)
                .add("Objects", PackageItem::getObjects)
                .build();
            private long objects;
            private int[] objectIds;

            public static Comparator<PackageItem> sortBy(String field, boolean ascendingOrder) {
                return ascendingOrder ? sortTable.get(field) : sortTable.get(field).reversed();
            }

            @Override
            public Object getBySearchType(SearchType type) {
                switch (type) {
                    case BY_NAME:
                        return getLabel();
                    case BY_PERCENT:
                        return getPercent();
                    case BY_OBJ_NUM:
                        return getObjects();
                    case BY_RETAINED_SIZE:
                        return getRetainedSize();
                    case BY_SHALLOW_SIZE:
                        return getShallowSize();
                    default:
                        ErrorUtil.shouldNotReachHere();
                }
                return null;
            }
        }
    }

    interface Histogram {
        enum Grouping {

            BY_CLASS,

            BY_SUPERCLASS,

            BY_CLASSLOADER,

            BY_PACKAGE;
        }

        interface ItemType {
            int CLASS = 1;
            int CLASS_LOADER = 2;
            int SUPER_CLASS = 5;
            int PACKAGE = 6;
        }

        @Data
        @NoArgsConstructor
        class Item implements Searchable {
            private static Map<String, Comparator<Item>> sortTable = new SortTableGenerator<Item>()
                .add("id", Item::getObjectId)
                .add("numberOfObjects", Item::getNumberOfObjects)
                .add("shallowSize", Item::getShallowSize)
                .add("retainedSize", Item::getRetainedSize)
                .build();
            public long numberOfObjects;
            public long shallowSize;
            public long retainedSize;
            public String label;
            public int objectId;
            public int type;

            public Item(int objectId, String label, int type, long numberOfObjects, long shallowSize,
                        long retainedSize) {
                this.objectId = objectId;
                this.label = label;
                this.type = type;
                this.numberOfObjects = numberOfObjects;
                this.shallowSize = shallowSize;
                this.retainedSize = retainedSize;
            }

            public static Comparator<Item> sortBy(String field, boolean ascendingOrder) {
                return ascendingOrder ? sortTable.get(field) : sortTable.get(field).reversed();
            }

            @Override
            public Object getBySearchType(SearchType type) {
                switch (type) {
                    case BY_NAME:
                        return getLabel();
                    case BY_OBJ_NUM:
                        return getNumberOfObjects();
                    case BY_RETAINED_SIZE:
                        return getRetainedSize();
                    case BY_SHALLOW_SIZE:
                        return getShallowSize();
                    default:
                        ErrorUtil.shouldNotReachHere();
                }
                return null;
            }
        }
    }

    interface DuplicatedClass {

        @Data
        class ClassItem implements Searchable {
            public String label;

            public int count;

            @Override
            public Object getBySearchType(SearchType type) {
                switch (type) {
                    case BY_NAME:
                        return getLabel();
                    case BY_CLASSLOADER_COUNT:
                        return (long) getCount();
                    default:
                        ErrorUtil.shouldNotReachHere();
                }
                return null;
            }
        }

        @Data
        class ClassLoaderItem {

            public String label;

            public String suffix;

            public int definedClassesCount;

            public int instantiatedObjectsCount;

            public int objectId;

            public boolean gCRoot;
        }
    }

    interface Thread {
        @Data
        class Summary {
            public long totalSize;

            public long shallowHeap;

            public long retainedHeap;
        }

        @Data
        class Item implements Searchable {

            public static Map<String, Comparator<Item>> sortTable = new SortTableGenerator<Item>()
                .add("id", Item::getObjectId)
                .add("shallowHeap", Item::getShallowSize)
                .add("retainedHeap", Item::getRetainedSize)
                .add("daemon", Item::isDaemon)
                .add("contextClassLoader", Item::getContextClassLoader)
                .add("name", Item::getName)
                .build();
            public int objectId;
            public String object;
            public String name;
            public long shallowSize;
            public long retainedSize;
            public String contextClassLoader;
            public boolean hasStack;
            public boolean daemon;

            public Item(int objectId, String object, String name, long shallowSize, long retainedSize,
                        String contextClassLoader, boolean hasStack, boolean daemon) {
                this.objectId = objectId;
                this.object = object;
                this.name = name;
                this.shallowSize = shallowSize;
                this.retainedSize = retainedSize;
                this.contextClassLoader = contextClassLoader;
                this.hasStack = hasStack;
                this.daemon = daemon;
            }

            public Item() {}

            public static Comparator<Item> sortBy(String field, boolean ascendingOrder) {
                return ascendingOrder ? sortTable.get(field) : sortTable.get(field).reversed();
            }

            @Override
            public Object getBySearchType(SearchType type) {
                switch (type) {
                    case BY_NAME:
                        return getName();
                    case BY_SHALLOW_SIZE:
                        return getShallowSize();
                    case BY_RETAINED_SIZE:
                        return getRetainedSize();
                    case BY_CONTEXT_CLASSLOADER_NAME:
                        return getContextClassLoader();
                    default:
                        ErrorUtil.shouldNotReachHere();
                }
                return null;
            }
        }

        @Data
        @EqualsAndHashCode(callSuper = true)
        class LocalVariable extends JavaObject {
        }

        @Data
        class StackFrame {

            public String stack;

            public boolean hasLocal;

            public boolean firstNonNativeFrame;

            public long maxLocalsRetainedSize;

            public StackFrame(String stack, boolean hasLocal, long maxLocalsRetainedSize) {
                this.stack = stack;
                this.hasLocal = hasLocal;
                this.maxLocalsRetainedSize = maxLocalsRetainedSize;
            }
        }
    }

    interface CalciteSQLResult {

        int TREE = 1;

        int TABLE = 2;

        int TEXT = 3;

        int getType();

        @Data
        class TableResult implements CalciteSQLResult {
            public int type = TABLE;

            public List<String> columns;

            public PageView<Entry> pv;

            public TableResult(List<String> columns, PageView<Entry> pv) {
                this.columns = columns;
                this.pv = pv;
            }

            @Data
            public static class Entry {

                public int objectId;

                public List<Object> values;

                public Entry(int objectId, List<Object> values) {
                    this.objectId = objectId;
                    this.values = values;
                }
            }

        }

        @Data
        class TextResult implements CalciteSQLResult {

            public int type = CalciteSQLResult.TEXT;

            public String text;

            public TextResult(String text) {
                this.text = text;
            }
        }

        @Data
        class TreeResult implements CalciteSQLResult {

            public PageView<JavaObject> pv;

            public int type = TREE;

            public TreeResult(PageView<JavaObject> pv) {
                this.pv = pv;
            }
        }

    }

    interface OQLResult {

        int TREE = 1;

        int TABLE = 2;

        int TEXT = 3;

        int getType();

        @Data
        class TableResult implements OQLResult {
            public int type = TABLE;

            public List<String> columns;

            public PageView<Entry> pv;

            public TableResult(List<String> columns, PageView<Entry> pv) {
                this.columns = columns;
                this.pv = pv;
            }

            @Data
            public static class Entry {

                public int objectId;

                public List<Object> values;

                public Entry(int objectId, List<Object> values) {
                    this.objectId = objectId;
                    this.values = values;
                }
            }

        }

        @Data
        class TextResult implements OQLResult {

            public int type = OQLResult.TEXT;

            public String text;

            public TextResult(String text) {
                this.text = text;
            }
        }

        @Data
        class TreeResult implements OQLResult {

            public PageView<JavaObject> pv;

            public int type = TREE;

            public TreeResult(PageView<JavaObject> pv) {
                this.pv = pv;
            }
        }

    }

    interface GCRootPath {

        List<String> EXCLUDES = Arrays.asList("java.lang.ref.WeakReference:referent",
                                              "java.lang.ref.SoftReference:referent");

        enum Grouping {
            FROM_GC_ROOTS,
            FROM_GC_ROOTS_BY_CLASS,
            FROM_OBJECTS_BY_CLASS
        }

        @Data
        class MergePathToGCRootsTreeNode {

            public int objectId;

            public String className;

            public int refObjects;

            public long shallowHeap;

            public long refShallowHeap;

            public long retainedHeap;

            public String suffix;

            public int objectType;

            public boolean gCRoot;
        }

        @Data
        class Item {

            public Node tree;

            public int count;

            public boolean hasMore;

        }

        @Data
        @EqualsAndHashCode(callSuper = true)
        class Node extends JavaObject {

            public boolean origin;

            public List<Node> children = new ArrayList<>();

            public void addChild(Node child) {
                children.add(child);
            }

            public Node getChild(int objectId) {
                for (Node child : children) {
                    if (child.getObjectId() == objectId) {
                        return child;
                    }
                }
                return null;
            }
        }
    }

    interface ClassReferrer {
        interface Type {
            int NEW = 0;
            int MIXED = 1;
            int OLD_FAD = 2;
        }

        @Data
        class Item {

            public String label;

            public int objects;

            public long shallowSize;

            public int objectId;

            public int[] objectIds;

            public int type;
        }
    }

    interface Comparison {

        @Data
        class Summary {

            public int totalSize;

            public long objects;

            public long shallowSize;
        }

        @Data
        class Item {

            public String className;

            public long objects;

            public long shallowSize;
        }
    }

    interface TheString {
        @Data
        class Item {
            public int objectId;
            public String label;
            public long shallowSize;
            public long retainedSize;
        }
    }

    interface GCRoot {
        @Data
        class Item {

            public String className;

            public int objects;

            public int objectId;

            public long shallowSize;

            public long retainedSize;
        }
    }

    interface DirectByteBuffer {
        @Data
        class Item {

            public int objectId;

            public String label;

            public int position;

            public int limit;

            public int capacity;
        }

        @Data
        class Summary {

            public int totalSize;

            public long position;

            public long limit;

            public long capacity;
        }
    }

    interface UnreachableObject {

        @Data
        class Item {

            public int objectId;

            public String className;

            public int objects;

            public long shallowSize;
        }

        @Data
        class Summary {

            public int totalSize;

            public int objects;

            public long shallowSize;
        }
    }

    interface Overview {

        @Data
        class BigObject {

            public String label;

            public int objectId;

            public double value;

            public String description;

            public BigObject(String label, int objectId, double value, String description) {
                this.label = label;
                this.objectId = objectId;
                this.value = value;
                this.description = description;
            }
        }

        @Data
        class Details {

            public String jvmInfo;

            public int identifierSize;

            public long creationDate;

            public int numberOfObjects;

            public int numberOfGCRoots;

            public int numberOfClasses;

            public int numberOfClassLoaders;

            public long usedHeapSize;

            public boolean generationInfoAvailable;

            public Details(String jvmInfo, int identifierSize, long creationDate, int numberOfObjects,
                           int numberOfGCRoots,
                           int numberOfClasses, int numberOfClassLoaders, long usedHeapSize,
                           boolean generationInfoAvailable) {
                this.jvmInfo = jvmInfo;
                this.identifierSize = identifierSize;
                this.creationDate = creationDate;
                this.numberOfObjects = numberOfObjects;
                this.numberOfGCRoots = numberOfGCRoots;
                this.numberOfClasses = numberOfClasses;
                this.numberOfClassLoaders = numberOfClassLoaders;
                this.usedHeapSize = usedHeapSize;
                this.generationInfoAvailable = generationInfoAvailable;
            }
        }
    }

    interface ClassLoader {

        @Data
        class Item {

            public int objectId;

            public String prefix;

            public String label;

            public boolean classLoader;

            public boolean hasParent;

            public int definedClasses;

            public int numberOfInstances;
        }

        @Data
        class Summary {

            public int totalSize;

            public int definedClasses;

            public int numberOfInstances;
        }
    }

    @Data
    class LeakReport {

        public boolean useful;

        public String info;

        public String name;

        public List<Slice> slices;

        public List<Record> records;

        @Data
        public static class Slice {

            public String label;

            public int objectId;

            public double value;

            public String desc;

            public Slice(String label, int objectId, double value, String desc) {
                this.label = label;
                this.objectId = objectId;
                this.value = value;
                this.desc = desc;
            }
        }

        @Data
        public static class Record {

            public String name;

            public String desc;

            public int index;

            public List<ShortestPath> paths;
        }

        @Data
        public static class ShortestPath {

            public String label;

            public long shallowSize;

            public long retainedSize;

            public int objectId;

            public int objectType;

            public boolean gCRoot;

            public List<ShortestPath> children;
        }
    }

    @Data
    class JavaObject {

        public static final int CLASS_TYPE = 1;

        public static final int CLASS_LOADER_TYPE = 2;

        public static final int ARRAY_TYPE = 3;

        public static final int NORMAL_TYPE = 4;
        // FIXME: can we generate these code automatically?
        public static Map<String, Comparator<JavaObject>> sortTable = new SortTableGenerator<JavaObject>()
            .add("id", JavaObject::getObjectId)
            .add("shallowHeap", JavaObject::getShallowSize)
            .add("retainedHeap", JavaObject::getRetainedSize)
            .add("label", JavaObject::getLabel)
            .build();
        public int objectId;
        public String prefix;
        public String label;
        public String suffix;
        public long shallowSize;
        public long retainedSize;
        public boolean hasInbound;
        public boolean hasOutbound;
        public int objectType;
        public boolean gCRoot;

        public static Comparator<JavaObject> sortBy(String field, boolean ascendingOrder) {
            return ascendingOrder ? sortTable.get(field) : sortTable.get(field).reversed();
        }
    }

    @Data
    class InspectorView {

        public long objectAddress;

        public String name;

        public boolean gCRoot;

        public int objectType;

        public String classLabel;

        public boolean classGCRoot;

        public String superClassName;

        public String classLoaderLabel;

        public boolean classLoaderGCRoot;

        public long shallowSize;

        public long retainedSize;

        public String gcRootInfo;

    }

    class FieldView {

        public int fieldType;

        public String name;

        public String value;

        public int objectId;

        public FieldView(int fieldType, String name, String value) {
            this.fieldType = fieldType;
            this.name = name;
            this.value = value;
        }

        public FieldView(int fieldType, String name, String value, int objectId) {
            this(fieldType, name, value);
            this.objectId = objectId;
        }

        public FieldView() {

        }
    }
}
