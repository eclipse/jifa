# Heap Dump Analysis

## Introduction

Heap dump analysis is a technical approach used to analyze Java heap related issues. A heap dump contains rich runtime
information such as objects, classes, and stack traces.

This feature can help developers quickly identify the root causes of Java heap related issues, such as memory leaks.

## Views

### Overview

This view displays basic information about the heap dump, such as the used size of heap, the number of objects, and the number
of classes.

### Leak Suspects

This view displays the objects that may cause memory leaks.

### Dominator Tree

This view displays the dominance relationships between objects.

### Histogram

This view displays information about the class and its instances, such as the number of objects and the size of heap
occupied.

### Threads

This view displays thread information, such as stack traces.

### Class Loaders

This view displays information about class loaders and the classes they have loaded.

### OQL

This view allows developers to perform object queries using OQL (Object Query Language).

### GC Roots

This view categorizes objects based on their GC Root type and Java type.

### Direct Byte Buffers

This view displays information about DirectByteBuffer, such as its capacity.

### Duplicated Classes

This view displays information about duplicate classes, such as the class loader that caused the class duplication and
the number of objects.

### Unreachable Objects

This view displays information about unreachable objects, such as the Java type, the number of objects.

### System Properties

This view displays the system properties used by the application, which are set either through `-Dkey=value` or by JDK.