# JFR Analysis

## Introduction

Performance profiling is a method of analyzing application performance, which can improve application performance, reduce IT costs, and improve user experience.
Java Flight Recorder (JFR for short) is a performance analysis tool built into Java.
After JFR is turned on, performance data of various dimensions can be continuously collected from the Java process and JFR files can be generated.
Through the analysis of JFR files, it can help locate performance problems in multiple dimensions such as CPU usage, memory allocation, Socket IO, File IO, and Lock contention.

## Views

### CPU

This view uses flame graph as the main display form, which can help quickly locate the reason why Java consumes high CPU, accurate to the method level, and can be filtered by Thread, Class, or Method.

### Allocation

If the Java program's memory throughput is high (for example, YoungGC is very frequent), it is likely that some code has created a large number of objects or arrays.
Using this view, you can easily find the method that requests the most memory, that is, the memory request hotspot.

### Wall Clock

This view is different from CPU hotspots. For CPU hotspots, threads will only be recorded when executing on the CPU, but threads may also be blocked or waiting. In this case, the CPU hotspot view is not very helpful, and the wall clock hotspot comes in handy. Wall clock hotspot does not distinguish between thread running status (e.g. executing on the CPU or blocked). Using the wall clock view, you can see the execution time of each method in the thread and find the method that takes up the most time. Note: The JFR that comes with the JDK does not have separate wall clock data. You can use the wall clock mode of [async-profiler](https://github.com/async-profiler/async-profiler) to generate a JFR file.

### Lock

When multiple threads compete for a lock, the thread that cannot grab the lock will be blocked and wait, which may cause performance problems. This view can help locate lock contention hot spots.

### Socket IO

This view can help locate Socket IO read/write hotspot methods and find out the method with the most Socket IO times or the longest time.

### File IO

This view can help find the method with the most file IO times or the longest time.

### Class Load

This view can help find out the method that triggers class loading the most times or takes the longest time.

### Thead Sleep

This view can help locate the method that calls Thread.sleep and sleeps the longest time.