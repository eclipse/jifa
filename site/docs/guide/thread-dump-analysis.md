# Thread Dump Analysis

## Introduction

Thread dump analysis is a technical approach used to analyze issues related to threads. A thread dump provides
information about the state and stack trace of all threads in a Java process at a specific moment. This feature can
assist developers in analyzing problems such as thread leaks, deadlocks, high CPU usage, and more.

## Views

### Thread Summary

This view groups all threads by type and displays information about each thread, including its state and stack trace.

### Thread Group Summary

This view groups all threads by thread pool and displays information about each thread, including its state and stack
trace.

### Java Monitors

This view displays information about Java Monitors and allows for a quick analysis of threads that are holding or
waiting for monitors.

### Call Site Tree

This view aggregates stack traces of all threads and can help analyze hot methods.