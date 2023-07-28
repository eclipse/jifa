# GC Log Analysis

A GC log includes details of the entire GC process and the resources it uses. 

Analyzing GC log can help troubleshoot long pause problems and optimize overall Java application performance.

### Supported Format
- OpenJDK 8, 11 and 17. Other versions may be not well supported.
- Serial GC, Parallel GC, CMS GC, G1 GC, ZGC


### Feature List

The supported features are as follows:

- Basic Information
- Diagnose
- Time Graph
- Pause Info
- Heap And Metaspace
- Phase And Cause
- Object Statistics
- JVM Options
- GC Details
- GC Log Compare
- Other features

### Implementation Overview

Then main entrance of the feature is in GCLogAnalyzer.parse. There are 3 main steps :

1. GCLogParser will parse the original GC log file into GCEvents, recording direct information from the file. This is done by first recognizing the format and collector of the log, then parse each line using corresponding parsing rules.

2. GCModel.calculateDerivedInfo will calculate more metrics we can get from original info, such as promotion of single event. All these data are saved in GCModel.

3. Frontend pages displays information through gclog backend apis.

### Sample

![GC Log Analysis Sample](https://raw.githubusercontent.com/wiki/eclipse/jifa/resources/jifa-gc-log-analysis-sample.jpg)
