# GC Log Analysis

## Introduction

GC (Garbage Collection) log analysis is a technique used to diagnose memory-related issues and analyze GC performance in
Java applications. When applications experience long pause, low GC performance or other GC related problems, GC Log
analysis provides detailed information about GC cause, duration and memory usage change in the heap memory.

By generating GC log files, we can obtain information about the GC execution of an application over a certain period of
time. This includes details such as the time, cause, and memory changes for each GC operation. GC log formats can vary
and contain extensive and specialized information, making it challenging for manual reading and analysis. With the use
of GC log analysis tools, users can quickly identify issues like memory leaks, long GC pauses, premature object
promotion, and other performance-affecting problems without the need to read through lengthy and complex GC logs.

GC log analysis also offers the ability to compare two GC logs or compare one GC log during different time periods. This
functionality can guide us in optimizing GC performance and understanding the impact of changes on GC behavior.

In summary, GC log analysis is a powerful tool that helps developers diagnose and address issues like memory leaks, long
GC pauses, and premature object promotion in Java applications. By conducting in-depth analysis of GC execution, we can
identify underlying problems and take appropriate optimization measures.

## Views

### Basic Information

Show some basic information about this GC log, such as the GC algorithm, the number of GC threads, the time covered by
the log, the currently selected time range, and the duration of the log.

### Diagnosis

Highlight significant issues identified in the GC log on a timeline. The most severe and prioritized problems will be
indicated, along with the time range when these issues occurred (clicking on the time range allows for data analysis).
Common troubleshooting and optimization methods will be provided.

### Time Graph

Visualize the GC log by presenting the memory size of different areas, reclamation, promotion and duration of various GC
events at different time points using line charts or scatter plots.

### Pause Info

Display the number of pauses within different time ranges in a visual manner. Provide statistical metrics related to GC
pauses, such as throughput, average pause time, and maximum pause time.

### Heap and Metaspace

Present average capacity, maximum usage, and post Full GC and Old GC average usage for young generation, old generation,
humongous objects, entire heap, and metaspace.

### Phase and Cause

Split GC events into phases or causes, showcasing the occurrence count, interval, and duration of each phase.

### Object Statistics

Provide statistics on object creation and object promotion.

### JVM Options

Display the current JVM options, categorized based on their relevance to GC.

### Log Details

Display detailed information about each GC occurrence, supporting queries based on GC cause, event types, and minimum
pause time.

