# GC Log Analysis

## Introduction

GC (Garbage Collection) log analysis is a technical approach used to analyze issues related to garbage collection. When
Java applications experience long pauses and response-time (RT) jitter caused by GC, GC logs can provide detailed
information about the causes, duration, memory changes, and other relevant details.

Different versions of the JDK and various GC algorithms have different log formats, and there is a series of JVM options
controlling the level of detail, making it more challenging to analyze logs directly. Using GC log analysis can help
developers quickly identify issues such as memory leaks, long pauses, premature object promotions, and other
performance-affecting problems. This feature also provides comparative analysis views, which can guide developers
in optimizing GC performance and understanding the impact of code or JVM option changes on GC performance.

## Views

### Basic Information

This view displays some basic information, such as the GC algorithm used by the application, the number of GC threads,
and the time period covered by the log.

### Diagnosis

This view analyzes the serious issues present in the application and marks them on a timeline. It also provides
information about the most critical problems that need to be addressed first, including the exact time they occurred and
general troubleshooting and optimization methods.

### Time Graph

This view presents data such as GC duration and memory changes in various regions through visual representation.

### Pause Info

This view displays metrics related to GC pauses, such as average pause time and maximum pause time.

### Heap and Metaspace

This view presents memory usage of the metaspace and each region of the heap.

### Phase and Cause

This view is split based on GC phases or causes, displaying metrics such as the number of occurrences, intervals, and
durations.

### Object Statistics

This view displays data related to object creation and promotion.

### JVM Options

This view displays the JVM options used by the application.

### Log Details

This view displays the details of each GC and supports querying based on dimensions such as the cause.