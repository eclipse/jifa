# JFR 分析

## 介绍

性能剖析是一种分析应用程序性能的方法，可以改善应用性能、降低IT成本、提升用户体验。
Java Flight Recorder（简称JFR）是内建在Java种的性能剖析工具。
开启JFR后，可以从Java进程中持续收集各种维度的性能数据，并生成JFR文件。
通过对JFR文件的分析，可以帮助定位CPU占用、内存申请、网络IO、文件IO、锁争抢等多种维度的性能问题。

## 视图

### CPU热点

此视图以火焰图为主要展现形式，可以帮助快速定位Java占用CPU高的的原因，精确到方法级别，并且可以按线程、类、方法进行过滤。  
主要依赖的JFR事件：
- jdk.ExecutionSample
- jdk.NativeMethodSample

### 内存申请热点

如果Java程序内存吞出较高（比如YoungGC较频繁），很可能是某些代码创建了大量的对象或者数组。
使用此视图可以很方便的找出内存申请最多的方法，即内存申请热点。  
主要依赖的JFR事件：
- jdk.ObjectAllocationInNewTLAB
- jdk.ObjectAllocationOutsideTLAB
- jdk.ObjectAllocationSample (JDK 16及以上, TODO)

### 墙钟热点

此视图和CPU热点有所不同。对于CPU热点，线程只有在CPU上执行时才会被记录到CPU热点中，但线程也有可能被阻塞或者主动等待，对于这种情况，CPU热点视图帮助不大，而墙钟热点视图则正好适合。墙钟热点不区分线程运行状态，无论线程在CPU上执行或者被阻塞，都会进行记录。使用墙钟视图，可以看出线程中每个方法的执行时间的大小，找出占用时间最多的方法。  
注：JDK自带的JFR没有单独的墙钟数据，可以用[async-profiler](https://github.com/async-profiler/async-profiler)的墙钟(wall)模式来生成JFR文件。  
主要依赖的JFR事件：
- jdk.ExecutionSample

### 锁争抢热点

当多个线程去争抢一个锁时，抢不到锁的线程会阻塞等待，可能导致性能问题。此视图可以帮助定位锁争抢热点方法。  
主要依赖的JFR事件：
- jdk.JavaMonitorEnter
- jdk.ThreadPark

### 网络IO热点

此视图可以帮助定位Socket IO读/写热点方法，找出Socket IO次数最多或者时间最长的方法。  
主要依赖的JFR事件：
- jdk.SocketRead
- jdk.SocketWrite

### 文件IO热点

此视图可以帮助定位文件 IO读/写热点方法，找出文件 IO次数最多或者时间最长的方法。  
主要依赖的JFR事件：
- jdk.FileWrite
- jdk.FileRead
- jdk.FileForce

### 类加载热点

此视图可以帮助定位触发类加载的热点方法，找出触发类加载次数最多或者时间最长的方法。  
主要依赖的JFR事件：
- jdk.ClassLoad

### 线程Sleep

此视图可以帮助定位调用Thread.sleep的热点方法。  
主要依赖的JFR事件：
- jdk.ThreadSleep