# Heap Dump Analysis

A heap dump is a snapshot of all the objects that are in the memory of the Java application at a certain moment.

Analyzing heap dump can help troubleshoot memory-leak problems and optimize memory usage.

### Supported Format
- OpenJDK HPROF (binary format)

### Feature List
Eclipse Jifa uses [Eclipse MAT](https://www.eclipse.org/mat/) as the underlying API ti implement heap dump analysis.

The supported features are as follows:

- Overview
- Leak Suspects
- GC Roots
- Dominator Tree
- Class Histogram
- Unreachable Objects
- Duplicated Classes
- Class Loaders  
- Direct Byte Buffer
- System Property
- Thread Info
- OQL
- Other features

### Sample
![Jifa Sample](https://raw.githubusercontent.com/wiki/eclipse/jifa/resources/jifa-sample.jpg)
