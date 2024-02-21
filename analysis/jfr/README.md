# JFR Analysis

Profiling is a type of runtime analysis. Java Flight Recorder (JFR) is the builtin Profiler for Java. It collects data on the fly and generates JFR files. JFR analysis gives you a birds-eye view of what is happening inside Java, such as CPU usage, memory allocation, and other activities of threads.

### Supported Format

- OpenJDK JFR (binary format)

### Feature List

The supported features are as follows:

- CPU
- Allocation
- Wall Clock (Only JFR files created by async-profiler with wall engine)
- File IO
- Socket IO
- Lock
- Class Load
- Thread Sleep
- Native Execution Sample
